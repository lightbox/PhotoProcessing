/*
 * Copyright (C) 2012 Lightbox
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lightbox.android.photoprocessing;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.lightbox.android.photoprocessing.utils.BitmapUtils;
import com.lightbox.android.photoprocessing.utils.FileUtils;
import com.lightbox.android.photoprocessing.utils.MediaUtils;

public class PhotoProcessingActivity extends Activity {
	private static final String TAG = "PhotoProcessingActivity";
	
	public static final int REQUEST_CODE_SELECT_PHOTO = 1;
	
	private static final String SAVE_STATE_PATH = "com.lightbox.android.photoprocessing.PhotoProcessing.mOriginalPhotoPath";
	private static final String SAVE_CURRENT_FILTER = "com.lightbox.android.photoprocessing.PhotoProcessing.mCurrentFilter";
	private static final String SAVE_EDIT_ACTIONS= "com.lightbox.android.photoprocessing.PhotoProcessing.mEditActions";
	
	private String mOriginalPhotoPath = null;
	private Bitmap mBitmap = null;
	private ImageView mImageView = null;
	private ListView mListView = null;
	
	private int mCurrentFilter = 0;
	private int mCurrentEditAction = 0;
	private ArrayList<Integer> mEditActions = new ArrayList<Integer>();
	
	private static FilterTask sFilterTask;	
	private static EditActionTask sEditActionTask;
	private static SavePhotoTask sSavePhotoTask;
		
	private ProgressDialog mProgressDialog = null;
	
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mImageView = (ImageView)findViewById(R.id.imageViewPhoto);
		
		mListView = (ListView)findViewById(R.id.optionsList);
		mListView.setVisibility(View.GONE);
	}
	
	@Override
	protected void onPause() {
		hideProgressDialog();
		super.onPause();
	}
	
	@Override
	protected void onResume() {
		if (sFilterTask != null) {
			sFilterTask.reattachActivity(this);
		}
		if (sEditActionTask != null) {
			sEditActionTask.reattachActivity(this);
		}
		if (sSavePhotoTask != null) {
			sSavePhotoTask.reattachActivity(this);
		}
		super.onResume();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(SAVE_STATE_PATH, mOriginalPhotoPath);
		outState.putInt(SAVE_CURRENT_FILTER, mCurrentFilter);
		outState.putIntegerArrayList(SAVE_EDIT_ACTIONS, mEditActions);
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		
		mOriginalPhotoPath = savedInstanceState.getString(SAVE_STATE_PATH);
		mCurrentFilter = savedInstanceState.getInt(SAVE_CURRENT_FILTER);
		mEditActions = savedInstanceState.getIntegerArrayList(SAVE_EDIT_ACTIONS);
		if (mEditActions == null) {
			mEditActions = new ArrayList<Integer>();
		}
		if (mOriginalPhotoPath != null) {
			loadFromCache();
			mImageView.setImageBitmap(mBitmap);
		}
	}
	
	@Override
	public void onBackPressed() {
		if (mListView.getVisibility() == View.VISIBLE) {
			mListView.setVisibility(View.GONE);
		} else {
			super.onBackPressed();
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if (requestCode == REQUEST_CODE_SELECT_PHOTO && resultCode == RESULT_OK) {
			if (mEditActions != null) {
				mEditActions.clear();
			}
			Uri photoUri = data.getData();
			mImageView.setImageBitmap(null);
			mOriginalPhotoPath = MediaUtils.getPath(this, photoUri);
			loadOriginalPhoto();
			mImageView.setImageBitmap(mBitmap);
			saveToCache(mBitmap);
		}
	}
	
	public void onLoadButtonClick(View v) {
		Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		startActivityForResult(i, REQUEST_CODE_SELECT_PHOTO);
	}
	
	public void onFilterButtonClick(View v) {
		mListView.setAdapter(new FilterListAdapter(this));
		mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
				sFilterTask = new FilterTask(PhotoProcessingActivity.this);
				mCurrentFilter = position;
				sFilterTask.execute(position);
			}
		});
		mListView.setVisibility(View.VISIBLE);
	}
	
	public void onEditButtonClick(View v) {
		mListView.setAdapter(new EditListAdapter(this));
		mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
				sEditActionTask = new EditActionTask(PhotoProcessingActivity.this);
				mCurrentEditAction = position;
				mEditActions.add(position);
				sEditActionTask.execute(position);
			}
		});
		mListView.setVisibility(View.VISIBLE);
	}
	
	public void onSaveButtonClick(View v) {
		sSavePhotoTask = new SavePhotoTask(this);
		sSavePhotoTask.execute();		
	}
	
	private void saveToCache(Bitmap bitmap) {
		if (bitmap == null || bitmap.isRecycled()) {
			return;
		}
		
		File cacheFile = new File(getCacheDir(), "cached.jpg");
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(cacheFile);
		} catch (FileNotFoundException e) {
			// do nothing
		} finally {
			if (fos != null) {
				bitmap.compress(CompressFormat.JPEG, 100, fos);
				try {
					fos.flush();
					fos.close();
				} catch (IOException e) {
					// Do nothing
				}
			}
		}
	}
	
	private void savePhoto(Bitmap bitmap) {
		File file = new File(mOriginalPhotoPath);
		File saveDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/Lightbox/");
		saveDir.mkdir();
		File saveFile = new File(saveDir, file.getName());
		
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(saveFile);
			bitmap.compress(CompressFormat.JPEG, 95, fos);
		} catch (FileNotFoundException e) {
			Log.w(TAG, e);
		} finally {
			if (fos != null) {
				try {
					fos.flush();
					fos.close();
				} catch (IOException e) {
					// Do nothing
				}
			}
		}
	}
	
	private void loadOriginalPhoto() {
		DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
		
		if (mBitmap != null) {
			mBitmap.recycle();
		}
		
		mBitmap = BitmapUtils.getSampledBitmap(mOriginalPhotoPath, displayMetrics.widthPixels, displayMetrics.heightPixels);
		
		if (mBitmap != null && !mBitmap.isMutable()) {
			mBitmap = PhotoProcessing.makeBitmapMutable(mBitmap);
		}
	}
	
	private void showTempPhotoInImageView() {
		if (mBitmap != null) {
			Bitmap bitmap = Bitmap.createScaledBitmap(mBitmap, mBitmap.getWidth()/4, mBitmap.getHeight()/4, true);
			mImageView.setImageBitmap(bitmap);
		}
	}
	
	private void loadFromCache() {
		DisplayMetrics displayMetrics = getResources().getDisplayMetrics();

		if (mBitmap != null) {
			mBitmap.recycle();
		}
		
		File cacheFile = new File(getCacheDir(), "cached.jpg");
		mBitmap = BitmapUtils.getSampledBitmap(cacheFile.getAbsolutePath(), displayMetrics.widthPixels, displayMetrics.heightPixels);
	}
	
	private void showFilterProgressDialog() {
		String message = (mCurrentFilter == 0 ? getString(R.string.reverting_to_original) : getString(R.string.applying_filter, getString(PhotoProcessing.FILTERS[mCurrentFilter])));
		mProgressDialog = ProgressDialog.show(this, "", message);
	}
	
	private void showEditActionProgressDialog() {
		String message = "";
		switch (mCurrentEditAction) {
		case 0: // Flip
			message = getString(R.string.flipping);
			break;
		case 1: // Rotate 90 degrees right
			message = getString(R.string.rotating_90_right);
			break;
		case 2: // Rotate 90 degrees left
			message = getString(R.string.rotating_90_left);
			break;
		case 3: // Rotate 180 degrees
			message = getString(R.string.rotating_180);
			break;
		}
		mProgressDialog = ProgressDialog.show(this, "", message);
	}
	
	private void showSavingProgressDialog() {
		String message = "Saving...";
		mProgressDialog = ProgressDialog.show(this, "", message);
	}
	
	private void hideProgressDialog() {
		if (mProgressDialog != null && mProgressDialog.isShowing()) {
			mProgressDialog.dismiss();
		}
	}
	
	private static class FilterListAdapter extends BaseAdapter {
		private LayoutInflater mInflator;
		private Context mContext;
		
		public FilterListAdapter(Context context) {
			mContext = context;
			mInflator = LayoutInflater.from(context);
		}
		
		@Override
		public int getCount() {
			return PhotoProcessing.FILTERS.length;
		}

		@Override
		public Object getItem(int position) {
			return mContext.getString(PhotoProcessing.FILTERS[position]);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			if (view == null) {
				view = mInflator.inflate(R.layout.filter_list_item, null);
			}
			
			((TextView)view.findViewById(R.id.filterNameTextView)).setText((CharSequence)getItem(position));
			
			return view;
		}
	}
	
	private static class EditListAdapter extends BaseAdapter {
		private LayoutInflater mInflator;
		private Context mContext;
		
		public EditListAdapter(Context context) {
			mContext = context;
			mInflator = LayoutInflater.from(context);
		}

		@Override
		public int getCount() {
			return PhotoProcessing.EDIT_ACTIONS.length;
		}

		@Override
		public Object getItem(int position) {
			return mContext.getString(PhotoProcessing.EDIT_ACTIONS[position]);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			if (view == null) {
				view = mInflator.inflate(R.layout.edit_list_item, null);
			}
			
			((TextView)view.findViewById(R.id.filterNameTextView)).setText((CharSequence)getItem(position));
			
			return view;
		}
	}
	
	private static class FilterTask extends AsyncTask<Integer, Void, Bitmap> {
		WeakReference<PhotoProcessingActivity> mActivityRef;
		
		public FilterTask(PhotoProcessingActivity activity) {
			mActivityRef = new WeakReference<PhotoProcessingActivity>(activity);
		}
		
		public void reattachActivity(PhotoProcessingActivity activity) {
			mActivityRef = new WeakReference<PhotoProcessingActivity>(activity);
			if (getStatus().equals(Status.RUNNING)) {
				activity.showFilterProgressDialog();
			}
		}
		
		private PhotoProcessingActivity getActivity() {
			if (mActivityRef == null) {
				return null;
			}
			
			return mActivityRef.get();
		}
		
		@Override
		protected void onPreExecute() {
			PhotoProcessingActivity activity = getActivity();
			if (activity != null) {
				activity.showFilterProgressDialog();
				activity.showTempPhotoInImageView();
			}
		}
		
		@Override
		protected Bitmap doInBackground(Integer... params) {		
			PhotoProcessingActivity activity = getActivity();
			
			if (activity != null) {
				activity.loadOriginalPhoto();
				int position = params[0];
				Bitmap bitmap = PhotoProcessing.filterPhoto(activity.mBitmap, position);
				for (Integer editAction : activity.mEditActions) {
					bitmap = PhotoProcessing.applyEditAction(bitmap, editAction);
				}
				activity.saveToCache(bitmap);
				
				return bitmap;
			}
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Bitmap result) {	
			PhotoProcessingActivity activity = getActivity();
			if (activity != null) {
				activity.mBitmap = result;
				activity.mImageView.setImageBitmap(result);
				activity.hideProgressDialog();
			}
		}
	}
	
	private static class EditActionTask extends AsyncTask<Integer, Void, Bitmap> {
		WeakReference<PhotoProcessingActivity> mActivityRef;
		
		public EditActionTask(PhotoProcessingActivity activity) {
			mActivityRef = new WeakReference<PhotoProcessingActivity>(activity);
		}
		
		public void reattachActivity(PhotoProcessingActivity activity) {
			mActivityRef = new WeakReference<PhotoProcessingActivity>(activity);
			if (getStatus().equals(Status.RUNNING)) {
				activity.showFilterProgressDialog();
			}
		}
		
		private PhotoProcessingActivity getActivity() {
			if (mActivityRef == null) {
				return null;
			}
			
			return mActivityRef.get();
		}
		
		@Override
		protected void onPreExecute() {
			PhotoProcessingActivity activity = getActivity();
			if (activity != null) {
				activity.showEditActionProgressDialog();
				activity.showTempPhotoInImageView();
			}
		}
		
		@Override
		protected Bitmap doInBackground(Integer... params) {		
			PhotoProcessingActivity activity = getActivity();
			
			if (activity != null) {
				int position = params[0];
				Bitmap bitmap = PhotoProcessing.applyEditAction(activity.mBitmap, position);
				activity.saveToCache(bitmap);
				
				return bitmap;
			}
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Bitmap result) {	
			PhotoProcessingActivity activity = getActivity();
			if (activity != null) {
				activity.mBitmap = result;
				activity.mImageView.setImageBitmap(result);
				activity.hideProgressDialog();
			}
		}
	}
	
	private static class SavePhotoTask extends AsyncTask<Void, Void, Void> {
		WeakReference<PhotoProcessingActivity> mActivityRef;
		
		public SavePhotoTask(PhotoProcessingActivity activity) {
			mActivityRef = new WeakReference<PhotoProcessingActivity>(activity);
		}
		
		public void reattachActivity(PhotoProcessingActivity activity) {
			mActivityRef = new WeakReference<PhotoProcessingActivity>(activity);
			if (getStatus().equals(Status.RUNNING)) {
				activity.showSavingProgressDialog();
			}
		}
		
		private PhotoProcessingActivity getActivity() {
			if (mActivityRef == null) {
				return null;
			}
			
			return mActivityRef.get();
		}
		
		@Override
		protected void onPreExecute() {
			PhotoProcessingActivity activity = getActivity();
			if (activity != null) {
				activity.showSavingProgressDialog();
			}
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			PhotoProcessingActivity activity = getActivity();
			if (activity != null) {
				File jpegFile = new File(activity.mOriginalPhotoPath);
				try {
					byte[] jpegData = FileUtils.readFileToByteArray(jpegFile);
					PhotoProcessing.nativeLoadResizedJpegBitmap(jpegData, jpegData.length, 1024 * 1024 * 2);
					Bitmap bitmap = PhotoProcessing.filterPhoto(null, activity.mCurrentFilter);
					for (Integer editAction : activity.mEditActions) {
						bitmap = PhotoProcessing.applyEditAction(bitmap, editAction);
					}
					activity.savePhoto(bitmap);
				} catch (IOException e) {
					Log.w(TAG, e);
				} 
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			PhotoProcessingActivity activity = getActivity();
			if (activity != null) {
				activity.hideProgressDialog();
			}
		}
	}
}
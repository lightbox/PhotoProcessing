/**
 * Copyright (c) 2012 Lightbox
 */
package com.lightbox.android.photoprocessing.utils;

import java.io.IOException;

import android.app.Activity;
import android.database.Cursor;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;

/** 
 * MediaUtils 
 * @author nilesh
 */
public class MediaUtils {
	/** Used to tag logs */
	@SuppressWarnings("unused")
	private static final String TAG = "MediaUtils";
	
	public static String getPath(Activity activity, Uri uri) {
	    String[] projection = { MediaStore.Images.Media.DATA };
	    Cursor cursor = activity.managedQuery(uri, projection, null, null, null);
	    activity.startManagingCursor(cursor);
	    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
	    cursor.moveToFirst();
	    return cursor.getString(column_index);
	}
	
	public static int getExifOrientation(String filepath) {
		int degree = 0;
		ExifInterface exif = null;
		try {
			exif = new ExifInterface(filepath);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		if (exif != null) {
			int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
			if (orientation != -1) {
				// We only recognise a subset of orientation tag values.
				switch (orientation) {
				case ExifInterface.ORIENTATION_ROTATE_90:
					degree = 90;
					break;
				case ExifInterface.ORIENTATION_ROTATE_180:
					degree = 180;
					break;
				case ExifInterface.ORIENTATION_ROTATE_270:
					degree = 270;
					break;
				}

			}
		}
		return degree;
	}
}

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

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;

/** 
 * PhotoProcessing 
 * @author Nilesh Patel
 */
public class PhotoProcessing {
	/** Used to tag logs */
	@SuppressWarnings("unused")
	private static final String TAG = "PhotoProcessing";
	
	public static final int[] FILTERS = {R.string.filter_original,
													R.string.filter_instafix,
													R.string.filter_ansel,
													R.string.filter_testino,
													R.string.filter_xpro,
													R.string.filter_retro,
													R.string.filter_bw,
													R.string.filter_sepia,
													R.string.filter_cyano,
													R.string.filter_georgia,
													R.string.filter_sahara,
													R.string.filter_hdr};
	
	public static final int[] EDIT_ACTIONS = {R.string.edit_action_flip, R.string.edit_action_rotate_90_right, R.string.edit_action_rotate_90_left, R.string.edit_action_rotate_180};
	
	public static Bitmap filterPhoto(Bitmap bitmap, int position) {
		if (bitmap != null) { //USe current bitmap in native memory
			sendBitmapToNative(bitmap);
		}
		switch (position) {
		case 0: // Original
			break;
		case 1: // Instafix
			nativeApplyInstafix();
			break;
		case 2: // Ansel
			nativeApplyAnsel();
			break;
		case 3: // Testino
			nativeApplyTestino();
			break;
		case 4: // XPro
			nativeApplyXPro();
			break;
		case 5: // Retro
			nativeApplyRetro();
			break;
		case 6: // Black & White
			nativeApplyBW();
			break;
		case 7: // Sepia
			nativeApplySepia();
			break;
		case 8: // Cyano
			nativeApplyCyano();
			break;
		case 9: // Georgia
			nativeApplyGeorgia();
			break;
		case 10: // Sahara
			nativeApplySahara();
			break;
		case 11: // HDR
			nativeApplyHDR();
			break;
		}
		Bitmap filteredBitmap = getBitmapFromNative(bitmap);
		nativeDeleteBitmap();
		return filteredBitmap;
	}
	
	public static Bitmap applyEditAction(Bitmap bitmap, int position) {
		switch (position) {
		case 0: // Flip
			bitmap = flipHorizontally(bitmap);
			break;
		case 1: // Rotate 90 right
			bitmap = rotate(bitmap, 90);
			break;
		case 2: // Rotate 90 left
			bitmap = rotate(bitmap, 270);
			break;
		case 3: // Rotate 180
			bitmap = rotate(bitmap, 180);
			break;
		}
		
		return bitmap;
	}

	
	///////////////////////////////////////////////
	
	static {
		System.loadLibrary("photoprocessing");
	}
	
	public static native int nativeInitBitmap(int width, int height);
	public static native void nativeGetBitmapRow(int y, int[] pixels);
	public static native void nativeSetBitmapRow(int y, int[] pixels);
	public static native int nativeGetBitmapWidth();
	public static native int nativeGetBitmapHeight();
	public static native void nativeDeleteBitmap();
	public static native int nativeRotate90();
	public static native void nativeRotate180();
	public static native void nativeFlipHorizontally();

	public static native void nativeApplyInstafix();
	public static native void nativeApplyAnsel();
	public static native void nativeApplyTestino();
	public static native void nativeApplyXPro();
	public static native void nativeApplyRetro();
	public static native void nativeApplyBW();
	public static native void nativeApplySepia();
	public static native void nativeApplyCyano();
	public static native void nativeApplyGeorgia();
	public static native void nativeApplySahara();
	public static native void nativeApplyHDR();
	
	public static native void nativeLoadResizedJpegBitmap(byte[] jpegData, int size, int maxPixels);
	public static native void nativeResizeBitmap(int newWidth, int newHeight);
	
	private static void sendBitmapToNative(Bitmap bitmap) {
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		nativeInitBitmap(width, height);
		int[] pixels = new int[width];
		for (int y = 0; y < height; y++) {
			bitmap.getPixels(pixels, 0, width, 0, y, width, 1);
			nativeSetBitmapRow(y, pixels);
		}
	}
	
	private static Bitmap getBitmapFromNative(Bitmap bitmap) {
		int width = nativeGetBitmapWidth();
		int height = nativeGetBitmapHeight();
		
		if (bitmap == null || width != bitmap.getWidth() || height != bitmap.getHeight() || !bitmap.isMutable()) { //in case it was rotated and the dimensions changed
			Config config = Config.ARGB_8888;
			if (bitmap != null) {
				config = bitmap.getConfig();
				bitmap.recycle();
			}
			bitmap = Bitmap.createBitmap(width, height, config);
		}

		int[] pixels = new int[width];
		for (int y = 0; y < height; y++) {
			nativeGetBitmapRow(y, pixels);
			bitmap.setPixels(pixels, 0, width, 0, y, width, 1);
		}
				
		return bitmap;
	}
	
	public static Bitmap makeBitmapMutable(Bitmap bitmap) {
		sendBitmapToNative(bitmap);
		return getBitmapFromNative(bitmap);
	}
	
	public static Bitmap rotate(Bitmap bitmap, int angle) {
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		Config config = bitmap.getConfig();
		nativeInitBitmap(width, height);
		sendBitmapToNative(bitmap);
		
		if (angle == 90) {
			nativeRotate90();
			bitmap.recycle();
			bitmap = Bitmap.createBitmap(height, width, config);
			bitmap = getBitmapFromNative(bitmap);
			nativeDeleteBitmap();
		} else if (angle == 180) {
			nativeRotate180();
			bitmap.recycle();
			bitmap = Bitmap.createBitmap(width, height, config);
			bitmap = getBitmapFromNative(bitmap);
			nativeDeleteBitmap();
		} else if (angle == 270) {
			nativeRotate180();
			nativeRotate90();
			bitmap.recycle();
			bitmap = Bitmap.createBitmap(height, width, config);
			bitmap = getBitmapFromNative(bitmap);
			nativeDeleteBitmap();
		}
		
		return bitmap;
	}
	
	public static Bitmap flipHorizontally(Bitmap bitmap) {
		nativeInitBitmap(bitmap.getWidth(), bitmap.getHeight());
		sendBitmapToNative(bitmap);
		nativeFlipHorizontally();
		bitmap = getBitmapFromNative(bitmap);
		nativeDeleteBitmap();
		return bitmap;
	}
}

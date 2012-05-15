/**
 * Copyright (c) 2012 Lightbox
 */
package com.lightbox.android.photoprocessing.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.util.FloatMath;

/** 
 * BitmapUtils 
 * @author nilesh
 */
public class BitmapUtils {
	/** Used to tag logs */
	@SuppressWarnings("unused")
	private static final String TAG = "BitmapUtils";
	
	public static Bitmap getSampledBitmap(String filePath, int reqWidth, int reqHeight) {		
		Options options = new Options();
		options.inJustDecodeBounds = true;
		
		BitmapFactory.decodeFile(filePath, options);
		
		// Raw height and width of image
	    final int height = options.outHeight;
	    final int width = options.outWidth;
	    int inSampleSize = 1;
	
	    if (height > reqHeight || width > reqWidth) {
	        if (width > height) {
	            inSampleSize = (int)FloatMath.floor(((float)height / reqHeight)+0.5f); //Math.round((float)height / (float)reqHeight);
	        } else {
	            inSampleSize = (int)FloatMath.floor(((float)width / reqWidth)+0.5f); //Math.round((float)width / (float)reqWidth);
	        }
	    }
	    
	    options.inSampleSize = inSampleSize;
	    options.inJustDecodeBounds = false;
	    	    
	    return BitmapFactory.decodeFile(filePath, options);
	}
}

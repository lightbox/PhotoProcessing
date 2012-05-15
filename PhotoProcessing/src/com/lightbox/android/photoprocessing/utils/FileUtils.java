/**
 * Copyright (c) 2012 Lightbox
 */
package com.lightbox.android.photoprocessing.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/** 
 * FileUtils 
 * @author Nilesh Patel
 */
public class FileUtils {
	/** Used to tag logs */
	@SuppressWarnings("unused")
	private static final String TAG = "FileUtils";
	
	public static byte[] readFileToByteArray(File file) throws IOException {
		InputStream inputStream = null;
		try {
			inputStream = new FileInputStream(file);
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024 * 4];
			int n = 0;
			while (-1 != (n = inputStream.read(buffer))) {
				output.write(buffer, 0, n);
			}
			return output.toByteArray();
		} finally {
			try {
				if (inputStream != null) {
					inputStream.close();
				}
			} catch (IOException e) {
				// Do nothing
			}
		}
	}
}

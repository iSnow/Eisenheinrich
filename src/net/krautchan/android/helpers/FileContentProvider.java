package net.krautchan.android.helpers;

/*
 * Copyright (C) 2011 Johannes Jander (johannes@jandermail.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Arrays;

import net.krautchan.android.Defaults;
import android.annotation.SuppressLint;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

public class FileContentProvider extends ContentProvider {
	CustomExceptionHandler exceptionHandler = new CustomExceptionHandler("eisenheinrich", "http://eisenheinrich.datensalat.net:8080/Eisenweb/upload/logfile/test", null);
	public static final String TAG = "FileContentProvider";
	public static final String URI_PREFIX = "content://net.krautchan.filecontentprovider";
	public static String[] SUFFIXES = { "gif", "jpg", "jpeg", "png", "psd",
	"tiff" };
	public static String[] MIME_TYPES = { "image/gif", "image/jpg",
		"image/jpg", "image/png", "image/vnd.adobe.photoshop", "image/tiff" };


	public static String constructUri(String url) {
		Uri uri = Uri.parse(url);
		return uri.isAbsolute() ? url : URI_PREFIX + url;
	} 

	@Override
	public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
		ParcelFileDescriptor parcel = null;
		String filePath = Defaults.IMAGE_TEMP_DIR+"/"+uri.getPath().replace("/", "");
		if (null != filePath) {
			File result = FileHelpers.getSDFile (filePath);
			if (null != result) {
				parcel = ParcelFileDescriptor.open(result, ParcelFileDescriptor.MODE_READ_ONLY);
			} else {
				throw new FileNotFoundException (uri.toString());
			}
		} 
		return parcel;
	}

	@Override
	public boolean onCreate() {
		return true;
	}

	@Override
	public int delete(Uri uri, String s, String[] as) {
		throw new UnsupportedOperationException(
		"Not supported by this provider");
	}

	@Override
	public String getType(Uri uri) {
		int delim = uri.getLastPathSegment().lastIndexOf('.');
		String suffix = uri.getLastPathSegment().substring(delim + 1)
		.toLowerCase();
		for (int i = 0; i < SUFFIXES.length; i++) {
			if (suffix.equals(SUFFIXES[i])) {
				return MIME_TYPES[i];
			}
		}
		return null;
	}
	

	@SuppressLint("Override")
	public String[] getStreamTypes (Uri uri, String mimeTypeFilter) {
		String[] types = new String[1];
		int delim = uri.getLastPathSegment().lastIndexOf('.');
		String suffix = uri.getLastPathSegment().substring(delim + 1).toLowerCase();
		for (int i = 0; i < SUFFIXES.length; i++) {
			if (suffix.equals(SUFFIXES[i])) {
				types[0] =  MIME_TYPES[i];
			}
		}
		return types;
	}

	@Override
	public Uri insert(Uri uri, ContentValues contentvalues) {
		UnsupportedOperationException e = new  UnsupportedOperationException(
				"Not supported by this provider: "+uri.toString());

		exceptionHandler.uncaughtException(null, e);
		throw e;
	}

	/*@Override
	public Cursor query(Uri uri, String[] as, String s, String[] as1, String s1) {
		UnsupportedOperationException e = new  UnsupportedOperationException(
				"Not supported by this provider: "+uri.toString()+" "+Arrays.toString(as)+" "+s+" "+Arrays.toString(as1)+" "+s1);

		exceptionHandler.uncaughtException(null, e);
		throw e;
	}*/

	@Override
	public Cursor query(Uri uri, String[] as, String s, String[] as1, String s1) {
		File file = new File(uri.getPath());
		MatrixCursor cursor = new MatrixCursor(new String[] { "_data", "mime_type" });

		byte[] data = null;

		String filePath = Defaults.IMAGE_TEMP_DIR+"/"+uri.getPath().replace("/", "");
		File result = FileHelpers.getSDFile (filePath);
		if (result.exists()) {
			try {
				if (null != result) {
					long size = result.length();
						// limit the max file size somewhat arbitrarily here to 10Mb
					if (size < 10 * 1000 * 1000) {
						data = new byte[(int)size];
						InputStream is = new BufferedInputStream (new FileInputStream (result));
						is.read(data);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				exceptionHandler.uncaughtException(null, e);
			} 
		}
		
		if (null != data) {
			Log.i("FL", "read " + data.length + " from " + file);
			cursor.addRow(new Object[] { data, getType(uri) });
		}

		return cursor;
	}


	@Override
	public int update(Uri uri, ContentValues contentvalues, String s, String[] as) {
		UnsupportedOperationException e = new  UnsupportedOperationException(
				"Not supported by this provider: "+uri.toString()+" "+Arrays.toString(as)+" "+s);

		exceptionHandler.uncaughtException(null, e);
		throw e;
	}

}

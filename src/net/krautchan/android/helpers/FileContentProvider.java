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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import net.krautchan.android.Defaults;
import net.krautchan.android.Eisenheinrich;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

public class FileContentProvider extends ContentProvider {

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
		String filePath = null;
		File result = null;
		try {
			final String url = Defaults.FILE_PATH + uri.getPath();
			HttpClient client = Eisenheinrich.getInstance().getHttpClient();
			HttpGet request = new HttpGet(url); 
			HttpResponse response = client.execute(request);
			Log.v(TAG, "GET response: " + response.getStatusLine());
			HttpEntity responseEntity = response.getEntity();
			InputStream st = responseEntity.getContent();
			filePath = Defaults.IMAGE_DIR+"/"+uri.getPath().replace("/", "");
			FileHelpers.writeToSDFile(filePath, st) ;
			st.close(); 
		} catch (Exception e) {
			Log.v(TAG, "EXCEPTION: " + e.getMessage());
		}
		if (null != filePath) {
			result = FileHelpers.getSDFile (filePath);
			if (null != result) {
				ParcelFileDescriptor parcel = ParcelFileDescriptor.open(result, ParcelFileDescriptor.MODE_READ_ONLY);
				return parcel;
			}
		}
		return null;
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

	@Override
	public Uri insert(Uri uri, ContentValues contentvalues) {
		throw new UnsupportedOperationException(
				"Not supported by this provider");
	}

	@Override
	public Cursor query(Uri uri, String[] as, String s, String[] as1, String s1) {
		throw new UnsupportedOperationException(
				"Not supported by this provider");
	}

	@Override
	public int update(Uri uri, ContentValues contentvalues, String s,
			String[] as) {
		throw new UnsupportedOperationException(
				"Not supported by this provider");
	}

}

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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;

import net.krautchan.android.Eisenheinrich;

import android.os.Environment;

//FIXME this is a copy and paste mess, DRY up!
public class FileHelpers {

	public static boolean writeToSDFile(String fileName, String payLoad) {
		FileOutputStream fos = null;
		ObjectOutputStream out = null;
		File baseDir = getOrCreateSDDirectory ();
		if (null == baseDir)
			return false;
		try {
			if (baseDir.canWrite()) {
				File f = new File (baseDir.getAbsolutePath() + "/" + fileName);
				if (!f.exists()) {
					f.createNewFile();
				}
				fos = new FileOutputStream(f);
				out = new ObjectOutputStream(fos);
				out.writeObject(payLoad);
				out.close();
				fos.close();
				return true;
			} 
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return false;
	}
	
	//TODO make this use the java nio methods for writing 
	public static boolean writeToSDFile(String fileName, InputStream input) {
		FileOutputStream fos = null;
		File baseDir = getOrCreateSDDirectory ();
		if (null == baseDir)
			return false;
		try {
			if (baseDir.canWrite()) {
				File target = new File (baseDir.getAbsolutePath() + "/" + fileName);
				if (!target.exists()) {
					String path = target.getAbsolutePath();
					int delim = path.lastIndexOf("/");
					File dir = new File (path.substring(0, delim));
					dir.mkdirs();
				}
				fos = new FileOutputStream(target);
				int c = input.read();
				while (c != -1) {
					fos.write((char)c);
					c = input.read();
				}
				fos.close();
				return true;
			} 
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return false;
	}
	
	public static File getSDFile (String fileName) {
		File baseDir = getOrCreateSDDirectory ();
		if (null == baseDir)
			return null;
		try {
			File result = new File (baseDir, fileName);
			return result;
		} catch (Exception ex) {
			return null;
		}
	}
	
	private static File getOrCreateSDDirectory () {
		String state = Environment.getExternalStorageState();
	    boolean mExternalStorageAvailable = false;
		boolean mExternalStorageWriteable = false;
		if (Environment.MEDIA_MOUNTED.equals(state)) {
	        mExternalStorageAvailable  = mExternalStorageWriteable  = true;
	    } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
	        mExternalStorageAvailable = true;
	        mExternalStorageWriteable = false;
	    } else {
	        mExternalStorageAvailable = mExternalStorageWriteable = false;
	    }
		if ((mExternalStorageAvailable) && (mExternalStorageWriteable)) {	
			File dir = new File (Environment.getExternalStorageDirectory(), Eisenheinrich.DEFAULTS.SD_DIR);
			if (!dir.exists()) {
				dir.mkdirs();
			}
			return dir;
		}
		return null;
	}
	
	public static boolean createSDDirectory (String dirName) {
		File parent = getOrCreateSDDirectory ();
		if (null == parent) {
			return false;
		}
		File dir = new File (parent, dirName);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		return dir.exists();
	}
}

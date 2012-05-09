package net.krautchan.android.activity;

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

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.ipaulpro.afilechooser.FileChooserActivity;
import com.ipaulpro.afilechooser.utils.FileUtils;

public class KCFileChooserActivity extends FileChooserActivity {
	    private static final String TAG = "FileSelectorTestActivity";

	    @Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        // We must check to ensure that the calling Intent is not Intent.ACTION_GET_INTENT
	        if (Intent.ACTION_MAIN.equals(getIntent().getAction())) {
	            // Display the file chooser with all file types
	            showFileChooser("Title", "*/*");
	        }
	    }

	    @Override
	    protected void onFileSelect(File file) {
	        if (file != null) {
	            final Context context = getApplicationContext();

	            // Get the path of the Selected File.
	            final String path = file.getAbsolutePath();
	            Log.d(TAG, "File path: " + path);

	            // Get the MIME type of the Selected File.          
	            String mimeType = FileUtils.getMimeType(context, file);
	            Log.d(TAG, "File MIME type: " + mimeType);

	            // Get the Uri of the Selected File
	            final Uri uri = Uri.fromFile(file);

	            // Get the thumbnail of the Selected File, if image/video
	            // final Bitmap bm = FileUtils.getThumbnail(context, uri, mimeType);

	            // Here you can return any data from above to the calling Activity  
	            Bundle bundle = new Bundle();

	            bundle.putString("filename", uri.toString());
	           	Intent mIntent = new Intent();
	            mIntent.putExtras(bundle);
	            if (getParent() == null) {
	                  setResult(KCFileChooserActivity.RESULT_OK, mIntent);
	            } else {
	                getParent().setResult(KCFileChooserActivity.RESULT_OK, mIntent);
	            }

	            finish();
	        }   
	    }
	    
	    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		    if (resultCode == RESULT_OK)
		        if (requestCode == REQUEST_CODE) {
		            if (data.hasExtra("tmp")) {
		                String tmp = data.getStringExtra("tmp");
		                Toast.makeText(this, "filename: " + tmp, Toast.LENGTH_LONG).show();
		                Log.i("Filename:", tmp);
		            }
		        }
		}

	    @Override
	    protected void onFileError(Exception e) {
	        Log.e(TAG, "File select error", e);
	        finish();
	    }

	    @Override
	    protected void onFileSelectCancel() {
	        Log.d(TAG, "File selections canceled");
	        finish();
	    }

	    @Override
	    protected void onFileDisconnect() {
	        Log.d(TAG, "External storage disconneted");
	        finish();
	    }
	}

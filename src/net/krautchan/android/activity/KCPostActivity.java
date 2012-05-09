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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import net.krautchan.R;
import net.krautchan.android.Eisenheinrich;
import net.krautchan.android.helpers.ActivityHelpers;
import net.krautchan.android.network.AsyncPoster;
import net.krautchan.android.network.AsyncPoster.AsyncPosterPeer;
import net.krautchan.android.network.PostVariables;
import net.krautchan.data.KCBoard;
import net.krautchan.data.KCThread;
import net.krautchan.data.PostActivityParams;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.Toast;
import android.widget.TableLayout.LayoutParams;

public class KCPostActivity extends Activity {
	static final String TAG = "KCPostActivity";
	private static final int MAX_IMAGES = 4;//max 4 images per posting
	private static final int NUM_COLS = 2;//2 rows of image buttons
	private String curBoardName;
	private Long curThreadKCNum;
	//private byte[][] files = new byte[MAX_IMAGES][]; 
	//private String[] fileNames = new String[MAX_IMAGES]; 
	private List<ImageButton> buttons = new ArrayList<ImageButton>();
	private int imageClicked = -1; //FIXME this is an ugly way to track which image button was clicked. See if we can set some userdata on the intent or get the control in onActivityResult()
	private int imageRows = 1;
	
	@Override
	public void onCreate(Bundle savedInstanceState) { 
		super.onCreate(savedInstanceState);        
		calculateImageRows();
		setContentView(R.layout.post_view);
		Bundle b = getIntent().getExtras();
	    byte[] arguments = b.getByteArray("threadparams");
	    ByteArrayInputStream bitch = new ByteArrayInputStream(arguments);
	    ObjectInputStream in;
	    PostActivityParams params = null;
	    try {
			in = new ObjectInputStream(bitch);
		    params = (PostActivityParams)in.readObject();
		    curThreadKCNum = params.curThreadKCNum;
		    curBoardName = params.curBoardName;
		    Button okButton = (Button)findViewById(R.id.ok_button);
		    okButton.setOnClickListener(new View.OnClickListener() {
			    public void onClick(View v) {
			    	HttpClient httpclient = Eisenheinrich.getInstance().getHttpClient();
			    	PostVariables vars = new PostVariables();
			    	vars.boardName = curBoardName;
			    	vars.threadNumber = curThreadKCNum.toString();
			    	List<AsyncPosterPeer> peers = new ArrayList<AsyncPosterPeer>();
			    	peers.add(Eisenheinrich.posterPeer);
			    	peers.add(new AsyncPosterPeer() {
						@Override
						public void storePostVariables(PostVariables vars) {
						}

						@Override
						public PostVariables getPostVariables() {
							return null;
						}

						@Override
						public void notifyDone(boolean successful) {
							if (successful) {
								KCPostActivity.this.finish();
							} else {
								Toast.makeText(KCPostActivity.this, "Pfostierung failed: ", Toast.LENGTH_LONG).show();
								
							}
						}
			    		
			    	});
			    	AsyncPoster poster = new AsyncPoster(vars, httpclient, peers);
			    	vars.posterName = ((EditText) KCPostActivity.this
							.findViewById(R.id.edit_poster_name))
							.getText().toString();
			    	vars.title = ((EditText) KCPostActivity.this
							.findViewById(R.id.edit_posting_title))
							.getText().toString();
			    	vars.content = ((EditText) KCPostActivity.this
							.findViewById(R.id.edit_posting_content))
							.getText().toString();
			    	vars.sage = ((CheckBox) KCPostActivity.this
							.findViewById(R.id.sage))
							.isChecked();
			    	poster.postInThread();
			    }
		    });
		    Button cancelButton = (Button)findViewById(R.id.cancel_button);
		    cancelButton.setOnClickListener(new View.OnClickListener() {
		    	public void onClick(View v) {
		    		((EditText) KCPostActivity.this.findViewById(R.id.edit_poster_name)).setText("");
		    		((EditText) KCPostActivity.this.findViewById(R.id.edit_posting_title)).setText("");
		    		((EditText) KCPostActivity.this.findViewById(R.id.edit_posting_content)).setText("");
		    		((CheckBox) KCPostActivity.this.findViewById(R.id.sage)).setChecked(false);
		    		KCPostActivity.this.finish();
		    	}
		    });
		    TableLayout table = (TableLayout) findViewById(R.id.posting_buttons_table);
		    View.OnClickListener listener = new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					imageClicked = -1;
					for (ImageButton b : buttons) {
						imageClicked++;
						if (b.equals(v)) {
							openFileChooser ();
							break;
						}
					}
				}
		    };
		    for (int i = 0; i < imageRows; i++) {
		    	ImageButton[] bts = ActivityHelpers.addImageButtonRow(NUM_COLS, table, listener, this);
		    	for (int j = 0; j < bts.length; j++) {
		    		buttons.add(bts[j]);
		    	}
			}
	    } catch (StreamCorruptedException e) {
			Toast toast = Toast.makeText(this, "KCPostActivity::onCreate failed: "+e.getMessage(), Toast.LENGTH_LONG);
			toast.show();
		} catch (IOException e) {
			Toast toast = Toast.makeText(this, "KCPostActivity::onCreate failed: "+e.getMessage(), Toast.LENGTH_LONG);
			toast.show();
		} catch (ClassNotFoundException e) {
			Toast toast = Toast.makeText(this, "KCPostActivity::onCreate failed: "+e.getMessage(), Toast.LENGTH_LONG);
			toast.show();
		}
	}
	

	private void openFileChooser () {
		try {
    		// Use OpenIntents File picker if available on the system
        	Intent intent = new Intent("org.openintents.action.PICK_DIRECTORY");
        	intent.setData(Uri.parse("file:///sdcard"));
        	intent.putExtra("org.openintents.extra.TITLE", "Please select a folder");
        	intent.putExtra("org.openintents.extra.BUTTON_TEXT", "Use this folder");
        	
        	startActivityForResult(intent, 1);
    	} catch (ActivityNotFoundException ex) {
    		try {
    			//Fall back to aFileChooser if OI File picker is not installed
        		Intent in = new Intent(KCPostActivity.this,
                        net.krautchan.android.activity.KCFileChooserActivity.class);
                in.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult (in, KCFileChooserActivity.REQUEST_CODE);
    		}  catch (ActivityNotFoundException ex2) {
        		Toast toast = Toast.makeText(KCPostActivity.this, "No File Manager installed or no SD card", 
        				Toast.LENGTH_LONG);
        		toast.show();
    		}
    	}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data){
		if (null == data) {
			return;
		}
        switch(requestCode){
        case KCFileChooserActivity.REQUEST_CODE:
              if (resultCode == Activity.RESULT_OK) {
            	  String uri = data.getDataString();
            	  if (null != uri) {
            		  Log.i(TAG, uri);
            		  Uri imgUri=Uri.parse(uri);
            		  buttons.get(imageClicked).setImageURI(imgUri);  
            		  try {
            			Bitmap b = ActivityHelpers.scaleDownBitmap(ActivityHelpers.loadBitmap(new URL(imgUri.toString())), 125, true);
						buttons.get(imageClicked).setImageBitmap(b);
					} catch (MalformedURLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
            	  }
              }
              break;
		}
        Log.i(TAG, "onActivityResult - done");
	}


	/*protected void postInThread(String name, String title, String content, boolean sage) {
		HttpClient httpclient = new DefaultHttpClient();
	    HttpContext localContext = new BasicHttpContext();
		httpclient.getParams().setParameter("http.useragent",Eisenheinrich.getInstance().USER_AGENT);
		HttpPost httppost = new HttpPost("http://krautchan.net/post");

		try {
		    MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
		    entity.addPart("internal_n", new StringBody(name)); // Bernd name
		    entity.addPart("internal_s", new StringBody(title)); // Post subject
		    entity.addPart("internal_t", new StringBody(content));  // Comment
			if (sage)
				entity.addPart("sage", new StringBody("1"));// Säge
			entity.addPart("forward", new StringBody("thread")); // forward to thread or board -> thread for us
			entity.addPart("board", new StringBody(curBoardName)); // board
			if (null != curThreadKCNum) {
				entity.addPart("parent", new StringBody(curThreadKCNum.toString())); // thread ID
			}
			for (int i = 0; i < MAX_IMAGES; i++) {
				if (files[i] != null) {
					entity.addPart(fileNames[i], new FileBody(new File (Environment.getExternalStorageDirectory(), "/DCIM/"+fileNames[i])));
				}
			}
			httppost.setEntity(entity);
			HttpResponse response = httpclient.execute(httppost, localContext);
			this.finish();
			StatusLine sl = response.getStatusLine();
			System.out.println (sl);
			Header headers[] = response.getAllHeaders();
			for (Header h:headers) {
				System.out.println (h.getName()+" "+h.getValue());
			}
		} catch (Exception e) {
			e.printStackTrace();
			// FIXME Do logging & reporting
		} 
	}*/
	
	@SuppressWarnings("unused")
	private void calculateImageRows () {
	    if (MAX_IMAGES % NUM_COLS == 0) { // ignore warnings
	    	imageRows = MAX_IMAGES / NUM_COLS;
	    } else {
	    	imageRows = (MAX_IMAGES / NUM_COLS) +1;
	    }
	}
	
	
}
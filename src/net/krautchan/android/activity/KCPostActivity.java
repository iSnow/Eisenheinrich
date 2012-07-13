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
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.krautchan.R;
import net.krautchan.android.Eisenheinrich;
import net.krautchan.android.helpers.ActivityHelpers;
import net.krautchan.android.helpers.CustomExceptionHandler;
import net.krautchan.android.network.AsyncPoster;
import net.krautchan.android.network.AsyncPoster.AsyncPosterPeer;
import net.krautchan.android.network.PostVariables;
import net.krautchan.data.KCBoard;
import net.krautchan.data.PostActivityParams;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TableLayout;
import android.widget.Toast;

public class KCPostActivity extends Activity {
	private static final String TAG = "KCPostActivity";
	private static final ArrayList<String> FILE_TYPES; 
	static {
		final String[] FILE_TYPE_ARRAY = {"gif", "jpg", "jpeg", "png"};
		FILE_TYPES = new ArrayList<String>(Arrays.asList(FILE_TYPE_ARRAY));
	}
	private static final int MAX_IMAGES = 4;//max 4 images per posting
	private static final int NUM_COLS = 2;//2 rows of image buttons
	private PostVariables vars;
	private List<ImageButton> buttons = new ArrayList<ImageButton>();
	private int imageClicked = -1; //FIXME this is an ugly way to track which image button was clicked. See if we can set some userdata on the intent or get the control in onActivityResult()
	private int imageRows = 1;
	
	@Override
	public void onCreate(Bundle savedInstanceState) { 
		super.onCreate(savedInstanceState);   
		Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler(
		        "eisenheinrich", "http://eisenheinrich.datensalat.net:8080/Eisenweb/upload/logfile/test", this));
		vars = new PostVariables();
		vars.files = new Uri[MAX_IMAGES];
		calculateImageRows();
		setContentView(R.layout.post_view);
		Bundle b = getIntent().getExtras();
	    byte[] arguments = b.getByteArray("threadparams");
	    String contentPreset = b.getString("contentpreset");
	    ByteArrayInputStream bitch = new ByteArrayInputStream(arguments);
	    ObjectInputStream in;
	    PostActivityParams params = null;
	    try {
			in = new ObjectInputStream(bitch);
		    params = (PostActivityParams)in.readObject();
		    KCBoard board = Eisenheinrich.GLOBALS.BOARD_CACHE.get(params.curBoardDbId);
		    vars.boardName = board.shortName;
		    if (null != params.curThreadKCNum) {
		    	vars.threadNumber = params.curThreadKCNum.toString();
		    }
		    final EditText et = (EditText)findViewById(R.id.edit_posting_content);
		    et.setText(contentPreset);
		    final Button okButton = (Button)findViewById(R.id.ok_button);
		    okButton.setOnClickListener(new View.OnClickListener() {
			    public void onClick(View v) {
			    	findViewById(R.id.postview_spinner).setVisibility(View.VISIBLE);
			    	okButton.setEnabled(false);
			    	((Button)findViewById(R.id.cancel_button)).setEnabled(false);
			    	DefaultHttpClient httpclient = Eisenheinrich.getInstance().getHttpClient();
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
						public void notifyDone(boolean successful, final String message) {
							if (successful) {
								final Intent data = KCPostActivity.this.getIntent();
								if (getParent() == null) {
								    setResult(Activity.RESULT_OK, data);
								} else {
								    getParent().setResult(Activity.RESULT_OK, data);
								}

								KCPostActivity.this.finish();
							} else {
								if (message.equals(R.string.banned_message)) {
									KCPostActivity.this.runOnUiThread(new Runnable () {
										@Override
										public void run() {	
											findViewById(R.id.banned_error).setVisibility(View.VISIBLE);
									    	findViewById(R.id.postview_spinner).setVisibility(View.GONE);
									    	okButton.setEnabled(true);
									    	((Button)findViewById(R.id.cancel_button)).setEnabled(true);
											Toast.makeText(KCPostActivity.this, message, Toast.LENGTH_LONG).show();
										}
								    }); 
								}
							}
						} 		
			    	});
			    	AsyncPoster poster = new AsyncPoster(vars, httpclient, Eisenheinrich.DEFAULTS, Eisenheinrich.GLOBALS, peers);
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
		    if (Eisenheinrich.GLOBALS.BOARD_CACHE.get(params.curBoardDbId).banned) {
		    	
		    }
		    final Button cancelButton = (Button)findViewById(R.id.cancel_button);
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
            	  String uriString = data.getDataString();
            	  if (null != uriString) {
            		  String[] components = uriString.split("\\.");
            		  String suffix = components[components.length-1];
            		  if ((null != suffix) && (suffix.length() != 0)) {
            			  if (FILE_TYPES.contains(suffix.toLowerCase())) {
            				  Log.i(TAG, uriString);
                    		  Uri imgUri=Uri.parse(uriString);
                    		  buttons.get(imageClicked).setImageURI(imgUri); 
                    		  vars.files[imageClicked] = imgUri;
                    		  try {
                    			Bitmap b = ActivityHelpers.scaleDownBitmap(ActivityHelpers.loadBitmap(new URL(imgUri.toString())), 125, true);
        						buttons.get(imageClicked).setImageBitmap(b);
        					} catch (MalformedURLException e) {
        						// TODO Auto-generated catch block
        						e.printStackTrace();
        					}
            			  }
            		  }
            	  }
              }
              break;
		}
        Log.i(TAG, "onActivityResult - done");
	}

	
	@SuppressWarnings("unused")
	private void calculateImageRows () {
	    if (MAX_IMAGES % NUM_COLS == 0) { // ignore warnings
	    	imageRows = MAX_IMAGES / NUM_COLS;
	    } else {
	    	imageRows = (MAX_IMAGES / NUM_COLS) +1;
	    }
	}
	
	
}
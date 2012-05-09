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

import java.io.*;
import java.util.*;
import junit.framework.Assert;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.*;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.Toast;

import net.krautchan.R;
import net.krautchan.android.Eisenheinrich;
import net.krautchan.android.helpers.FileContentProvider;
import net.krautchan.android.helpers.ActivityHelpers;
import net.krautchan.backend.HtmlCreator;
import net.krautchan.data.KCPosting;
import net.krautchan.data.KCThread;
import net.krautchan.data.KODataListener;

public class KCThreadViewActivity extends Activity {
	private static final String 	TAG = "KCThreadViewActivity";
	private PostingListener 		pListener = new PostingListener();
	private static String 			template = null;
	private String 					html = null;
	private WebView 				webView;
	private boolean					webViewBack = true;
	//private Handler 				mHandler = new Handler();
	private String 					boardName = null;
	private Long 					boardId = null;
	private KCThread 				thread = null;
	private boolean 				javascriptInterfaceBroken = true;
	private Handler 				progressHandler = null;

	@Override
	public void onCreate(Bundle bndl) { 
		super.onCreate(bndl);
		View v = this.getLayoutInflater().inflate(R.layout.kc_web_view, null);
		webView = (WebView) v.findViewById(R.id.kcWebView);
		setContentView(v);
		findViewById(R.id.threadview_watcher_wrapper).setVisibility(View.VISIBLE);
		final ProgressBar progress = (ProgressBar)findViewById(R.id.threadview_watcher);
	    progress.setMax(100);
	    progress.setProgress(0);
	    progressHandler = new Handler() {
	        public void handleMessage(Message msg) {
	        	if (0 == msg.arg1) {
	        		findViewById(R.id.threadview_watcher_wrapper).setVisibility(View.GONE);
				    progress.setMax(100);
				    progress.setProgress(0);
				    findViewById(R.id.webview_spinner).setVisibility(View.VISIBLE);
	        	} else if (1 == msg.arg1) {
		        	progress.incrementProgressBy(5);
	        	} else if (2 == msg.arg1) {
				    findViewById(R.id.webview_spinner).setVisibility(View.GONE);
	        	}
	        }
	    };
		webView.setBackgroundColor(Color.BLACK);
		WebSettings webSettings = webView.getSettings();
		webSettings.setSavePassword(false);
		webSettings.setSaveFormData(false);
		webSettings.setJavaScriptEnabled(true);
		webSettings.setSupportZoom(false);
		webSettings.setAllowFileAccess(true);
		webSettings.setJavaScriptCanOpenWindowsAutomatically(false);
		webView.setWebChromeClient(new KCWebChromeClient());
		//webView.addJavascriptInterface(new DemoJavaScriptInterface(), "demo");
		webView.setWebViewClient(new KCWebViewClient());
		
		// TODO: http://krautchan.net/ajax/checkpost?board=b

		/*
		 * Workaround for
		 * https://code.google.com/p/android/issues/detail?id=12987
		 * &can=1&q=webview
		 * %20link&colspec=ID%20Type%20Status%20Owner%20Summary%20Stars WebView
		 * is borked on android 2.3 Workaround courtesy of
		 * http://quitenoteworthy
		 * .blogspot.com/2010/12/handling-android-23-webviews-broken.html
		 */
		try {
			if ("2.3".equals(Build.VERSION.RELEASE)) {
				javascriptInterfaceBroken = true;
			}
		} catch (Exception e) {
			// Ignore, and assume user javascript interface is working
			// correctly.
		}

		// Add javascript interface only if it's not broken
		if (!javascriptInterfaceBroken) {
			webView.addJavascriptInterface(this, "jshandler");
		}

		if (null == template) {
			try {
				InputStream is = this.getAssets().open(
						"kd_thread_view_template.html");
				BufferedReader r = new BufferedReader(new InputStreamReader(is));
				StringBuilder builder = new StringBuilder();
				String line;
				while ((line = r.readLine()) != null) {
					builder.append(line);
				}
				r.close();
				is.close();
				r = null;
				template = builder.toString();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}

		if (bndl != null) {
			webView.restoreState(bndl);
			renderHtml(bndl.getString("html"));
		} else {
			Bundle b = getIntent().getExtras();
			boardName = b.getString("boardName");
			boardId = b.getLong("boardId");
			Assert.assertNotNull(boardId);
			byte[] threadS = b.getByteArray("thread");
			ByteArrayInputStream bitch = new ByteArrayInputStream(threadS);
			ObjectInputStream in;
			try {
				in = new ObjectInputStream(bitch);
				thread = (KCThread) in.readObject();
				thread.board_id = boardId;
			} catch (StreamCorruptedException e) {
				Toast toast = Toast.makeText(this, "WebPageLoader::onCreate failed: " + e.getMessage(), Toast.LENGTH_LONG);
				toast.show();
			} catch (IOException e) {
				Toast toast = Toast.makeText(this, "WebPageLoader::onCreate failed: " + e.getMessage(), Toast.LENGTH_LONG);
				toast.show();
			} catch (ClassNotFoundException e) {
				Toast toast = Toast.makeText(this, "WebPageLoader::onCreate failed: " + e.getMessage(), Toast.LENGTH_LONG);
				toast.show();
			}
			if ((null != thread) && (null != pListener)) {
				Eisenheinrich.getInstance().addPostListener(new PostingListener());
				// String rssUrl =
				// "http://krautchan.net/rss/thread-posts/"+boardName;
				/*
				 * try { thread = KCParser.addPostingsToThread (rssUrl, thread);
				 * } catch (Exception e) { Toast.makeText(this,
				 * "WebPageLoader::onCreate failed: "+e.getMessage(),
				 * Toast.LENGTH_LONG); }
				 */
			}
		}
	}
	
	public void onBackPressed () {
		if (!webViewBack) {
			super.onBackPressed();
			//webView.clearHistory();
		}
		else if (webView.canGoBack()) {
			//webView.goBack();
			webViewBack = false;
			renderHtml(html); 
			webView.clearHistory();
		} else {
			super.onBackPressed();
		}
	}
	

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		webView.saveState(outState);
		outState.putString("html", html);
	}

	private void renderHtml(String html) {
		webView.loadDataWithBaseURL("http://krautchan.net/", html, "text/html",
				"utf-8", null);
		this.html = html;
	}

	private final class PostingListener implements KODataListener<KCPosting> {
		@Override
		public void notifyAdded(KCPosting item) {
			thread.addPosting(item);
			((ProgressBar)findViewById(R.id.threadview_watcher)).incrementProgressBy(5);
			/*
			 * Map<Long, KCPosting> postings = thread.postings; if
			 * (!postings.containsKey(item.dbId)) { postings.put(item.dbId,
			 * item); runOnUiThread(new Runnable() { public void run() {
			 * //adapter.add(item); //adapter.notifyDataSetChanged(); } }); }
			 */
		}

		@Override
		public void notifyDone() {
			List<KCPosting> postings = new ArrayList<KCPosting>();
			for (Long id : thread.getIds()) {
				postings.add(thread.getPosting(id));
			}
			Message msg = progressHandler.obtainMessage();
        	msg.arg1 = 0;
        	progressHandler.sendMessage(msg);
			renderHtml(HtmlCreator.addPostings(postings, template));
		}

		@Override
		public void notifyError(Exception ex) {
			KCThreadViewActivity.this.finish();
		}
	}

	private final class KCWebViewClient extends WebViewClient {
		
		 @Override
		    public boolean shouldOverrideUrlLoading(WebView  view, String  url){
		        return true;
		    }
		 
		 
		    // Override URL Loading
		    @Override
		    public void onLoadResource(WebView  view, String  url){
		        if( url.equals("http://cnn.com") ){
		            // do whatever you want
		        }
		        super.onLoadResource(view, url);
		    }


		@Override
		public void onPageFinished(WebView view, String url) {
			Message msg = progressHandler.obtainMessage();
        	msg.arg1 = 2;
        	progressHandler.sendMessage(msg);
			super.onPageFinished(view, url);

			// If running on 2.3, send javascript to the WebView to handle the
			// function(s)
			// we used to use in the Javascript-to-Java bridge.
			if (javascriptInterfaceBroken) {
				String handleGingerbreadStupidity = "javascript:function openQuestion(id) { window.location='http://jshandler:openQuestion:'+id; }; "
						+ "javascript: function handler() { this.openQuestion=openQuestion; }; "
						+ "javascript: var jshandler = new handler();";
				view.loadUrl(handleGingerbreadStupidity);
			}
			/*Toast toast = Toast.makeText(KCThreadViewActivity.this, "loaded", Toast.LENGTH_LONG);
			toast.show();*/
		}

	}

	/*final class DemoJavaScriptInterface {

		DemoJavaScriptInterface() {
		}*/

		/**
		 * This is not called on the UI thread. Post a runnable to invoke
		 * loadUrl on the UI thread.
		 */
		/*public void clickOnAndroid() {
			mHandler.post(new Runnable() {
				public void run() {
					webView.loadUrl("javascript:wave()");
				}
			});

		}
	}*/
	
	private void viewImage (String fileName) {
		Uri uri = Uri.parse(FileContentProvider.URI_PREFIX+"/"+fileName);
		startActivity(new Intent(Intent.ACTION_VIEW, uri));
	}
	
	/*
	 * Lifted from:
	 * http://it-ride.blogspot.com/2010/04/android-youtube-intent.html
	 */
	private void startVideo(String videoID) {
		String id = videoID.replace("watch?v=", "");
		Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:"+ id));
		List<ResolveInfo> list = getPackageManager().queryIntentActivities(i,
				PackageManager.MATCH_DEFAULT_ONLY);
		if (list.size() > 0) {
			startActivity(i);
		} else {
			Toast toast = Toast.makeText(getApplicationContext(), "Could not open Youtube player", Toast.LENGTH_LONG);
			toast.show();
		}
	}

	/**
	 * Provides a hook for calling "alert" from javascript. We use it to work
	 * around the broken JS-Bridge
	 * Args are in the form command:type:id
	 * Commands Defined: 
	 *  - open
	 * Types defined: 
	 *  - youtube
	 *  - image
	 */
	final class KCWebChromeClient extends WebChromeClient {
		@Override
		public boolean onJsAlert(WebView view, String url, String message,
				JsResult result) {
			Log.d(TAG, message);
			result.confirm();
			String args[] = message.split(":");
			if (args[0].equals("open")) {
				if (args[1].equals("youtube")) {
					startVideo(args[2]);
				} else if (args[1].equals("image")) {
					viewImage (args[2]);
				}
			}
			return true;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.options_menu_webview, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.bookmark: 
			Eisenheinrich.getInstance().dbHelper.bookmarkThread(thread);
			return true;
		case R.id.reload: {
			findViewById(R.id.threadview_watcher_wrapper).setVisibility(View.VISIBLE);
			thread.clearPostings();
			ActivityHelpers.switchToThread(thread, boardName, boardId,  this);
		}
			return true;
		case R.id.prefs:
			return true;
		case R.id.reply: 
			ActivityHelpers.createThreadMask (thread, boardName, this);
			return true;
		case R.id.home: {
			Intent intent = new Intent(KCThreadViewActivity.this,
					EisenheinrichActivity.class);
			startActivity(intent);
		}
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}
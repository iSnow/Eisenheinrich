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
import android.content.ActivityNotFoundException;
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
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import net.krautchan.R;
import net.krautchan.android.Eisenheinrich;
import net.krautchan.android.dialog.BannedDialog;
import net.krautchan.android.helpers.FileContentProvider;
import net.krautchan.android.helpers.ActivityHelpers;
import net.krautchan.backend.HtmlCreator;
import net.krautchan.data.KCBoard;
import net.krautchan.data.KCPosting;
import net.krautchan.data.KCThread;
import net.krautchan.data.KODataListener;
import net.krautchan.parser.KCPageParser;

public class KCThreadViewActivity extends Activity {
	private static final String 	TAG = "KCThreadViewActivity";
	private PostingListener 		pListener = new PostingListener();
	private static String 			template = null;
	private String 					html = null;
	private WebView 				webView;
	private boolean					webViewBack = true;
	private Handler 				mHandler = new Handler();
	private String 					boardName = null;
	private Long 					boardId = null;
	private KCThread 				thread = null;
	private String					token;
	private boolean 				javascriptInterfaceBroken = true;
	private Handler 				progressHandler = null;
	private boolean 				visitedPostsCollapsible = true;
	private boolean 				visitedPostsAreCollapsed = true;

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
		
		//TODO: review http://stackoverflow.com/questions/7424510/uncaught-typeerror-when-using-a-javascriptinterface
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
				visitedPostsCollapsible = false;
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
				InputStream is = this.getAssets().open("kd_thread_view_template.html");
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
			visitedPostsCollapsible = b.getBoolean("visitedPostsCollapsible");
			Assert.assertNotNull(boardId);
			/*TODO since we don't get the live thread object but a different deserialized,
				transmitting it does not make sense. So we only read the KCnum
				Once we have a Cache or database backend for threads, transmit the
				thread db-ID
			 */
				thread = new KCThread();
				thread.kcNummer = b.getLong("threadId");
				thread.board_id = boardId;
				token = "http://krautchan.net/" + boardName + "/thread-" + thread.kcNummer + ".html";
				thread.uri = token;
				String title = "/"+boardName+"/"+thread.kcNummer;
				KCBoard board = Eisenheinrich.GLOBALS.BOARD_CACHE.get(boardId);
				if (board.banned) {
					title = title + " ("+this.getString(R.string.banned)+")";
				}
				this.setTitle(title);
			
			if ((null != thread) && (null != pListener)) {
				Eisenheinrich.getInstance().addPostListener(pListener);
			}
		}
		if (javascriptInterfaceBroken) {
			visitedPostsCollapsible = false;
		}
		if (visitedPostsCollapsible) {
			Button toggleCollapsedButton = (Button)findViewById(R.id.show_collapsed);
		    toggleCollapsedButton.setOnClickListener(new View.OnClickListener() {
			    public void onClick(View v) {
			    	v.setVisibility(View.GONE);
			    	visitedPostsAreCollapsed = !visitedPostsAreCollapsed;
			    	webView.loadUrl("javascript:toggleCollapsed ("+visitedPostsAreCollapsed+")");
			    }
		    });
		}
	}
	
	public void onBackPressed () {
		if (!webViewBack) {
			Eisenheinrich.getInstance().removePostListener(pListener);
			super.onBackPressed();
			//webView.clearHistory();
		}
		else if (webView.canGoBack()) {
			//webView.goBack();
			webViewBack = false;
			renderHtml(html); 
			webView.clearHistory();
		} else {
			Eisenheinrich.getInstance().removePostListener(pListener);
			super.onBackPressed();
		}
	}
	

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		webView.saveState(outState);
		outState.putString("html", html);
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle inState) {
		//String html = inState.getString("html");
		webView.restoreState(inState);
		//super.onRestoreInstanceState(inState);
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}

	@Override
	protected void onStop() {
	    super.onStop();
	}

	@Override
	protected void onRestart() {
	    super.onRestart();
	}

	private void renderHtml(String html) {
		webView.loadDataWithBaseURL("http://krautchan.net/", html, "text/html", "utf-8", null);
		this.html = html;
	}

	private final class PostingListener implements KODataListener<KCPosting> {
		@Override
		public void notifyAdded(KCPosting item, Object token) {
			if (KCThreadViewActivity.this.token.equals(token)) {
				thread.addPosting(item);
				((ProgressBar)findViewById(R.id.threadview_watcher)).incrementProgressBy(5);
			}
		}

		@Override
		public void notifyDone(Object token) {
			if (KCThreadViewActivity.this.token.equals(token)) {
				Message msg = progressHandler.obtainMessage();
	        	msg.arg1 = 0;
	        	progressHandler.sendMessage(msg);
	        	String locTemplate = template;
	        	if (null != thread.previousLastKcNum) {
	        		locTemplate = locTemplate.replace("@@CURPOST@@", thread.previousLastKcNum.toString());
	        	} else {
	        		locTemplate = locTemplate.replace("@@CURPOST@@", "null");
	        	}
	        	renderHtml (HtmlCreator.htmlForThread(thread, locTemplate));
			}
		}

		@Override
		public void notifyError(Exception ex, Object token) {
			if (KCThreadViewActivity.this.token.equals(token)) {
				KCThreadViewActivity.this.finish();
			}
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
				Log.d(TAG, "onPageFinished::GingerbreadStupidity Path");
				/*String handleGingerbreadStupidity = "javascript:function openQuestion(id) { window.location='http://jshandler:openQuestion:'+id; }; "
						+ "javascript: function handler() { this.openQuestion=openQuestion; }; "
						+ "javascript: var jshandler = new handler();";*/
				String handleGingerbreadStupidity = "javascript:gotToPrevLast()";
				view.loadUrl(handleGingerbreadStupidity);
			} else {
				Log.d(TAG, "onPageFinished::Sane Path");
				webView.loadUrl("javascript:gotToPrevLast()");
			}
		}

	}

	final class DemoJavaScriptInterface {

		DemoJavaScriptInterface() {
		}

		/**
		 * This is not called on the UI thread. Post a runnable to invoke
		 * loadUrl on the UI thread.
		 */
		public void clickOnAndroid() {
			mHandler.post(new Runnable() {
				public void run() {
					webView.loadUrl("test('hey')");
				}
			});

		}
	}
	
	private void openExternalLink(String url) {
		if (!url.startsWith("http://") && !url.startsWith("https://")) {
			url = "http://" + url;
		}
		Intent browser = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
		try {
			startActivity(browser);
		} catch (ActivityNotFoundException ex) {
			 //FIXME implement some toast or dialog shit
		}
	}
	
	private void viewImage (String fileName) {
		Uri uri = Uri.parse(FileContentProvider.URI_PREFIX+"/"+fileName);
		startActivity(new Intent(Intent.ACTION_VIEW, uri));
	}
	
	/*
	 * Lifted from:
	 * http://it-ride.blogspot.com/2010/04/android-youtube-intent.html
	 */
	private void startVideo(String videoID) {
		String id = videoID.replace("youtube.com/watch?v=", "");
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
		public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
			Log.d(TAG, message);
			result.confirm();
			String args[] = message.split(":");
			if (args[0].equals("open")) {
				if (args[1].equals("ytlink")) {
					startVideo(args[2]);
				} else if (args[1].equals("image")) {
					viewImage (args[2]);
				} else if (args[1].equals("extlink")) {
					openExternalLink (args[2]);
				}  else if (args[1].equals("kclink")) {
					String[] parts = args[2].split("/");
					List<KCBoard> boards = Eisenheinrich.GLOBALS.BOARD_CACHE.getAll();
					Iterator<KCBoard> iter = boards.iterator();
					boolean found = false;
					KCBoard board = null;
					while (iter.hasNext() && (!found)) {
						board = iter.next();
						if (board.shortName.equals(parts[1])) {
							found = true;
						}
					}
					if (null != board) {
						prepareForRerender(board, Long.parseLong(parts[2]));
						//ActivityHelpers.switchToThread(Long.parseLong(parts[2]), parts[1], board.dbId,  KCThreadViewActivity.this);
					}
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
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (0 == requestCode) {
			reload();
		} else {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	private void prepareForRerender(KCBoard board, long threadKcNum) {
		thread.board_id = board.dbId;
		thread.kcNummer = threadKcNum;
		boardName = board.shortName;
		thread.clearPostings();
		findViewById(R.id.threadview_watcher_wrapper).setVisibility(View.VISIBLE);
		String title = "/"+boardName+"/"+thread.kcNummer;
		if (board.banned) {
			title = title + " ("+this.getString(R.string.banned)+")";
		}
		KCThreadViewActivity.this.setTitle(title);
		token = "http://krautchan.net/" + board.shortName + "/thread-" + threadKcNum + ".html";
		Thread t = new Thread(new KCPageParser("http://krautchan.net/" + board.shortName + "/thread-" + threadKcNum + ".html", board.dbId)
			.setBasePath("http://krautchan.net/")
			.setThreadHandler(
					Eisenheinrich.getInstance().getThreadListener())
			.setPostingHandler(
					Eisenheinrich.getInstance().getPostListener()));
		t.start();
	}
	
	private void reload() {
		if (visitedPostsCollapsible) {
			findViewById(R.id.show_collapsed).setVisibility(View.VISIBLE);
			visitedPostsAreCollapsed = true;
		}
		findViewById(R.id.threadview_watcher_wrapper).setVisibility(View.VISIBLE);
		thread.previousLastKcNum = thread.getLastPosting().kcNummer;		
		thread.clearPostings();
		ActivityHelpers.switchToThread(thread.kcNummer, boardName, boardId,  this);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.bookmark: 
			Eisenheinrich.getInstance().dbHelper.bookmarkThread(thread);
			return true;
		case R.id.reload: 
			reload();
			return true;
		case R.id.prefs:
			return true;
		case R.id.reply: 
			KCBoard board = Eisenheinrich.GLOBALS.BOARD_CACHE.get(boardId);
			if ((board.banned) && (null == Eisenheinrich.GLOBALS.KOMTUR_CODE)) {
				new BannedDialog (this).show();
				Toast.makeText(KCThreadViewActivity.this, R.string.banned_message, Toast.LENGTH_LONG).show();
			} else {
				ActivityHelpers.createThreadMask (thread, boardName, this);
			}
			return true;
		case R.id.home: 
			Intent intent = new Intent(KCThreadViewActivity.this, EisenheinrichActivity.class);
			startActivity(intent);
			this.finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}
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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import junit.framework.Assert;
import net.krautchan.R;
import net.krautchan.android.Eisenheinrich;
import net.krautchan.android.dialog.BannedDialog;
import net.krautchan.android.helpers.ActivityHelpers;
import net.krautchan.android.helpers.CustomExceptionHandler;
import net.krautchan.android.helpers.FileContentProvider;
import net.krautchan.android.widget.CommandBar;
import net.krautchan.data.KCBoard;
import net.krautchan.data.KCPosting;
import net.krautchan.data.KCThread;
import net.krautchan.data.KODataListener;
import net.krautchan.parser.KCPageParser;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebStorage.QuotaUpdater;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;

@SuppressLint("SetJavaScriptEnabled") 
public class KCThreadViewActivity extends Activity {
	private WebView 				webView;
	private static final String 	TAG = "KCThreadViewActivity";
	private CommandBar 				cmdBar;
	private PostingListener 		pListener = new PostingListener();
	private String					citation = "";		
	private static String 			template = null;
	private int						progressIncrement = 5;
	private Handler 				mHandler = new Handler();
	private KCBoard 				board = null;
	private KCThread 				thread = null;
	private String					token;
	private boolean 				javascriptInterfaceBroken = false;
	private Handler 				progressHandler = null;
	private boolean					pageFinished = false;
	private boolean 				visitedPostsAreCollapsed = true;
	private boolean					missedPostings = false;
	Set<KCPosting> 					postings;

	@Override
	public void onCreate(Bundle bndl) {
		super.onCreate(bndl);
		missedPostings = false;
		postings = new CopyOnWriteArraySet<KCPosting>();
		Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler(
		        "eisenheinrich", "http://eisenheinrich.datensalat.net:8080/Eisenweb/upload/logfile/test", this));

		View v = this.getLayoutInflater().inflate(R.layout.kc_web_view, null);
	    cmdBar = (CommandBar) v.findViewById(R.id.command_bar);
		webView = (WebView) v.findViewById(R.id.kcWebView);
		setContentView(v);
		
	    progressHandler = new Handler() {
	        public void handleMessage(Message msg) {
	        	if (0 == msg.arg1) {
	        		cmdBar.hideProgressBar();
	        	} else if (1 == msg.arg1) {
	        		cmdBar.incrementProgressBy (progressIncrement);
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
		webView.setWebViewClient(new KCWebViewClient());
		// TODO: http://krautchan.net/ajax/checkpost?board=b
		
		//TODO: review http://stackoverflow.com/questions/7424510/uncaught-typeerror-when-using-a-javascriptinterface
		/*
		 * Workaround for
		 * https://code.google.com/p/android/issues/detail?id=12987 WebView
		 * is borked on android 2.3 Workaround courtesy of
		 * http://quitenoteworthy.blogspot.com/2010/12/handling-android-23-webviews-broken.html
		 */
		
		try {
			if ((Build.VERSION.SDK_INT == 9) || (Build.VERSION.SDK_INT == 10)) { //Android 2.3.x
				javascriptInterfaceBroken = true;
				webView.setWebChromeClient(new KCWebChromeClient());
			} else {
				webView.addJavascriptInterface(new JavaScriptInterface (this), "Android");
				webView.setWebChromeClient(new KCWebChromeClient());
			}
		} catch (Exception e) {
			javascriptInterfaceBroken = true;
		}
		if (null != bndl) {
			webView.restoreState(bndl);  
			Log.i("THREADVIEW", "onCreate RESTORE Bndls");
		} else {
			bndl = getIntent().getExtras();
		}
		Long threadId = bndl.getLong("threadId");
		thread = Eisenheinrich.GLOBALS.getThreadCache().get(threadId);
		Assert.assertNotNull("Assertion thread != null failed in KCThreadView::onCreate() "+threadId, thread);
		if (null != pListener) {
			Eisenheinrich.getInstance().addPostListener(pListener);
		}
		Assert.assertNotNull("Assertion thread.boardId != null failed in KCThreadView::onCreate() "+threadId, thread.board_id);
				
		token = thread.uri;
		progressIncrement = bndl.getInt("progressIncrement");
		board = Eisenheinrich.GLOBALS.getBoardCache().get(thread.board_id);
		setTitle(board);
		if (null == template) {
			template = prepareTemplate (getPageTemplate ());
		} 
		if ((null != thread) && (null != thread.getFirstPosting())) {
    		String locTemplate = template.replace("<ul id='kc-postlist'>", "<ul id='kc-postlist'><li class='odd unread' id='"+thread.getFirstPosting().dbId+"'>"+thread.getFirstPosting().asHtml(Eisenheinrich.GLOBALS.shouldShowImages())+"</li>");
			renderHtml(locTemplate);
		}  else {
			renderHtml(template);
		}

		if (Eisenheinrich.GLOBALS.areVisitedPostsCollapsible()) {
			Button toggleCollapsedButton = (Button)findViewById(R.id.show_collapsed);
		    toggleCollapsedButton.setOnClickListener(new View.OnClickListener() {
			    public void onClick(View v) {
			    	v.setVisibility(View.GONE);
			    	visitedPostsAreCollapsed = !visitedPostsAreCollapsed;
			    	webView.loadUrl("javascript:showCollapsed ("+visitedPostsAreCollapsed+")");
			    }
		    });
			showHideCollapsedButton ();
		} 
		Log.i("THREADVIEW", "onCreate done");
	}
	
	private void showHideCollapsedButton () {
		if (!Eisenheinrich.GLOBALS.areVisitedPostsCollapsible()) {
			return;
		}
		runOnUiThread(new Runnable() {
	        public void run() {
				Button toggleCollapsedButton = (Button)findViewById(R.id.show_collapsed);
				if ((null == thread) || (null == thread.previousLastKcNum)) {
					toggleCollapsedButton.setVisibility(View.GONE);
				} else {
					toggleCollapsedButton.setVisibility(View.VISIBLE);
				}
	        }
		});
	}
	
	private String prepareTemplate (String locTemplate) {
		Log.i("THREADVIEW", "prepareTemplate");
		locTemplate = locTemplate.replace("@@JSBRIDGESANE@@", Boolean.toString(!javascriptInterfaceBroken));
		if ((null != thread) && (null != thread.previousLastKcNum)) {
    		locTemplate = locTemplate.replace("@@CURPOST@@", thread.previousLastKcNum.toString());
    	} else {
    		locTemplate = locTemplate.replace("@@CURPOST@@", "null");
    	}
		String gingerbreadFix = "";
    	if (javascriptInterfaceBroken) {
        	gingerbreadFix = "function handler() { " +
				"this.openKcLink = function(url){alert(\"open:kclink:\"+url)}; " +
        		"this.openExternalLink = function(url){alert(\"open:extlink:\"+url)}; " +
        		"this.openYouTubeVideo = function(url){alert(\"open:ytlink:\"+videoId)}; " +
        		"this.openImage = function(url){alert(\"open:image:\"+fileName)}; " +
        		"this.citePosting = function(postid){alert(\"cite:\"+postid)}; " +
        		"this.debugString = function(str){alert(\"debugstr:\"+str)};" +
        		"}; " +
        		"var Android = new handler();";
    	}
    	locTemplate = locTemplate.replace("@@GINGERBREADFIX@@", gingerbreadFix);
    	return locTemplate;
	}
	
	public void onBackPressed () {
			super.onBackPressed();
			if ((null != thread) && (thread.getLastPosting() != null)) {
				thread.previousLastKcNum = thread.getLastPosting().kcNummer;
			} 

			Button toggleCollapsedButton = (Button)findViewById(R.id.show_collapsed);
			toggleCollapsedButton.setVisibility(View.GONE);
			Eisenheinrich.getInstance().removePostListener(pListener);
			thread = null;
			showHideCollapsedButton();
			citation = "";
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		//Log.i("THREADVIEW", "onSaveInstanceState");
		outState.putLong("threadId", thread.dbId);
		outState.putLong("threadKcNum", thread.kcNummer);
		outState.putLong("boardId", thread.board_id);
		outState.putString("token", thread.uri);
		webView.saveState(outState);
		if (thread.getLastPosting() != null) {
			thread.previousLastKcNum = thread.getLastPosting().kcNummer;
		} 
		//Log.i("THREADVIEW", "onSaveInstanceState done");
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle inState) {
		//Log.i("THREADVIEW", "onRestoreInstanceState");
		webView.restoreState(inState);
		thread = Eisenheinrich.GLOBALS.getThreadCache().get(inState.getLong("threadId"));
		if (null != thread) {
			Log.i("THREADVIEW", "onRestoreInstanceState done. Thread: "+thread.dbId);
			Thread t = new Thread(new KCPageParser(thread)
			.setBasePath("http://krautchan.net/")
			.setThreadHandler(
				Eisenheinrich.getInstance().getThreadListener())
			.setPostingHandler(
				Eisenheinrich.getInstance().getPostListener()));
			t.start();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onStop() {
		if ((null != thread) && (thread.getLastPosting() != null)) {
			thread.previousLastKcNum = thread.getLastPosting().kcNummer;
		} 
	    super.onStop();
	}

	@Override
	protected void onRestart() {
	    super.onRestart();
	} 

	private void renderHtml(final String html) {
		runOnUiThread(new Runnable() {
	        public void run() {
	        	webView.loadDataWithBaseURL("http://krautchan.net/", html, "text/html", "utf-8", null);
	        }
		});
	}
	
	private void renderPosting (final KCPosting posting, boolean read, boolean even, boolean first) {
		Log.i("THREADVIEW", "Render Posting: "+posting.kcNummer);
		String classStr = ""; 
		if (!first) {
			if (read) {
				classStr = "read";
			} else {
				classStr = "unread";
			}
		}
		if (even) {
			classStr += " even";
		} else {
			classStr += " odd"; 
		}
		String content = posting.asHtml(Eisenheinrich.GLOBALS.shouldShowImages());
		content = content.replaceAll("'", '\\'+"\\'").replaceAll("[\n\r]", " ").replaceAll("\"", "\\\\\"");
		final String cStr = classStr;
		final String cContent = content; 
		runOnUiThread(new Runnable() {
	        public void run() {
	        	webView.loadUrl("javascript:appendPost('"+cContent+"', '"+cStr+"', '"+posting.dbId+"');");
	        }
		});
	}
	
	private void renderBacklog (Long reference, boolean even) {
		if (missedPostings) {
			Iterator<KCPosting> iter = postings.iterator();
			Log.i("THREADVIEW", "RENDER BACKLOG2");
			while (iter.hasNext()) {
				KCPosting p = iter.next();
				Log.i("THREADVIEW", ">>>MISSED POSTING: "+p.kcNummer);
				boolean read = ((null != reference) && (p.kcNummer < reference));
				renderPosting (p, read, even, false);
				even = !even;
			}
			missedPostings = false;
		} else {
			Log.i("THREADVIEW", "NO BACKLOG2 TO RENDER: ");
		}
	}

	private final class PostingListener implements KODataListener<KCPosting> {
		private boolean even = false; 
		
		@Override
		public void notifyAdded(KCPosting item, Object token) {
			if (KCThreadViewActivity.this.token.equals(token)) {
				even = !even;
				cmdBar.incrementProgressBy(progressIncrement);
				Log.i("THREADVIEW", "notifyAdded 1: "+item.kcNummer);
				if (pageFinished) {
					renderBacklog (thread.previousLastKcNum, even);
					renderPosting (item, ((thread.previousLastKcNum != null) && (item.kcNummer <= thread.previousLastKcNum)), even, false);
				} else {
					Log.i("THREADVIEW", ">Missing POSTING: "+item.kcNummer);
					missedPostings = true;
					postings.add(item);
				}
			} 
		}  

		@Override
		public void notifyDone(Object token) {
			if (KCThreadViewActivity.this.token.equals(token)) {
				Log.i("THREADVIEW", "notifyDone");
				Eisenheinrich.getInstance().dbHelper.persistThread(thread);
				Message msg = progressHandler.obtainMessage();
	        	msg.arg1 = 0;
	        	progressHandler.sendMessage(msg);
	        	//FIXME remove next line and implement thread caching.
	        	thread.recalc();
	        	stopSpinner ();
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
			super.onPageFinished(view, url);
			pageFinished = true;
			if ((null != thread) && (null != thread.previousLastKcNum)) {
				renderBacklog (thread.previousLastKcNum, false);
			} else {
				renderBacklog (null, false);
			}
			Log.i("THREADVIEW", "Page finished");
			stopSpinner ();
		}
	} 
	
	private void stopSpinner () {
		Message msg = progressHandler.obtainMessage();
    	msg.arg1 = 2;
    	progressHandler.sendMessage(msg);
	}

	final class JavaScriptInterface {
		Context context;

	    JavaScriptInterface(Context c) {
	        context = c;
	    }

	    public void citePosting(final String postid) {
	    	mHandler.post(new Runnable() {
				public void run() {
					KCThreadViewActivity.this.citePosting(postid);
				}
			});
	    }
	    
	    public void openExternalLink(final String url) {
			mHandler.post(new Runnable() {
				public void run() {
					KCThreadViewActivity.this.openExternalLink(url);
				}
			});
		}
	    
	    public void openKcLink(final String url) {
	    	mHandler.post(new Runnable() {
				public void run() {
					KCThreadViewActivity.this.openKcLink(url) ;
				}
			});
	    }
		
	    public void openImage (final String fileName) {
			mHandler.post(new Runnable() {
				public void run() {
					KCThreadViewActivity.this.openImage (fileName);
				}
			});
		}
		
	    public void openYouTubeVideo(final String videoID) {
			mHandler.post(new Runnable() {
				public void run() {
					KCThreadViewActivity.this.openYouTubeVideo(videoID);
				}
			});
		}
	    
	    public void debugString(final String str) {
	    	if (null != str) {
	    		System.out.println (str);
	    	}
	    }
	}
	
	public void citePosting(String postid) {
    	long postDbId = -1;
    	try {
    		postDbId = Long.parseLong(postid);
    		KCPosting post = KCThreadViewActivity.this.thread.getPosting(postDbId);
    		if (null == post) {
    			return;
    		}
    		citation += ">>"+post.kcNummer+":\n"+post.getKcStyledContent()+"\n";
    		
    	} catch (Exception ex) {
    		Log.e(TAG, "citePosting failed: "+ex.getMessage());
	        return;
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
			Toast.makeText(getApplicationContext(), this.getText(R.string.could_not_open_browser), Toast.LENGTH_LONG).show();
		}
	}
	
	private void openKcLink(String url) {
		String[] parts = url.split("/");
		List<KCBoard> boards = Eisenheinrich.GLOBALS.getBoardCache().getAll();
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
			try {
			Long id = Long.parseLong(parts[2]);
			prepareForRerender(id);
			} catch (NumberFormatException ex) {
				openExternalLink (url);
			}
			//ActivityHelpers.switchToThread(Long.parseLong(parts[2]), parts[1], board.dbId,  KCThreadViewActivity.this);
		}
	}
	
	private void openImage (String fileName) {
		try {
			final Uri uri = Uri.parse(FileContentProvider.URI_PREFIX+"/"+fileName);
			new Thread(new Runnable() {
				@Override
				public void run() {
					String filePath = ActivityHelpers.downloadToFile(uri);
					if (null != filePath) {
						Intent intent = new Intent();
						intent.setAction(android.content.Intent.ACTION_VIEW);
						intent.setDataAndType(Uri.fromFile(new File(filePath)), "image/*");
						startActivity(intent);
					}
				}
				
			}).start();
		} catch (ActivityNotFoundException ex) {
			Toast.makeText(getApplicationContext(), this.getText(R.string.could_not_open_image_viewer), Toast.LENGTH_LONG).show();
		}
	}
	
	/*
	 * Lifted from:
	 * http://it-ride.blogspot.com/2010/04/android-youtube-intent.html
	 */
	private void openYouTubeVideo(String videoID) {
		String id = videoID.replace("youtube.com/watch?v=", "");
		Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:"+ id));
		List<ResolveInfo> list = getPackageManager().queryIntentActivities(i,
				PackageManager.MATCH_DEFAULT_ONLY);
		if (list.size() > 0) {
			startActivity(i);
		} else {
			Toast.makeText(getApplicationContext(), getText(R.string.could_not_open_youtube_viewer), Toast.LENGTH_LONG).show();
		}
	}

	/**
	 * Provides a hook for calling "alert" from javascript. We use it to work
	 * around the broken JS-Bridge
	 * Args are in the form command:type:id
	 * Commands Defined: 
	 *  - open
	 *  - cite
	 * Types defined: 
	 *  - kclink
	 *  - external link
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
					openYouTubeVideo(args[2]);
				} else if (args[1].equals("image")) {
					openImage (args[2]);
				} else if (args[1].equals("extlink")) {
					openExternalLink (args[2]);
				}  else if (args[1].equals("kclink")) {
					openKcLink (args[2]);
				}
			} else if (args[0].equals("cite")){
				citePosting (args[1]); 
			}
			return true;
		}

		@Override
		public boolean onConsoleMessage(ConsoleMessage cm) {
			//Log.e(TAG, ">>>"+cm.message());
			//FileHelpers.writeToSDFile("_log_.txt", cm.message());
			return super.onConsoleMessage(cm); 
		}

		@Override
		public void onReachedMaxAppCacheSize(long spaceNeeded,
				long totalUsedQuota, QuotaUpdater quotaUpdater) {
			Log.e(TAG, "MaxAppCacheSize reached");
			super.onReachedMaxAppCacheSize(spaceNeeded, totalUsedQuota, quotaUpdater);
		}
		
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.options_menu_webview, menu);
		return true;
	}
	
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		citation = "";
		if (-1 == resultCode) {
			reload();
		} else {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}
	
	private String getPageTemplate() {
		final String templateName = "kc_thread_view_template.html";
		String template = null;
		String css = Eisenheinrich.STYLES.getStyles();
		InputStream templateStream = null;
		try {
			templateStream = this.getAssets().open(templateName);
			BufferedReader r = new BufferedReader(new InputStreamReader(templateStream));
			StringBuilder builder = new StringBuilder();
			String line;
			while ((line = r.readLine()) != null) {
				builder.append(line);
			}
			r.close();
			templateStream.close();
			r = null;
			template = builder.toString();
			if (null != css) {
				template = template.replace("@@CSS@@", css);
			}
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
		}
		return template;
	}

	private void prepareForRerender(long threadKcNum) {
		thread.board_id = board.dbId;
		thread.kcNummer = threadKcNum;
		thread.clearPostings();
		cmdBar.showProgressBar();
		setTitle (board);
		token = "http://krautchan.net/" + board.shortName + "/thread-" + threadKcNum + ".html";
		Thread t = new Thread(new KCPageParser("http://krautchan.net/" + board.shortName + "/thread-" + threadKcNum + ".html", board.dbId)
			.setBasePath("http://krautchan.net/")
			.setThreadHandler(
					Eisenheinrich.getInstance().getThreadListener())
			.setPostingHandler(
					Eisenheinrich.getInstance().getPostListener()));
		t.start();
	}
	
	private void setTitle (KCBoard board) {
		String title = "/"+board.shortName+"/"+thread.kcNummer;
		if (board.banned) {
			title = title + " ("+this.getString(R.string.banned)+")";
		}
		cmdBar.setTitle(title);
	}
	
	private void reload() {
		if (Eisenheinrich.GLOBALS.areVisitedPostsCollapsible()) {
			findViewById(R.id.show_collapsed).setVisibility(View.VISIBLE);
			visitedPostsAreCollapsed = true;
	    	webView.loadUrl("javascript:markAllPostingsRead ()");
		} 
		webView.loadUrl("javascript:showCollapsed (true)");
		cmdBar.showProgressBar();
		missedPostings = false;
		postings = new LinkedHashSet<KCPosting>(); 
		Thread t = new Thread(new KCPageParser(thread)
			.setBasePath("http://krautchan.net/")
			.setThreadHandler(
				Eisenheinrich.getInstance().getThreadListener())
			.setPostingHandler(
				Eisenheinrich.getInstance().getPostListener()));
		t.start();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
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
			KCBoard board = Eisenheinrich.GLOBALS.getBoardCache().get(thread.board_id);
 			String cc = Eisenheinrich.GLOBALS.getKomturCode();
			if ((board.banned) && (null == cc)) {
				new BannedDialog (this).show();
				Toast.makeText(KCThreadViewActivity.this, R.string.banned_message, Toast.LENGTH_LONG).show();
			} else {
				ActivityHelpers.createThreadMask (thread, thread.board_id, citation, this);
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
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Process;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableLayout.LayoutParams;
import android.widget.TableRow;
import net.krautchan.R; 
import net.krautchan.android.Eisenheinrich;
import net.krautchan.android.dialog.AboutDialog;
import net.krautchan.android.dialog.DisclaimerDialog;
import net.krautchan.android.dialog.UpdateDialog;
import net.krautchan.android.helpers.ActivityHelpers;
import net.krautchan.android.helpers.CustomExceptionHandler;
import net.krautchan.android.helpers.FileHelpers;
import net.krautchan.android.network.ThreadExistenceCheck;
import net.krautchan.android.network.ThreadExistenceCheck.ThreadExistencePeer;
import net.krautchan.android.network.CookieHelper;
import net.krautchan.android.network.UpdateCheck;
import net.krautchan.android.network.UpdateCheck.UpdateCheckPeer;
import net.krautchan.data.KCBoard;
import net.krautchan.data.KCThread;

public class EisenheinrichActivity extends Activity {
	public static final String TAG = "EisenheinrichActivity";
	public static Properties pr = null;
	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Properties defaults = null;
		try {
			InputStream is = getAssets().open("settings.txt");
			defaults = new Properties();
			defaults.load(is);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (null == defaults) {
			pr = new Properties ();
		} else {
			pr = new Properties (defaults);
		}
		try {
			File globalsFile = FileHelpers.getSDFile ("settings.txt");
			if (globalsFile.exists()) {
				InputStream is = null;
				is = new FileInputStream (globalsFile);
				pr.load(is);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler(
		        "eisenheinrich", "http://eisenheinrich.datensalat.net:8080/Eisenweb/upload/logfile/test", this));
		setContentView(R.layout.main_view);
		
		if (Eisenheinrich.GLOBALS.isDebugVersion()) {
			findViewById (R.id.debug_marker).setVisibility(View.VISIBLE);
		}
		
	    Button goKCButton = (Button)findViewById(R.id.main_goto_kc);
	    goKCButton.setOnClickListener(new View.OnClickListener() {
		    public void onClick(View v) {
				EisenheinrichActivity.this.startActivity(new Intent(EisenheinrichActivity.this, KCBoardListActivity.class));
		    }
	    });
	    
	    UpdateCheck up = new UpdateCheck(new UpdatePeer(), Eisenheinrich.getInstance().getHttpClient(), Eisenheinrich.DEFAULTS.UPDATE_VERSION_URL);
	    up.checkForUpdate(this);
	    
	    CookieHelper.getSessionCookie(Eisenheinrich.GLOBALS);
	    
		Prefs prefs = Prefs.getInstance();
		if (null == prefs) {
			this.finish();
		} else {
			try {
				//FIXME we get nullpointer exceptions in the line below. Prefs should not be an activity
				if (!prefs.isDisclaimerAck()) {
					DisclaimerDialog dlg = new DisclaimerDialog(this);
					dlg.show();
				}
			} catch (IOException e) {
				Process.killProcess(Process.myPid());
			}
		}
	}
	
	
	@Override
	protected void onResume() {
		super.onResume();
		TableLayout table = null;
		table = (TableLayout) findViewById(R.id.bookmark_table);
		if (null != table) {
			table.removeAllViews();
			Collection <KCThread> threads = Eisenheinrich.getInstance().dbHelper.getBookmarks();
			ThreadExistencePeer bmp = new BookmarkPeer(threads);
			ThreadExistenceCheck bmc = new ThreadExistenceCheck(threads, bmp);
			bmc.checkThreads();
		}
	}

	@Override
	public void onBackPressed() {
		Process.killProcess(Process.myPid());
		/*super.onBackPressed();
		Eisenheinrich.getInstance().dbHelper.close();
		this.finish();*/
	}
	
	public void showBookmarks (Collection <KCThread> bookmarks, boolean[] valid) {
		Collection <KCThread> validBookmarks = new HashSet<KCThread>();
		Collection<KCBoard> boards = Eisenheinrich.getInstance().dbHelper.getBoards();
		int count = 0;
		for (KCThread thread: bookmarks) {
			KCBoard board = getBoard (boards, thread.board_id);
			if (null != board) {
				if (valid[count]) {
					validBookmarks.add(thread);
				} else {
					Eisenheinrich.getInstance().dbHelper.deleteThread(thread.dbId);
				}
			}
			count++;
		}
		showBookmarks (validBookmarks);
	}
	
	public void showBookmarks (Collection <KCThread> validBookmarks) {
		if (validBookmarks.isEmpty()) {
			return;
		}
		int numThreads = validBookmarks.size();
		final int maxNumColumns = 3;
		int numCols = maxNumColumns;
		// try to get an even distribution of cols and rows
		while ((numCols >= 1) && ((numThreads / numCols) < numCols)) {
			numCols--; 
		} 
		
		int remainder = numThreads % numCols;
		int rows = (int) Math.round(Math.floor((double)numThreads / (double)numCols));
		TableLayout table = null;
		table = (TableLayout) findViewById(R.id.bookmark_table);
        Iterator<KCThread> iter = validBookmarks.iterator();
        if (0 != remainder) {
        	addBookmarksRow (iter, remainder, table);
        }
		for (int i = 0; i < rows; i++) {
			addBookmarksRow (iter, numCols, table);
		}
	}
	
	private void addBookmarksRow (Iterator<KCThread> iter, int numColumns, TableLayout table) {
		if ((null == table)|| (null == iter)){
			return;
		}
		int digestLengths[] = {50, 25, 17};
		if (numColumns > digestLengths.length) {
			throw new IllegalArgumentException ("numColumns can't be more than "+digestLengths.length);
		}
		TableRow row = new TableRow(this);
		Collection<KCBoard> boards = Eisenheinrich.getInstance().dbHelper.getBoards();
        row.setLayoutParams(new LayoutParams(
                LayoutParams.FILL_PARENT,
                LayoutParams.WRAP_CONTENT));
		for (int j = 0; j < numColumns; j++) {			
			Button bt = new Button(this);
			if (iter.hasNext()) {
				KCThread thread = iter.next();
				KCBoard board = getBoard (boards, thread.board_id);
				if (null != board) {
					String digest = thread.digest;
					if (digest.length() > digestLengths[numColumns-1]) {
						digest = digest.substring(0, digestLengths[numColumns-1]);
					}
					bt.setText("/"+board.shortName+"/ - "+thread.kcNummer.toString()+"\n"+digest);
					bt.setBackgroundDrawable(getResources().getDrawable(R.drawable.button));
				    bt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
				    bt.setTextColor(getResources().getColor(R.color.White));
				    bt.setOnClickListener(new BookmarkOnClickListener(thread, board));
				    row.addView(bt);
				}   
			}
		}	        
		table.addView(row);
	}
	
	private static KCBoard getBoard (Collection<KCBoard> boards, Long boardID) {
		for (KCBoard board: boards) {
			if (board.dbId.longValue() == boardID.longValue()) {
				return board;
			}
		}
		return null;
	}
	
	private final class BookmarkOnClickListener implements View.OnClickListener {
		private KCThread thread;
		private KCBoard board;
		
		public BookmarkOnClickListener(KCThread thread, KCBoard board) {
			super();
			this.thread = thread;
			this.board = board;
		}
		
		@Override
		public void onClick(View v) {
			ActivityHelpers.switchToThread (thread, EisenheinrichActivity.this);
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.options_menu_main, menu);
	    return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.main_about:
			new AboutDialog(this).show();
			return true;
		case R.id.prefs:
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	private class BookmarkPeer implements ThreadExistencePeer  {
		//private final Collection <KCThread> bookmarks;
		private Long lastId;
		private final Collection <KCThread> valids = new HashSet<KCThread>();
		
		public BookmarkPeer(Collection<KCThread> bookmarks) {
			super();
			Iterator<KCThread> iter = bookmarks.iterator();
			while (iter.hasNext()) {
				lastId = iter.next().dbId;
			}
			//this.bookmarks = bookmarks;
		}

		/*@Override
		public void threadsChecked(final boolean[] valid) {
			EisenheinrichActivity.this.runOnUiThread(new Runnable () {
				@Override
				public void run() {	
					showBookmarks (bookmarks, valid);
				}
		    }); 
			
		}*/

		@Override
		public void threadChecked(KCThread thread, boolean valid) {
			if (valid) {
				valids.add(thread);
			}
			if (null != lastId) {
				if (thread.dbId.longValue() == lastId.longValue()) {
					EisenheinrichActivity.this.runOnUiThread(new Runnable () {
						@Override
						public void run() {	
							showBookmarks (valids);
						}
				    }); 
				}
			}
		}
	};
	
	private class UpdatePeer implements UpdateCheckPeer {

		@Override
		public void updateAvailable() {
			EisenheinrichActivity.this.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					if (!isFinishing()) {
						new UpdateDialog (EisenheinrichActivity.this).show();
				    }
				}
				
			});
		}
	}
}
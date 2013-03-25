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
import java.util.Timer;
import java.util.TimerTask;

import net.krautchan.R;
import net.krautchan.android.Defaults;
import net.krautchan.android.Eisenheinrich;
import net.krautchan.android.dialog.BannedDialog;
import net.krautchan.android.dialog.GoToThreadDialog;
import net.krautchan.android.helpers.ActivityHelpers;
import net.krautchan.android.helpers.CustomExceptionHandler;
import net.krautchan.android.widget.CommandBar;
import net.krautchan.android.widget.ThreadListAdapter;
import net.krautchan.data.KCBoard;
import net.krautchan.data.KCThread;
import net.krautchan.data.KODataListener;
import net.krautchan.parser.KCPageParser;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

public class KCThreadListActivity extends Activity implements ProvidesBoards, ProvidesThreads {
	protected static final String TAG = "KCThreadListActivity";
	private ListView list = null;
	private CommandBar cmdBar;
	private Eisenheinrich heini = Eisenheinrich.getInstance();
	private ThreadListAdapter adapter = null;
	private KCBoard curBoard;
	private String token;
	private String title; 
	//TODO at some point, factor this out into a cache
	//private List<KCThread> threads = new CopyOnWriteArrayList<KCThread>();
	private Timer siteReachableWatchdog = new Timer();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);         
		Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler(
		        "eisenheinrich", "http://eisenheinrich.datensalat.net:8080/Eisenweb/upload/logfile/test", this));
		setContentView(R.layout.thread_list);
		list = (ListView)findViewById(R.id.thread_listview);
		adapter = new ThreadListAdapter(this, this, this, R.layout.thread_list_item);
	    Bundle bndl;
	    if (null != savedInstanceState) {
	    	bndl = savedInstanceState;
			curBoard = Eisenheinrich.GLOBALS.getBoardCache().get(bndl.getLong("boardId"));
			Log.i("THREADLIST", "onCreate. Board: "+curBoard.shortName +" - "+curBoard.name);
			Thread t = new Thread (new KCPageParser(curBoard.uri, curBoard.dbId)
			.setBasePath(Defaults.BASE_URL)
			.setThreadHandler(Eisenheinrich.getInstance().getThreadListener())
			.setPostingHandler(Eisenheinrich.getInstance().getPostListener())
			);
		t.start();
	    } else {
	    	bndl = getIntent().getExtras();
	    	byte[] boardS = bndl.getByteArray("board");
		    ByteArrayInputStream bitch = new ByteArrayInputStream(boardS);
	    	ObjectInputStream in;
			try {
				in = new ObjectInputStream(bitch);
				curBoard = (KCBoard)in.readObject();
			} catch (StreamCorruptedException e) {
				Toast toast = Toast.makeText(this, "KCThreadListActivity::onCreate failed: "+e.getMessage(), Toast.LENGTH_LONG);
				toast.show();
			} catch (IOException e) {
				Toast toast = Toast.makeText(this, "KCThreadListActivity::onCreate failed: "+e.getMessage(), Toast.LENGTH_LONG);
				toast.show();
			} catch (ClassNotFoundException e) {
				Toast toast = Toast.makeText(this, "KCThreadListActivity::onCreate failed: "+e.getMessage(), Toast.LENGTH_LONG);
				toast.show();
			}
	    }
	    
	    cmdBar = (CommandBar) findViewById(R.id.command_bar);
	    
	    new AlertDialog.Builder(KCThreadListActivity.this)
        .setMessage (R.string.bord_unreachable)
        .setTitle(R.string.error_network)
        .setPositiveButton (android.R.string.yes, new OnClickListener () {
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				KCThreadListActivity.this.finish();
			}})
        .create();
	    
	    final Handler progressHandler = new Handler() {
	        public void handleMessage(Message msg) {
	        	if (0 == msg.arg1) {
	        		cmdBar.hideProgressBar();
	        	} else if (1 == msg.arg1) {
	        		cmdBar.incrementProgressBy(10);
	        	}
	        }
	    };
	    token = bndl.getString("token");
		
	    Timer tm = new Timer();
		tm.schedule(new TimerTask() {
			@Override
			public void run() {
				KCThreadListActivity.this.runOnUiThread(new Runnable () {
					@Override
					public void run() {	
					    adjustTitle ();
					}
			    }); 
			}
		}, 5000);
		list.setAdapter(adapter);
		heini.addThreadListener(new KODataListener<KCThread>() {
			@Override
			public void notifyAdded(final KCThread item, Object token) {
				if (KCThreadListActivity.this.token.equals(token)) {
		        	siteReachableWatchdog.cancel();
					// cannot add to the collection backing the adapter from a thread that is
					// not the UI thread. Therefore, post a runnable to UI thread to handle this
					runOnUiThread(new Runnable() {
				        public void run() {
							//threads.add(item);
							adapter.add(item);
							adapter.notifyDataSetChanged(); 
							cmdBar.incrementProgressBy(10);
				        }
				    });
				}
			}

			@Override
			public void notifyDone(Object token) {
				Message msg = progressHandler.obtainMessage();
	        	msg.arg1 = 0;
	        	progressHandler.sendMessage(msg);
			}
			
			@Override
			public void notifyError(Exception ex, Object token) {
				KCThreadListActivity.this.finish();
			}
		});
		

		list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View v, int position, long id) {
				if (null != siteReachableWatchdog) {
					siteReachableWatchdog.cancel();
				}
				/*Iterator<KCThread> iter = threads.iterator();
				KCThread curThread = null;
				boolean found = false;
				while (iter.hasNext() && !found) {
					curThread = iter.next();
					found = curThread.dbId == id;
				}*/
				KCThread curThread = KCThreadListActivity.this.getThread(id);
				if (null != curThread) {
					ActivityHelpers.switchToThread (curThread, KCThreadListActivity.this); 
				}
			}
		});
		registerForContextMenu(list);
		list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener(){ 
			@Override 
			public boolean onItemLongClick(AdapterView<?> av, View v, int pos, long id) { 
				Eisenheinrich.getInstance();
				/*Iterator<KCThread> iter = threads.iterator();
				KCThread curThread = null;
				boolean found = false;
				while (iter.hasNext() && !found) {
					curThread = iter.next();
					found = curThread.dbId == id;
				}
				if (found) {*/
				KCThread curThread = Eisenheinrich.GLOBALS.getThreadCache().get(id);
				if (null != curThread) {
					list.setTag(curThread);
				}
				openContextMenu(list);
			    return true;  // avoid extra click events
           } 
      }); 
	}

	/*@Override
	public KCThread getThread(long dbId) {
		Iterator<KCThread> iter = threads.iterator();
		while (iter.hasNext()) {
			KCThread t = iter.next();
			if (t.dbId == dbId) {
				return t;
			}
		}
		return null;
	}*/
	
	@Override
	public KCThread getThread(long dbId) {
		return Eisenheinrich.GLOBALS.getThreadCache().get(dbId);
	}
	
	@Override
	public KCBoard getBoard(long dbId) {
		return Eisenheinrich.GLOBALS.getBoardCache().get(dbId);
	}
	
	@Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
	super.onCreateContextMenu(menu, v, menuInfo);
		menu.setHeaderTitle("Context Menu");
		menu.add(0, v.getId(), 0, R.string.option_bookmark);
		menu.add(0, v.getId(), 0, R.string.option_hide);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		View v = this.findViewById(item.getItemId());
		KCThread thread = (KCThread) v.getTag();
		String title = (String)item.getTitle();
		if (title.equals(getString(R.string.option_bookmark))){	       		
			Eisenheinrich.getInstance().dbHelper.bookmarkThread(thread);
		} else if (title.equals(getString(R.string.option_hide))){
			adapter.hide(thread);
			thread.hidden = true;
			Eisenheinrich.getInstance().dbHelper.persistThread(thread);
		} else {
			return false;
		}
		return true;
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	    super.onConfigurationChanged(newConfig);
	    if ((newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) || (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)) {
	        adapter.notifyDataSetChanged();
	    } 
	}
	
	@Override
	protected void onStop() {
	    super.onStop();
	    list.setVisibility(View.GONE);
		Eisenheinrich.getInstance();
		Eisenheinrich.GLOBALS.getThreadCache().freeze();
	}

	@Override
	protected void onRestart() {
	    super.onRestart();
	    list.setVisibility(View.VISIBLE);
	    adapter.notifyDataSetChanged();
	    adjustTitle ();
	}
	

	@Override
	protected void onRestoreInstanceState(Bundle inState) {
		super.onRestoreInstanceState(inState);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putLong("boardId", curBoard.dbId);
		outState.putString("token", token);
		Eisenheinrich.getInstance();
		Eisenheinrich.GLOBALS.getThreadCache().freeze();
	}
	
	private void adjustTitle () {
		KCBoard board = Eisenheinrich.GLOBALS.getBoardCache().get(curBoard.dbId);
		if (null == board) {
			return;
		}
		title = "/"+curBoard.shortName+"/ - "+curBoard.name;
	    if (board.banned) {
	    	cmdBar.setTitle(title + " ("+this.getString(R.string.banned)+")");
		} else {
			cmdBar.setTitle(title);
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.options_menu_threadlist, menu);
	    return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.go_thread:
				new GoToThreadDialog (this, curBoard, Eisenheinrich.getInstance().getHttpClient()).showDialog();
				return true;
			case R.id.bookmark:
				return true;
			case R.id.reload: {
				//threads.clear(); 
				adapter.clear();
				adapter.notifyDataSetInvalidated();
				new Thread (new KCPageParser(curBoard.uri, curBoard.dbId)
					.setBasePath("http://krautchan.net/")
					.setThreadHandler(Eisenheinrich.getInstance().getThreadListener())
					.setPostingHandler(Eisenheinrich.getInstance().getPostListener())
					).start();
				cmdBar.showProgressBar();
				return true; 
			}
			case R.id.new_thread: {
				KCBoard board = Eisenheinrich.GLOBALS.getBoardCache().get(curBoard.dbId);
				if ((board.banned) && (null == Eisenheinrich.GLOBALS.getKomturCode())) {
					new BannedDialog (this).show();
					Toast.makeText(KCThreadListActivity.this, R.string.banned_message, Toast.LENGTH_LONG).show();
				} else {
					ActivityHelpers.createThreadMask (null, curBoard.dbId, "", this);
				}
				return true; 
			}
			case R.id.prefs:
				return true;
			case R.id.forward:
				return true;
			case R.id.home:
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}


}
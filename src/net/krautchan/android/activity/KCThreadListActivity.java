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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

import net.krautchan.R;
import net.krautchan.android.Eisenheinrich;
import net.krautchan.android.dialog.GoToThreadDialog;
import net.krautchan.android.helpers.ActivityHelpers;
import net.krautchan.data.KCBoard;
import net.krautchan.data.KCPosting;
import net.krautchan.data.KCThread;
import net.krautchan.data.KODataListener;
import net.krautchan.parser.KCPageParser;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class KCThreadListActivity extends Activity {
	protected static final String TAG = "KCThreadListActivity";
	ListView list = null;
	private Eisenheinrich heini = Eisenheinrich.getInstance();
	private ProgressBar progress = null;
	ThreadListAdapter adapter = null;
	KCBoard curBoard;
	//TODO at some point, factor this out into a cache
	List<KCThread> threads = new CopyOnWriteArrayList<KCThread>();
	private Timer siteReachableWatchdog = new Timer();
	private AlertDialog siteDownDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);         
		setContentView(R.layout.thread_list);
		list = (ListView)findViewById(R.id.thread_listview);
		adapter = new ThreadListAdapter(this, this, 0, 0);
	    Bundle b = getIntent().getExtras();
	    byte[] boardS = b.getByteArray("board");
	    ByteArrayInputStream bitch = new ByteArrayInputStream(boardS);
	    ObjectInputStream in;
	    final View progWrapper = findViewById(R.id.threadlist_watcher_wrapper);
	    progress = (ProgressBar)findViewById(R.id.threadlist_watcher);
	    progress.setMax(100);
	    progress.setProgress(0);
	    siteDownDialog = new AlertDialog.Builder(KCThreadListActivity.this)
        .setMessage ("Der Krautkanal ist nicht erreichbar oder dein Netz ist unten")
		//.setView(myView)
        .setTitle("KC Down")
        .setPositiveButton (android.R.string.yes, new OnClickListener () {
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				KCThreadListActivity.this.finish();
			}})
        .create();
	    //TODO den Watchdog-Timer auch beim onRestart- und so weiter handler einbauen
	    /*siteReachableWatchdog.schedule(new TimerTask () {
			@Override
			public void run() {
				KCThreadListActivity.this.runOnUiThread(new Runnable() {
				    public void run() {
				    	siteDownDialog.show();
				    }
				});
			}
	    	
	    }, 10000);*/
	    final Handler progressHandler = new Handler() {
	        public void handleMessage(Message msg) {
	        	if (0 == msg.arg1) {
	        		progWrapper.setVisibility(View.GONE);
				    progress.setMax(100);
				    progress.setProgress(0);
	        	} else if (1 == msg.arg1) {
		        	progress.incrementProgressBy(10);
	        	}
	        }
	    };
		try {
			in = new ObjectInputStream(bitch);
		    curBoard = (KCBoard)in.readObject();
		    this.setTitle("/"+curBoard.shortName+"/ - "+curBoard.name);
			list.setAdapter(adapter);
			heini.addThreadListener(new KODataListener<KCThread>() {
				@Override
				public void notifyAdded(final KCThread item) {
		        	siteReachableWatchdog.cancel();
					// cannot add to the collection backing the adapter from a thread that is
					// not the UI thread. Therefore, post a runnable to UI thread to handle this
					runOnUiThread(new Runnable() {
				        public void run() {
				        	item.uri = Eisenheinrich.DEFAULTS.BASE_PATH+"/"+curBoard.shortName+"/"+item.kcNummer+".html";
							threads.add(item);
				        	adapter.add(item);
				        	adapter.notifyDataSetChanged();  
				        	progress.incrementProgressBy(10);
				        }
				    });
				}

				@Override
				public void notifyDone() {
					Message msg = progressHandler.obtainMessage();
		        	msg.arg1 = 0;
		        	progressHandler.sendMessage(msg);
				}
				
				@Override
				public void notifyError(Exception ex) {
					KCThreadListActivity.this.finish();
				}
			});
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

		list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View v, int position, long id) {
				if (null != siteReachableWatchdog) {
					siteReachableWatchdog.cancel();
				}
				Iterator<KCThread> iter = threads.iterator();
				KCThread curThread = null;
				boolean found = false;
				while (iter.hasNext() && !found) {
					curThread = iter.next();
					found = curThread.dbId == id;
				}
				if (null != curThread) {
					ActivityHelpers.switchToThread (curThread, curBoard.shortName, curBoard.dbId, KCThreadListActivity.this);
				}
			}
		});
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
	}

	@Override
	protected void onRestart() {
	    super.onRestart();
	    list.setVisibility(View.VISIBLE);
	}
	
	
	final class ThreadListAdapter extends ArrayAdapter<KCThread> {
		private final KCThreadListActivity kcThreadListActivity;
		ArrayList<Long> ids = new ArrayList<Long>();
		
		ThreadListAdapter(KCThreadListActivity kcThreadListActivity, Context context, int resource,
				int textViewResourceId) {
			super(context, resource, textViewResourceId);
			this.kcThreadListActivity = kcThreadListActivity;
		}
		
		@Override
		public void add(KCThread thread) {
			if (!ids.contains(thread.dbId)) {
				ids.add(thread.dbId);
				super.add(thread);
			}
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = null;
			if (null == convertView) {
				v = this.kcThreadListActivity.getLayoutInflater().inflate(R.layout.thread_list_item, null);
			} else {
				v = convertView;
			}
			//TODO fix the way we access threads here - stop iterating each time
			Iterator<KCThread> iter = threads.iterator();
			KCThread item = null;
			int count = 0;
			while (iter.hasNext() && count++ <= position) {
				item = iter.next();
			}
			if (item == null) {
				TextView shortLabel = (TextView) v.findViewById(R.id.threadListNumber);
				shortLabel.setText("Nothing found");
			} else {
				TextView numberLabel = (TextView) v.findViewById(R.id.threadListNumber);
				String numLabel = "null";
				if (null != item.kcNummer) {
					numLabel = item.kcNummer.toString();
				};
				numberLabel.setText(numLabel);
				Collection<Long> postIds = item.getIds();
				if (postIds.isEmpty()) {
					numberLabel.setText("No Posting found");
				} else {
					KCPosting post = item.getFirstPosting();
					if (null != post) {
						TextView titleLabel = (TextView) v.findViewById(R.id.threadListTitle);
						if ((null != post.title) && (post.title.length() != 0)) {
							titleLabel.setText(post.title);
						} else {
							titleLabel.setVisibility(View.GONE);
						}
						TextView dateLabel = (TextView) v.findViewById(R.id.threadListDate);
						dateLabel.setText(post.creationShortDate);
						TextView authorLabel = (TextView) v.findViewById(R.id.threadListAuthor);
						authorLabel.setText(post.user);
						TextView contentLabel = (TextView) v.findViewById(R.id.threadListContent);
						contentLabel.setText(item.digest);
					}
				}
			}
			return v;
		}
		
		
		@Override
		public int getCount() {
			return threads.size();
		}

		@Override
		public long getItemId(int position) {
			Iterator<KCThread> iter = threads.iterator();
			if (null == iter)
				return -1;
			KCThread item = null;
			int count = 0;
			while (iter.hasNext() && count++ <= position) {
				item = iter.next();
			}
			if (null == item)
				return -1;
			return item.dbId;
		}

		
		@Override
		public boolean hasStableIds() {
			return true;
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
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.go_thread:
			new GoToThreadDialog (this, curBoard, Eisenheinrich.getInstance().getHttpClient()).showDialog();
			return true;
		case R.id.bookmark:
			return true;
		case R.id.reload: {
			threads.clear(); 
			adapter.notifyDataSetInvalidated();
			new Thread (new KCPageParser()
				.setBasePath("http://krautchan.net/")
				.setUrl("http://krautchan.net/board/"+curBoard.shortName+"/0")
				.setThreadHandler(Eisenheinrich.getInstance().getThreadListener())
				.setPostingHandler(Eisenheinrich.getInstance().getPostListener())
				).start();
			View progWrapper = findViewById(R.id.threadlist_watcher_wrapper);
			progWrapper.setVisibility(View.VISIBLE);
			return true; 
		}
		case R.id.new_thread: {
			ActivityHelpers.createThreadMask (null, curBoard.shortName, this);
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
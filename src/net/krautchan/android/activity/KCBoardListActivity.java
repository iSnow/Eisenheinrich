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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import net.krautchan.android.Eisenheinrich;
import net.krautchan.data.KCBoard;
import net.krautchan.parser.KCBoardListParser;
import net.krautchan.parser.KCPageParser;
import net.krautchan.R;
import android.app.ListActivity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class KCBoardListActivity extends ListActivity {
	protected static final String TAG = "KCBoardListActivity";
	private Map<String, KCBoard> boards = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// TODO Move part of the code into the DatabaseHelper, no need to copy between collection and map all the time
		boolean gotBoards = false;
		try {
			boards = new LinkedHashMap<String, KCBoard>();
			Collection<KCBoard> boardL = Eisenheinrich.getInstance().dbHelper.getBoards();
			for (KCBoard board : boardL) {
				boards.put(board.shortName, board);
			}
			gotBoards = boards.size() > 0;
		} catch (Exception ex) {
			gotBoards = false;
		}
		
		if (!gotBoards) {
			try {
				InputStream is = this.getAssets().open("nav.html");
				BufferedReader r = new BufferedReader(new InputStreamReader(is));
				StringBuilder builder = new StringBuilder();
				String line;
				while ((line = r.readLine()) != null) {
					builder.append(line); 
				}
				r.close();
				r = null;
				String nav = builder.toString();
				
				boards = KCBoardListParser.getBoardList(nav);
			} catch (IOException e) {
				String[] mStrings = new String[] { "Exception", e.getMessage() };
				setListAdapter(new ArrayAdapter<String>(this, R.layout.board_list_item, mStrings) {
					@Override
					public View getView(int position, View convertView, ViewGroup parent) {
						View row = getLayoutInflater().inflate(R.layout.board_list_item, null);
						((TextView) row).setText(getItem(position));
						return row;
					}
				});
			}
		}
		setListAdapter(new BoardListAdapter(this, boards));
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	    super.onConfigurationChanged(newConfig);
	    // Checks the orientation of the screen
	    if ((newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) || (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)) {
	        BoardListAdapter ad = (BoardListAdapter)this.getListAdapter();
	        ad.notifyDataSetChanged();
	    } 
	}
	
	@Override
	protected void onRestart() {
	    super.onRestart();
	    setFilteredBoards();
		Eisenheinrich.getInstance().dbHelper.persistBoards(boards);
	    checkNetwork();
	}


	@Override
	protected void onResume() {
		super.onResume();
		setFilteredBoards();
		Eisenheinrich.getInstance().dbHelper.persistBoards(boards);
		checkNetwork();
	}

	@Override
	protected void onListItemClick(ListView lv, View v, int position, long id) {
		ByteArrayOutputStream boss = new ByteArrayOutputStream ();
		ObjectOutputStream out;
		try {
			out = new ObjectOutputStream(boss);

			Iterator<String> iter = boards.keySet().iterator();
			String key = null;
			KCBoard board = null;
			boolean found = false;
			while (iter.hasNext() && !found) {
				key = iter.next();
				board = boards.get(key);
				found = board.dbId == id;
			}
			out.writeObject(board);
			out.close();
			boss.close();
			final KCBoard curBoard = board;
			Thread t = new Thread (new KCPageParser()
				.setBasePath("http://krautchan.net/")
				.setUrl("http://krautchan.net/board/"+curBoard.shortName+"/0")
				.setThreadHandler(Eisenheinrich.getInstance().getThreadListener())
				.setPostingHandler(Eisenheinrich.getInstance().getPostListener())
				);
			t.start();

			Bundle b = new Bundle();
			b.putByteArray("board", boss.toByteArray());

			Intent intent = new Intent(this, KCThreadListActivity.class);
			intent.putExtras(b);
			startActivity(intent); 
		} catch (IOException e) {
			Toast toast = Toast.makeText(this, "KCBoardListActivity::onListItemClick failed: "+e.getMessage(), Toast.LENGTH_LONG);
			toast.show();
		}
	}

	private void setFilteredBoards () {
		List<String> selectedBoards = Eisenheinrich.getInstance().getSelectedBoards();
	    if (null != selectedBoards) {
		    LinkedHashMap<String, KCBoard> newBoards = new LinkedHashMap<String, KCBoard>();
		    for (String key: boards.keySet()) {
		    	KCBoard board = boards.get(key);
		    	if (selectedBoards.contains(board.shortName)) {
		    		board.show = true;
		    		newBoards.put(key, board);
		    	} else {
		    		board.show = false;
		    	}
		    }
		    BoardListAdapter ad = (BoardListAdapter)this.getListAdapter();
		    ad.setBoards(newBoards);
	        ad.notifyDataSetChanged();
	    }
	}
	
	private void checkNetwork() {
		 if (!Eisenheinrich.getInstance().isNetworkAvailable()) {
		    	Timer tm = new Timer();
				tm.schedule(new TimerTask() {
					@Override
					public void run() {
				    	KCBoardListActivity.this.runOnUiThread(new Runnable () {
							@Override
							public void run() {	
								Toast toast = Toast.makeText(KCBoardListActivity.this, "Kein Netz", Toast.LENGTH_LONG);	
								toast.show();
							}
					    }); 
					}
				}, 1000);
		    }
	}

	protected class BoardListAdapter extends ArrayAdapter<String> {
		private final static int viewId = R.layout.board_list_item;
		private LayoutInflater inflater;
		Map<String, KCBoard> rowVals;

		public BoardListAdapter(ListActivity context, Map<String, KCBoard> boards) {
			super(context, viewId);
			this.inflater = context.getLayoutInflater();
			this.rowVals = boards;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = null;
			if (null == convertView) {
				v = inflater.inflate(R.layout.board_list_item, null);
				TextView shortLabel = (TextView) v.findViewById(R.id.boardlist_shortname);
				TextView longLabel = (TextView) v.findViewById(R.id.boardlist_longname);
				shortLabel.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/juicebold.ttf"));
				longLabel.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/juiceregular.ttf"));
			} else {
				v = convertView;
			}

			TextView shortLabel = (TextView) v.findViewById(R.id.boardlist_shortname);
			shortLabel.setText(getShortName(position));

			TextView longLabel = (TextView) v.findViewById(R.id.boardlist_longname);
			longLabel.setText(rowVals.get(getShortName(position)).name);
			return v;
		}


		@Override
		public int getCount() {
			return rowVals.size();
		}

		@Override
		public long getItemId(int position) {
			Iterator<String> iter = rowVals.keySet().iterator();
			String key = null;
			int count = 0;
			while (iter.hasNext() && count++ <= position) {
				key = iter.next();
			}
			return rowVals.get(key).dbId;
		}

		private String getShortName(int position) {
			if (position > rowVals.size()) {
				throw new IllegalArgumentException(
				"BoardListAdapter:: getShortName -> position > rowVals");
			}
			Iterator<String> iter = rowVals.keySet().iterator();
			int pos = 0;
			while (pos < position) {
				iter.next();
				pos++;
			}
			String key = iter.next();
			return key;
		}
		
		public void setBoards (Map<String, KCBoard> boards) {
			this.rowVals = boards;
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.options_menu_boardlist, menu);
	    return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.forward:
			return true;
		case R.id.reload:
			return true;
		case R.id.edit_boards:
			KCBoardListActivity.this.startActivity(new Intent(KCBoardListActivity.this, KCEditBoardListActivity.class));
			return true;
		case R.id.prefs:
			return true;
		case R.id.home:
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

}
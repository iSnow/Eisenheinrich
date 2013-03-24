package net.krautchan.android.dialog;

/*
* Copyright (C) 2013 Johannes Jander (johannes@jandermail.de)
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

import java.util.Iterator;
import java.util.List;

import net.krautchan.R;
import net.krautchan.android.Eisenheinrich;
import net.krautchan.android.activity.ProvidesBoards;
import net.krautchan.android.activity.ProvidesThreads;
import net.krautchan.android.helpers.ActivityHelpers;
import net.krautchan.android.widget.ThreadListAdapter;
import net.krautchan.data.KCBoard;
import net.krautchan.data.KCThread;
import android.app.Activity;
import android.app.AlertDialog;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class ThreadHistoryDialog  implements ProvidesThreads, ProvidesBoards {
	private Activity parentActivity; 
	private String title;
	private AlertDialog dialog;
	
	public ThreadHistoryDialog (Activity parent) {
		this.parentActivity = parent;
	}
	
	public void show () {
		View dialogView = parentActivity.getLayoutInflater().inflate(R.layout.thread_history_dialog, null);

		dialog = new AlertDialog.Builder(parentActivity) 
    	.setView(dialogView)
        .setTitle(title)
		.create();
        dialog.show();

		final ThreadListAdapter adapter = new ThreadListAdapter(this, this, dialogView.getContext(), R.layout.thread_list_item);
		ListView list = (ListView) dialog.findViewById(R.id.thread_dialog_listview);
		list.setAdapter(adapter);
		List<KCThread> threads = Eisenheinrich.GLOBALS.getThreadCache().getAll();
		for (int i = threads.size()-1; i >= 0; i--) {
			adapter.add(threads.get(i));
		}
		adapter.notifyDataSetChanged();
		list.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				dialog.dismiss();
				KCThread t = adapter.getItem(position);
				if (null != t) {
					ActivityHelpers.switchToThread (t, parentActivity); 
				}
			}
		});
	}
	
	
	@Override
	public KCBoard getBoard(long dbId) {
		Iterator<KCBoard> iter = Eisenheinrich.GLOBALS.getBoardCache().getAll().iterator();
		while (iter.hasNext()) {
			KCBoard b = iter.next();
			if (b.dbId == dbId) {
				return b;
			}
		}
		return null;
	}

	@Override
	public KCThread getThread(long dbId) {
		Iterator<KCThread> iter = Eisenheinrich.GLOBALS.getThreadCache().getAll().iterator();
		while (iter.hasNext()) {
			KCThread t = iter.next();
			if (t.dbId == dbId) {
				return t;
			}
		}
		return null;
	}
	
	/*final static protected class ThreadListAdapter extends ArrayAdapter<KCThread> {
		private static SimpleDateFormat dfShort = new SimpleDateFormat ("dd.MM. HH:mm");
		private final ThreadHistoryDialog kcThreadListActivity;
		private ArrayList<Long> ids = new ArrayList<Long>();
		private LayoutInflater mInflater;
		private int mViewResourceId;

		ThreadListAdapter(ThreadHistoryDialog kcThreadListActivity, Context context, int textViewResourceId) {
			super(context,  textViewResourceId);
			this.kcThreadListActivity = kcThreadListActivity;
			mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			mViewResourceId = textViewResourceId;
		}

		@Override
		public void add(KCThread thread) {
			if ((!ids.contains(thread.dbId)) && (!thread.hidden)) {
				ids.add(ids.size(), thread.dbId);
				super.add(thread);
			}
		}

		public void remove (KCThread thread) {
			ids.remove(thread.dbId);
			super.remove(thread);
		}

		public void hide (KCThread thread) {
			remove(thread);
			notifyDataSetChanged();
		}

		public void clear () {
			ids.clear();
		}

		@Override
		public View getDropDownView (int position, View convertView, ViewGroup parent) {
			return getView(position, convertView, parent);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = null;
			if (null == convertView) {
				//v = mInflater.inflate(mViewResourceId, null);
				v = mInflater.inflate(mViewResourceId, null);
			} else {
				v = convertView;
			}
			Long id = ids.get(position);
			KCThread thread = kcThreadListActivity.getThread (id);
			if (thread == null) {
				TextView shortLabel = (TextView) v.findViewById(R.id.threadListNumber);
				shortLabel.setText("Nothing found");
			} else {
				TextView numberLabel = (TextView) v.findViewById(R.id.threadListNumber);
				String numLabel = "null";
				if (null != thread.kcNummer) {
					numLabel = thread.kcNummer.toString();
				};
				numberLabel.setText(numLabel);

				TextView dateLabel = (TextView) v.findViewById(R.id.threadListDate);
				dateLabel.setText(dfShort.format(thread.firstPostDate));

				TextView contentLabel = (TextView) v.findViewById(R.id.threadListContent);
				contentLabel.setText(thread.digest);
				TextView numPostsLabel = (TextView) v.findViewById(R.id.threadListNumPostings);
				numPostsLabel.setText(" "+thread.numPostings+ " Posts");

			}
			return v;
		}


		@Override
		public int getCount() {
			return ids.size();
		}

		@Override
		public long getItemId(int position) {
			Iterator<Long> iter = ids.iterator();
			int count = 0;
			long id = -1;
			while (iter.hasNext() && count++ <= position) {
				id = iter.next();
				while (iter.hasNext() && count <= position) {
					id = iter.next();
				}
			}
			return id; 
			
		}


		@Override
		public boolean hasStableIds() {
			return true;
		}
	}*/

	
}

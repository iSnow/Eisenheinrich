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

import java.util.Collections;
import java.util.Comparator;
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
	private AlertDialog dialog;
	
	public ThreadHistoryDialog (Activity parent) {
		this.parentActivity = parent;
	}
	
	public void show () {
		View dialogView = parentActivity.getLayoutInflater().inflate(R.layout.thread_history_dialog, null);

		dialog = new AlertDialog.Builder(parentActivity) 
    	.setView(dialogView)
        .setTitle(R.string.thread_history)
		.create();
        dialog.show();

		final ThreadListAdapter adapter = new ThreadListAdapter(this, this, dialogView.getContext(), R.layout.cmdbar_history_item);
		ListView list = (ListView) dialogView.findViewById(R.id.thread_dialog_listview);
		list.setAdapter(adapter);
		List<KCThread> threads = Eisenheinrich.GLOBALS.getThreadCache().getAll();
 		Collections.sort(threads, new Comparator<KCThread>() {
			@Override
			public int compare(KCThread lhs, KCThread rhs) {
				if ((rhs.visited == null) || (lhs.visited == null)) {
					return 0;
				}
				return (rhs.visited.compareTo(lhs.visited));
			}});
		adapter.clear();
		for (KCThread t : threads) {
			if (t.visited != null) {
				adapter.add(t);
			}
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
		return  Eisenheinrich.GLOBALS.getBoardCache().get(dbId);
	}

	@Override
	public KCThread getThread(long dbId) {
		return Eisenheinrich.GLOBALS.getThreadCache().get(dbId);
	}
	
	
}

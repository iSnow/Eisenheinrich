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
		//TextView v2 = ((TextView)dialogView.findViewById(R.id.threadListDate));
		//v2.setTextColor(parentActivity.getResources().getColor(R.color.LightGrey));
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
	
	
}

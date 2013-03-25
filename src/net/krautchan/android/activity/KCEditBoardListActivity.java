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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.krautchan.R;
import net.krautchan.android.Eisenheinrich;
import net.krautchan.android.helpers.CustomExceptionHandler;
import net.krautchan.data.KCBoard;
import android.app.Activity;
import android.app.ListActivity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Activity class for hiding and showing boards from the KCBoardListActivity
 * @author snow
 *
 */
public class KCEditBoardListActivity extends ListActivity {
	protected static final String TAG = "KCEditBoardListActivity";
	private Map<String, KCBoard> boards = null;
	private ListView list;
	private EditBoardListAdapter adapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler(
		        "eisenheinrich", "http://eisenheinrich.datensalat.net:8080/Eisenweb/upload/logfile/test", this));

		this.setContentView(R.layout.edit_board_list_view);
		list = getListView();
		list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		list.setItemsCanFocus(false);
		
		// TODO Move part of the code into the DatabaseHelper, no need to copy between collection and map all the time
		ArrayList<KCBoard> boardsL  = new ArrayList<KCBoard>();
		boolean gotBoards = false;
		try {
			boards = new LinkedHashMap<String, KCBoard>();
			boardsL.addAll(Eisenheinrich.getInstance().dbHelper.getBoards());
			for (KCBoard board : boardsL) {
				boards.put(board.shortName, board);
			}
			gotBoards = boards.size() > 0;
		} catch (Exception ex) {
			gotBoards = false;
		}
		
		if (!gotBoards) {
			this.finish();
		}
		adapter = new EditBoardListAdapter(this, boards);
		list.setAdapter(adapter);
		final int itemCount = list.getCount();
		for (int i = 0; i < itemCount; ++i) {
			list.setItemChecked(i, boardsL.get(i).show);
		}
		list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View v, int position, long id) {
				System.out.println (position);
			}
		});
		Button okButton = (Button)findViewById(R.id.edit_board_list_button_ok);
	    okButton.setOnClickListener(new View.OnClickListener() {
		    public void onClick(View v) {
		    	final SparseBooleanArray checkedItems = list.getCheckedItemPositions();
				if (checkedItems != null) {		
					List<KCBoard> storedBoards = Eisenheinrich.GLOBALS.getBoardCache().getAll();
					for (KCBoard board : storedBoards) {
						board.show = false; 
					}
					final int checkedItemsCount = checkedItems.size();
					for (int i = 0; i < checkedItemsCount; ++i) {
						final int position = checkedItems.keyAt(i);
						
						KCBoard board = Eisenheinrich.GLOBALS.getBoardCache().get(adapter.getItemId(position));
						board.show = checkedItems.valueAt(i);
					}
				};
		    	Eisenheinrich.GLOBALS.getBoardCache().freeze();
		    	KCEditBoardListActivity.this.finish();
		    }
	    });
	    
	    Button cancelButton = (Button)findViewById(R.id.edit_board_list_button_cancel);
	    cancelButton.setOnClickListener(new View.OnClickListener() {
		    public void onClick(View v) {
		    	KCEditBoardListActivity.this.finish();
		    }
	    });
	    
	    Button toggleButton = (Button)findViewById(R.id.edit_board_list_button_toggle);
	    toggleButton.setOnClickListener(new View.OnClickListener() {
		    public void onClick(View v) {
		    	toggleFullSelection();
		    }
	    });
	}
	
	
	private List<String> getSelectedItems() {
		final SparseBooleanArray checkedItems = list.getCheckedItemPositions();
		List<String> selectedBoards = new ArrayList<String>();
		if (checkedItems != null) {		
			final int checkedItemsCount = checkedItems.size();
			for (int i = 0; i < checkedItemsCount; ++i) {
				final int position = checkedItems.keyAt(i);
				final boolean isChecked = checkedItems.valueAt(i);
				KCBoard board = Eisenheinrich.GLOBALS.getBoardCache().get(adapter.getItemId(position));
				/*if (isChecked) {
					selectedBoards.add(adapter.getShortName(position));
				}*/
			}
		}
		if (selectedBoards.size() == 0) {
			return null;
		}
		return selectedBoards;
	}
	
	private void toggleFullSelection() {
		/*fullSelectionState = !fullSelectionState;
		final int itemCount = list.getCount();
		for (int i = 0; i < itemCount; ++i) {
			list.setItemChecked(i, fullSelectionState);
		}*/
	}
	
	
	protected class EditBoardListAdapter extends ArrayAdapter<String> {
		private LayoutInflater inflater;
		Map<String, KCBoard> rowVals;

		public EditBoardListAdapter(Activity context, Map<String, KCBoard> boards) {
			super(context, android.R.layout.simple_list_item_multiple_choice);
			this.inflater = context.getLayoutInflater();
			this.rowVals = boards;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = null;
			if (null == convertView) {
				v = inflater.inflate(R.layout.edit_board_list_item, null);
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
			//((InertCheckBox)v.findViewById(R.id.boardlist_hide_item)).setChecked(rowVals.get(getShortName(position)).show);
			//((CheckableLinearLayout)v).setChecked(rowVals.get(getShortName(position)).show);
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
		
		

		@Override
		public boolean hasStableIds() {
			return true;
		}

		public String getShortName(int position) {
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
	}
}

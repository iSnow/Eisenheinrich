package net.krautchan.android.activity;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.krautchan.R;
import net.krautchan.android.Eisenheinrich;
import net.krautchan.data.KCThread;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class SpinnerActivity extends Activity {
	protected int mPos;
	protected String mSelection;
	protected ThreadListAdapter mAdapter;
	public static final int DEFAULT_POSITION = 2;
	public static final String PREFERENCES_FILE = "SpinnerPrefs";
	public static final String PROPERTY_DELIMITER = "=";
	public static final String POSITION_KEY = "Position";
	public static final String SELECTION_KEY = "Selection";
	public static final String POSITION_MARKER = POSITION_KEY + PROPERTY_DELIMITER;
	public static final String SELECTION_MARKER = SELECTION_KEY + PROPERTY_DELIMITER;

	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.thread_history_spinner);
		Spinner spinner = (Spinner) findViewById(R.id.thread_history_spinner_view);

		mAdapter = new ThreadListAdapter(this, this, R.layout.thread_list, R.layout.spinner_list_item);
		spinner.setAdapter(mAdapter);
		OnItemSelectedListener spinnerListener = new ThreadHistoryItemSelectedListener(this,this.mAdapter);

		spinner.setOnItemSelectedListener(spinnerListener);


		TextView history = (TextView) findViewById(R.id.board_list_history2);
		history.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/fontawesome-webfont.ttf"));
		
		List<KCThread> threads = Eisenheinrich.GLOBALS.getThreadCache().getAll();
		for (int i = threads.size()-1; i >= 0; i--) {
			mAdapter.add(threads.get(i));
		}
	}


	public class ThreadHistoryItemSelectedListener implements OnItemSelectedListener {
		ThreadListAdapter mLocalAdapter;
		Context mLocalContext;

		public ThreadHistoryItemSelectedListener(Context c, ThreadListAdapter ad) {
			this.mLocalContext = c;
			this.mLocalAdapter = ad;
		}

		public void onItemSelected(AdapterView<?> parent, View v, int pos, long row) {
			SpinnerActivity.this.mPos = pos;
			SpinnerActivity.this.mSelection = parent.getItemAtPosition(pos).toString();
			TextView resultText = (TextView)findViewById(R.id.SpinnerResult2);
			resultText.setText(SpinnerActivity.this.mSelection);
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (!writeInstanceState(this)) {
			Toast.makeText(this,
					"Failed to write state!", Toast.LENGTH_LONG).show();
		}
	}
	

	@Override
	public void onResume() {
		super.onResume();
		if (!readInstanceState(this)) setInitialState();
		Spinner restoreSpinner = (Spinner)findViewById(R.layout.thread_history_spinner);
		//restoreSpinner.setSelection(getSpinnerPosition());
	}

	public void setInitialState() {
		this.mPos = DEFAULT_POSITION;
	}

	public boolean readInstanceState(Context c) {
		SharedPreferences p = c.getSharedPreferences(PREFERENCES_FILE, MODE_WORLD_READABLE);
		this.mPos = p.getInt(POSITION_KEY, SpinnerActivity.DEFAULT_POSITION);
		this.mSelection = p.getString(SELECTION_KEY, "");
		return (p.contains(POSITION_KEY));
	}

	public boolean writeInstanceState(Context c) {
		SharedPreferences p = c.getSharedPreferences(SpinnerActivity.PREFERENCES_FILE, MODE_WORLD_READABLE);
		SharedPreferences.Editor e = p.edit();
		e.putInt(POSITION_KEY, this.mPos);
		e.putString(SELECTION_KEY, this.mSelection);
		return (e.commit());
	}

	public int getSpinnerPosition() {
		return this.mPos;
	}

	public void setSpinnerPosition(int pos) {
		this.mPos = pos;
	}

	public String getSpinnerSelection() {
		return this.mSelection;
	}

	public void setSpinnerSelection(String selection) {
		this.mSelection = selection;
	}

	protected KCThread getThread(long dbId) {
		Eisenheinrich.getInstance();
		Iterator<KCThread> iter = Eisenheinrich.GLOBALS.getThreadCache().getAll().iterator();
		while (iter.hasNext()) {
			KCThread t = iter.next();
			if (t.dbId == dbId) {
				return t;
			}
		}
		return null;
	}


	final static class ThreadListAdapter extends ArrayAdapter<KCThread> {
		private static SimpleDateFormat dfShort = new SimpleDateFormat ("dd.MM. HH:mm");
		private final SpinnerActivity kcThreadListActivity;
		ArrayList<Long> ids = new ArrayList<Long>();
		private LayoutInflater mInflater;
		private int mViewResourceId;

		ThreadListAdapter(SpinnerActivity kcThreadListActivity, Context context, int resource,
				int textViewResourceId) {
			super(context, textViewResourceId);
			this.kcThreadListActivity = kcThreadListActivity;
			mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			mViewResourceId = textViewResourceId;
		}

		@Override
		public void add(KCThread thread) {
			if ((!ids.contains(thread.dbId)) && (!thread.hidden)) {
				//ids.add(thread.dbId);
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
				v = mInflater.inflate(mViewResourceId, null);
			} else {
				v = convertView;
			}
			Long id = ids.get(position);
			KCThread thread = kcThreadListActivity.getThread (id);
			if (thread == null) {
				TextView shortLabel = (TextView) v.findViewById(R.id.threadSpinnerListNumber);
				shortLabel.setText("Nothing found");
			} else {
				TextView numberLabel = (TextView) v.findViewById(R.id.threadSpinnerListNumber);
				String numLabel = "null";
				if (null != thread.kcNummer) {
					numLabel = thread.kcNummer.toString();
				};
				numberLabel.setText(numLabel);

				TextView dateLabel = (TextView) v.findViewById(R.id.threadSpinnerListDate);
				dateLabel.setText(dfShort.format(thread.firstPostDate));

				TextView contentLabel = (TextView) v.findViewById(R.id.threadSpinnerListContent);
				contentLabel.setText(thread.digest);
				TextView numPostsLabel = (TextView) v.findViewById(R.id.threadSpinnerListNumPostings);
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
			/*Iterator<KCThread> iter = threads.iterator();
			if (null == iter)
				return -1;
			KCThread item = null;
			int count = 0;
			while (iter.hasNext() && count++ <= position) {
				item = iter.next();
			}
			if (null == item)
				return -1;
			return item.dbId;*/
		}


		@Override
		public boolean hasStableIds() {
			return true;
		}
	}

}
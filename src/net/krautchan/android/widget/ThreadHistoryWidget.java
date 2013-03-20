package net.krautchan.android.widget;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.krautchan.R;
import net.krautchan.android.Eisenheinrich;
import net.krautchan.data.KCThread;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

public class ThreadHistoryWidget extends ViewGroup {
	protected int mPos;
	protected String mSelection;
	protected ThreadListAdapter adapter;
	private Spinner spinner;

	public ThreadHistoryWidget(Context context) {
		super(context);
		prepare(context);
	}

	public ThreadHistoryWidget(Context context, AttributeSet attrs) {
		super(context, attrs);
		prepare(context);
	}

	public ThreadHistoryWidget(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		prepare(context);
	}
	
	protected void prepare(Context context) {
	    LayoutInflater inflater = (LayoutInflater) context
	        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    View v = inflater.inflate(R.layout.thread_history_widget, this, true);
	
		spinner = (Spinner) v.findViewById(R.id.thread_history_spinner_view);
		adapter = new ThreadListAdapter(this, v.getContext(), R.layout.spinner_list_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		OnItemSelectedListener spinnerListener = new ThreadHistoryItemSelectedListener(v.getContext(), adapter);
		spinner.setOnItemSelectedListener(spinnerListener);
		
	
		List<KCThread> threads = Eisenheinrich.GLOBALS.getThreadCache().getAll();
		for (int i = threads.size()-1; i >= 0; i--) {
			adapter.add(threads.get(i));
		}
		adapter.notifyDataSetChanged();
	}


	protected class ThreadHistoryItemSelectedListener implements OnItemSelectedListener {
		ThreadListAdapter mLocalAdapter;
		Context mLocalContext;

		public ThreadHistoryItemSelectedListener(Context c, ThreadListAdapter ad) {
			this.mLocalContext = c;
			this.mLocalAdapter = ad;
		}

		public void onItemSelected(AdapterView<?> parent, View v, int pos, long row) {
			ThreadHistoryWidget.this.mPos = pos;
			ThreadHistoryWidget.this.mSelection = parent.getItemAtPosition(pos).toString();
			TextView resultText = (TextView)findViewById(R.id.SpinnerResult2);
			resultText.setText(ThreadHistoryWidget.this.mSelection);
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
			System.out.println ("VOID");
			
		}
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
	

	@Override
	protected void onLayout(boolean arg0, int arg1, int arg2, int arg3, int arg4) {
		spinner.layout(arg1, arg2, arg3, arg4);
	}


	final static protected class ThreadListAdapter extends ArrayAdapter<KCThread> {
		private static DateFormat dfShort = SimpleDateFormat.getDateTimeInstance();
		private final ThreadHistoryWidget kcThreadListActivity;
		ArrayList<Long> ids = new ArrayList<Long>();
		private LayoutInflater mInflater;
		private int mViewResourceId;

		ThreadListAdapter(ThreadHistoryWidget kcThreadListActivity, Context context, int textViewResourceId) {
			super(context, R.id.SpinnerResult2, textViewResourceId);
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
				//v = mInflater.inflate(mViewResourceId, null);
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

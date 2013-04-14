package net.krautchan.android.widget;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

import net.krautchan.R;
import net.krautchan.android.activity.ProvidesBoards;
import net.krautchan.android.activity.ProvidesThreads;
import net.krautchan.data.KCThread;
import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class ThreadListAdapter extends ArrayAdapter<KCThread> {
		@SuppressLint("SimpleDateFormat")
		private static SimpleDateFormat dfShort = new SimpleDateFormat ("dd.MM. HH:mm");
		private final ProvidesBoards boardProvider;
		private final ProvidesThreads threadProvider;
		private ArrayList<Long> ids;
		private LayoutInflater mInflater;
		private int mViewResourceId;

		public ThreadListAdapter(ProvidesBoards bProv, ProvidesThreads tProv, Context context, int textViewResourceId) {
			super(context,  textViewResourceId);
			boardProvider = bProv;
			threadProvider = tProv;
			mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			mViewResourceId = textViewResourceId;
			ids = new ArrayList<Long>();
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
			ids = new ArrayList<Long>();
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
			KCThread thread = threadProvider.getThread (id);
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
				
				TextView boardlabel = (TextView) v.findViewById(R.id.threadListBoard);
				if (null != boardlabel) {
					boardlabel.setText("/"+boardProvider.getBoard(thread.board_id).shortName+"/"); 
				}

				TextView dateLabel = (TextView) v.findViewById(R.id.threadListDate);
				dateLabel.setText(dfShort.format(thread.firstPostDate));

				TextView titleLabel = (TextView) v.findViewById(R.id.threadListTitle);
				if (null != thread.title) {
					titleLabel.setText(thread.title);
				} else {
					titleLabel.setText("");
				}
				TextView contentLabel = (TextView) v.findViewById(R.id.threadListContent);
				contentLabel.setText(thread.digest);
				TextView numPostsLabel = (TextView) v.findViewById(R.id.threadListNumPostings);
				if (null != numPostsLabel) {
					numPostsLabel.setText(" "+thread.numPostings+ " Posts");
				}

			}
			return v;
		}


		@Override
		public int getCount() {
			return ids.size();
		}

		@Override
		public long getItemId(int position) {
			if (position > ids.size()) {
				return -1;
			}
			return ids.get(position);
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}
	}


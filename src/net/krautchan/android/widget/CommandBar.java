package net.krautchan.android.widget;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.krautchan.R;
import net.krautchan.android.Eisenheinrich;
import net.krautchan.android.activity.ProvidesBoards;
import net.krautchan.android.activity.ProvidesThreads;
import net.krautchan.android.dialog.ThreadHistoryDialog;
import net.krautchan.data.KCBoard;
import net.krautchan.data.KCThread;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.PaintDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

public class CommandBar extends ViewGroup implements ProvidesThreads, ProvidesBoards {
	private ThreadListAdapter adapter;
	private static final int MAX_PROGRESS = 100;
	private int progressState = 0;
	private ProgressBar progress;

	public CommandBar(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		prepare(context, attrs);
	}

	public CommandBar(Context context, AttributeSet attrs) {
		super(context, attrs);
		prepare(context, attrs);
	}

	public CommandBar(Context context) {
		super(context);
		prepare(context, null);
	}
	
	protected void prepare(final Context context, AttributeSet attrs) {
	    LayoutInflater inflater = (LayoutInflater) context
	        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    ViewGroup parentView = (ViewGroup) inflater.inflate(R.layout.command_bar, this, true);
	    
	    final View viewHeader = (View)parentView.findViewById(R.id.command_bar_widget);
	    TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CommandBar);
	    boolean showProgress = a.getBoolean (R.styleable.CommandBar_show_progress, false);
	    a.recycle();
	   
		ShapeDrawable.ShaderFactory sf = new ShapeDrawable.ShaderFactory() {
		    @Override
		    public Shader resize(int width, int height) {
		        LinearGradient lg = new LinearGradient(0, 0, 0, viewHeader.getHeight(),
		            new int[] { 
		        		getResources().getColor(R.color.Greengradient3a), 
		        		getResources().getColor(R.color.Greengradient3b), 
		                getResources().getColor(R.color.Greengradient3c), 
		                getResources().getColor(R.color.Greengradient3d), 
		                getResources().getColor(R.color.Greendarkborder) },
		            new float[] {
		                0, 0.45f, 0.55f, 0.98f, 1 },
		            Shader.TileMode.REPEAT);
		         return lg;
		    }
		};
		PaintDrawable p = new PaintDrawable();
		p.setShape(new RectShape());
		p.setShaderFactory(sf);
		viewHeader.setBackgroundDrawable(p);
		

		TextView history = (TextView) parentView.findViewById(R.id.history_button);
		history.setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/fontawesome-webfont.ttf"));
		history.setClickable(true);
		
		history.setTextColor(getResources().getColor(R.color.White));
		history.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				List<KCThread> threads = Eisenheinrich.GLOBALS.getThreadCache().getAll();
				for (int i = threads.size()-1; i >= 0; i--) {
					adapter.add(threads.get(i));
				}
				adapter.notifyDataSetChanged();
				ThreadHistoryDialog d = new ThreadHistoryDialog((Activity) context);
				d.show();
			}	
		});
		
		TextView options = (TextView) parentView.findViewById(R.id.options_button);
		options.setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/AndroidIcons.ttf"));
		options.setClickable(true);
		
		options.setTextColor(getResources().getColor(R.color.White));
		options.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Activity parent = (Activity) context;
				parent.openOptionsMenu();
			}	
		});
		
		adapter = new ThreadListAdapter(this, this, context, R.layout.spinner_list_item);
		
		progress = (ProgressBar) viewHeader.findViewById(R.id.progressbar);
	    progress.setMax(MAX_PROGRESS);
	    progressState = 0;
	    progress.setProgress(progressState);
	    if (!showProgress) {
	    	progress.setVisibility(GONE);
	    }
	}
	
	public void hideProgressBar() {
		progress.setVisibility(View.GONE);
		progress.setMax(MAX_PROGRESS);	    
		progressState = 0;
	    progress.setProgress(progressState);
		progress.setProgress(0);
	}
	
	public void setProgress(int increment) {
		progressState += increment;
		progress.setProgress(progressState);
	}
	
	public void setTitle (String title) {
		TextView headline = (TextView)findViewById(R.id.headline);
		headline.setText(title);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		
		View container = findViewById(R.id.command_bar_widget);
		container.layout(0, 0, r, this.getHeight());
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {	
		this.measureChildren(widthMeasureSpec, heightMeasureSpec);
		LayoutParams p = this.getLayoutParams();
		View container = findViewById(R.id.command_bar_widget);
		LayoutParams p2 = container.getLayoutParams();
		setMeasuredDimension(LayoutParams.MATCH_PARENT, p2.height);
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
	
	final static protected class ThreadListAdapter extends ArrayAdapter<KCThread> {
		@SuppressLint("SimpleDateFormat")
		private static SimpleDateFormat dfShort = new SimpleDateFormat ("dd.MM. HH:mm");
		private final ProvidesBoards boardProvider;
		private final ProvidesThreads threadProvider;
		private ArrayList<Long> ids = new ArrayList<Long>();
		private LayoutInflater mInflater;
		private int mViewResourceId;

		ThreadListAdapter(ProvidesBoards bProv, ProvidesThreads tProv, Context context, int textViewResourceId) {
			super(context,  textViewResourceId);
			boardProvider = bProv;
			threadProvider = tProv;
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
					boardlabel.setText(boardProvider.getBoard(thread.board_id).shortName);
				}

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
	}


}

package net.krautchan.android.widget;

import net.krautchan.R;
import net.krautchan.android.Eisenheinrich;
import net.krautchan.android.activity.ProvidesBoards;
import net.krautchan.android.activity.ProvidesThreads;
import net.krautchan.android.dialog.ThreadHistoryDialog;
import net.krautchan.data.KCBoard;
import net.krautchan.data.KCThread;
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
import android.widget.ProgressBar;
import android.widget.TextView;

public class CommandBar extends ViewGroup implements ProvidesThreads, ProvidesBoards {
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
		
		
		progress = (ProgressBar) viewHeader.findViewById(R.id.progressbar);
	    progress.setMax(MAX_PROGRESS);
	    progressState = 0;
	    progress.setProgress(progressState);
	    if (!showProgress) {
	    	progress.setVisibility(GONE);
	    }
	}
	
	public void showProgressBar() {
		progress.setVisibility(View.VISIBLE);
	}
	
	public void hideProgressBar() {
		progress.setVisibility(View.GONE);
		progress.setMax(MAX_PROGRESS);	    
		progressState = 0;
	    progress.setProgress(progressState);
		progress.setProgress(0);
	}
	
	public void incrementProgressBy (int increment) {
		progressState += increment;
		progress.setProgress(progressState);
	}
	
	public void setTitle (int resourceId) {
		setTitle (getResources().getString(resourceId));
	}
	
	public void setTitle (String title) {
		if (null == title) 
			return;
		TextView headline = (TextView)findViewById(R.id.headline);
		String locTitle = (title.length() < 33) ? title : title.substring(0, 32); 
		headline.setText(locTitle);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		View container = findViewById(R.id.command_bar_widget);
		container.layout(0, 0, r, this.getHeight());
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {	
		this.measureChildren(widthMeasureSpec, heightMeasureSpec);
		View container = findViewById(R.id.command_bar_widget);
		LayoutParams p2 = container.getLayoutParams();
		setMeasuredDimension(LayoutParams.MATCH_PARENT, p2.height);
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

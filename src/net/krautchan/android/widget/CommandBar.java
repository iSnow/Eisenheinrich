package net.krautchan.android.widget;

import net.krautchan.R;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class CommandBar  extends ViewGroup {

	public CommandBar(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public CommandBar(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public CommandBar(Context context) {
		super(context);
	}
	
	protected void prepare(Context context) {
	    LayoutInflater inflater = (LayoutInflater) context
	        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    View v = inflater.inflate(R.layout.command_bar, this, true);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		// TODO Auto-generated method stub
		
	}
	
}

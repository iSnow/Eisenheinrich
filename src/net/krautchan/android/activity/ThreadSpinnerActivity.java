package net.krautchan.android.activity;


import net.krautchan.R;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class ThreadSpinnerActivity extends Activity {
	protected int mPos;
	protected String mSelection;
	//private ThreadHistoryWidget spinner;
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
		//spinner = (ThreadHistoryWidget) findViewById(R.layout.thread_history_widget);

		TextView history = (TextView) findViewById(R.id.board_list_history2);
		history.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/fontawesome-webfont.ttf"));
		
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


}
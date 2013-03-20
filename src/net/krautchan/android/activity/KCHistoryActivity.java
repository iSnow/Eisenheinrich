package net.krautchan.android.activity;

import android.app.ListActivity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;

public class KCHistoryActivity extends ListActivity {


	@Override
	public void onCreate(Bundle savedInstanceState) {
		
	}
	
	@Override
	public ListAdapter getListAdapter() {
		// TODO Auto-generated method stub
		return super.getListAdapter();
	}

	@Override
	public ListView getListView() {
		// TODO Auto-generated method stub
		return super.getListView();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		// TODO Auto-generated method stub
		super.onListItemClick(l, v, position, id);
	}

	@Override
	public void setListAdapter(ListAdapter adapter) {
		// TODO Auto-generated method stub
		super.setListAdapter(adapter);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// TODO Auto-generated method stub
		super.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
	}
	
	

}

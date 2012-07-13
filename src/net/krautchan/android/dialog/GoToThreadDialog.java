package net.krautchan.android.dialog;

import java.io.IOException;

import net.krautchan.R;
import net.krautchan.android.helpers.ActivityHelpers;
import net.krautchan.data.KCBoard;
import net.krautchan.data.KCThread;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpHead;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;

public class GoToThreadDialog {
	private HttpClient httpClient;
	private AlertDialog dlg = null;
	private Activity parent;
	private KCBoard board;
	
	public GoToThreadDialog (Activity parent, KCBoard board, HttpClient httpClient) {
		this.parent = parent;
		this.board = board;
		this.httpClient = httpClient;
	}
	
	//FIXME there's bullshit-code here, remove second onclick-handler
	public boolean showDialog () {
		Builder builder = new AlertDialog.Builder(parent)
		.setPositiveButton (android.R.string.yes, new OnClickListener () {
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				Long tNum = getThreadNum (dlg);
				if (null != tNum) {
					startGoToThreadRunner (tNum);
				}
			}})
        .setNegativeButton (android.R.string.cancel, new OnClickListener () {
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				dlg.dismiss();
			}
		});
		
		ScrollView scroll = new ScrollView(parent);
	    scroll.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
	    scroll.addView(parent.getLayoutInflater().inflate(R.layout.thread_input_dialog, null));
	    dlg = builder.setView(scroll)
			.setTitle(R.string.load_thread)
			.create();
		dlg.show();
		Button okButton = dlg.getButton(DialogInterface.BUTTON_POSITIVE);
		okButton.setOnClickListener(new android.view.View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Long tNum = getThreadNum (dlg);
				if (null != tNum) {
					startGoToThreadRunner (tNum);
				}
			}
		});
		return true;
	}
	
	private void startGoToThreadRunner (final Long tNum) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				String url = "http://krautchan.net/"+board.shortName+"/thread-"+tNum+".html";
				final KCThread newThread = new KCThread();
				newThread.kcNummer = tNum;
				newThread.uri = url;
				newThread.board_id = board.dbId;
				HttpHead req = new HttpHead(url);
				try {
					HttpResponse res = httpClient.execute(req);
					StatusLine sl = res.getStatusLine();
					int code = sl.getStatusCode();
					if ((code == 200) || (code == 304)) {
			        	
			        	parent.runOnUiThread(new Runnable () {
							@Override
							public void run() {	
					        	dlg.dismiss();
								ActivityHelpers.switchToThread (newThread, parent);
							}
			        	});
					} else {
						parent.runOnUiThread(new Runnable () {
							@Override
							public void run() {	
								dlg.findViewById(R.id.threadinput_notfound).setVisibility(View.VISIBLE);
							}
						});
					}
				} catch (ClientProtocolException e) {
					// TODO Auto-generated catch block
				} catch (IOException e) {
					// TODO Auto-generated catch block
				}
			}
			
		}).start();
		
	}
	
	private Long getThreadNum (AlertDialog dlg) {
		String numStr = ((EditText)dlg.findViewById(R.id.thread_num)).getText().toString();
		Long kcNum = null;
		try {
			kcNum = Long.decode(numStr);
			return kcNum;
		} catch (NumberFormatException ex) {
			dlg.findViewById(R.id.threadinput_error).setVisibility(View.VISIBLE);
		}
		return null;
	}
}

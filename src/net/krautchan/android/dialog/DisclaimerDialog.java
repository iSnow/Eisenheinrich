package net.krautchan.android.dialog;

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.krautchan.R;
import net.krautchan.android.activity.EisenheinrichActivity;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.res.AssetManager;
import android.os.Process;
import android.widget.TextView;

public class DisclaimerDialog {
	private EisenheinrichActivity parent; 
	//private String fileName;
	private String message;
	private String title;
	private AlertDialog disclaimerDialog;
	
	public DisclaimerDialog (Activity parent) {
		this.parent = (EisenheinrichActivity)parent;
	}
	
	public void show ()  throws IOException {
		readMessage();
		TextView myView = new TextView(parent.getApplicationContext());
		myView.setText(message);
		myView.setTextSize(13);
		disclaimerDialog = new AlertDialog.Builder(parent) 
        //.setMessage (message)
		.setView(myView)
        .setTitle(title)
        .setPositiveButton (android.R.string.yes, new OnClickListener () {
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				parent.setDisclaimerAck(true);
				disclaimerDialog.dismiss();
			}})
        .setNegativeButton (android.R.string.no, new OnClickListener () {
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				parent.moveTaskToBack(true);
				Process.killProcess(Process.myPid());
			}}).create();
        disclaimerDialog.show();
	}
	
	private void readMessage() throws IOException {	
		StringBuilder builder = new StringBuilder();
		AssetManager m = parent.getAssets();
		String fName = parent.getString (R.string.disclaimer_filename);
		InputStream is = m.open(fName);
		BufferedReader r = new BufferedReader(new InputStreamReader(is));
		String line;
		while ((line = r.readLine()) != null) {
			builder.append(line);
		}
		r.close();
		message = builder.toString().replaceAll("\n", "").trim();
		Pattern titlePattern = Pattern.compile("<div class=\"titlebar\">(.+?)</div>");
		Matcher matcher = titlePattern.matcher(message);
		message = message.replaceAll("<div class=\"titlebar\">(.+?)</div>\\s*", "");
		title = "Nope";
		if (matcher.find()) {
			title = matcher.group(1);
		}
		message = message.replaceAll("\\<li\\>", "\n");
		message = message.replaceAll("\\<a.*?\\>.*?</a>", "");
		message = message.replaceAll("\\<.*?\\>", "");
		message = message.replaceAll("\n +", "\n");
		message = message.replaceAll(" +", " ");
		message = message.replaceAll("\\|", "\n\n");
		message = message.trim();	
	}
}

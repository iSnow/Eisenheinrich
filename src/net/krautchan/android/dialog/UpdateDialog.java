package net.krautchan.android.dialog;

import net.krautchan.R;
import net.krautchan.android.Eisenheinrich;

import android.app.Activity;
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
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;

public class UpdateDialog {
	private Activity parent; 
	private AlertDialog updateDialog;
	
	public UpdateDialog (Activity parent) {
		this.parent = parent;
	}
	
	public void show ()   {
		updateDialog = new AlertDialog.Builder(parent) 
        .setTitle(R.string.update_available_title)
        .setMessage(R.string.update_available_text)
        .setPositiveButton (android.R.string.yes, new OnClickListener () {
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				Intent browser = new Intent(Intent.ACTION_VIEW, Uri.parse(Eisenheinrich.DEFAULTS.UPDATE_PAGE));
				try {
					parent.startActivity(browser);
				} catch (ActivityNotFoundException ex) {
					 //FIXME implement some toast or dialog shit
				}
				
				updateDialog.dismiss();
			}})
        .setNegativeButton (android.R.string.no, new OnClickListener () {
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				updateDialog.dismiss();
			}}).create();
        updateDialog.show();
	}
}

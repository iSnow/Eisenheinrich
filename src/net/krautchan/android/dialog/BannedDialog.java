package net.krautchan.android.dialog;

/*
 * Copyright (C) 2012 Johannes Jander (johannes@jandermail.de)
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

import net.krautchan.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class BannedDialog {
	private Activity parent;
	private String message;
	private AlertDialog banDialog;

	public BannedDialog(Activity parent) {
		this.parent = parent;
	}

	public void show()  {
		Builder builder = new AlertDialog.Builder(parent).setPositiveButton(
				R.string.ok, new OnClickListener() {
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						banDialog.dismiss();
					}
				})
				.setView(parent.getLayoutInflater().inflate(R.layout.ban_dialog, null));
		banDialog = builder.create();
		banDialog.show();
		Button goPageButton = (Button) banDialog
				.findViewById(R.id.go_page_button);
		goPageButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent browser = new Intent(Intent.ACTION_VIEW, Uri
						.parse("http://krautchan.net/banned"));
				try {
					parent.startActivity(browser);
				} catch (ActivityNotFoundException ex) {
					// FIXME implement some toast or dialog shit
				}
			}
		});
		TextView myView = new TextView(parent.getApplicationContext());
		myView.setText(message);
		myView.setTextSize(13);
	}
}

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

import net.krautchan.R;
import net.krautchan.android.Eisenheinrich;
import net.krautchan.android.Globals;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Typeface;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class AboutDialog {
	private Activity parent;
	private AlertDialog dlg=null;
	
	public AboutDialog(Activity parent) {
		super();
		this.parent = parent;
	}

	public void show () {
		AboutDialogOnClickListener l = new AboutDialogOnClickListener();
		Builder builder = new AlertDialog.Builder(parent)
        	.setPositiveButton (R.string.ok, l)
        	.setView(parent.getLayoutInflater().inflate(R.layout.about_dialog, null));
		dlg = builder.create();
		l.setDialog(dlg);
		dlg.show();		 
		TextView nameLabel = (TextView) dlg.findViewById(R.id.about_headline);
		Typeface tf = Typeface.createFromAsset(parent.getAssets(), "fonts/juicebold.ttf");
		if (null == tf) {
			return;
		}
		nameLabel.setTypeface(tf);
		PackageInfo pinfo;
		try {
			PackageManager pm = parent.getPackageManager();
			if (null != pm) {
				pinfo = pm.getPackageInfo(parent.getPackageName(), 0);
			    String versionName = pinfo.versionName;
			    TextView t = (TextView)dlg.findViewById(R.id.about_version); 
			    t.setText("Version: "+versionName+" (beta)");
			}
		} catch (NameNotFoundException e) {
			//dont care
		}

		EditText et = (EditText)dlg.findViewById(R.id.ip_number);
		EditText kt = (EditText)dlg.findViewById(R.id.komtur_code);
		if (null != Eisenheinrich.GLOBALS.IP_NUMBER) {
			et.setText(Eisenheinrich.GLOBALS.IP_NUMBER);
			kt.setText(Eisenheinrich.GLOBALS.KOMTUR_CODE);
		} else {
			et.setVisibility(View.GONE);
			kt.setVisibility(View.GONE);
			dlg.findViewById(R.id.ip_number_label).setVisibility(View.GONE);
			dlg.findViewById(R.id.komtur_code_label).setVisibility(View.GONE);
		}
	}
	
	private class AboutDialogOnClickListener implements OnClickListener {
		private AlertDialog dlg;
		public AboutDialogOnClickListener() {
		}
		
		public void setDialog (AlertDialog dlg) {
			this.dlg = dlg;
		}

		@Override
		public void onClick(DialogInterface arg0, int arg1) {
			Globals globs = Eisenheinrich.GLOBALS;
			
			globs.KOMTUR_CODE = ((EditText)dlg.findViewById(R.id.komtur_code)).getText().toString();
		}
		
	}
}

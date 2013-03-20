package net.krautchan.android.activity;

import net.krautchan.R;
import net.krautchan.android.Eisenheinrich;
import net.krautchan.android.helpers.CustomExceptionHandler;
import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;
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

public class PreferencesActivity extends Activity {

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler(
		        "eisenheinrich", "http://eisenheinrich.datensalat.net:8080/Eisenweb/upload/logfile/test", this));

		EditText et = (EditText)findViewById(R.id.ip_number);
		EditText kt = (EditText)findViewById(R.id.komtur_code);
		if (null != Eisenheinrich.GLOBALS.getIpNumber()) {
			et.setText(Eisenheinrich.GLOBALS.getIpNumber());
			kt.setText(Eisenheinrich.GLOBALS.getKomturCode());
		} 
	}

}

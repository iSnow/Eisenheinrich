package net.krautchan.android.network;
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
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;

public class UpdateCheck {
	private final UpdateCheckPeer peer;
	private final HttpClient httpClient;
	private final String url;
	
	public UpdateCheck(UpdateCheckPeer peer, HttpClient httpClient, String url) {
		this.peer = peer;
		this.httpClient = httpClient;
		this.url = url;
	}


	public void checkForUpdate (Context ctx) {
		PackageInfo pInfo;
		try {
			pInfo = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
			final int version = pInfo.versionCode;
			new Thread(new Runnable() {

				@Override
				public void run() {
					HttpGet request = new HttpGet (url); 
					try {
						HttpResponse response = httpClient.execute(request);
						BufferedReader reader = new BufferedReader (new InputStreamReader (response.getEntity().getContent()));
						int curChar;
						StringBuffer buf = new StringBuffer();
						curChar = reader.read();
						while (-1 != curChar) {
							buf.append((char)curChar);
							curChar = reader.read();
						}
						reader.close();
						if (response.getEntity() != null ) {
							response.getEntity().consumeContent();
						}
						if (Integer.parseInt(buf.toString()) > version) {
							peer.updateAvailable();
						}
					} catch (Exception e) {
						e.printStackTrace();
						//FIXME do something 
					}
					
				}}).start();
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
	
	public interface UpdateCheckPeer {
		public void updateAvailable();
	}
}

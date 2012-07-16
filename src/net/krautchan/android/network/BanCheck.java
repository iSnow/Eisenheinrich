package net.krautchan.android.network;

import java.nio.charset.Charset;

import net.krautchan.android.Defaults;
import net.krautchan.android.Globals;
import net.krautchan.data.KCBoard;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import android.util.Log;

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

public class BanCheck {
		private static final String TAG = "BanCheck";
		private static final String CHARSETNAME= "UTF-8";
		private static final Charset CHARSET = Charset.forName(CHARSETNAME);
		private DefaultHttpClient httpClient;
		private KCBoard board;
		private Globals globs;
		
		public BanCheck(KCBoard board, DefaultHttpClient httpClient, Globals globs) {
			super();
			this.httpClient = httpClient;
			this.board = board;
			this.globs = globs;
		}

		public void check4Ban() {
			new Thread(new Runnable() {
				public void run () {
					httpClient.getParams().setParameter("http.protocol.handle-redirects",false);
					httpClient.getParams().setParameter("http.protocol.content-charset", CHARSETNAME); 
					HttpConnectionParams.setSoTimeout(httpClient.getParams(), 30000);
					HttpConnectionParams.setConnectionTimeout(httpClient.getParams(), 30000); 

				    HttpContext localContext = new BasicHttpContext();
					HttpPost httppost = new HttpPost("http://krautchan.net/post");
					CookieStore cookieStore = new BasicCookieStore(); 
					BasicClientCookie cookie = new BasicClientCookie("desuchan.komturcode", globs.getKOMTUR_CODE());
					cookie.setDomain(Defaults.DOMAIN);
					cookie.setPath("/");
					cookieStore.addCookie(cookie); 
					httpClient.setCookieStore(cookieStore); 
					
					try {
					    MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE, null, CHARSET);
					    entity.addPart("internal_t", new StringBody(" :xD"));  // Comment
						entity.addPart("sage", new StringBody("1"));// Säge
						entity.addPart("forward", new StringBody("thread")); // forward to thread or board -> thread for us
						entity.addPart("board", new StringBody(board.shortName, CHARSET)); // board
						
						httppost.setEntity(entity);
						HttpResponse response = httpClient.execute(httppost, localContext);
						StatusLine sl = response.getStatusLine();
						BanCheck.this.board.banned = false;
						if (sl.getStatusCode() == 302) {
							Header headers[] = response.getAllHeaders();
							String location = null;
							for (Header h:headers) {
								if (h.getName().equals("Location")) {
									location = h.getValue();
								}
							}
							if ((null != location) && (location.startsWith("/banned"))) {
								BanCheck.this.board.banned = true;
								CookieHelper.getMyIP(globs);
							} 
						}
						if (response.getEntity() != null ) {
							response.getEntity().consumeContent();
						}
					} catch (Exception e) {
						Log.e(TAG, "Failed in check4Ban()", e);
					} 
				}
			}).start();
		}
	}


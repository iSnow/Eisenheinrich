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

import java.io.File;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;

import net.krautchan.R;
import net.krautchan.android.Defaults;
import net.krautchan.android.Eisenheinrich;
import net.krautchan.android.Globals;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import android.util.Log;

public class AsyncPoster {
	private static final String TAG = "AsyncPoster";
	//private static final String CHARSETNAME= "ISO-8859-1";
	private static final String CHARSETNAME= "UTF-8";
	private static final Charset CHARSET = Charset.forName(CHARSETNAME);
	private final PostVariables postVars;
	private Defaults defaults;
	private Globals globs;
	private List<AsyncPosterPeer> peers;
	private DefaultHttpClient httpClient;
	
	public AsyncPoster(PostVariables postVars, DefaultHttpClient httpClient, Defaults defaults, Globals globs, List<AsyncPosterPeer> peers) {
		super();
		this.postVars = postVars;
		this.httpClient = httpClient;
		this.peers = peers;
		this.defaults = defaults;
		this.globs = globs;
	}

	public void postInThread() {
		new Thread(new Runnable() {
			public void run () {
				httpClient.getParams().setParameter("http.protocol.handle-redirects",false);
				httpClient.getParams().setParameter("http.protocol.content-charset", CHARSETNAME); 
				HttpConnectionParams.setSoTimeout(httpClient.getParams(), 30000);
				HttpConnectionParams.setConnectionTimeout(httpClient.getParams(), 30000); 

			    HttpContext localContext = new BasicHttpContext();
				HttpPost httppost = new HttpPost(Defaults.POST_URL);
				
				CookieStore cookieStore = new BasicCookieStore(); 
				BasicClientCookie cookie = new BasicClientCookie("desuchan.komturcode", globs.getKomturCode());
				cookie.setDomain(defaults.DOMAIN);
				cookie.setPath("/");
				cookieStore.addCookie(cookie); 
				httpClient.setCookieStore(cookieStore); 
				
				try {
				    MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE, null, CHARSET);
				    entity.addPart("internal_n", new StringBody(postVars.posterName, CHARSET)); // Bernd name
				    entity.addPart("internal_s", new StringBody(postVars.title, CHARSET)); // Post subject
				    entity.addPart("internal_t", new StringBody(postVars.content, CHARSET));  // Comment
					if (postVars.sage)
						entity.addPart("sage", new StringBody("1"));// Säge
					entity.addPart("forward", new StringBody("thread")); // forward to thread or board -> thread for us
					entity.addPart("board", new StringBody(postVars.boardName, CHARSET)); // board
					if (null != postVars.threadNumber) {
						entity.addPart("parent", new StringBody(postVars.threadNumber)); // thread ID
					}
					if (null != postVars.files) {
						for (int i = 0; i < postVars.files.length; i++) {
							if (postVars.files[i] != null) {
								File f = new File(new URI(postVars.files[i].toString()));
								if (!f.exists()) {
									System.out.println (f.getAbsolutePath());
								}
								entity.addPart(f.getName(), new FileBody(f));
							}
						}
					}
					httppost.setEntity(entity);
					HttpResponse response = httpClient.execute(httppost, localContext);
					StatusLine sl = response.getStatusLine();
					if (sl.getStatusCode() == 302) {
						//System.out.println (sl);
						Header headers[] = response.getAllHeaders();
						String location = null;
						for (Header h:headers) {
							if (h.getName().equals("Location")) {
								location = h.getValue();
							}
							//System.out.println (h.getName()+" "+h.getValue());
						}
						if ((null != location) && (location.startsWith("/banned"))) {
							notifyPeers (false, Eisenheinrich.getInstance().getString(R.string.banned_message));
						} else {
							notifyPeers (true, null);
						}
					}
					if (response.getEntity() != null ) {
						response.getEntity().consumeContent();
					}
				} catch (Exception e) {
					notifyPeers (false, "Failed in postInThread() - "+e.getMessage());
					Log.e(TAG, "Failed in postInThread()", e);
				} 
			}
		}).start();
	}
	
	private void notifyPeers (boolean success, String message) {
		for (AsyncPosterPeer peer : peers) {
			peer.notifyDone(success, message);
		}
	}
	
	/*private static String getFileName (String fileUrl)  {
		int delim = fileUrl.lastIndexOf("/");
		return fileUrl.substring(delim+1);
	}*/
	
	public interface AsyncPosterPeer {
		public void storePostVariables (PostVariables vars);
		public PostVariables getPostVariables();
		public void notifyDone (boolean successful, String message);
	}
}

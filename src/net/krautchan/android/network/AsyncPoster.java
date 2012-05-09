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
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import android.os.Environment;
import android.util.Log;

public class AsyncPoster {
	private String TAG = "AsyncPoster";
	private final PostVariables postVars;
	private List<AsyncPosterPeer> peers;
	private HttpClient httpclient;
	
	public AsyncPoster(PostVariables postVars, HttpClient httpClient , List<AsyncPosterPeer> peers) {
		super();
		this.postVars = postVars;
		this.httpclient = httpClient;
		this.peers = peers;
	}

	public void postInThread() {
		new Thread(new Runnable() {
			public void run () {
			    HttpContext localContext = new BasicHttpContext();
				HttpPost httppost = new HttpPost("http://krautchan.net/post");
		
				try {
				    MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
				    entity.addPart("internal_n", new StringBody(postVars.posterName)); // Bernd name
				    entity.addPart("internal_s", new StringBody(postVars.title)); // Post subject
				    entity.addPart("internal_t", new StringBody(postVars.content));  // Comment
					if (postVars.sage)
						entity.addPart("sage", new StringBody("1"));// Säge
					entity.addPart("forward", new StringBody("thread")); // forward to thread or board -> thread for us
					entity.addPart("board", new StringBody(postVars.boardName)); // board
					if (null != postVars.threadNumber) {
						entity.addPart("parent", new StringBody(postVars.threadNumber)); // thread ID
					}
					if (null != postVars.fileUrl) {
						for (int i = 0; i < postVars.fileUrl.length; i++) {
							if (postVars.fileUrl[i] != null) {
								String fileName = getFileName (postVars.fileUrl[i]);
								entity.addPart(fileName, new FileBody(new File (Environment.getExternalStorageDirectory(), "/DCIM/"+fileName)));
							}
						}
					}
					httppost.setEntity(entity);
					HttpResponse response = httpclient.execute(httppost, localContext);
					notifyPeers (true);
					
					/*StatusLine sl = response.getStatusLine();
					System.out.println (sl);
					Header headers[] = response.getAllHeaders();
					for (Header h:headers) {
						System.out.println (h.getName()+" "+h.getValue());
					}*/
				} catch (Exception e) {
					notifyPeers (false);
					Log.e(TAG, "Failed in postInThread()", e);
				} 
			}
		}).start();
	}
	
	private void notifyPeers (boolean success) {
		for (AsyncPosterPeer peer : peers) {
			peer.notifyDone(true);
		}
	}
	
	private static String getFileName (String fileUrl)  {
		int delim = fileUrl.lastIndexOf("/");
		return fileUrl.substring(delim+1);
	}
	
	public interface AsyncPosterPeer {
		public void storePostVariables (PostVariables vars);
		public PostVariables getPostVariables();
		public void notifyDone (boolean successful);
	}
}

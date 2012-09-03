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

import java.util.Collection;

import net.krautchan.android.Defaults;
import net.krautchan.android.Eisenheinrich;
import net.krautchan.data.KCThread;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpHead;

public class ThreadExistenceCheck  {
	private ThreadExistencePeer peer;
	private HttpClient httpClient;
	private Collection <KCThread> threads;
	private int delay = 0;
	
	public ThreadExistenceCheck (Collection <KCThread> bookmarks, ThreadExistencePeer peer) {
		this.threads = bookmarks;
		this.peer = peer;
	}
	
	public void setDelay (int delay) {
		this.delay = delay;
	}
	
	public void checkThreads() {
		new Thread(new Runnable() {
			public void run() {
				httpClient = Eisenheinrich.getInstance().getHttpClient();
				int count = 0;
				for (KCThread thread: threads) {
					HttpHead req = new HttpHead(Defaults.BASE_URL+thread.uri);
					try {
						HttpResponse res = httpClient.execute(req);
						StatusLine sl = res.getStatusLine();
						int code = sl.getStatusCode();
						peer.threadChecked(thread, ((code == 200) || (code == 304)));
						if (res.getEntity() != null ) {
							res.getEntity().consumeContent();
						}
					} catch (Exception e) {
						// if we run into an exception (eg. timeout), assume the 
						// thread still exists and hope for a successful check next time
						peer.threadChecked(thread, true);
					} 
					count++;
				}
				try {
					Thread.sleep(delay);
				} catch (InterruptedException e) {}
			}
		}).start();
	} 
	
	public interface ThreadExistencePeer {
		public void threadChecked(KCThread thread, boolean valid);
	}
}

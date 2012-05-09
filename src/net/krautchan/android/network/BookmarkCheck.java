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

import java.io.IOException;
import java.util.Collection;

import net.krautchan.android.Eisenheinrich;
import net.krautchan.data.KCBoard;
import net.krautchan.data.KCThread;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpHead;

public class BookmarkCheck  {
	private BookmarkTesterPeer peer;
	private HttpClient httpClient;
	private Collection <KCThread> bookmarks;
	private Collection<KCBoard> boards;
	
	public BookmarkCheck (Collection <KCThread> bookmarks, BookmarkTesterPeer peer) {
		this.bookmarks = bookmarks;
		this.peer = peer;
		boards = Eisenheinrich.getInstance().dbHelper.getBoards();
	}
	
	public void checkBookmarks() {
		new Thread(new Runnable() {
			public void run() {
				httpClient = Eisenheinrich.getInstance().getHttpClient();
				boolean[] results = new boolean[bookmarks.size()];
				int count = 0;
				for (KCThread thread: bookmarks) {
					KCBoard board = getBoard (boards, thread.board_id);
					if (null != board) {
						String url = "http://krautchan.net/"+board.shortName+"/thread-"+thread.kcNummer+".html";
						HttpHead req = new HttpHead(url);
						try {
							HttpResponse res = httpClient.execute(req);
							StatusLine sl = res.getStatusLine();
							int code = sl.getStatusCode();
							results[count] = ((code == 200) || (code == 304));
						} catch (ClientProtocolException e) {
							e.printStackTrace();
							return;
						} catch (IOException e) {
							e.printStackTrace();
							return;
						}
					}
					count++;
				}
				peer.threadsChecked(results);
			}
		}).start();
	}
	
	public interface BookmarkTesterPeer {
		public void threadsChecked(boolean[] valid);
	}

	private static KCBoard getBoard (Collection<KCBoard> boards, Long boardID) {
		for (KCBoard board: boards) {
			if (board.dbId.longValue() == boardID.longValue()) {
				return board;
			}
		}
		return null;
	}
}

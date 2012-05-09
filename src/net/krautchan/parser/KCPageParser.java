package net.krautchan.parser;

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
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.widget.Toast;

import net.krautchan.android.Eisenheinrich;
import net.krautchan.android.activity.KCBoardListActivity;
import net.krautchan.data.KCPosting;
import net.krautchan.data.KCThread;
import net.krautchan.data.KODataListener;

public class KCPageParser implements Runnable {
	private String resolverPath = null;
	private KCPostingStreamParser pParser = null;
	private KCThreadStreamParser tParser = null;
	private KODataListener<KCThread> threadHandler = null;
	private KODataListener<KCPosting> postingHandler = null;
	private String url = null;

	
	public KCPageParser() {
		pParser = new KCPostingStreamParser();
		tParser = new KCThreadStreamParser();
		tParser.setPostingParser(pParser);
	}

	public List<KCThread> filterThreads (Reader reader, KCThreadStreamParser parser) throws Exception {
		parser.setBasePath(resolverPath);
		List<KCThread> threads = new ArrayList<KCThread>();
		char[]filter =  parser.getFilterMarker();
		ThreadState state = new ThreadState ();
		int curChar;
		int pos = 0;
		curChar = reader.read();
		while (-1 != curChar) {
			if ((state.curState == StateEnum.START) || (state.curState == StateEnum.READ_THREAD)) {
				if (curChar == filter[pos]) {
					pos++;
					if (pos == filter.length) {
						threads.add(parser.parse(reader));
						state.curState = StateEnum.START;
						pos = 0;
					}
				} else {
					pos = 0;
				}
			}
			curChar = reader.read();
		}
		parser.notifyDone();
		return threads;
	}
	
	public List<KCThread> filterThreads (Reader reader) throws Exception {
		return filterThreads(reader, tParser);
	}
	
	public KCPageParser setThreadHandler(KODataListener<KCThread> handler) {
		threadHandler = handler;
		tParser.setHandler(threadHandler);
		return this;
	}
	

	public KCPageParser setPostingHandler(KODataListener<KCPosting> postListener) {
		postingHandler = postListener;
		pParser.setHandler(postListener);
		return this;
	}
	
	public KCPageParser setUrl(String url) {
		this.url = url;
		return this;
	}

	@Override
	public void run() {
		if (null == threadHandler) {
			throw new IllegalArgumentException ("Cannot parse without a handler");
		}
		if (null == url) {
			throw new IllegalArgumentException ("Cannot parse a NULL url");
		}
		tParser.setHandler(threadHandler);
		final char[]filter =  tParser.getFilterMarker();
		DefaultHttpClient client = new DefaultHttpClient();
		HttpGet request = new HttpGet (url);
		try {
			HttpResponse response = client.execute(request);
			BufferedReader reader = new BufferedReader (new InputStreamReader (response.getEntity().getContent()));
			ThreadState state = new ThreadState ();
			int curChar;
			int pos = 0;
			curChar = reader.read();
			while (-1 != curChar) {
				if ((state.curState == StateEnum.START) || (state.curState == StateEnum.READ_THREAD)) {
					if (curChar == filter[pos]) {
						pos++;
						if (pos == filter.length) {
							tParser.parse(reader);
							if (null != threadHandler) {
								//threadHandler.notifyAdded(tParser.parse(reader));
							}
							state.curState = StateEnum.START;
							pos = 0;
						}
					} else {
						pos = 0;
					}
				}
				curChar = reader.read();
			}
			reader.close();
			tParser.notifyDone();
			
		} catch (Exception e) {
			e.printStackTrace();
			threadHandler.notifyError(e);
			//FIXME do something for heaven's sake!
		}
	}

	
	public KCPageParser setBasePath(String string) {
		resolverPath = string;
		return this;
	}

	//TODO decide: do we still need this? Wasn't the greatest idea ever.
	public enum StateEnum  {
		START, 
		START_THREAD,
		READ_THREAD,
		START_POST,
		READ_POST,
		END
	}
	
	//TODO decide: do we still need this? Wasn't the greatest idea ever.
	private static class ThreadState {
		public StateEnum curState = StateEnum.START;
	}
}

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

import java.io.Reader;
import net.krautchan.data.KCPosting;
import net.krautchan.data.KCThread;
import net.krautchan.data.KODataListener;

public class KCThreadStreamParser implements KCStreamParser<KCThread> {
	private String resolverPath = null;
	private KODataListener<KCThread> handler = null;
	private KCPostingStreamParser pParser = null;
	
	private char[][] startTags = {
			"class=\"thread\"".toCharArray()
	};
	
	public enum StateEnum  {
		START, 
		START_THREAD,
		READ_THREAD,
		START_POST,
		READ_POST,
		END
	}
	
	@Override
	public KCThread parse(Reader reader) throws Exception {
		KCThread thread = new KCThread();
		pParser.setBasePath(resolverPath);
		char threadFilter[] = startTags[0];
		char postFilter[] =  pParser.getFilterMarker();
		int curChar;
		int postPos = 0;
		int threadPos = 0;
		curChar = reader.read();
		while (-1 != curChar) {
			if (curChar == threadFilter[threadPos]) {
				threadPos++;
				if (threadPos == threadFilter.length) {
					pParser.notifyDone();
					handler.notifyAdded(thread);
					return thread;
				}
			} else {
				threadPos = 0;
			}	
			if (curChar == postFilter[postPos]) {
				postPos++;
				if (postPos == postFilter.length) {
					KCPosting post = pParser.parse(reader);
					thread.addPosting(post);
					postPos = 0;
				}
			} else {
				postPos = 0;
			}	
			curChar = reader.read();
		}
		pParser.notifyDone();
		handler.notifyAdded(thread);
		return thread;
	}	

	@Override
	public void setHandler(KODataListener<KCThread> handler) {
		this.handler = handler;
	}
	
	public void setPostingParser(KCPostingStreamParser parser) {
		pParser = parser;
	}

	@Override
	public char[] getFilterMarker() {
		return "id=\"thread_".toCharArray();
	}

	public void setBasePath(String resolverPath) {
		this.resolverPath = resolverPath;
	}
	
	@Override
	public void notifyDone() {
		handler.notifyDone();
	}

}

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
	private Object token;
	private long boardDbId;
	private KCPostingStreamParser pParser = null;
	
	private char[][] startTags = {
			"class=\"thread\"".toCharArray()
	};
	private char[] omittedInfo = "<span class=\"omittedinfo\">".toCharArray();
	
	public KCThread parse(Reader reader, KCThread thread) throws Exception {
		thread.board_id = boardDbId;
		pParser.setBasePath(resolverPath);
		char threadFilter[] = startTags[0];
		char postFilter[] =  pParser.getFilterMarker();
		int curChar;
		int postPos = 0;
		int threadPos = 0;
		int omittedPos = 0;
		String omitted="";
		curChar = reader.read();
		while (-1 != curChar) {
			if (curChar == threadFilter[threadPos]) {
				threadPos++;
				if (threadPos == threadFilter.length) {
					thread.recalc();
					pParser.notifyDone();
					handler.notifyAdded(thread, token);
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
					thread.numPostings++;
					postPos = 0;
				}
			} else {
				postPos = 0;
			}	
			if (curChar == omittedInfo[omittedPos]) {
				omittedPos++;
				if (omittedPos == omittedInfo.length) {
					curChar = reader.read();
					while ((curChar == ' ') || (curChar == '\r') || (curChar == '\n')) {
						curChar = reader.read();
					}
					while ((curChar >= '0') && ((curChar <= '9'))) {
						omitted = omitted + ((char)curChar); 
						curChar = reader.read();
					}
					thread.numPostings = Integer.parseInt(omitted);
					omittedPos = 0;
				}
			} else {
				omittedPos = 0;
			}	
			curChar = reader.read();
		}
		pParser.notifyDone();
		handler.notifyAdded(thread, token);
		return thread;
	}	
	
	@Override
	public KCThread parse(Reader reader) throws Exception {
		return parse (reader, new KCThread());
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
	
	public void setBoardId (long boardDbId) {
		this.boardDbId = boardDbId;
	}
	
	@Override
	public void notifyDone() {
		handler.notifyDone(token);
	}

	@Override
	public void setHandler(KODataListener<KCThread> handler, Object token) {
		this.handler = handler;
		this.token = token;
	}

}

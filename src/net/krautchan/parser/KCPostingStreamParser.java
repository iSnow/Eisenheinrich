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
import net.krautchan.data.KODataListener;

public class KCPostingStreamParser implements KCStreamParser<KCPosting> {
	private String resolverPath = null;
	private KODataListener<KCPosting> handler = null;
	private Object token;
	
	private static final char[][] startTags = {
		"<input name=\"post_".toCharArray(),                 
		"<span class=\"postsubject\">".toCharArray(),       
		"<span class=\"postername\">".toCharArray(),        
		"<span class=\"postdate\">".toCharArray(),   
		"div class=\"file_reply\">".toCharArray(),
		"div class=\"file_thread\">".toCharArray(),
		"<p id=\"post_text_".toCharArray()
	};
	
	private static final KCPosting.Fields[] fields = {
		KCPosting.Fields.KC_NUM,
		KCPosting.Fields.TITLE,
		KCPosting.Fields.USER,
		KCPosting.Fields.DATE,
		KCPosting.Fields.IMAGES,
		KCPosting.Fields.IMAGES,
		KCPosting.Fields.CONTENT
	};
	
	private static final char[][] endTags = {
		"\"".toCharArray(),                 
		"</span>".toCharArray(),       
		"</span>".toCharArray(),        
		"</span>".toCharArray(),       
		"<blockquote>".toCharArray(), 
		"<blockquote>".toCharArray(),
		"</blockquote>".toCharArray()
	};
				
	/**
	 * This method is probably a bit frightening for the reader - actually it is for me as the
	 * inventor too. The idea is that all fields defined in the KCPosting Fields enum are matched 
	 * simulanously against the chars read from reader.
	 * non-matching fields are not copied, but their startTags and endTags positions reset
	 */
	@Override
	public KCPosting parse(Reader reader) throws Exception {
		KCPosting post = new KCPosting ();
		//StringBuffer testBuf = new StringBuffer (200000);
		int curChar = -1;
		int[] startPositions = new int[startTags.length];
		for (int i = 0; i < startPositions.length; i++) {
			startPositions[i] = 0;
		}
		int[] endPositions = new int[startTags.length];
		for (int i = 0; i < endPositions.length; i++) {
			endPositions[i] = 0;
		}
		curChar = reader.read();
		while (-1 != curChar) {
			for (int curTag = 0; curTag < startPositions.length; curTag++) {
				if (curChar == startTags[curTag][startPositions[curTag]]) {
					startPositions[curTag]++;
					if (startPositions[curTag] == startTags[curTag].length) {
						StringBuffer buf = new StringBuffer (2000);
						startPositions[curTag] = 0;
						endPositions[curTag] = 0;
						while ((-1 != curChar) && (endPositions[curTag] != endTags[curTag].length)) {
							curChar = reader.read();
							buf.append((char)curChar);
							if (curChar == endTags[curTag][endPositions[curTag]]) {
								endPositions[curTag]++;
							} else {
								endPositions[curTag] = 0;
							}
						}
						buf.setLength(buf.length()-endTags[curTag].length);
						post.setField (fields[curTag], buf.toString());
						curTag++;
						if (curTag == startTags.length) {
							post.dbId = (long)(resolverPath+post.kcNummer).hashCode();
							if (null != handler) {
								handler.notifyAdded(post, token);
							}
							return post;
						}
						startPositions[curTag] = 0;
					}
				} else if (startPositions[curTag] != 0){
					startPositions[curTag] = 0;
				}
			}
			curChar = reader.read();
		} 
		/*if (curChar == -1) {
			throw new IllegalStateException ("KCPostingStreamParser missed a field in "+testBuf);
		}*/
		if (null != handler) {
			handler.notifyDone(token);
		}
		return null;
	}

	@Override
	public void setHandler(KODataListener<KCPosting> handler, Object token) {
		this.handler = handler;
		this.token = token;
	}


	@Override
	public char[] getFilterMarker() {
		return "<div class=\"postheader\">".toCharArray();
	}

	public void setBasePath(String resolverPath) {
		this.resolverPath = resolverPath;
	}

	@Override
	public void notifyDone() {
		handler.notifyDone(token);
	}

}

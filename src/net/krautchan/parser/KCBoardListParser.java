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

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import net.krautchan.data.KCBoard;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class KCBoardListParser {
	/*
	 * Contrary to the Thread and Posting parsers, which are called frequently, 
	 * this is not a stream parser, but uses JSoup for simplicity 
	 */

	private static Map<String, KCBoard> getBoardList(Document doc, String baseUrl) {
		Map<String, KCBoard> boards = new LinkedHashMap<String, KCBoard>();
		Elements boardlist = doc.select(".boardlist li");
		Iterator<Element> iter = boardlist.iterator();
		while (iter.hasNext()) {
			Element elem = iter.next();
			String idStr = elem.id();
			if (idStr.startsWith("board_")) {
				Elements links = elem.select("a");
				if ((null != links) && (null != links.get(0))) {
					KCBoard board = new KCBoard();
					board.uri = baseUrl+links.attr("href");
					String content = links.get(0).ownText();
					String[] keyVal = content.split("\\s+-\\s+");
					board.shortName = keyVal[0].trim().replaceAll("/", "");
					board.name = keyVal[1].trim();
					board.dbId = (long) (baseUrl+"/"+board.shortName).hashCode();
					boards.put(board.shortName, board);
				}
			}
		}
		return boards;
	}
	
	public static Map<String, KCBoard> getBoardList (String boardListUrlStr, String baseUrl, String userAgentName) throws IOException {
		Document doc = Jsoup.connect(boardListUrlStr)
		  .userAgent(userAgentName)
		  .cookie("auth", "token")
		  .timeout(3000)
		  .get();
		if ((null != baseUrl) && (baseUrl.endsWith("/"))) {
			baseUrl = baseUrl.substring(0, baseUrl.length() -1); 
		}
		return getBoardList(doc, baseUrl);
	}
		
	public static Map<String, KCBoard> getBoardList (String html, String baseUrl) throws IOException {
		Document doc = Jsoup.parse(html);
		return getBoardList(doc, baseUrl);
	}
}

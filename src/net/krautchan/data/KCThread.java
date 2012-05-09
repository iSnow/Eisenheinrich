package net.krautchan.data;
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
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringEscapeUtils;

public class KCThread extends KrautObject {
	private static final SimpleDateFormat df = new SimpleDateFormat ("EEE, dd MMM yyyy HH:mm:ss z");
	private static final SimpleDateFormat dfShort = new SimpleDateFormat ("dd.MM. HH:mm");
	public Long kcNummer = null;
	public Long board_id = null;
	public Long firstPostDate = null;
	public Long lastPostDate = null;
	public String digest = null;
	private Map<Long, KCPosting>  postings = new LinkedHashMap<Long, KCPosting>();
	
	public synchronized KCPosting getPosting (Long id) {
		return postings.get(id);
	}
	
	public synchronized KCPosting getFirstPosting () {
		if (!postings.isEmpty()) {
			return postings.entrySet().iterator().next().getValue();
		}
		return null;
	}
	
	public synchronized void addPosting (KCPosting posting) {
		if (postings.size() == 0) {
			kcNummer = posting.kcNummer;
			firstPostDate = posting.created;
			makeDigest (posting);
			dbId = (long)(baseUrl+"board/"+board_id+"/thread/"+kcNummer).hashCode();
		}
		lastPostDate = posting.created;
		Long id = posting.dbId;
		//if (!postings.containsKey(id)) {
		postings.put(id, posting);
		//}	
	}
	
	public synchronized void clearPostings () {
		postings.clear();	
	}
	
	public synchronized Collection<Long> getIds () {
		return postings.keySet();
	}
	
	private void makeDigest (KCPosting posting) {
		if (null == posting)
			digest = "";
		digest = posting.content;
		int len = digest.length();
		if (len > 250)
			len = 250;
		digest = digest.substring(0, len);
		digest = digest.replaceAll("[\n\r\u0085\u2028\u2029]", " ").replaceAll(" +", " ").trim();
		digest = StringEscapeUtils.unescapeHtml4(digest);
		digest = digest.replaceAll("<span class=\"spoiler\">.+?</span>", "");
		digest = digest.replaceAll("\\<.*?\\>", " ");
		digest = digest.replaceAll("https?://.+? ", " ");
		digest = digest.replaceAll(" +", " ");
		len = digest.length();
		if (len > 200)
			len = 200;
		digest = digest.substring(0, len);
		digest = digest.replaceAll("\\<.*", "");
		int pos = digest.length()-1;
		char c = digest.charAt(pos);
		while ((c != ' ') && (pos > 150)) {
			pos--;
			c = digest.charAt(pos);
		}
		digest = digest.trim();
		for (String img : posting.thumbs) {
			if ((img != null) && (img.length() > 0)) {
				digest += "\n   "+img;
			}
		}
	}
}

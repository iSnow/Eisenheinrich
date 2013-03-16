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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import junit.framework.Assert;

import org.apache.commons.lang3.StringEscapeUtils;

public class KCThread extends KrautObject {
	private static final long serialVersionUID = -8659957154306651426L;
	private static transient final SimpleDateFormat df = new SimpleDateFormat ("EEE, dd MMM yyyy HH:mm:ss z");
	private static transient final SimpleDateFormat dfShort = new SimpleDateFormat ("dd.MM. HH:mm");
	public Long kcNummer = null;
	public Long board_id = null;
	public Long firstPostDate = null;
	public Long lastPostDate = null;
	public String digest = null;
	public boolean hidden = false;
	public boolean bookmarked = false;
	public Long previousLastKcNum = null;
	public transient int numPostings = 0;
	
	private TreeSet<KCPosting> postings = new TreeSet<KCPosting>();
	
	public KCThread () {
	}
	
	public KCThread (String uri) {
		this.uri = uri;
		dbId = (long)uri.hashCode();
	}
	
	public synchronized KCPosting getPosting (Long id) {
		for (KCPosting posting : postings) {
			if (posting.dbId.compareTo(id) == 0) {
				return posting;
			}
		}
		return null;
	}
	
	public synchronized KCPosting getFirstPosting () {
		if (!postings.isEmpty()) {
			return postings.first();
		}
		return null;
	}
	
	public synchronized KCPosting  getLastPosting() {
		if (!postings.isEmpty()) {
			return postings.last();
		}
		return null;
	}
	
	public boolean containsPosting (KCPosting posting) {
		return postings.contains(posting);
	}
	
	public synchronized void addPosting (KCPosting posting) {
		if (null == kcNummer) {
			kcNummer = posting.kcNummer;
		}
		if (null == firstPostDate) {
			firstPostDate = posting.created;
			makeDigest (posting);
		}
		if (null == uri) {
			uri = posting.uri;
		}
		if (null == dbId) {
			dbId = (long)uri.hashCode();
		}
		if (null == digest) {
			makeDigest (posting);
		}
		lastPostDate = posting.created;
		if (!postings.contains(posting)) {
			posting.threadId = dbId;
			postings.add(posting);
		}
		if ((null != previousLastKcNum) && (previousLastKcNum < posting.kcNummer)) {
			previousLastKcNum = posting.kcNummer;
		}
		Assert.assertNotNull(dbId);	
	}
	
	public void recalc () {
		try {
			KCPosting posting = postings.first();
			
			if (null == digest) {
				makeDigest (posting);
			}
			if (null == firstPostDate) {
				firstPostDate = posting.created;
			}
			if ((null == dbId) && (null != uri)) {
				dbId = (long)uri.hashCode();
			} 
			Assert.assertNotNull(dbId);
		} catch (Exception e) {
			String trace = "Exception in KCThread "+kcNummer+" "+e.getClass().getCanonicalName()+"\n";
			for (StackTraceElement elem : e.getStackTrace()) {
				trace+= " "+elem.toString()+"\n";
			}
			System.err.println (trace);
		}
	}
	
	public synchronized void clearPostings () {
		postings.clear();	
	}
	
	public synchronized Collection<Long> getIds () {
		Collection<Long> ids = new ArrayList<Long>();
		for (KCPosting posting: postings) {
			ids.add(posting.dbId);
		}
		return ids;
	}
	
	public synchronized Set<KCPosting> getSortedPostings () {
		TreeSet<KCPosting> s = new TreeSet<KCPosting>();
		s.addAll(postings);
		return s;
	}
	
	private void makeDigest (KCPosting posting) {
		if (null == posting) {
			return;
		}
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
	

	public String toString() {
		String d = null;
		if (digest.length() > 39) {
			d = digest.substring(0, 40);
		} else {
			d = digest;
		}
		return "Thread: "+uri+"\n"
			+" KC Number: "+kcNummer+"\n"
			+" DB ID: "+dbId+"\n"
			+" Board: "+board_id+"\n"
			+" Digest: "+d+"..."+"\n"
			+" First posting: "+firstPostDate+"\n"
			+" Last posting: "+lastPostDate+"\n"
			
			;
	}

	
}

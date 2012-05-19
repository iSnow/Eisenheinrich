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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;

public class KCPosting extends KrautObject {
	private static final long serialVersionUID = 2343973223217952495L;
	private static final SimpleDateFormat df = new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss");
	// we get the date as: 		EEE, dd MMM yyyy HH:mm:ss z"
	// we need to write it as: 	2008-07-17T09:24:17Z
	//private static SimpleDateFormat df = new SimpleDateFormat ("EEE, dd MMM yyyy HH:mm:ss z");
	private static SimpleDateFormat dfShort = new SimpleDateFormat ("dd.MM. HH:mm");
	private static SimpleDateFormat dfOut = new SimpleDateFormat ("yyyy-MM-dd'T'HH:mm:ss");
	private static final Pattern imgPat = Pattern.compile("href=\"/download/(\\d+\\..+?)/(.+?)\"");
	public Long 	kcNummer;
	public Long 	threadId;
	public Long 	threadKcNummer;
	public Long		created;
	public String 	title;
	public String 	creationDate;
	public String 	creationShortDate;
	public String 	content;
	public String 	user;
	public String 	tripCode;
	public boolean 	sage;
	//<img style="display: block" id='thumbnail_93878459' src=/thumbnails/1314213328002.jpg" ...
	public String[] thumbs = new String[4];
	//<a href="/download/1314213328002.jpg/DSC00030.JPG"
	public String[] imgs = new String[4];
	
	public static enum Fields  {
		KC_NUM,
		TITLE,
		USER,
		DATE,
		IMAGES,
		CONTENT
	}
	
	public void cleanRssContent () {
		String locContent = content;
		//locContent = StringEscapeUtils.unescapeHtml4(locContent);
		locContent = locContent.replaceAll("<p>", "");
		locContent = locContent.replaceAll("</p>", " ");
		int delim = locContent.indexOf("<a href=\"http://krautchan.net/download/");
		if (delim != -1) {
			String imgSection = locContent.substring(delim).trim();
			locContent = locContent.substring(0, delim-1).trim();
			content = "<p><span>"+locContent+"</span></p>";
			content = content + "<div class=\"image-container\">"+imgSection+"</div>";
		} else {
			content = "<p><span>"+locContent+"</span></p>";
		}
		//content = content.replaceAll(">>>(\\d+)</a>", "><span class=\"kclnk\">☛</span> $1</a>");
		content = content.replaceAll(">>>(\\d+)</a>", " class=\"kclnk\" onclick='quoteClick(this); return false;'><span>&gt;&gt;</span> $1</a>");
		
		//content = content.replaceAll(">&gt;&gt;(\\d+)</a>", " onclick='fn(this); return false;'><span class=\"kclnk\">&gt;&gt;</span> $1</a>");
		content = content.replaceAll(">&gt;&gt;(\\d+)</a>", " class=\"kclnk\" onclick='quoteClick(this); return false;'><span>&gt;&gt;</span> $1</a>");
	
		delim = title.indexOf(":");
		String kcNum  = title.substring(0, delim);
		title = title.replace(kcNum+":", "").trim();
		kcNummer = Long.parseLong(kcNum);
		if (title.endsWith("...")) // yeah, I am too dumb to find out how to escape '.' in Java regexp
			title = title.substring (0, title.length()-3);
		title = title.replaceAll("<br>", "").trim();

		locContent = locContent.replace("<p>", "");
		locContent = locContent.replaceAll("<a href=\"/resolve/.+?\">", "");
		locContent = locContent.replaceAll("</a>", "");
		/*locContent = locContent.replaceAll("<span.*?>", "");
		locContent = locContent.replaceAll("</span", "");*/
		locContent = locContent.replaceAll("<br>", " ").trim();
		if ((locContent.trim().length() == 0) || (locContent.startsWith(title))) {
			title = null;
		} else if (title.length() > 40) {
			title = title.substring(0, 40)+"...";
		}
	}
	
	public void sanitizeContent () {
		String locContent = content;
		locContent = StringEscapeUtils.unescapeHtml4(locContent);
		locContent = locContent.replaceAll("<p>", "");
		locContent = locContent.replaceAll("</p>", " ");
		
		locContent = locContent.replaceAll("onclick=\"highlightPost\\(\\'\\d+\\'\\);\"", "");		
		locContent = locContent.replaceAll(">>>(\\d+)</a>", " onclick='quoteClick(this); return false;' class=\"kclnk\"><span class=\"kclnk\">&gt;&gt;</span> $1</a>");

		locContent = locContent.replaceAll("<a href=\"/resolve/.+?\">", "");
		locContent = locContent.replaceAll("https?://www.youtube.com/(.+?)([\\s<\\.])", "<a href=\"http://www.youtube.com/$1\" class=\"youtubelink\" onclick=\"alert('open:youtube:$1');return false;\">YouTube</a>$2");
		locContent = locContent.replaceAll("https?://youtu.be/(.+?)([\\s<\\.])", "<a href=\"http://www.youtube.com/watch?v=$1\" class=\"youtubelink\" onclick=\"alert('open:youtube:$1');return false;\">YouTube</a>$2");
		locContent = locContent.replaceAll("https?://(www)?\\.*(.+?)/(.+?)([\\s<>\\(\\)\\.])", "<a href=\"http://$2/$3\" class=\"extlink\" onclick=\"alert('open:ext:$2/$3');return false;\">$2</a>$4");
			
		content = "<p><span>"+locContent.trim()+"</span></p>";
	}
	
	
	public void setField(Fields fields, String arg) throws ParseException {
		switch (fields) {
			case KC_NUM: { 
				if (null != arg) {
					kcNummer = Long.parseLong(arg);
					dbId = (long)(baseUrl+threadId+kcNummer).hashCode(); 
				}
				break;
			}
			case TITLE: { 
				title = StringEscapeUtils.unescapeHtml4(arg);
				break;
			}
			case USER: { 
				user = arg;
				break;
			}
			case DATE: { 
				int pos = arg.lastIndexOf('.');
				Date cDate = df.parse(arg.substring(0, pos));
				created = cDate.getTime();
				creationShortDate = dfShort.format(cDate);
				creationDate = dfOut.format(cDate);
				break;
			}
			case IMAGES: {
				Matcher m = imgPat.matcher(arg);
				int i = 0;
				while (m.find()) {
					imgs[i] = m.group(1);
					thumbs[i] = m.group(2);
					i++;
				}
				break;
			}
			case CONTENT: { 
				content = arg.replaceFirst(kcNummer+"\">\\s*", "");
				sanitizeContent();
				break;
			}
			default: 
				throw new IllegalStateException ("Illegal State in KCPosting:setField");
		}
	}
}

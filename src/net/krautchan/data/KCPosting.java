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

public class KCPosting extends KrautObject implements Comparable<KCPosting>{
	private static final long serialVersionUID = 2343973223217952495L;
	private static final SimpleDateFormat df = new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss");
	// we get the date as: 		EEE, dd MMM yyyy HH:mm:ss z"
	// we need to write it as: 	2008-07-17T09:24:17Z
	private static SimpleDateFormat dfShort = new SimpleDateFormat ("dd.MM. HH:mm");
	private static SimpleDateFormat dfOut = new SimpleDateFormat ("yyyy-MM-dd'T'HH:mm:ss");
	//private static final Pattern imgPat = Pattern.compile("href=\"/download/(\\d+\\..+?)/(.+?)\"");
	private static final Pattern imgPat = Pattern.compile("a\\s+href=\"/files/(\\d+\\..+?)\".+?src=\"?/thumbnails/(\\d+\\..+?)\\s\"?", Pattern.DOTALL);
	private static final Pattern linkPat = Pattern.compile("https?://w?w?w?\\.?(.+?)/(.+?)([\"\\s<>\\(\\)])");
	private static final Pattern kcLinkPat = Pattern.compile("<a href=\".+\">>>(\\d+)</a>");
	private static final Pattern uriPat = Pattern.compile("href=\"(.+?)\"");
	private static final Pattern spoilerPat = Pattern.compile("<span class=\"spoiler\">(.+?)</span>", Pattern.DOTALL);
	private static final Pattern quotePat = Pattern.compile("<span class=\"quote\">(.+?)</span>", Pattern.DOTALL);
	public Long 	kcNummer;
	public Long 	threadId;
	public Long 	threadKcNummer;
	public Long		created;
	public String 	title;
	public String 	creationDate;
	public String 	creationShortDate;
	public String 	content;
	public String	originalContent;
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
		URI,
		IMAGES,
		CONTENT
	}
	
	public void sanitizeContent () {
		String locContent = content;
		locContent = StringEscapeUtils.unescapeHtml4(locContent);
		locContent = locContent.replaceAll("<p>", "");
		locContent = locContent.replaceAll("</p>", " ");
		originalContent = locContent;
		
		locContent = locContent.replaceAll("onclick=\"highlightPost\\(\\'\\d+\\'\\);\"", "");		
		locContent = locContent.replaceAll(">>>(\\d+)</a>", " onclick='quoteClick(this); return false;' class=\"kclink\">&gt;&gt; $1</a>");

		locContent = locContent.replaceAll("<a href=\"/resolve(/.+?)\"\\s*>.+?</a>", "<a href=\"/resolve$1\" class=\"kclink\" onclick=\"Android.openKcLink('$1');return false;\">&gt;&gt; $1</a>");
		Matcher m = linkPat.matcher(locContent);
		//CharBuffer buf = CharBuffer.allocate(locContent.length()+1000);
		StringBuffer buf = new StringBuffer(locContent.length()+1000);
		int end = 0;
		while (m.find()) {
			int gc = m.groupCount();
			if (gc > 0) {
				buf.append(locContent.substring(end, m.start()));
				end = m.end();
				String host = m.group(1);
				String name = host;
				String styleClass="extlink";
				String androidFunction = "openExternalLink";
				String url = m.group(1)+"/"+m.group(2);
				if ((host.contains("youtube")) || (host.contains("youtu.be"))) {
					styleClass="ytlink";
					name = "YouTube";
					androidFunction = "openYouTubeVideo";
				} else if (host.contains("krautchan.net")){
					styleClass="kclink";
					name = ">>";
					host = "";
					androidFunction = "openKcLink";
				}
				buf.append("<a href=\"http://"+m.group(1)+"/"+m.group(2)+"\" class=\""+styleClass+"\" onclick=\"Android."+androidFunction+"('"+url+"');return false;\">"+name+"</a>"+m.group(3));
			}
		}
		buf.append(locContent.substring(end, locContent.length()));
		content = "<p><span>"+buf.toString().trim()+"</span></p>";
	}
	
	public String asHtml  (boolean showImages) {
		String innerHtml = "<div class=\"posthead\">" +
		"<p class=\"headline\"><b>"+kcNummer+"</b><time class='timeago' datetime='"+creationDate+"'>"+creationDate+"</time></p>";
		if (null != title) {
			innerHtml += "<p class=\"topic\">"+title+"</p>";
		}
		innerHtml += "</div>";
		innerHtml += content;
		if (showImages && imgs.length > 0) {
			innerHtml += "<div class=\"image-container\">";
			for (int i = 0; i < imgs.length; i++) {
				if (null != imgs[i]) {
					innerHtml += "<a class=\"kcimglink\" href=\"/files/"+imgs[i]+"\" onclick=\"Android.openImage('"+imgs[i]+"');return false;\"><img src=\"/thumbnails/"+thumbs[i]+"\"></a>";
				}
			}
			innerHtml += "</div>";
		}
		return "<div id='"+kcNummer+"'>"+innerHtml+"</div>";
	}
	
	public String getKcStyledContent() {
		String kcStyledContent = originalContent.replaceAll("<br>", "\n>");
		Matcher kcMatcher = kcLinkPat.matcher(kcStyledContent);
		while (kcMatcher.find()) {
			kcStyledContent = kcMatcher.replaceAll(">>"+kcMatcher.group(1));
		}
		Matcher spoilerMatcher = spoilerPat.matcher(kcStyledContent);
		while (spoilerMatcher.find()) {
			kcStyledContent = ">>"+spoilerMatcher.replaceAll("[spoiler]"+spoilerMatcher.group(1)+"[/spoiler]");
		}
		Matcher quoteMatcher = quotePat.matcher(kcStyledContent);
		while (quoteMatcher.find()) {
			kcStyledContent = quoteMatcher.replaceFirst(quoteMatcher.group(1));
			quoteMatcher = quotePat.matcher(kcStyledContent);
		}
		kcStyledContent = ">"+kcStyledContent;
		return kcStyledContent.trim(); 
	}
	
	public void setField(Fields fields, String arg) throws ParseException {
		switch (fields) {
			case KC_NUM: { 
				if (null != arg) {
					kcNummer = Long.parseLong(arg);
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
			case URI: { 
				Matcher m = uriPat.matcher(arg);
				if (m.find()) {
					uri = m.group(1);
					dbId = (long)uri.hashCode();
				}
				break;
			}
			case IMAGES: {
				Matcher m = imgPat.matcher(arg);
				int i = 0;
				while (m.find() && i < imgs.length) {
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
	
	public String toString() {
		return "Post: "+kcNummer+" - "+content.substring(0, 40);
	}

	@Override
	public int compareTo(KCPosting arg0) {
		return kcNummer.compareTo(arg0.kcNummer);
	}
}

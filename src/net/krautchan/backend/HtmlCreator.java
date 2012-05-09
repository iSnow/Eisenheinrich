package net.krautchan.backend;

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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.krautchan.data.*;



public class HtmlCreator {
	
	public static String addPostings (List<KCPosting> postings, String template) {
		Collections.sort(postings, new Comparator<KCPosting>() {
			@Override
			public int compare(KCPosting p1, KCPosting p2) {
				return p1.kcNummer.intValue() - p2.kcNummer.intValue();
			}
			
		});
		String html = template;
		for (KCPosting post : postings) {
			String innerHtml = "<p class=\"headline\"><b>"+post.kcNummer+"</b><time class='timeago' datetime='"+post.creationDate+"'>"+post.creationDate+"</time></p>";
			if (null != post.title) {
				innerHtml += "<p class=\"topic\">"+post.title+"</p>";
			}
			innerHtml += post.content;
			if (post.imgs.length > 0) {
				innerHtml += "<div class=\"image-container\">";
				for (int i = 0; i < post.imgs.length; i++) {
					if (null != post.imgs[i]) {
						innerHtml += "<a href=\"/files/"+post.imgs[i]+"\" target=\"_blank\"  onclick=\"alert('open:image:"+post.imgs[i]+"');return false;\"><img src=\"/thumbnails/"+post.imgs[i]+"\"></a>";
					}
				}
				innerHtml += "</div>";
			}
			html = html.replace("</ul>", "<li id='"+post.dbId+"'><div id='"+post.kcNummer+"'>"+innerHtml+"</div></li></ul>");
		}
		//FileHelpers.writeToSDFile("out_"+(new Date().getTime()+".html"), html);
		return html;
	}
}

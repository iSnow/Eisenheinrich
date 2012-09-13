package net.krautchan.android;

import java.util.Properties;

import org.apache.http.cookie.Cookie;

import net.krautchan.backend.Cache;
import net.krautchan.data.KCBoard;
import net.krautchan.data.KCThread;

/*
* Copyright (C) 2012 Johannes Jander (johannes@jandermail.de)
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


public class Globals {
	private String 				USER_AGENT = null;
	private Cache<KCBoard>  	BOARD_CACHE = new Cache<KCBoard>();
	private Cache<KCThread>  	THREAD_CACHE = new Cache<KCThread>(500);
	private boolean				DEBUG = true;
	
	private String 				IP_NUMBER = null;
	private Cookie				SESSION_COOKIE = null;
	private String				KOMTUR_CODE = null;
	
	private boolean 			VISITED_POSTS_COLLAPSIBLE = true;
	private boolean				SHOW_IMAGES = false;
	

	public Globals (Properties pr) {
		USER_AGENT = pr.getProperty("USER_AGENT");
		DEBUG = pr.getProperty("DEBUG").equalsIgnoreCase("true");
		KOMTUR_CODE = pr.getProperty("KOMTUR_CODE");
		VISITED_POSTS_COLLAPSIBLE = pr.getProperty("VISITED_POSTS_COLLAPSIBLE").equalsIgnoreCase("true");
		SHOW_IMAGES = pr.getProperty("SHOW_IMAGES").equalsIgnoreCase("true");
	}
	
	
	public boolean isDebugVersion() {
		return DEBUG;
	}

	public String getUserAgentString() {
		return USER_AGENT;
	}
	
	public Cache<KCBoard> getBoardCache() {
		return BOARD_CACHE;
	}
	
	public String getIpNumber() {
		return IP_NUMBER;
	}

	public Cookie getSessionCookie() {
		return SESSION_COOKIE;
	}

	public String getKomturCode() {
		return KOMTUR_CODE;
	}

	public boolean areVisitedPostsCollapsible() {
		return VISITED_POSTS_COLLAPSIBLE;
	}

	public boolean shouldShowImages() {
		return SHOW_IMAGES;
	}
	
	public void setDebugVersion (boolean debug) {
		DEBUG = debug;
	}

	public void setIpNumber(String ipNumber) {
		IP_NUMBER = ipNumber;
	}

	public void setSessionCookie(Cookie cookie) {
		SESSION_COOKIE = cookie;
	}

	public void setKomturCode(String code) {
		KOMTUR_CODE = code;
	}

	public void setVisitedPostsCollapsible(boolean collapsible) {
		VISITED_POSTS_COLLAPSIBLE = collapsible;
	}

	public void setShowImages(boolean show) {
		SHOW_IMAGES = show;
	}

	public void setUserAgentString(String userAgent) {
		USER_AGENT = userAgent;
	}
	
	public void setBoardCache (Cache<KCBoard> cache) {
		BOARD_CACHE = cache;
	}

	public Cache<KCThread> getThreadCache() {
		return THREAD_CACHE;
	}

	public void setThreadCache(Cache<KCThread> cache) {
		THREAD_CACHE = cache;
	}

}

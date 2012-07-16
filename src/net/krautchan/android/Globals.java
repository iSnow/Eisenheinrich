package net.krautchan.android;

import java.util.Properties;

import org.apache.http.cookie.Cookie;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;

import net.krautchan.backend.Cache;
import net.krautchan.data.KCBoard;

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
	
	
	public boolean isDEBUG() {
		return DEBUG;
	}

	public String getUSER_AGENT() {
		return USER_AGENT;
	}
	
	public Cache<KCBoard> getBOARD_CACHE() {
		return BOARD_CACHE;
	}
	
	public String getIP_NUMBER() {
		return IP_NUMBER;
	}

	public Cookie getSESSION_COOKIE() {
		return SESSION_COOKIE;
	}

	public String getKOMTUR_CODE() {
		return KOMTUR_CODE;
	}

	public boolean isVISITED_POSTS_COLLAPSIBLE() {
		return VISITED_POSTS_COLLAPSIBLE;
	}

	public boolean isSHOW_IMAGES() {
		return SHOW_IMAGES;
	}
	
	public void setDEBUG(boolean dEBUG) {
		DEBUG = dEBUG;
	}

	public void setIP_NUMBER(String iP_NUMBER) {
		IP_NUMBER = iP_NUMBER;
	}

	public void setSESSION_COOKIE(Cookie sESSION_COOKIE) {
		SESSION_COOKIE = sESSION_COOKIE;
	}

	public void setKOMTUR_CODE(String kOMTUR_CODE) {
		KOMTUR_CODE = kOMTUR_CODE;
	}

	public void setVISITED_POSTS_COLLAPSIBLE(boolean vISITED_POSTS_COLLAPSIBLE) {
		VISITED_POSTS_COLLAPSIBLE = vISITED_POSTS_COLLAPSIBLE;
	}

	public void setSHOW_IMAGES(boolean sHOW_IMAGES) {
		SHOW_IMAGES = sHOW_IMAGES;
	}

	public void setUSER_AGENT(String uSER_AGENT) {
		USER_AGENT = uSER_AGENT;
	}
	
	public void setBOARD_CACHE(Cache<KCBoard> bOARD_CACHE) {
		BOARD_CACHE = bOARD_CACHE;
	}

}

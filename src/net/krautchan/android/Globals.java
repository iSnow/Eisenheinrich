package net.krautchan.android;

import org.apache.http.cookie.Cookie;

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
	public boolean				DEBUG = true;
	public String 				USER_AGENT = null;
	public String 				IP_NUMBER = null;
	public Cookie				SESSION_COOKIE = null;
	public String				KOMTUR_CODE = null;
	public Cache<KCBoard>  		BOARD_CACHE = null;
}

package net.krautchan.android;

import android.os.Build;
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


public class Defaults {
	public static final String DOMAIN = "krautchan.net";
	public static final String BASE_URL = "https://krautchan.net/";
	public static final String POST_URL  = "https://krautchan.net/post";
	public static final String FILE_PATH = "https://krautchan.net/files";
	public static final String SD_DIR = "eisenheinrich";
	public static final String IMAGE_DIR = "/images";
	public static final String HOME_PAGE = "http://eisenheinrich.datensalat.net/";
	public static final String UPDATE_VERSION_URL = "http://eisenheinrich.datensalat.net/backend/version/sdk/"+Build.VERSION.SDK_INT;
	public static final String UPDATE_PAGE = "http://eisenheinrich.datensalat.net/mobile.html";
}
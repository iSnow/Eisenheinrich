package net.krautchan.android.network;

import java.io.IOException;

import net.krautchan.android.Defaults;
import net.krautchan.android.Globals;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
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

public class CookieHelper {

	public static void getMyIP(final Defaults defaults, final Globals globs) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				Document doc;
				try {
					doc = Jsoup.connect("http://checkip.dyndns.org/")
							.userAgent(globs.USER_AGENT)
							.cookie("auth", "token")
							.timeout(3000)
							.get();
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}
				Element body = doc.body();
				if (null == body) {
					return;
				}
				String segments[] = body.text().split("\\s+");
				if (segments.length > 0) {
					globs.IP_NUMBER = segments[3];
				} else {
					return;
				}
			}
		}).start();
	}
}

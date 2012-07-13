package net.krautchan.android;

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



import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import com.osbcp.cssparser.CSSParser;
import com.osbcp.cssparser.Rule;

import net.krautchan.android.helpers.FileHelpers;
import net.krautchan.android.network.AsyncPoster.AsyncPosterPeer;
import net.krautchan.android.network.CookieHelper;
import net.krautchan.android.network.PostVariables;
import net.krautchan.backend.Cache;
import net.krautchan.backend.DatabaseHelper;
import net.krautchan.data.KCBoard;
import net.krautchan.data.KCPosting;
import net.krautchan.data.KCThread;
import net.krautchan.data.KODataListener;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;

public class Eisenheinrich extends Application {
	public static Defaults DEFAULTS = new Defaults();
	public static Globals  GLOBALS = new Globals();
	public static Styles   STYLES; 
	private List<String> selectedBoards;
	public boolean hasImagesDir = false;
	private static Eisenheinrich sInstance;
	public DatabaseHelper dbHelper = new DatabaseHelper (this);
	private static List<KODataListener<KCThread>> tListeners = new ArrayList<KODataListener<KCThread>>();
	private static ConcurrentLinkedQueue<KCThread> threadQ = new ConcurrentLinkedQueue<KCThread>();
	private static KODataListener<KCThread> threadListener = new KODataListener<KCThread>() {
		@Override
		public void notifyAdded(KCThread item, Object token) {
			threadQ.add(item);
			for (KODataListener<KCThread> listener: tListeners) {
				listener.notifyAdded(item, token);
			}
		}

		@Override
		public void notifyDone(Object token) {
			for (KODataListener<KCThread> listener: tListeners) {
				listener.notifyDone(token);
			}
		}

		@Override
		public void notifyError(Exception ex, Object token) {
			for (KODataListener<KCThread> listener: tListeners) {
				listener.notifyError(ex, token);
			}
		}
	};

	private static List<KODataListener<KCPosting>> pListeners = new ArrayList<KODataListener<KCPosting>>();
	private static ConcurrentLinkedQueue<KCPosting> postQ = new ConcurrentLinkedQueue<KCPosting>();
	private static KODataListener<KCPosting> postListener = new KODataListener<KCPosting>() {
		@Override
		public void notifyAdded(KCPosting item, Object token) {
			//threadQ.add(item);
			for (KODataListener<KCPosting> listener: pListeners) {
				listener.notifyAdded(item, token);
			}
		}

		@Override
		public void notifyDone(Object token) {
			for (KODataListener<KCPosting> listener: pListeners) {
				listener.notifyDone(token);
			}
		}

		@Override
		public void notifyError(Exception ex, Object token) {
			for (KODataListener<KCPosting> listener: pListeners) {
				listener.notifyError(ex, token);
			}
		}
	};

	public static AsyncPosterPeer posterPeer = new AsyncPosterPeer() {
		private PostVariables vars;
		@Override
		public void storePostVariables(PostVariables vars) {
			this.vars = vars;
		}

		@Override
		public void notifyDone(boolean successful, String message) {
			if (successful)
				this.vars = null;
		}

		@Override
		public PostVariables getPostVariables() {
			return vars;
		}
	};

	public static Eisenheinrich getInstance() {
		return sInstance;
	}

	@Override
	public void onCreate() {
		super.onCreate();  
		sInstance = this;
		sInstance.initializeInstance();
	}

	protected void initializeInstance() {
		GLOBALS.BOARD_CACHE = new Cache<KCBoard>();
		GLOBALS.BOARD_CACHE.add(dbHelper.getBoards());
		GLOBALS.USER_AGENT = getUserAgentString ();
		hasImagesDir = FileHelpers.createSDDirectory(DEFAULTS.IMAGE_DIR);
		STYLES = new Styles(this);
		/*sessionHandler = new SessionHandler( 
            this.getSharedPreferences( "PREFS_PRIVATE", Context.MODE_PRIVATE ) );*/
	}

	public boolean isNetworkAvailable() {
		Context context = getApplicationContext();
		ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connectivity == null) {
			return false;
		} else {
			NetworkInfo[] info = connectivity.getAllNetworkInfo();
			if (info != null) {
				for (int i = 0; i < info.length; i++) {
					if (info[i].getState() == NetworkInfo.State.CONNECTED) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public ConcurrentLinkedQueue<KCThread> getThreadQueue () {
		return threadQ;
	}

	public KODataListener<KCThread> getThreadListener () {
		return threadListener;
	}

	public void addThreadListener (KODataListener<KCThread> listener) {
		tListeners.add(listener);
	}

	public void removeThreadListener (KODataListener<KCThread> listener) {
		if (tListeners.contains(listener)) {
			tListeners.remove(listener);
		}
	}

	public ConcurrentLinkedQueue<KCPosting> getPostQueue () {
		return postQ;
	}

	public KODataListener<KCPosting> getPostListener () {
		return postListener;
	}

	public List<String> getSelectedBoards() {
		return selectedBoards;
	}

	public void setSelectedBoards(List<String> selectedBoards) {
		this.selectedBoards = selectedBoards;
	}

	public void addPostListener (KODataListener<KCPosting> listener) {
		pListeners.add(listener);
	}

	public void removePostListener (KODataListener<KCPosting> listener) {
		if (pListeners.contains(listener)) {
			pListeners.remove(listener);
		}
	}

	public PackageInfo getVersionInfo () {
		PackageInfo pInfo;
		try {
			pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
		} catch (NameNotFoundException e) {
			return null;
		}
		return pInfo;
	}

	protected String getUserAgentString () {
		PackageInfo pInfo = getVersionInfo();
		int versionNumber = -1;
		if (null != pInfo) {
			versionNumber = pInfo.versionCode;
		}
		return "Mozilla/5.0 (android"+Build.VERSION.RELEASE+") Eisenheinrich "+versionNumber;

	}


	public DefaultHttpClient getHttpClient () {
		HttpParams httpParameters = new BasicHttpParams();
		// Set the timeout in milliseconds until a connection is established.
		int timeoutConnection = 3000;
		HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
		// Set the default socket timeout (SO_TIMEOUT) 
		// in milliseconds which is the timeout for waiting for data.
		int timeoutSocket = 10000;
		HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
		httpParameters.setParameter( "http.useragent", GLOBALS.USER_AGENT);
		DefaultHttpClient httpclient = new DefaultHttpClient(httpParameters);
		return httpclient;
	}
}

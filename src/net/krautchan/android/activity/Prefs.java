package net.krautchan.android.activity;

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.krautchan.android.Eisenheinrich;
import net.krautchan.backend.DatabaseHelper;
import net.krautchan.data.KCBoard;
import net.krautchan.parser.KCBoardListParser;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

// FIXME doing this as an Activity was a dumb idea. Move to Eisenheinrich.java
public class Prefs extends Activity {
	private static Prefs sInstance;
	private boolean disclaimerAck = false;

    @Override
    protected void onCreate(Bundle state){ 
       super.onCreate(state);
       sInstance = this;
       sInstance.initializeInstance();
       /*
       File file = new File ("/data/data/net.krautchan/cache/webviewCache/");
       if (file.exists()) {
    	   File files[] = file.listFiles();
    	  
       }
       */
       new Thread (new Runnable () {
		@Override
		public void run() {
			try {
				Map<String, KCBoard> boards = KCBoardListParser.getBoardList("http://krautchan.net/nav", "http://krautchan.net/", Eisenheinrich.GLOBALS.getUserAgentString());
				ArrayList<KCBoard> boardL = new ArrayList<KCBoard>();
				
				for (Entry<String, KCBoard> entry : boards.entrySet()) {
					boardL.add(entry.getValue());
				}
				
				HashMap<Long, KCBoard> storedBoardMap = new HashMap<Long, KCBoard>();
				Collection <KCBoard> storedBoards = Eisenheinrich.GLOBALS.getBoardCache().getAll();
				for (KCBoard board : storedBoards) {
					storedBoardMap.put(board.dbId, board);
				}
				for (KCBoard board : boardL) {
					KCBoard storedBoard = storedBoardMap.get(board.dbId);
					if (null != storedBoard) {
						board.show = storedBoard.show; 
						storedBoard.name = board.name;
					}
				}
				
				DatabaseHelper dbH = Eisenheinrich.getInstance().dbHelper;
				dbH.persistBoards(boardL);
			} catch (IOException e) {
				// do nothing, just terminate, ain't so important
			}
		}
    	   
       }).start();
       // Restore preferences
       SharedPreferences settings = getPreferences(0);
       disclaimerAck = settings.getBoolean("disclaimerAck", false);
       startActivity(new Intent(this, EisenheinrichActivity.class));
    }

    public synchronized static Prefs getInstance() {
    	return sInstance;
    }
    
    protected void initializeInstance() {
        SharedPreferences settings = getPreferences(0);
        disclaimerAck = settings.getBoolean("disclaimerAck", false);
    }

	public boolean isDisclaimerAck() {
		return disclaimerAck;
	}

	public void setDisclaimerAck(boolean disclaimerAck) {
		this.disclaimerAck = disclaimerAck;
		SharedPreferences settings = getPreferences(0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean("disclaimerAck", disclaimerAck);
		editor.commit();
	}
    
    
}
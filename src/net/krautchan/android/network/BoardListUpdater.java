package net.krautchan.android.network;

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

public class BoardListUpdater {

	
	public void updateBoards () {
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
	}
}

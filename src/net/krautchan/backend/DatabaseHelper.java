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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;

import junit.framework.Assert;
import net.krautchan.data.KCBoard;
import net.krautchan.data.KCThread;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {
	private static final String TAG = "DatabaseHelper";
	private final static String	DBASE_NAME 		= "Schlaubernd";
	private final static int 	VERSION_NUM 	=  13;
	private static final String BOARD_TABLE		= "board";
	private static final String THREAD_TABLE	= "thread";
	private static final String POST_TABLE		= "posting";
	@SuppressWarnings("unused")
	private static boolean debug = false;

	public DatabaseHelper(Context context) {
		super(context, DBASE_NAME, null, VERSION_NUM); 
	}
	
	public void setDebug (boolean debug) {
		DatabaseHelper.debug = debug;
		if (debug) {
			logDbStats();
			try {
                File sd = Environment.getExternalStorageDirectory();

                if (sd.canWrite()) {
                    String currentDBPath = getReadableDatabase().getPath();
                    //"//data//"+ Eisenheinrich.getInstance().getPackageName() +"//databases//"+DBASE_NAME;
                    File currentDB = new File(currentDBPath);
                    File backupDB = new File(sd, DBASE_NAME+".db");

                        FileChannel src = new FileInputStream(currentDB).getChannel();
                        FileChannel dst = new FileOutputStream(backupDB).getChannel();
                        dst.transferFrom(src, 0, src.size());
                        src.close();
                        dst.close();

                }
            } catch (Exception e) {
            	Log.e (TAG, e.getMessage());
            }
		}
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		try {
			createBoardTable (db);
			createThreadTable (db);
			createPostingable (db);
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		try {  
			db.beginTransaction();
			db.execSQL("DROP TABLE IF EXISTS "+BOARD_TABLE);
			db.execSQL("DROP TABLE IF EXISTS "+THREAD_TABLE);
			db.execSQL("DROP TABLE IF EXISTS "+POST_TABLE);
			db.setTransactionSuccessful();
			db.endTransaction();
			db.beginTransaction();
			createBoardTable(db);
			createThreadTable(db);
			createPostingable (db);
			db.setTransactionSuccessful();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			db.endTransaction();
		}
	}
	 
	@Override
	public void close() {
		SQLiteDatabase db = getReadableDatabase();
		db.close();
	}

	public Collection <KCBoard> getBoards () {
		ConcurrentLinkedQueue<KCBoard> boards = new ConcurrentLinkedQueue<KCBoard>();
		Cursor c = retrieveAllBoards();
		c.moveToFirst();
		while (!c.isAfterLast()) {
			KCBoard board = new KCBoard();
			board.dbId = c.getLong (c.getColumnIndex("_id")); 
			board.uri = c.getString(c.getColumnIndex("url"));
			board.shortName = c.getString(c.getColumnIndex("shortname"));
			board.name = c.getString(c.getColumnIndex("name"));
			board.show = c.getInt(c.getColumnIndex("show")) != 0;
			boards.add(board);
			c.moveToNext();
		}
		c.close();
		return boards;
	}
	
	public void deleteThread(Long dbId) {
		SQLiteDatabase db = getReadableDatabase();
		try {
			db.beginTransaction();
			db.delete(THREAD_TABLE, "ID="+dbId, null);
			db.setTransactionSuccessful();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			db.endTransaction();
		}
	}
	
	public void bookmarkThread (KCThread thread) {
		thread.bookmarked = true;
		persistThread (thread);
	}
	
	public void persistThread (KCThread thread) {
		if (null == thread) {
			return;
		}
		if (null == thread.dbId) {
			throw new IllegalArgumentException ("Thread ID must not be null");
		}
		if (null == thread.board_id) {
			throw new IllegalArgumentException ("Board ID must not be null");
		}
		if (null == thread.kcNummer) {
			throw new IllegalArgumentException ("KC Number must not be null");
		}
		if (null == thread.uri) {
			throw new IllegalArgumentException ("Thread URI must not be null");
		}
		if (null == thread.digest) {
			throw new IllegalArgumentException ("Thread Digest must not be null");
		}
		if (null == thread.firstPostDate) {
			throw new IllegalArgumentException ("First post time must not be null");
		}
		SQLiteDatabase db = getReadableDatabase();
		try{
			if (getThread (thread.dbId) != null) {
				String whereClause = "id=" + thread.dbId;
				ContentValues valHolder = new ContentValues();
				valHolder.put("fk_board", thread.board_id);
				valHolder.put("kc_number", thread.kcNummer);
				valHolder.put("last_kc_number", thread.previousLastKcNum);
				valHolder.put("url", thread.uri);
				valHolder.put("first_post_date", thread.firstPostDate);
				valHolder.put("digest", thread.digest);
				valHolder.put("time_inserted", new Date().getTime());
				valHolder.put("offset", 0);
				if (thread.bookmarked) { // don't overwrite bookmark signal
					valHolder.put("is_bookmarked", 1);
				}
				valHolder.put("is_hidden", thread.hidden ? 1 : 0);
				valHolder.put("is_visited", (thread.visited == null) ?  0 : thread.visited);
				db.update(THREAD_TABLE, valHolder, whereClause, null);
			} else {
				ContentValues valHolder = new ContentValues();
				valHolder.put("id", thread.dbId);
				valHolder.put("fk_board", thread.board_id);
				valHolder.put("kc_number", thread.kcNummer);
				valHolder.put("last_kc_number", thread.previousLastKcNum);
				valHolder.put("url", thread.uri);
				valHolder.put("first_post_date", thread.firstPostDate);
				valHolder.put("digest", thread.digest);
				valHolder.put("time_inserted", new Date().getTime());
				valHolder.put("offset", 0);
				valHolder.put("is_bookmarked", thread.bookmarked ? 1 : 0);
				valHolder.put("is_hidden", thread.hidden ? 1 : 0);
				valHolder.put("is_visited", (thread.visited == null) ?  0 : thread.visited);
				db.insert(THREAD_TABLE, null, valHolder);
			} 
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}
	
	public KCThread getThread (Long id) {
		Assert.assertNotNull(id);
		KCThread thread = null;
		Cursor c = retrieveThread(id);
		c.moveToFirst();
		while (!c.isAfterLast()) {
			thread = populateThread (c);
			c.moveToNext();
		}
		c.close();
		return thread;
	}
	
	public Collection <KCThread> getAllThreads () {
		return getThreads (false);
	}
	
	public Collection <KCThread> getBookmarks () {
		return getThreads (true);
	}
	
	private Collection <KCThread> getThreads (boolean bookmarksOnly) {
		ConcurrentLinkedQueue<KCThread> threads = new ConcurrentLinkedQueue<KCThread>();
		Cursor c = retrieveAllThreads(bookmarksOnly);
		c.moveToFirst();
		while (!c.isAfterLast()) {
			KCThread thread = populateThread (c);
			threads.add(thread);
			c.moveToNext();
		}
		c.close();
		return threads;
	}
	
	public void persistBoards (Collection<KCBoard> boards) {
		
		SQLiteDatabase db = getReadableDatabase();
		try{
			db.beginTransaction();
			db.execSQL("drop table "+BOARD_TABLE);
			createBoardTable (db);
			int loop = 1;
			for (KCBoard board :boards) {
				ContentValues valHolder = new ContentValues();
				valHolder.put("id", board.dbId);
				valHolder.put("shortname", board.shortName);
				valHolder.put("name", board.name);
				valHolder.put("url", board.uri);
				valHolder.put("sort_order", loop++);
				valHolder.put("show", board.show ? 1 : 0);
				db.insert(BOARD_TABLE, null, valHolder);
			}
			db.setTransactionSuccessful();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			db.endTransaction();
			db.execSQL("VACUUM");
		}
	}
	
	private KCThread populateThread (Cursor c) {
		KCThread thread = null;
		try {
			thread = new KCThread();
			thread.dbId = c.getLong (c.getColumnIndex("_id")); 
			thread.kcNummer = c.getLong (c.getColumnIndex("kc_number")); 
			thread.previousLastKcNum = c.getLong (c.getColumnIndex("last_kc_number")); 
			thread.uri = c.getString(c.getColumnIndex("t_url"));
			thread.board_id = c.getLong (c.getColumnIndex("b_id")); 
			thread.firstPostDate = c.getLong (c.getColumnIndex("first_post_date"));
			thread.digest = c.getString(c.getColumnIndex("digest"));
			thread.hidden = (c.getInt(c.getColumnIndex("is_hidden")) == 1);
			thread.bookmarked = (c.getInt(c.getColumnIndex("is_bookmarked")) == 1);
			thread.visited = c.getLong(c.getColumnIndex("is_visited"));
		} catch (IllegalStateException ex) {
			System.err.print(ex.getMessage());
		}
		return thread;
	}	

	private Cursor retrieveAllBoards() {
		SQLiteDatabase db = getReadableDatabase();
		return db.query(BOARD_TABLE, new String[] { "id as _id",
				"shortname", "name", "url", "show" }, null, null, null, null, "sort_order");
	}
	
	private Cursor retrieveAllThreads(boolean bookmarksOnly) {
		SQLiteDatabase db = getReadableDatabase();
		String query = "select " +
				" t.id _id, " +
				" b.id b_id, " +
				" t.kc_number, " +
				" t.last_kc_number, "+
				" t.url t_url, " +
				" t.digest, " +
				" t.first_post_date first_post_date, "+
				" t.is_bookmarked is_bookmarked, " +
				" t.is_hidden is_hidden, " +
				" t.is_visited is_visited " +
				" from "
			+THREAD_TABLE+" t join "+BOARD_TABLE+" b "
			+" on t.fk_board = b.id ";
		if (bookmarksOnly) {
			query += " where t.is_bookmarked = 1";
		}
		return db.rawQuery(query, null);
	}
	
	private Cursor retrieveThread(Long id) {
		SQLiteDatabase db = getReadableDatabase();
		String query = "select " 
				+ " t.id _id, " 
				+ " b.id b_id, " 
				+ " t.kc_number, " 
				+ " t.last_kc_number, "
				+ " t.url t_url, "
				+ " t.digest digest, "
				+ " t.first_post_date first_post_date, " 
				+ " t.is_bookmarked is_bookmarked, "
				+ " t.is_hidden is_hidden, "
				+ " t.is_visited is_visited "
			+" from "+THREAD_TABLE+" t join "+BOARD_TABLE+" b "
			+" on t.fk_board = b.id  where t.id = CAST(? AS INTEGER)" ;
		return db.rawQuery(query,  new String[]{String.valueOf(id)});
	}

	private void createBoardTable (SQLiteDatabase db) throws SQLException {
		db.execSQL("create table "+BOARD_TABLE+" (" +
				" id integer primary key," +
				" shortname text," +
				" name text," +
				" url text," +
				" sort_order integer," +
				" show integer)");		
	}
	
	private void createThreadTable (SQLiteDatabase db) throws SQLException {
		db.execSQL("create table "+THREAD_TABLE+" (" 
				+ " id integer primary key,"
				+ " fk_board integer not null, " 
				+ " kc_number integer not null, "
				+ " last_kc_number integer, "
				+ " url text not null, " 
				+ " digest text not null, " 
				+ " first_post_date integer not null, " 
				+ " time_inserted integer not null, " 
				+ " offset integer not null, " 
				+ " is_bookmarked integer, " 
				+ " is_hidden integer, "
				+ " is_visited integer, "
				+" FOREIGN KEY(fk_board) REFERENCES "+BOARD_TABLE+"(id))");	
	}
	
	private void createPostingable (SQLiteDatabase db) throws SQLException {
		db.execSQL("create table "+POST_TABLE+" (" 
				+ " id integer primary key,"
				+ " fk_thread integer not null, " 
				+ " kc_number integer not null, "
				+ " post_date integer not null, " 
				+ " html text not null, " 
				+ " img0 text, " 
				+ " img1 text, " 
				+ " img2 text, " 
				+ " img3 text, " 
				+" FOREIGN KEY(fk_thread) REFERENCES "+THREAD_TABLE+"(id))");	
	}
	
	public void logDbStats () {
		Collection<KCBoard> boards = getBoards ();
		Log.d(TAG, "Number of Boards: "+boards.size());
		for (KCBoard board : boards) {
			Log.d (TAG, " Board "+board.shortName +" - "+board.name);
		}
		Collection<KCThread> threads = getAllThreads();
		for (KCThread thread : threads) {
			Log.d(TAG, " Thread "+thread.dbId+" - "+thread.kcNummer+" - "+thread.uri+" - "+thread.digest); 
		}
		Log.d(TAG, "Number of Threads: "+threads.size());
	}
}

package com.dany.groupbox;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBAdapter extends SQLiteOpenHelper {

	private static final String DB_NAME = "EVENTS_DB";
	private static final int DB_VERSION = 1;

	private static final String TABLE_NAME = "DATA";

	private static final String COL_ID = "_id";
	public static final String COL_NAME = "name";
	public static final String COL_START_TIME = "start";
	public static final String COL_END_TIME = "end";
	public static final String COL_FOLDER = "folder";
	public static final String COL_SOCIAL_API = "socials";
	public static final String COL_MESSAGE = "message";
	public static final String COL_SERVER_ID = "server_id";

	public DBAdapter(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		String tableCreateString = null;

		tableCreateString = "CREATE TABLE " + TABLE_NAME + " (" +
				COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
				COL_NAME + " TEXT, " +
				COL_START_TIME + " INTEGER, " +
				COL_END_TIME + " INTEGER, " +
				COL_FOLDER + " TEXT, " +
				COL_SOCIAL_API + " INTEGER, " +
				COL_MESSAGE + " TEXT, " +
				COL_SERVER_ID + " TEXT"
				+ ");";
		Log.d("SQLQueryExec", tableCreateString);
		db.execSQL(tableCreateString);

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// TODO Auto-generated method stub
	}

	public int insertEvent(EventHolder event){
		ContentValues cv = new ContentValues();
		cv.put(COL_NAME, event.getName());
		cv.put(COL_START_TIME, event.getStart());
		cv.put(COL_END_TIME, event.getEnd());
		cv.put(COL_FOLDER, event.getFolder());
		cv.put(COL_SOCIAL_API, event.getSocials());
		cv.put(COL_MESSAGE, event.getMessage());
	
		SQLiteDatabase db = this.getWritableDatabase();
		long rowId = db.insertOrThrow(TABLE_NAME, null, cv);
		db.close();
		
		return (int) rowId;
	}
	
	public void setServerIDToEvent(int id, String serverID){
		ContentValues cv = new ContentValues();
		cv.put(COL_SERVER_ID, serverID);
		
		String where = COL_ID + "=" + id;
		
		SQLiteDatabase db = this.getWritableDatabase();
		db.update(TABLE_NAME, cv, where, null);
		db.close();
	}
	
	public void removeEvent(int id){
		SQLiteDatabase db = this.getWritableDatabase();
		String where = COL_ID + "=" + id;
		db.delete(TABLE_NAME, where, null);
		db.close();
	}
	
	public Cursor queryAllEvents(){
		Cursor c = null;
		
		SQLiteDatabase db = this.getReadableDatabase();
		c = db.query(TABLE_NAME, null, null, null, null, null, "_id DESC");
		//db.close(); This Causes exception: "Cannot perform operation because connection pool has been closed
		
		return c;
	}
	
	public EventHolder querySingleEvent(int id){
		EventHolder event = null;
		
		String where = COL_ID + "=" + id;
		
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor c = db.query(TABLE_NAME, null, where, null, null, null, null);
		
		if(c.getCount() > 0){
			c.moveToFirst();
			event = new EventHolder(
					c.getString(c.getColumnIndex(COL_NAME)),
					c.getLong(c.getColumnIndex(COL_START_TIME)),
					c.getLong(c.getColumnIndex(COL_END_TIME)),
					c.getString(c.getColumnIndex(COL_FOLDER)),
					c.getInt(c.getColumnIndex(COL_SOCIAL_API)),
					c.getString(c.getColumnIndex(COL_MESSAGE)));
			event.setServerID(c.getString(c.getColumnIndex(COL_SERVER_ID)));
		}
		
		db.close();
		
		return event;
	}
}

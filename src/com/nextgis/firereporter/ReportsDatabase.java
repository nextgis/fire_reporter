/*******************************************************************************
*
* MainActivity
* ---------------------------------------------------------
* Report and view fires
*
* Copyright (C) 2013 NextGIS (http://nextgis.ru)
*
* This source is free software; you can redistribute it and/or modify it under
* the terms of the GNU General Public License as published by the Free
* Software Foundation; either version 2 of the License, or (at your option)
* any later version.
*
* This code is distributed in the hope that it will be useful, but WITHOUT ANY
* WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
* details.
*
* A copy of the GNU General Public License is available on the World Wide Web
* at <http://www.gnu.org/copyleft/gpl.html>. You can also obtain it by writing
* to the Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston,
* MA 02111-1307, USA.
*
*******************************************************************************/
package com.nextgis.firereporter;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class ReportsDatabase extends SQLiteOpenHelper {
	
	private static final String TAG = "firereporter:PositionDatabase";
	
	public static final String TABLE_POS = "reports";
	public static final String COLUMN_ID = "rowid";
	public static final String COLUMN_LAT = "lat";
	public static final String COLUMN_LON = "lon";
	public static final String COLUMN_AZIMUTH = "azimuth";
	public static final String COLUMN_DISTANCE = "distance";
	public static final String COLUMN_COMMENT = "comment";
	public static final String COLUMN_DATE = "report_date";
	
	private static final String DATABASE_NAME = "fires_reports.db";
	private static final int DATABASE_VERSION = 1;
	
	private static final String DATABASE_CREATE = "create table "
			+ TABLE_POS + "( " 
			+ COLUMN_ID + " integer primary key autoincrement, "
			+ COLUMN_DATE + " DEFAULT CURRENT_TIMESTAMP, "
			+ COLUMN_LAT + " real," 
			+ COLUMN_LON + " real," 
			+ COLUMN_AZIMUTH + " real, "
			+ COLUMN_DISTANCE + " real, "
			+ COLUMN_COMMENT + " text );";
	
	public ReportsDatabase(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(DATABASE_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// Logs that the database is being upgraded
        Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");

        // Kills the table and existing data
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_POS);

        // Recreates the database with a new version
        onCreate(db);
	}

}

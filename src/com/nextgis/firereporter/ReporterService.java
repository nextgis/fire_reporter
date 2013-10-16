/*******************************************************************************
 * Project:  Fire reporter
 * Purpose:  Report and view fires
 * Author:   Dmitry Baryshnikov (aka Bishop), polimax@mail.ru
 *******************************************************************************
* Copyright (C) 2011,2013 NextGIS (http://nextgis.ru)
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import android.os.AsyncTask;
import android.os.IBinder;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;

public class ReporterService extends Service {
	
	private static final String TAG = "ReporterService";
    
    public static final String ACTION_START = "com.nextgis.firereporter.sendpos.action.START";
    public static final String ACTION_STOP = "com.nextgis.firereporter.sendpos.action.STOP";

	private SQLiteDatabase ReportsDB;
	private ReportsDatabase dbHelper;
	
	private SendFireDataTask fireDataSender = null;
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void onCreate() {
		Log.d(TAG, "onCreate()");
		super.onCreate();
		
        dbHelper = new ReportsDatabase(this.getApplicationContext());
        ReportsDB = dbHelper.getWritableDatabase(); 
        
        //HttpGetter.IsNetworkAvailible(getApplicationContext());
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestroy()");
		super.onDestroy();
		
		dbHelper.close();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "Received start id " + startId + ": " + intent);
		super.onStartCommand(intent, flags, startId);
		
		if(intent == null)
			return START_STICKY;
		String action = intent.getAction();
        if (action.equals(ACTION_STOP))
        {
        	this.stopSelf(); 
        }
        else if(action.equals(ACTION_START))
        {
        	Log.d(TAG, "Action " + ACTION_START);
        	Context c = this.getApplicationContext();
           	SharedPreferences prefs = getSharedPreferences(MainActivity.PREFERENCES, MODE_PRIVATE | MODE_MULTI_PROCESS); 
           	long nMinTimeBetweenSend = prefs.getLong(SettingsActivity.KEY_PREF_INTERVAL + "_long", DateUtils.MINUTE_IN_MILLIS);
           	boolean bSendInSuspend = prefs.getBoolean(SettingsActivity.KEY_PREF_SEND_IN_SUSPEND, true);

           	if(!HttpGetter.IsNetworkAvailible(c)){
               	ScheduleNextUpdate(c, nMinTimeBetweenSend, bSendInSuspend);
               	Log.d(TAG, "network not availible");
               	return START_NOT_STICKY;
        	}
           	
           	Cursor cursor = ReportsDB.query(ReportsDatabase.TABLE_POS, null, null, null, null, null, null);
           	Log.d(TAG, "record count = " + cursor.getCount());
           	if(cursor.getCount() < 1){           		
           		this.stopSelf(); 
           		return START_NOT_STICKY;
           	}
           	
         	if(fireDataSender == null){
         		Log.d(TAG, "new fireDataSender");
         		fireDataSender = new SendFireDataTask();
         		fireDataSender.execute(this.getApplicationContext());
         		ScheduleNextUpdate(c, nMinTimeBetweenSend, bSendInSuspend);
         	}
        	else if(fireDataSender.getStatus() == AsyncTask.Status.FINISHED){
        		Log.d(TAG, "exist fireDataSender");
        		fireDataSender.execute(this.getApplicationContext());
        		ScheduleNextUpdate(c, nMinTimeBetweenSend, bSendInSuspend);
        	}
        	else if(fireDataSender.getStatus() == AsyncTask.Status.PENDING || fireDataSender.getStatus() == AsyncTask.Status.RUNNING){
        		Log.d(TAG, "exist fireDataSender executing");
        		ScheduleNextUpdate(c, nMinTimeBetweenSend, bSendInSuspend);
        	}
        	else{
        		Log.d(TAG, "unexpected behaviour");
        		this.stopSelf();
        	}
        }        
        return START_NOT_STICKY;
	}	
	
    public void ScheduleNextUpdate(Context context, long nMinTimeBetweenSend, boolean bEnergyEconomy){
		if(context == null)
			return;

		Intent intent = new Intent(ReporterService.ACTION_START);
		PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

       // The update frequency should often be user configurable.  This is not.

		long currentTimeMillis = System.currentTimeMillis();
		long nextUpdateTimeMillis = currentTimeMillis + nMinTimeBetweenSend;
		Time nextUpdateTime = new Time();
		nextUpdateTime.set(nextUpdateTimeMillis);

		AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		if(bEnergyEconomy)
			alarmManager.set(AlarmManager.RTC, nextUpdateTimeMillis, pendingIntent);
		else
			alarmManager.set(AlarmManager.RTC_WAKEUP, nextUpdateTimeMillis, pendingIntent);
	}

	private class SendFireDataTask extends AsyncTask<Context, Void, Void> {
		 @Override
	     protected Void doInBackground(Context... context) {
			Log.d(TAG, "SendPostionData");

           	SharedPreferences prefs = getSharedPreferences(MainActivity.PREFERENCES, MODE_PRIVATE | MODE_MULTI_PROCESS); 
           	String sHost = prefs.getString(SettingsActivity.KEY_PREF_SRV_USER, getResources().getString(R.string.stDefaultServer));
            String sUser = prefs.getString(SettingsActivity.KEY_PREF_SRV_USER_USER, "firereporter");
            String sPass = prefs.getString(SettingsActivity.KEY_PREF_SRV_USER_PASS, "8QdA4");

			//Queue records to send
			Cursor cursor = ReportsDB.query(ReportsDatabase.TABLE_POS, null, null, null, null, null, null);
	    	cursor.moveToFirst();
	    	Log.d(TAG, "record count: " + cursor.getCount());
		    	
	    	List<Long> delete_ids = new ArrayList<Long>();
		   	
	    	while(!cursor.isAfterLast())
	    	{
	    
	           	if(!HttpGetter.IsNetworkAvailible(context[0])){
	           		break;
	        	}
	            
	            String sLat = cursor.getString(cursor.getColumnIndexOrThrow(ReportsDatabase.COLUMN_LAT));
	            String sLon = cursor.getString(cursor.getColumnIndexOrThrow(ReportsDatabase.COLUMN_LON));
	            String sAz = cursor.getString(cursor.getColumnIndexOrThrow(ReportsDatabase.COLUMN_AZIMUTH));
	            String sDist = cursor.getString(cursor.getColumnIndexOrThrow(ReportsDatabase.COLUMN_DISTANCE));
	            String sComment = cursor.getString(cursor.getColumnIndexOrThrow(ReportsDatabase.COLUMN_COMMENT));
	            String sDate = cursor.getString(cursor.getColumnIndexOrThrow(ReportsDatabase.COLUMN_DATE));
		    	Long nId = cursor.getLong(cursor.getColumnIndexOrThrow(ReportsDatabase.COLUMN_ID));
		    	
		    	List<NameValuePair> params = new LinkedList<NameValuePair>();
		    	params.add(new BasicNameValuePair("function", "store_row"));
		    	params.add(new BasicNameValuePair("lat", sLat));
		    	params.add(new BasicNameValuePair("lon", sLon));
		    	params.add(new BasicNameValuePair("az", sAz));
		    	params.add(new BasicNameValuePair("dist", sDist));
		    	params.add(new BasicNameValuePair("comment", sComment));
		    	params.add(new BasicNameValuePair("date", sDate));
		    	params.add(new BasicNameValuePair("user", sUser));
		    	params.add(new BasicNameValuePair("pass", sPass));
		    	
	            String sData = URLEncodedUtils.format(params, "utf-8");
		    	
		    	if(SendData(sHost + "?" + sData))
		    	{
		    		//delete record
		    		delete_ids.add(nId);
		    	}
		    	cursor.moveToNext();
	    	}
			cursor.close();   
			
			for(int i = 0; i < delete_ids.size(); i++)
			{
				ReportsDB.delete(ReportsDatabase.TABLE_POS, ReportsDatabase.COLUMN_ID + " = " + delete_ids.get(i), null);
			}
			 
			return null;
		}
			
		protected boolean SendData(String sCommand)
		{
			Log.d(TAG, "SendData: command=" + sCommand);
	
			HttpClient httpClient = new DefaultHttpClient();
			HttpGet httpGet = new HttpGet(sCommand);
			try {
				HttpResponse response = httpClient.execute(httpGet);
				if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
					HttpEntity entity = response.getEntity();
					JSONObject jsonMainObject = null;
					boolean bHasErrors = true;
					try {
						jsonMainObject = new JSONObject(EntityUtils.toString(entity));
						if(jsonMainObject != null)
							bHasErrors = jsonMainObject.getBoolean("error");
					} catch (ParseException e) {
						e.printStackTrace();
					} catch (JSONException e) {
						e.printStackTrace();
					}					
			    	  
					return !bHasErrors;
				}
			} catch (ClientProtocolException e) {
				e.printStackTrace();
				return false;
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			return false;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			//stopSelf(); 
		}
	}
}

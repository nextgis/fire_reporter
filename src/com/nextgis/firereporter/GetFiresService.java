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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.ResultReceiver;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.widget.Toast;


public class GetFiresService extends Service {
	private NotificationManager mNM;
	
    private Handler mFillDataHandler; 
    private ResultReceiver mUserNasaReceiver, mScanexReceiver;
    private LocationManager mLocManager;
    private int mnFilter;
    private int mnCurrentExec;
    private Map<Integer, FireItem> mmoFires; 
    private String msScanexLoginCookie;
    
	public final static int SERVICE_START = 1;
	public final static int SERVICE_STOP = 2;
	public final static int SERVICE_ERROR = 3;
	public final static int SERVICE_DATA = 4;
	public final static int SERVICE_DESTROY = 5;
	public final static int SERVICE_UPDATE = 6;
	public final static int SERVICE_SCANEXSTART = 7;
	public final static int SERVICE_SCANEXDATA = 8;
	
	protected Time mSanextCookieTime;
	protected final static int USER_ID = 777;
	
	protected Map<Long, SubscriptionItem> mmoSubscriptions;

    
	@Override
	public void onCreate() {
		Log.d(MainActivity.TAG, "onCreate()");
		super.onCreate();
		
		Prepare();
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(MainActivity.TAG, "Received start id " + startId + ": " + intent);
		super.onStartCommand(intent, flags, startId);

		int nCommnad = intent.getIntExtra("command", SERVICE_START);
		mnFilter = intent.getIntExtra("src", MainActivity.SRC_NASA | MainActivity.SRC_USER);
		
		SharedPreferences prefs = getSharedPreferences(MainActivity.PREFERENCES, MODE_PRIVATE | MODE_MULTI_PROCESS);
		long nUpdateInterval = prefs.getLong(SettingsActivity.KEY_PREF_UPDATE_DATA_TIME + "_long",  DateUtils.MINUTE_IN_MILLIS); //15
		boolean bEnergyEconomy = prefs.getBoolean(SettingsActivity.KEY_PREF_SERVICE_BATT_SAVE, true);
		
		switch(nCommnad){
		case SERVICE_START:
			mUserNasaReceiver = intent.getParcelableExtra("receiver");		

			if(mnCurrentExec < 1){
				mUserNasaReceiver.send(SERVICE_START, new Bundle());
			}
			Log.d(MainActivity.TAG, "GetFiresService service started");
			if((mnFilter & MainActivity.SRC_NASA) !=0 ){
				mnCurrentExec++;
				GetNasaData(false);
			}

			if((mnFilter & MainActivity.SRC_USER) !=0 ){
				mnCurrentExec++;
				GetUserData(false);
			}
			
			// plan next start
			ScheduleNextUpdate(this, nCommnad, nUpdateInterval, bEnergyEconomy);
			break;
		case SERVICE_SCANEXSTART:
			mScanexReceiver = intent.getParcelableExtra("receiver_scanex");		

			if(mnCurrentExec < 1){
				mScanexReceiver.send(SERVICE_SCANEXSTART, new Bundle());
			}
			Log.d(MainActivity.TAG, "GetFiresService service started");
			mnCurrentExec++;
			GetScanexData(false);
			
			// plan next start
			ScheduleNextUpdate(this, nCommnad, nUpdateInterval, bEnergyEconomy);
			break;
			
		case SERVICE_STOP:
			Log.d(MainActivity.TAG, "GetFiresService service stopped");
			ScheduleNextUpdate(this, SERVICE_DESTROY, 150, true);
			stopSelf();
			break;
		case SERVICE_DESTROY:
			stopSelf();
			break;
		case SERVICE_UPDATE:
			mUserNasaReceiver = intent.getParcelableExtra("receiver");		
			mScanexReceiver = intent.getParcelableExtra("receiver_scanex");		
			break;
		case SERVICE_DATA:
			mUserNasaReceiver = intent.getParcelableExtra("receiver");		
			for(FireItem item : mmoFires.values()){
				SendItem(item);
			}
			break;
		case SERVICE_SCANEXDATA:
			mScanexReceiver = intent.getParcelableExtra("receiver_scanex");		
			GetScanexData(false);
			break;
		}
		
        return START_STICKY;
	}
    
	protected void Prepare(){
		mLocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		mnCurrentExec = 0;
		mmoFires = new HashMap<Integer, FireItem>();
		mSanextCookieTime = new Time();
		mSanextCookieTime.setToNow();
		msScanexLoginCookie = new String("not_set");
		
    	mFillDataHandler = new Handler() {
    	    public void handleMessage(Message msg) {
    	    	
    	    	mnCurrentExec--;    	    	
    	    	
    	    	Bundle resultData = msg.getData();
    	    	boolean bHaveErr = resultData.getBoolean("error");
    	    	if(bHaveErr){
    	    		SendError(resultData.getString("err_msq"));
    	    	}
    	    	else{
    	    		int nType = resultData.getInt("src");
	    			String sData = resultData.getString("json");
    	    		switch(nType){
    	    		case 3:
    	    			FillScanexData(nType, sData);
    	    			break;
    	    		case 4:
    	    			msScanexLoginCookie = sData;
    	    			mSanextCookieTime.setToNow();
    	    			GetScanexData(false);
    	    			break;
    	    		default:
    	    			FillData(nType, sData);
    	    			break;
    	    		}
    	    	}
    	    	GetDataStoped();
    	    };
    	};	
	 }

	 protected void ScheduleNextUpdate(Context context, int nCommand, long nMinTimeBetweenSend, boolean bEnergyEconomy)
	 {
		if(context == null)
			return;

		Intent intent = new Intent(MainActivity.INTENT_NAME);	
        intent.putExtra("receiver", mUserNasaReceiver);
        intent.putExtra("receiver_scanex", mScanexReceiver);
        intent.putExtra("command", nCommand);
        intent.putExtra("src", mnFilter);

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

	protected void GetUserData(boolean bShowProgress){
        Location currentLocation = null;
        String sLat = null, sLon = null;

        if(mLocManager != null){
        	currentLocation = mLocManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);        
        	if(currentLocation == null){
        		currentLocation = mLocManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        	}
        
	        if(currentLocation == null){
	        	SendError(getString(R.string.noLocation));
	        }
	        else {
	            sLat = Double.toString(currentLocation.getLatitude());
	            sLon = Double.toString(currentLocation.getLongitude());
	        }
        }
        
	    SharedPreferences prefs = getSharedPreferences(MainActivity.PREFERENCES, MODE_PRIVATE | MODE_MULTI_PROCESS);
        String sURL = prefs.getString(SettingsActivity.KEY_PREF_SRV_USER, getResources().getString(R.string.stDefaultServer));
        String sLogin = prefs.getString(SettingsActivity.KEY_PREF_SRV_USER_USER, "firereporter");
        String sPass = prefs.getString(SettingsActivity.KEY_PREF_SRV_USER_PASS, "8QdA4");
        int nDayInterval = prefs.getInt(SettingsActivity.KEY_PREF_SEARCH_DAY_INTERVAL + "_int", 5);
        int fetchRows = prefs.getInt(SettingsActivity.KEY_PREF_ROW_COUNT + "_int", 15);
        int searchRadius = prefs.getInt(SettingsActivity.KEY_PREF_FIRE_SEARCH_RADIUS + "int", 5) * 1000;//meters
        boolean searchByDate = prefs.getBoolean("search_current_date", false);

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        long diff = 86400000L * nDayInterval;//1000 * 60 * 60 * 24 * 5;// 5 days  
        date.setTime(date.getTime() - diff);
        String dt = dateFormat.format(date);
        
        String sFullURL = sURL + "?function=get_rows_user&user=" + sLogin + "&pass=" + sPass+ "&limit=" + fetchRows + "&radius=" + searchRadius;//
        if(searchByDate){
        	sFullURL += "&date=" + dt;
        }
        if(sLat.length() > 0 && sLon.length() > 0){
        	sFullURL += "&lat=" + sLat + "&lon=" + sLon;        	
        }

        //SELECT * FROM (SELECT id, report_date, latitude, longitude, round(ST_Distance_Sphere(ST_PointFromText('POINT(37.506247479468584 55.536129316315055)', 4326), fires.geom)) AS dist FROM fires WHERE ST_Intersects(fires.geom, ST_GeomFromText('POLYGON((32.5062474795 60.5361293163, 42.5062474795 60.5361293163, 42.5062474795 50.5361293163, 32.5062474795 50.5361293163, 32.5062474795 60.5361293163))', 4326) ) AND CAST(report_date as date) >= '2013-09-27')t WHERE dist <= 5000 LIMIT 15
	    //String sRemoteData = "http://gis-lab.info/data/zp-gis/soft/fires.php?function=get_rows_nasa&user=fire_usr&pass=J59DY&limit=5";
        new HttpGetter(this, 1, getResources().getString(R.string.stDownLoading), mFillDataHandler, bShowProgress).execute(sFullURL);
	}
	
	protected void GetNasaData(boolean bShowProgress){
        Location currentLocation = null;
        String sLat = null, sLon = null;

        if(mLocManager != null){
	        currentLocation = mLocManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
	        if(currentLocation == null){
	        	currentLocation = mLocManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
	        }
	        
	        if(currentLocation == null){
	        	SendError(getString(R.string.noLocation));
	        }
	        else {
	            sLat = Double.toString(currentLocation.getLatitude());
	            sLon = Double.toString(currentLocation.getLongitude());
	        }
        }
        
	    SharedPreferences prefs = getSharedPreferences(MainActivity.PREFERENCES, MODE_PRIVATE | MODE_MULTI_PROCESS);
        String sURL = prefs.getString(SettingsActivity.KEY_PREF_SRV_NASA, getResources().getString(R.string.stDefaultServer));
        String sLogin = prefs.getString(SettingsActivity.KEY_PREF_SRV_NASA_USER, "fire_usr");
        String sPass = prefs.getString(SettingsActivity.KEY_PREF_SRV_NASA_PASS, "J59DY");
        int nDayInterval = prefs.getInt(SettingsActivity.KEY_PREF_SEARCH_DAY_INTERVAL + "_int", 5);
        int fetchRows = prefs.getInt(SettingsActivity.KEY_PREF_ROW_COUNT + "_int", 15);
        int searchRadius = prefs.getInt(SettingsActivity.KEY_PREF_FIRE_SEARCH_RADIUS + "int", 5) * 1000;//meters
        boolean searchByDate = prefs.getBoolean(SettingsActivity.KEY_PREF_SEARCH_CURR_DAY, false);

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        long diff = 86400000L * nDayInterval;//1000 * 60 * 60 * 24 * 5;// 5 days  
        date.setTime(date.getTime() - diff);
        String dt = dateFormat.format(date);
        
        String sFullURL = sURL + "?function=get_rows_nasa&user=" + sLogin + "&pass=" + sPass+ "&limit=" + fetchRows + "&radius=" + searchRadius;//
        if(searchByDate){
        	sFullURL += "&date=" + dt;
        }
        if(sLat.length() > 0 && sLon.length() > 0){
        	sFullURL += "&lat=" + sLat + "&lon=" + sLon;        	
        }

	    //String sRemoteData = "http://gis-lab.info/data/zp-gis/soft/fires.php?function=get_rows_nasa&user=fire_usr&pass=J59DY&limit=5";
        //if(!oNasa.getStatus().equals(AsyncTask.Status.RUNNING) && !oNasa.getStatus().equals(AsyncTask.Status.PENDING))
        new HttpGetter(this, 2, getResources().getString(R.string.stDownLoading), mFillDataHandler, bShowProgress).execute(sFullURL);
    }
	
	protected void GetDataStoped(){
		if(mnCurrentExec < 1){
			if(mUserNasaReceiver != null){
				mUserNasaReceiver.send(SERVICE_STOP, new Bundle());			
			}
			else if(mScanexReceiver != null){
				mScanexReceiver.send(SERVICE_STOP, new Bundle());			
			}
		}
	}

	protected void SendError(String sErrMsg){
		if(mUserNasaReceiver != null){
			Bundle b = new Bundle();
			b.putString("err_msq", sErrMsg);
			mUserNasaReceiver.send(SERVICE_ERROR, b);
		
			GetDataStoped();
		}
		
		if(mScanexReceiver != null){
			Bundle b = new Bundle();
			b.putString("err_msq", sErrMsg);
			mScanexReceiver.send(SERVICE_ERROR, b);	
			
			GetDataStoped();
		}
	}
	
	protected void SendItem(FireItem item){
		if(mUserNasaReceiver == null)
			return;
		Bundle b = new Bundle();
		b.putParcelable("item", item);
		mUserNasaReceiver.send(SERVICE_DATA, b);
	}
	
	protected void FillScanexData(int nType, String sJSON){
		GetDataStoped();
		try {
			String sSubData = removeJsonT(sJSON);
			JSONObject rootobj = new JSONObject(sSubData);
			String sStatus = rootobj.getString("Status");
			if(sStatus.equals("OK")){
				//6. store data to db and in map
				JSONArray oResults = rootobj.getJSONArray("Result");
				for (int i = 0; i < oResults.length(); i++) {
					JSONObject jsonObject = oResults.getJSONObject(i);
					long nID = jsonObject.getLong("ID");
					
					if(mmoSubscriptions.get(nID) == null){
						String sTitle = jsonObject.getString("Title");
						String sLayerName = jsonObject.getString("sLayerName");
						String sWKT = jsonObject.getString("wkt");
						boolean bSMSEnable = jsonObject.getBoolean("SMSEnable");	
						
						mmoSubscriptions.put(nID, new SubscriptionItem(nID, sTitle, sLayerName, sWKT, bSMSEnable));
						
						//get notifications http://fires.kosmosnimki.ru/SAPI/Subscribe/GetData/55?dt=2013-08-06&CallBackName=44
						
						/*44({
							"Status": "OK",
							"ErrorInfo": "",
							"Result": [[1306601,
							"61.917, 63.090",
							68,
							27.4,
							"\u003ca href=\u0027http://maps.kosmosnimki.ru/TileService.ashx/apikeyV6IAK16QRG/mapT42E9?SERVICE=WMS&request=GetMap&version=1.3&layers=C7B2E6510209444E80673F3C37519F7E,FFE60CFA7DAF498381F811C08A5E8CF5,T42E9.A78AC25E0D924258B5AF40048C21F7E7_dt04102013&styles=&crs=EPSG:3395&transparent=FALSE&format=image/jpeg&width=460&height=460&bbox=6987987,8766592,7058307,8836912\u0027\u003e \u003cimg src=\u0027http://maps.kosmosnimki.ru/TileService.ashx/apikeyV6IAK16QRG/mapT42E9?SERVICE=WMS&request=GetMap&version=1.3&layers=C7B2E6510209444E80673F3C37519F7E,FFE60CFA7DAF498381F811C08A5E8CF5,T42E9.A78AC25E0D924258B5AF40048C21F7E7_dt04102013&styles=&crs=EPSG:3395&transparent=FALSE&format=image/jpeg&width=100&height=100&bbox=6987987,8766592,7058307,8836912\u0027 width=\u0027{4}\u0027 height=\u0027{5}\u0027 /\u003e\u003c/a\u003e",
							"\u003ca href=\u0027http://fires.kosmosnimki.ru/?x=63.09&y=61.917&z=11&dt=04.10.2013\u0027 target=\"_blank\"\u003eView on the map\u003c/a\u003e",
							"Fire",
							"Агириш",
							"\/Date(1380859500000)\/",
							"http://maps.kosmosnimki.ru/TileService.ashx/apikeyV6IAK16QRG/mapT42E9?SERVICE=WMS&request=GetMap&version=1.3&layers=C7B2E6510209444E80673F3C37519F7E,FFE60CFA7DAF498381F811C08A5E8CF5,T42E9.A78AC25E0D924258B5AF40048C21F7E7_dt04102013&styles=&crs=EPSG:3395&transparent=FALSE&format=image/jpeg&width=460&height=460&bbox=6987987,8766592,7058307,8836912",
							null]]
						})*/
					}
				}
			}
			else
			{
				SendError( rootobj.getString("ErrorInfo") );			
			}
		} catch (JSONException e) {
			SendError( e.getLocalizedMessage() );
			e.printStackTrace();
		}
	}
	
	protected void FillData(int nType, String sJSON){
		GetDataStoped();
		try {
			JSONObject jsonMainObject = new JSONObject(sJSON);
			if(jsonMainObject.getBoolean("error")){
				String sMsg = jsonMainObject.getString("msg");
				SendError(sMsg);
				return;
			}
			
			if(jsonMainObject.has("rows") && !jsonMainObject.isNull("rows")){
	
				JSONArray jsonArray = jsonMainObject.getJSONArray("rows");
				for (int i = 0; i < jsonArray.length(); i++) {
					JSONObject jsonObject = jsonArray.getJSONObject(i);
					//Log.i(ParseJSON.class.getName(), jsonObject.getString("text"));
					long nId = jsonObject.getLong("fid");
	
					int nKey = (int) (nType * 10000000 + nId);
					if(mmoFires.containsKey(nKey))
						continue;
	
	
					int nIconId = 0;
					if(nType == 1){//user
						nIconId = R.drawable.ic_eye;	
					}
					else if(nType == 2){//nasa
						nIconId = R.drawable.ic_nasa;
					}
	
					String sDate = jsonObject.getString("date");
					DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
					Date dtFire = dateFormat.parse(sDate);
					double dfLat = jsonObject.getDouble("lat");
					double dfLon = jsonObject.getDouble("lon");
					double dfDist = jsonObject.getDouble("dist");
	
					FireItem item = new FireItem(this, nType, nId, dtFire, dfLon, dfLat, dfDist, nIconId, "");
					mmoFires.put(nKey, item);
					
					SendItem(item);
				}	 
			}
		} catch (Exception e) {
			SendError(e.getLocalizedMessage());//	      e.printStackTrace();
		}	
	}	
	
	protected void GetScanexData(boolean bShowProgress){
		//1. If data present in map send them
		//2. if data not present - check if data exist in db
		//3. load data from db and send it to client
		//4. update data from internet
		//4.1 if no login cookie - get it
		//2 hours = 120 min = 7200 sec = 7200000
		Time testTime = new Time();
		testTime.setToNow();
		if(msScanexLoginCookie.equals("setting"))
			return;
		if(msScanexLoginCookie.equals("not_set") || msScanexLoginCookie.length() == 0 || testTime.toMillis(true) - mSanextCookieTime.toMillis(true) > 7200000){
		    SharedPreferences prefs = getSharedPreferences(MainActivity.PREFERENCES, MODE_PRIVATE | MODE_MULTI_PROCESS);
	        String sLogin = prefs.getString(SettingsActivity.KEY_PREF_SRV_SCAN_USER, "new@kosmosnimki.ru");
	        String sPass = prefs.getString(SettingsActivity.KEY_PREF_SRV_SCAN_PASS, "test123");
	        msScanexLoginCookie = "setting";
			new HttpScanexLogin(this, 4, getResources().getString(R.string.stChecking), mFillDataHandler, bShowProgress).execute(sLogin, sPass);
			return;
		}
		//5. send updates to client
        new HttpGetter(this, 3, getResources().getString(R.string.stDownLoading), mFillDataHandler, bShowProgress).execute("http://fires.kosmosnimki.ru/SAPI/Subscribe/Get/?CallBackName=" + USER_ID, msScanexLoginCookie);
	}
	
	public static String removeJsonT(String sData){
		return sData.substring(4, sData.length() - 1);
	}
}

/*
else if(nType == 3){//scanex
	//nIconId = R.drawable.ic_nasa;
	//oNasa = null;
	String sCookie = sJSON; 
    new HttpGetter(getActivity(), 3, getResources().getString(R.string.stDownLoading), mFillDataHandler, true).execute("http://fires.kosmosnimki.ru/SAPI/Account/Get/", sCookie);
//
	
}*/
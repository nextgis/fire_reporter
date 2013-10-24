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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.ResultReceiver;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;


public class GetFiresService extends Service {
	private NotificationManager mNotificationManager;
	NotificationCompat.Builder mBuilder;
	NotificationCompat.InboxStyle mInboxStyle;
	private final static int STATE_NOTIFY_ID = 777;
	
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
	public final static int SERVICE_SCANEXDATAUPDATE = 9;
	public final static int SERVICE_NOTIFY_DISMISSED = 10;

	public final static int SCANEX_SUBSCRIPTION = 1;
	public final static int SCANEX_NOTIFICATION = 2;

	protected final static String SCANEX_FILE = "scanex.json"; 
	public final static String SCANEX_API = "http://fires.kosmosnimki.ru/SAPI"; 

	public final static String SUBSCRIPTION_ID = "subscription_id"; 
	public final static String NOTIFICATION_ID = "notification_id";
	public final static String COMMAND = "command";
	public final static String ITEM = "item";
	public final static String RECEIVER_SCANEX = "receiver_scanex";
	public final static String RECEIVER = "receiver";
	public final static String TYPE = "type";
	public final static String ERR_MSG = "err_msq";
	public final static String SOURCE = "source";
	public final static String ERROR = "error";
	public final static String JSON = "json";
	
	protected Time mSanextCookieTime;
	protected final static int USER_ID = 777;
	
	protected int nUserCount, nNasaCount, nScanexCount;

	
	private Map<Long, ScanexSubscriptionItem> mmoSubscriptions;

    
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

		if(intent == null)
			return START_STICKY;
		
		int nCommnad = intent.getIntExtra(COMMAND, SERVICE_START);
		
		SharedPreferences prefs = getSharedPreferences(MainActivity.PREFERENCES, MODE_PRIVATE | MODE_MULTI_PROCESS);
		long nUpdateInterval = prefs.getLong(SettingsActivity.KEY_PREF_UPDATE_DATA_TIME + "_long", 30 * DateUtils.MINUTE_IN_MILLIS); //15
		boolean bEnergyEconomy = prefs.getBoolean(SettingsActivity.KEY_PREF_SERVICE_BATT_SAVE, true);
		
		switch(nCommnad){
		case SERVICE_START:
			mnFilter = intent.getIntExtra(SOURCE, MainActivity.SRC_NASA | MainActivity.SRC_USER);
			mUserNasaReceiver = intent.getParcelableExtra(RECEIVER);		

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
			
			mScanexReceiver = intent.getParcelableExtra(RECEIVER_SCANEX);		

			if(mnCurrentExec < 1){
				mScanexReceiver.send(SERVICE_SCANEXSTART, new Bundle());
			}
			Log.d(MainActivity.TAG, "GetFiresService service started");
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
			mUserNasaReceiver = intent.getParcelableExtra(RECEIVER);		
			mScanexReceiver = intent.getParcelableExtra(RECEIVER_SCANEX);		
			break;
		case SERVICE_DATA:
			mUserNasaReceiver = intent.getParcelableExtra(RECEIVER);		
			for(FireItem item : mmoFires.values()){
				SendItem(item);
			}
			break;
		case SERVICE_SCANEXDATA:
			mScanexReceiver = intent.getParcelableExtra(RECEIVER_SCANEX);		
			for(ScanexSubscriptionItem Item : mmoSubscriptions.values()){
				SendScanexItem(Item);
			}

			break;
		case SERVICE_SCANEXDATAUPDATE:
			long nSubscirbeId = intent.getLongExtra(SUBSCRIPTION_ID, -1);
			long nNotificationId = intent.getLongExtra(NOTIFICATION_ID, -1);
			ScanexSubscriptionItem subscribe = mmoSubscriptions.get(nSubscirbeId);
			if(subscribe != null){
				ScanexNotificationItem notification = subscribe.GetItems().get(nNotificationId);
				if(notification != null){
					notification.setWatched(true);
				}
			}
			break;
		case SERVICE_NOTIFY_DISMISSED:
			nUserCount = 0;
			nNasaCount = 0; 
			nScanexCount = 0;
			
			mInboxStyle = new NotificationCompat.InboxStyle();
			mInboxStyle.setBigContentTitle(getString(R.string.stNewFireNotificationDetailes));

			break;
		}
		
        return START_STICKY;
	}
    
	protected void Prepare(){
		mLocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		mnCurrentExec = 0;
		mmoFires = new HashMap<Integer, FireItem>();
		
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		nUserCount = 0;
		nNasaCount = 0; 
		nScanexCount = 0;
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
		stackBuilder.addParentStack(MainActivity.class);
		stackBuilder.addNextIntent(new Intent(this, MainActivity.class));
		//stackBuilder.addNextIntent(new Intent(this, ScanexNotificationsActivity.class));
		PendingIntent resultPendingIntent = stackBuilder.getPendingIntent( 0, PendingIntent.FLAG_UPDATE_CURRENT );
		
		mBuilder =
		        new NotificationCompat.Builder(this)
		        .setSmallIcon(R.drawable.ic_fire_small)
		        .setContentTitle(getString(R.string.stNewFireNotifications))
		        .setLights(Color.RED, 250, 1500);		
		mBuilder.setContentIntent(resultPendingIntent);
		
		Intent intent = new Intent(MainActivity.INTENT_NAME);	
        intent.putExtra(COMMAND, SERVICE_NOTIFY_DISMISSED);
		PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		mBuilder.setDeleteIntent(pendingIntent);
		
		mInboxStyle = new NotificationCompat.InboxStyle();
		mInboxStyle.setBigContentTitle(getString(R.string.stNewFireNotificationDetailes));
		
		LoadScanexData();
		
		mSanextCookieTime = new Time();
		mSanextCookieTime.setToNow();
		msScanexLoginCookie = new String("not_set");
		
    	mFillDataHandler = new Handler() {
    	    public void handleMessage(Message msg) {
    	    	
    	    	mnCurrentExec--;    	    	
    	    	
    	    	Bundle resultData = msg.getData();
    	    	boolean bHaveErr = resultData.getBoolean(ERROR);
    	    	if(bHaveErr){
    	    		SendError(resultData.getString(ERR_MSG));
    	    	}
    	    	else{
    	    		int nType = resultData.getInt(SOURCE);
	    			String sData = resultData.getString(JSON);
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
        intent.putExtra(RECEIVER, mUserNasaReceiver);
        intent.putExtra(RECEIVER_SCANEX, mScanexReceiver);
        intent.putExtra(COMMAND, nCommand);
        intent.putExtra(SOURCE, mnFilter);

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
	        	return;
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
        int searchRadius = prefs.getInt(SettingsActivity.KEY_PREF_FIRE_SEARCH_RADIUS + "_int", 5) * 1000;//meters
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
	        	return;
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
        int searchRadius = prefs.getInt(SettingsActivity.KEY_PREF_FIRE_SEARCH_RADIUS + "_int", 5) * 1000;//meters
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

			if(mScanexReceiver != null){
				mScanexReceiver.send(SERVICE_STOP, new Bundle());			
			}
			mnCurrentExec = 0;
		}
	}

	protected void SendError(String sErrMsg){
		if(mUserNasaReceiver != null){
			Bundle b = new Bundle();
			b.putString(ERR_MSG, sErrMsg);
			mUserNasaReceiver.send(SERVICE_ERROR, b);
		
			GetDataStoped();
		}
		
		if(mScanexReceiver != null){
			Bundle b = new Bundle();
			b.putString(ERR_MSG, sErrMsg);
			mScanexReceiver.send(SERVICE_ERROR, b);	
			
			GetDataStoped();
		}
	}
	
	protected void SendItem(FireItem item){
		if(mUserNasaReceiver == null)
			return;
		Bundle b = new Bundle();
		b.putParcelable(ITEM, item);
		mUserNasaReceiver.send(SERVICE_DATA, b);
	}
	
	protected void SendScanexItem(ScanexSubscriptionItem item){
		if(mScanexReceiver == null)
			return;
		Bundle b = new Bundle();
		b.putLong(SUBSCRIPTION_ID, item.GetId());
		b.putInt(TYPE, SCANEX_SUBSCRIPTION);
		b.putParcelable(ITEM, item);
		mScanexReceiver.send(SERVICE_SCANEXDATA, b);
	}
	
	protected void FillScanexData(int nType, String sJSON){
		GetDataStoped();
		try {
			String sSubData = removeJsonT(sJSON);
			JSONObject rootobj = new JSONObject(sSubData);
			String sStatus = rootobj.getString("Status");
			if(sStatus.equals("OK")){
				//6. store data to db and in map
				List<Long> naIDs = new ArrayList<Long>(); 
				JSONArray oResults = rootobj.getJSONArray("Result");
				for (int i = 0; i < oResults.length(); i++) {
					JSONObject jsonObject = oResults.getJSONObject(i);
					long nID = jsonObject.getLong("ID");
					naIDs.add(nID);
						
					// Add new items

					if(!mmoSubscriptions.containsKey(nID)){
						String sTitle = jsonObject.getString("Title");
						String sLayerName = jsonObject.getString("LayerName");
						String sWKT = jsonObject.getString("wkt");
						boolean bSMSEnable = jsonObject.getBoolean("SMSEnable");	
						
						ScanexSubscriptionItem Item = new ScanexSubscriptionItem(this, nID, sTitle, sLayerName, sWKT, bSMSEnable);
						mmoSubscriptions.put(nID, Item);
						SendScanexItem(Item);						
					}

					mmoSubscriptions.get(nID).UpdateFromRemote(msScanexLoginCookie);
					
				}
				
				// Remove deleted items
				for(Long id : mmoSubscriptions.keySet()){
					if(!naIDs.contains(id)){
						mmoSubscriptions.remove(id);
					}
				}
				StoreScanexData();
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
				for(int i = 0; i < jsonArray.length(); i++) {
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
	
					FireItem item = new FireItem(this, nType, nId, dtFire, dfLon, dfLat, dfDist, nIconId);
					mmoFires.put(nKey, item);
					
					SendItem(item);
					onNotify(nType, item.GetShortCoordinates() + "/" + dfDist/1000 + " " + getString(R.string.km) + "/" + item.GetDateAsString());
				}	 
			}
		} catch (Exception e) {
			SendError(e.getLocalizedMessage());//	      e.printStackTrace();
		}	
	}	
	
	protected void GetScanexData(boolean bShowProgress){
		if(mmoSubscriptions.size() > 0)
			StoreScanexData();
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
	        mnCurrentExec++;
			new ScanexHttpLogin(this, 4, getResources().getString(R.string.stChecking), mFillDataHandler, bShowProgress).execute(sLogin, sPass);
			return;
		}
		//5. send updates to client
		mnCurrentExec++;
        new HttpGetter(this, 3, getResources().getString(R.string.stDownLoading), mFillDataHandler, bShowProgress).execute(SCANEX_API + "/Subscribe/Get/?CallBackName=" + USER_ID, msScanexLoginCookie);
	}
	
	public static String removeJsonT(String sData){
		return sData.substring(4, sData.length() - 1);
	}
	
	protected boolean writeToFile(File filePath, String sData){
		try{
			FileOutputStream os = new FileOutputStream(filePath, false);
			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(os);
	        outputStreamWriter.write(sData);
	        outputStreamWriter.close();
	        return true;
		}
		catch(IOException e){
	    	SendError( e.getLocalizedMessage() );
	    	return false;
		}		
	}

	protected String readFromFile(File filePath) {

	    String ret = "";

	    try {
	    	FileInputStream inputStream = new FileInputStream(filePath);

	        if ( inputStream != null ) {
	            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
	            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
	            String receiveString = "";
	            StringBuilder stringBuilder = new StringBuilder();

	            while ( (receiveString = bufferedReader.readLine()) != null ) {
	                stringBuilder.append(receiveString);
	            }

	            inputStream.close();
	            ret = stringBuilder.toString();
	        }
	    }
	    catch (FileNotFoundException e) {
	    	SendError( e.getLocalizedMessage() );
	    } catch (IOException e) {
	    	SendError( e.getLocalizedMessage() );
	    }

	    return ret;
	}

	@Override
	public void onDestroy() {
		mNotificationManager.cancelAll();
		StoreScanexData();
		super.onDestroy();
	}
	
	protected void LoadScanexData(){
		mmoSubscriptions = new HashMap<Long, ScanexSubscriptionItem>(); 
		File file = new File(getExternalFilesDir(null), SCANEX_FILE);
		String sData = readFromFile(file);
		try {
			JSONObject oJSONRoot = new JSONObject(sData);
			JSONArray jsonArray = oJSONRoot.getJSONArray("subscriptions");
			
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject jsonObject = jsonArray.getJSONObject(i);
				
				ScanexSubscriptionItem Item = new ScanexSubscriptionItem(this, jsonObject);
				if(Item.GetId() != -1){
					mmoSubscriptions.put(Item.GetId(), Item);
				}
			}
			
		} catch (JSONException e) {
			SendError( e.getLocalizedMessage() );
		}
	}
	
	protected void StoreScanexData(){
		try {
			JSONObject oJSONRoot = new JSONObject();
			JSONArray oJSONSubscriptions = new JSONArray();
			oJSONRoot.put("subscriptions", oJSONSubscriptions);
			for(ScanexSubscriptionItem Item : mmoSubscriptions.values()){
				oJSONSubscriptions.put(Item.getAsJSON());
			}
			
			File file = new File(getExternalFilesDir(null), SCANEX_FILE);
			writeToFile(file, oJSONRoot.toString());
			
		} catch (JSONException e) {
			SendError( e.getLocalizedMessage() );
		}
	}

	public void onNewNotifictation(long subscriptionID, ScanexNotificationItem item) {
		String sAdds = item.GetPlace() == null ? item.GetType() : item.GetPlace();
		onNotify(3, item.GetShortCoordinates() + "/" + sAdds + "/" + item.GetDateAsString());
		
		if(mScanexReceiver == null)
			return;
		Bundle b = new Bundle();
		b.putLong(SUBSCRIPTION_ID, subscriptionID);
		b.putLong(NOTIFICATION_ID, item.GetId());
		b.putInt(TYPE, SCANEX_NOTIFICATION);
		b.putParcelable(ITEM, item);
		mScanexReceiver.send(SERVICE_SCANEXDATA, b);
		
	}
	
	protected void onNotify(int nType, String sMsg){

		String sFullMsg;
		if(nType == 1){			//user
			nUserCount++;
			sFullMsg = getString(R.string.stUser) + sMsg;
		}
		else if(nType == 2){	//nasa
			nNasaCount++;
			sFullMsg = getString(R.string.stNasa) + sMsg;
		}
		else if(nType == 3){	//scanex
			nScanexCount++;
			sFullMsg = getString(R.string.stScanex) + sMsg;
		}
		else{
			return;
		}

		String sSumm = getString(R.string.stScanex) + nScanexCount + ", " + getString(R.string.stUser) +  nUserCount + ", " + getString(R.string.stNasa) + nNasaCount;
		mBuilder.setContentText(sSumm);
		
		mInboxStyle.setSummaryText(sSumm);		
		mInboxStyle.addLine(sFullMsg);

		// Moves the big view style object into the notification object.
		mBuilder.setStyle(mInboxStyle);

		mNotificationManager.notify(STATE_NOTIFY_ID, mBuilder.build());
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
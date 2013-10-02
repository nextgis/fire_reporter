/*******************************************************************************
*
* FireReporter
* ---------------------------------------------------------
* Report and view fires
*
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SpinnerAdapter;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.nextgis.firereporter.FireListAdapter.FireItem;
import com.nextgis.firereporter.R;

public class FireReporter extends SherlockActivity implements OnNavigationListener{
    public final static String PREFERENCES = "FireReporter";
    
	public final static int MENU_REPORT = 1;
	public final static int MENU_PLACE = 2;
	public final static int MENU_REFRESH = 3;
	public final static int MENU_SETTINGS = 4;
	public final static int MENU_ABOUT = 5;
	
	
    private ListView mListFireInfo;
    private List <FireItem> mFireList;
    private int mPosition;
    protected FireListAdapter mListAdapter;
    private Handler mFillDataHandler; 
    private LocationManager mlocManager;
    private boolean bGotData;
    private HttpGetter oUser = null, oNasa = null;
	@Override
	  public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    mFireList = new ArrayList<FireItem>();
	    
        // initialize the default settings
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
	    
	    bGotData = false;
	    
	    mlocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

	    setContentView(R.layout.main);
	    
	    if(savedInstanceState != null){
	    	mFireList = savedInstanceState.getParcelableArrayList("list");
	    	bGotData = savedInstanceState.getBoolean("gotdata");
	    }
    
        mFillDataHandler = new Handler() {
            public void handleMessage(Message msg) {
            	Bundle resultData = msg.getData();
            	boolean bHaveErr = resultData.getBoolean("error");
            	if(bHaveErr){
            		Toast.makeText(FireReporter.this, resultData.getString("err_msq"), Toast.LENGTH_LONG).show();
            	}
            	else{
            		int nType = resultData.getInt("src");
            		String sData = resultData.getString("json");
            		FillData(nType, sData);
            	}
            };
        };	    
	    
	       
	    ActionBar actionBar = getSupportActionBar();
	    
	    SpinnerAdapter mSpinnerAdapter = ArrayAdapter.createFromResource(this, R.array.fires_src, android.R.layout.simple_spinner_dropdown_item);	    
	    actionBar.setDisplayShowTitleEnabled(false);
	    actionBar.setNavigationMode(com.actionbarsherlock.app.ActionBar.NAVIGATION_MODE_LIST);
	    actionBar.setListNavigationCallbacks(mSpinnerAdapter, this);
	    

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(FireReporter.this);
        mPosition = prefs.getInt("CURRENT_VIEW", 0);
	    actionBar.setSelectedNavigationItem(mPosition);
	    
	    // load list
	    mListFireInfo = (ListView)findViewById(R.id.Mainlist);
        // create new adapter
	    mListAdapter = new FireListAdapter(this, mFireList);
        // set adapter to list view
        mListFireInfo.setAdapter(mListAdapter);
        

        // implement event when an item on list view is selected
        mListFireInfo.setOnItemClickListener(new OnItemClickListener(){
        	public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
        		// get the list adapter
        		FireListAdapter appFireListAdapter = (FireListAdapter)parent.getAdapter();
        		// get selected item on the list
        		FireItem item = (FireItem)appFireListAdapter.getItem(pos);
        		// launch the selected application
        		String sURL = item.GetUrl();
        		if(sURL.length() > 0){
        			Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(sURL));
        			startActivity(browserIntent);
        		}
        	}

		});
         
        if(mFireList.isEmpty())
        	GetData(false);
	  }

	  @Override
	  public boolean onCreateOptionsMenu(Menu menu) {
	    
			menu.add(com.actionbarsherlock.view.Menu.NONE, MENU_SETTINGS, com.actionbarsherlock.view.Menu.NONE, R.string.tabSettings)
	       .setIcon(R.drawable.ic_action_settings)
	       .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);		
			
			menu.add(com.actionbarsherlock.view.Menu.NONE, MENU_ABOUT, com.actionbarsherlock.view.Menu.NONE, R.string.tabAbout)
			.setIcon(R.drawable.ic_action_about)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);	
			
			menu.add(com.actionbarsherlock.view.Menu.NONE, MENU_PLACE, com.actionbarsherlock.view.Menu.NONE, R.string.sPlace)
			.setIcon(R.drawable.ic_location_place)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
			
			menu.add(com.actionbarsherlock.view.Menu.NONE, MENU_REFRESH, com.actionbarsherlock.view.Menu.NONE, R.string.sRefresh)
			.setIcon(R.drawable.ic_navigation_refresh)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
			
			return true;
	    
	  }

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
	        case android.R.id.home:
	            // app icon in action bar clicked; go home
	            Intent intentMain = new Intent(this, FireReporter.class);
	            intentMain.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	            startActivity(intentMain);
	            return true;
	        case MENU_SETTINGS:
	            // app icon in action bar clicked; go home
	            Intent intentSet = new Intent(this, SettingsMain.class);
	            intentSet.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
	            startActivity(intentSet);
	            return true;
	        case MENU_ABOUT:
	            // app icon in action bar clicked; go home
	            Intent intentAbout = new Intent(this, AboutReporter.class);
	            intentAbout.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
	            startActivity(intentAbout);
	            return true;	  
	        case MENU_REFRESH:
	        	bGotData = false;
	        	GetData(true);
	        	return true;
	        case MENU_PLACE:
	            Intent intentSendReport = new Intent(this, SendReport.class);
	            intentSendReport.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
	            startActivity(intentSendReport);
	        	return true;
	    }
		
		return super.onOptionsItemSelected(item);
		
	}
	
	protected void GetData(boolean bShoProgress){
		if(bGotData)
			return;
		bGotData = true;
		mFireList.clear();
		CancelDownload();
		switch(mPosition){
			case 0://all
				GetUserData(bShoProgress);
				GetNasaData(bShoProgress);
				GetScanexData();
				break;
			case 1://user
				GetUserData(bShoProgress);
				break;
			case 2://nasa
				GetNasaData(bShoProgress);
				break;
			case 3://scanex
				GetScanexData();
				break;
		}
	}
	
	protected void GetUserData(boolean bShowProgress){
        Location currentLocation = null;
        String sLat = null, sLon = null;
        if(mlocManager != null){
        	currentLocation = mlocManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);        
        	if(currentLocation == null){
        		currentLocation = mlocManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        	}
        
	        if(currentLocation == null){
	        	Toast.makeText(this, getString(R.string.noLocation), Toast.LENGTH_LONG).show();
	        }
	        else {
	            sLat = Double.toString(currentLocation.getLatitude());
	            sLon = Double.toString(currentLocation.getLongitude());
	        }
        }
        
	    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(FireReporter.this);
        String sURL = prefs.getString(SettingsFragment.KEY_PREF_SRV_USER, getResources().getString(R.string.stDefaultServer));
        String sLogin = prefs.getString(SettingsFragment.KEY_PREF_SRV_USER_USER, "firereporter");
        String sPass = prefs.getString(SettingsFragment.KEY_PREF_SRV_USER_PASS, "8QdA4");
        int nDayInterval = prefs.getInt(SettingsFragment.KEY_PREF_SEARCH_DAY_INTERVAL + "_int", 5);
        int fetchRows = prefs.getInt(SettingsFragment.KEY_PREF_ROW_COUNT + "_int", 15);
        int searchRadius = prefs.getInt(SettingsFragment.KEY_PREF_FIRE_SEARCH_RADIUS + "int", 5) * 1000;//meters
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
        oUser = new HttpGetter(FireReporter.this, 1, getResources().getString(R.string.stDownLoading), mFillDataHandler, bShowProgress);
       	oUser.execute(sFullURL);
	}
	
	protected void GetNasaData(boolean bShowProgress){
        Location currentLocation = null;
        String sLat = null, sLon = null;
        if(mlocManager != null){
	        currentLocation = mlocManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
	        if(currentLocation == null){
	        	currentLocation = mlocManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
	        }
	        
	        if(currentLocation == null){
	        	Toast.makeText(this, getString(R.string.noLocation), Toast.LENGTH_LONG).show();
	        }
	        else {
	            sLat = Double.toString(currentLocation.getLatitude());
	            sLon = Double.toString(currentLocation.getLongitude());
	        }
        }
        
	    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(FireReporter.this);
        String sURL = prefs.getString(SettingsFragment.KEY_PREF_SRV_NASA, getResources().getString(R.string.stDefaultServer));
        String sLogin = prefs.getString(SettingsFragment.KEY_PREF_SRV_NASA_USER, "fire_usr");
        String sPass = prefs.getString(SettingsFragment.KEY_PREF_SRV_NASA_PASS, "J59DY");
        int nDayInterval = prefs.getInt(SettingsFragment.KEY_PREF_SEARCH_DAY_INTERVAL + "_int", 5);
        int fetchRows = prefs.getInt(SettingsFragment.KEY_PREF_ROW_COUNT + "_int", 15);
        int searchRadius = prefs.getInt(SettingsFragment.KEY_PREF_FIRE_SEARCH_RADIUS + "int", 5) * 1000;//meters
        boolean searchByDate = prefs.getBoolean(SettingsFragment.KEY_PREF_SEARCH_CURR_DAY, false);

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
        oNasa = new HttpGetter(FireReporter.this, 2, getResources().getString(R.string.stDownLoading), mFillDataHandler, bShowProgress);
        oNasa.execute(sFullURL);
    }
	
	protected void GetScanexData(){
		mFireList.add(new FireItem(getBaseContext(), 3, 0, Calendar.getInstance().getTime(), 37, 55, -1, R.drawable.ic_scan, "http://ya.ru"));
		mListAdapter.notifyDataSetChanged();
	}
	
	protected void FillData(int nType, String sJSON){
		int nIconId = 0;
		if(nType == 1){//user
			nIconId = R.drawable.ic_eye;		
    		oUser = null;    	
		}
		else if(nType == 2){//nasa
			nIconId = R.drawable.ic_nasa;
			oNasa = null;
		}
		
	    try {
	    	JSONObject jsonMainObject = new JSONObject(sJSON);
	    	if(jsonMainObject.getBoolean("error")){
	    	  String sMsg = jsonMainObject.getString("msg");
	    	  Toast.makeText(this, sMsg, Toast.LENGTH_LONG).show();
	    	  return;
	      }
	      
	      JSONArray jsonArray = jsonMainObject.getJSONArray("rows");
	      for (int i = 0; i < jsonArray.length(); i++) {
	    	  JSONObject jsonObject = jsonArray.getJSONObject(i);
	          //Log.i(ParseJSON.class.getName(), jsonObject.getString("text"));
	    	  long nId = jsonObject.getLong("fid");
	    	  String sDate = jsonObject.getString("date");
	          DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	    	  Date dtFire = dateFormat.parse(sDate);
	    	  double dfLat = jsonObject.getDouble("lat");
	    	  double dfLon = jsonObject.getDouble("lon");
	    	  double dfDist = jsonObject.getDouble("dist");

	    	  boolean bAdd = true;
	    	  for(int j = 0; j < mFireList.size(); j++){
	    		  FireItem item = mFireList.get(j);
	    		  if(item.GetId() == nId && item.GetType() == nType){
	    			  bAdd = false;
	    			  break;
	    		  }
	    	  }
	    		  
	    	  if(bAdd)	  
	    		  mFireList.add(new FireItem(FireReporter.this, nType, nId, dtFire, dfLon, dfLat, dfDist, nIconId, ""));		
	      }	 
	      
	    } catch (Exception e) {
	      e.printStackTrace();
	    }	
	    
	    Collections.sort(mFireList, new FireItemComparator());
    
	    mListAdapter.notifyDataSetChanged();
	}
	
	public class FireItemComparator implements Comparator<FireItem>
	{
	    public int compare(FireItem left, FireItem right) {
	    	long nInterval = left.GetDate().getTime() - right.GetDate().getTime();
	    	if(nInterval == 0){
	    		nInterval = (long) (left.GetDistance() - right.GetDistance());
	    	}
	    	
	    	if (nInterval < 0)
	    		return 1;
	    	else if (nInterval > 0)
	    		return -1;
	    	else
	    		return 0;
	    }
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		outState.putParcelableArrayList("list", (ArrayList<? extends Parcelable>) mFireList);
		outState.putBoolean("gotdata", bGotData);
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		
		CancelDownload();
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		
		CancelDownload();
	}
	
	protected void CancelDownload(){

		if(oUser != null){
			//oUser.DismissDowloadDialog();
			oUser.Abort();
			oUser = null;
		}

		if(oNasa != null){
			//oNasa.DismissDowloadDialog();
			oNasa.Abort();
			oNasa = null;
		}
	}

	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		
		Editor editor = PreferenceManager.getDefaultSharedPreferences(FireReporter.this).edit();
		editor.putInt("CURRENT_VIEW", itemPosition);
		editor.commit();
		
		mPosition = itemPosition;
		bGotData = false;
		GetData(true);

	    return true;
	}
}



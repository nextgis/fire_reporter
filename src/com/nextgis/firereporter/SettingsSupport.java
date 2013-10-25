/*******************************************************************************
 * Project:  Fire reporter
 * Purpose:  Report and view fires
 * Author:   Dmitry Baryshnikov (aka Bishop), polimax@mail.ru
 ******************************************************************************
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

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceClickListener;
import android.text.format.DateUtils;
import android.widget.Toast;

public class SettingsSupport  implements OnSharedPreferenceChangeListener{
    private ListPreference mSendDataIntervalPref;
    private CheckBoxPreference mSaveBattPref;    
    private CheckBoxPreference mNotifyLEDPref;    
    private CheckBoxPreference mPlaySoundPref;    
    private CheckBoxPreference mVibroPref;    
    private EditTextPreference mRowCountPref;
    private ListPreference mDayIntervalPref;
    private CheckBoxPreference mSearchCurrentDatePref;
    private EditTextPreference mFireSearchRadiusPref;     
	
    private EditTextPreference mNasaServerPref;
    private EditTextPreference mNasaServerUserPref;
    private EditTextPreference mNasaServerPassPref;
    
    private EditTextPreference mUserServerPref;
    private EditTextPreference mUserServerUserPref;
    private EditTextPreference mUserServerPassPref;
    
    //private EditTextPreference mScanServerPref;
    private EditTextPreference mScanServerUserPref;
    private EditTextPreference mScanServerPassPref;    

    private Handler mReturnHandler;
    
    private PreferenceScreen screen;
    private Context context;

	public SettingsSupport(Context context, PreferenceScreen screen) {
		this.screen = screen;
		this.context = context;

        // Load the preferences from an XML resource
        //addPreferencesFromResource(R.xml.preferences);
        
        mSendDataIntervalPref = (ListPreference) screen.findPreference(SettingsActivity.KEY_PREF_INTERVAL);
        if(mSendDataIntervalPref != null){
            int index = mSendDataIntervalPref.findIndexOfValue( mSendDataIntervalPref.getValue() );           
            if(index >= 0){
            	mSendDataIntervalPref.setSummary(mSendDataIntervalPref.getEntries()[index]);
            }
            else{
            	mSendDataIntervalPref.setSummary((String) mSendDataIntervalPref.getValue()); 
            }
        }
        
        mSaveBattPref = (CheckBoxPreference) screen.findPreference(SettingsActivity.KEY_PREF_SERVICE_BATT_SAVE);
        if(mSaveBattPref != null)
        	mSaveBattPref.setSummary(mSaveBattPref.isChecked() ? R.string.stOn : R.string.stOff);	    

        mNotifyLEDPref = (CheckBoxPreference) screen.findPreference(SettingsActivity.KEY_PREF_NOTIFY_LED);
        if(mNotifyLEDPref != null)
        	mNotifyLEDPref.setSummary(mNotifyLEDPref.isChecked() ? R.string.stOn : R.string.stOff);	    

        mPlaySoundPref = (CheckBoxPreference) screen.findPreference(SettingsActivity.KEY_PREF_NOTIFY_SOUND);
        if(mPlaySoundPref != null)
        	mPlaySoundPref.setSummary(mPlaySoundPref.isChecked() ? R.string.stOn : R.string.stOff);	    

        mVibroPref = (CheckBoxPreference) screen.findPreference(SettingsActivity.KEY_PREF_NOTIFY_VIBRO);
        if(mVibroPref != null)
        	mVibroPref.setSummary(mVibroPref.isChecked() ? R.string.stOn : R.string.stOff);	    
        
        mRowCountPref = (EditTextPreference) screen.findPreference(SettingsActivity.KEY_PREF_ROW_COUNT);
        if(mRowCountPref != null)
        	mRowCountPref.setSummary((String) mRowCountPref.getText());
        
        mFireSearchRadiusPref = (EditTextPreference) screen.findPreference(SettingsActivity.KEY_PREF_FIRE_SEARCH_RADIUS);
        if(mFireSearchRadiusPref != null)
        	mFireSearchRadiusPref.setSummary((String) mFireSearchRadiusPref.getText() + " " +  context.getString(R.string.km));
        
        mDayIntervalPref = (ListPreference) screen.findPreference(SettingsActivity.KEY_PREF_SEARCH_DAY_INTERVAL);
        if(mDayIntervalPref != null){
            int index = mDayIntervalPref.findIndexOfValue( mDayIntervalPref.getValue() );           
            if(index >= 0){
            	mDayIntervalPref.setSummary(mDayIntervalPref.getEntries()[index]);
            }
            else{
               	mDayIntervalPref.setSummary((String) mDayIntervalPref.getValue());
            }
        }
         
        mSearchCurrentDatePref = (CheckBoxPreference) screen.findPreference(SettingsActivity.KEY_PREF_SEARCH_CURR_DAY);
        if(mSearchCurrentDatePref != null)
        	mSearchCurrentDatePref.setSummary(mSearchCurrentDatePref.isChecked() ? R.string.stSearchCurrentDayOn : R.string.stSearchCurrentDayOff);	    
        
        mNasaServerPref = (EditTextPreference) screen.findPreference(SettingsActivity.KEY_PREF_SRV_NASA);
        if(mNasaServerPref != null)
        	mNasaServerPref.setSummary((String) mNasaServerPref.getText());
        mNasaServerUserPref = (EditTextPreference) screen.findPreference(SettingsActivity.KEY_PREF_SRV_NASA_USER);
        if(mNasaServerUserPref != null)
        	mNasaServerUserPref.setSummary((String) mNasaServerUserPref.getText());
        
        mUserServerPref = (EditTextPreference) screen.findPreference(SettingsActivity.KEY_PREF_SRV_USER);
        if(mUserServerPref != null)
        	mUserServerPref.setSummary((String) mUserServerPref.getText());
        mUserServerUserPref = (EditTextPreference) screen.findPreference(SettingsActivity.KEY_PREF_SRV_USER_USER);
        if(mUserServerUserPref != null)
        	mUserServerUserPref.setSummary((String) mUserServerUserPref.getText());

        mScanServerUserPref = (EditTextPreference) screen.findPreference(SettingsActivity.KEY_PREF_SRV_SCAN_USER);
        if(mScanServerUserPref != null)
        	mScanServerUserPref.setSummary((String) mScanServerUserPref.getText());
        
        mNasaServerPassPref = (EditTextPreference) screen.findPreference(SettingsActivity.KEY_PREF_SRV_NASA_PASS);
        mUserServerPassPref = (EditTextPreference) screen.findPreference(SettingsActivity.KEY_PREF_SRV_USER_PASS);
        mScanServerPassPref = (EditTextPreference) screen.findPreference(SettingsActivity.KEY_PREF_SRV_SCAN_PASS);
        
        final Preference checkNasaConnPref = (Preference) screen.findPreference(SettingsActivity.KEY_PREF_SRV_NASA_CHECK_CONN);
        final Preference checkUserConnPref = (Preference) screen.findPreference(SettingsActivity.KEY_PREF_SRV_USER_CHECK_CONN);
        final Preference checkScanConnPref = (Preference) screen.findPreference(SettingsActivity.KEY_PREF_SRV_SCAN_CHECK_CONN);
        
        mReturnHandler = new Handler() {
            @SuppressLint("NewApi")
			public void handleMessage(Message msg) {
            	Bundle resultData = msg.getData();
            	
            	boolean bHaveErr = resultData.getBoolean(GetFiresService.ERROR);
            	int nType = resultData.getInt(GetFiresService.SOURCE);
            	if(bHaveErr){
            		Toast.makeText(SettingsSupport.this.context, resultData.getString(GetFiresService.ERR_MSG), Toast.LENGTH_LONG).show();
            		if(nType == 1){//user
            			checkUserConnPref.setSummary(R.string.stConnectionFailed);
            			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            				checkUserConnPref.setIcon(R.drawable.ic_alerts_and_states_error);
            		}
            		else if(nType == 2){//nasa
            			checkNasaConnPref.setSummary(R.string.stConnectionFailed);
            			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            				checkNasaConnPref.setIcon(R.drawable.ic_alerts_and_states_error);
            		}
            		else if(nType == 3){//scanex
            			checkScanConnPref.setSummary(R.string.stConnectionFailed);
            			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            				checkScanConnPref.setIcon(R.drawable.ic_alerts_and_states_error);
            		}
            	}
            	else{           		            	
	    			
	    			String sData = resultData.getString(GetFiresService.JSON);
	    			if(nType == 1){//user
	    		    	JSONObject jsonMainObject;
						try {
							jsonMainObject = new JSONObject(sData);
		    		    	if(jsonMainObject.getBoolean("error")){
		    		    	  String sMsg = jsonMainObject.getString("msg");
		    		    	  Toast.makeText(SettingsSupport.this.context, sMsg, Toast.LENGTH_LONG).show();
		    		    	  checkUserConnPref.setSummary(R.string.stConnectionFailed);
		    		    	  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
		    		    		  checkUserConnPref.setIcon(R.drawable.ic_alerts_and_states_error);
		    		    	}
		    		    	else {
		    		    		checkUserConnPref.setSummary(R.string.stConnectionSucceeded);
		    		    		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
		    		    			checkUserConnPref.setIcon(R.drawable.ic_navigation_accept);
		    		    	}
						} catch (JSONException e) {
							Toast.makeText(SettingsSupport.this.context, e.toString(), Toast.LENGTH_LONG).show();
							e.printStackTrace();
		    		    	checkUserConnPref.setSummary(R.string.sCheckDBConnSummary);
						}
					}
	    			else if(nType == 2){//nasa
	    		    	JSONObject jsonMainObject;
						try {
							jsonMainObject = new JSONObject(sData);
		    		    	if(jsonMainObject.getBoolean("error")){
		    		    	  String sMsg = jsonMainObject.getString("msg");
		    		    	  Toast.makeText(SettingsSupport.this.context, sMsg, Toast.LENGTH_LONG).show();
		    		    	  checkNasaConnPref.setSummary(R.string.stConnectionFailed);
		    		    	  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
		    		    		  checkNasaConnPref.setIcon(R.drawable.ic_alerts_and_states_error);
		    		    	}
		    		    	else {
		    		    		checkNasaConnPref.setSummary(R.string.stConnectionSucceeded);
		    		    		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
		    		    			checkNasaConnPref.setIcon(R.drawable.ic_navigation_accept);
		    		    	}
						} catch (JSONException e) {
							Toast.makeText(SettingsSupport.this.context, e.toString(), Toast.LENGTH_LONG).show();
							e.printStackTrace();
							checkNasaConnPref.setSummary(R.string.sCheckDBConnSummary);
						}
	    			}
	    			else if(nType == 3){//scanex
	    				if(sData.length() == 0){
	    					String sMsg = "Connect failed";
	    					Toast.makeText(SettingsSupport.this.context, sMsg, Toast.LENGTH_LONG).show();
		    		    	checkScanConnPref.setSummary(R.string.stConnectionFailed);
		    		    	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
		    		    		checkScanConnPref.setIcon(R.drawable.ic_alerts_and_states_error);
	    				}
	    				else{
	    					checkScanConnPref.setSummary(R.string.stConnectionSucceeded);
	    					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
	    						checkScanConnPref.setIcon(R.drawable.ic_navigation_accept);
	    					
	    	        		new HttpGetter(SettingsSupport.this.context, 4, SettingsSupport.this.context.getString(R.string.stChecking), mReturnHandler, true).execute("http://fires.kosmosnimki.ru/SAPI/Account/Get/?CallBackName=" + GetFiresService.USER_ID, sData);
	    				}
	    			}
	    			else if(nType == 4){//scanex detailes
	    				try {
	    					String sSubData = GetFiresService.removeJsonT(sData);
							JSONObject rootobj = new JSONObject(sSubData);
							String sStatus = rootobj.getString("Status");
							if(sStatus.equals("OK")){
								JSONObject resobj =  rootobj.getJSONObject("Result");
								String sName = "";
								if(!resobj.isNull("FullName"))
									sName = resobj.getString("FullName");
								String sPhone = "";
								if(!resobj.isNull("Phone"))
									sPhone = resobj.getString("Phone");
								//add properties
								if(sPhone.length() > 0){
									Preference PhonePref = new Preference(SettingsSupport.this.context);
									PhonePref.setTitle(R.string.stScanexServerUserPhone);
									PhonePref.setSummary(sPhone);
									PhonePref.setOrder(2);
									SettingsSupport.this.screen.addPreference(PhonePref);
								}
								
								if(sName.length() > 0){
									Preference NamePref = new Preference(SettingsSupport.this.context);
									NamePref.setTitle(R.string.stScanexServerUserFullName);
									NamePref.setSummary(sName);
									NamePref.setOrder(2);
									SettingsSupport.this.screen.addPreference(NamePref);
								}
								
							}
							else
							{
								Toast.makeText(SettingsSupport.this.context, rootobj.getString("ErrorInfo"), Toast.LENGTH_LONG).show();
							}
						} catch (JSONException e) {
							Toast.makeText(SettingsSupport.this.context, e.toString(), Toast.LENGTH_LONG).show();
							e.printStackTrace();
						}
	    			}
            	}
            };
        };        
        
        if(checkNasaConnPref != null)
        checkNasaConnPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
        	public boolean onPreferenceClick(Preference preference) {
        		String sURL = mNasaServerPref.getText() + "?function=test_conn_nasa&user=" + mNasaServerUserPref.getText() + "&pass=" + mNasaServerPassPref.getText();
        		new HttpGetter(SettingsSupport.this.context, 2, SettingsSupport.this.context.getString(R.string.stChecking), mReturnHandler, true).execute(sURL);
        		return true;
        	}
        });
        
        if(checkUserConnPref != null)
        checkUserConnPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
           	     String sURL = mUserServerPref.getText() + "?function=test_conn_user&user=" + mUserServerUserPref.getText() + "&pass=" + mUserServerPassPref.getText();
        		 new HttpGetter(SettingsSupport.this.context, 1, SettingsSupport.this.context.getString(R.string.stChecking), mReturnHandler, true).execute(sURL);
				 return true;
            }
        });
        
        if(checkScanConnPref != null)
        checkScanConnPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
            	//String sURL = mUserServerPref.getText() + "?function=test_conn_nasa&user=" + mUserServerUserPref.getText() + "&pass=" + mUserServerPassPref.getText();
        		new ScanexHttpLogin(SettingsSupport.this.context, 3, SettingsSupport.this.context.getString(R.string.stChecking), mReturnHandler, true).execute(mScanServerUserPref.getText(), mScanServerPassPref.getText());
				return true;
            }
        });  
    }

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		CharSequence newVal = "";
		Preference Pref = screen.findPreference(key);
		if(key.equals(SettingsActivity.KEY_PREF_INTERVAL)){
			newVal = sharedPreferences.getString(key, "35");
    		Editor editor = screen.getSharedPreferences().edit();
    		long nVal = Long.parseLong((String) newVal) * DateUtils.MINUTE_IN_MILLIS;
    		editor.putLong(key + "_long", nVal);
    		editor.commit();
    		
			SharedPreferences mySharedPreferences = context.getSharedPreferences(MainActivity.PREFERENCES, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor1 = mySharedPreferences.edit();
            editor1.putLong(key + "_long", nVal);
            editor1.commit();   
            
            int nIndex = mSendDataIntervalPref.findIndexOfValue((String) newVal);
            if(nIndex >= 0){
            	mSendDataIntervalPref.setSummary((String) mSendDataIntervalPref.getEntries()[nIndex]);
            }
            return;
		}
		else if(key.equals(SettingsActivity.KEY_PREF_SEARCH_DAY_INTERVAL)){
			newVal = sharedPreferences.getString(key, "5");
    		Editor editor = screen.getSharedPreferences().edit();
    		int nVal = Integer.parseInt((String) newVal);
    		editor.putInt(key + "_int", nVal);
    		editor.commit();
    		
			SharedPreferences mySharedPreferences = context.getSharedPreferences(MainActivity.PREFERENCES, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor1 = mySharedPreferences.edit();
            editor1.putInt(key + "_int", nVal);
            editor1.commit();   
            
            int nIndex = mDayIntervalPref.findIndexOfValue((String) newVal);
            if(nIndex >= 0){
            	mDayIntervalPref.setSummary((String) mDayIntervalPref.getEntries()[nIndex]);
            }
            return;
		}
		else if(key.equals(SettingsActivity.KEY_PREF_ROW_COUNT)){
			newVal = sharedPreferences.getString(key, "");
			int nVal = Integer.parseInt((String) newVal);
    		Editor editor = screen.getSharedPreferences().edit();
    		editor.putInt(key + "_int", nVal);
    		editor.commit();
    		
			SharedPreferences mySharedPreferences = context.getSharedPreferences(MainActivity.PREFERENCES, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor1 = mySharedPreferences.edit();
            editor1.putInt(key + "_int", nVal);
            editor1.commit();    		

		} 	
		else if(key.equals(SettingsActivity.KEY_PREF_FIRE_SEARCH_RADIUS)){
			newVal = sharedPreferences.getString(key, "");
			int nVal = Integer.parseInt((String) newVal);
    		Editor editor = screen.getSharedPreferences().edit();
    		editor.putInt(key + "_int", nVal);
    		editor.commit();
    		
			SharedPreferences mySharedPreferences = context.getSharedPreferences(MainActivity.PREFERENCES, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor1 = mySharedPreferences.edit();
            editor1.putInt(key + "_int", nVal);
            editor1.commit();    
            
            Pref.setSummary(newVal + " " + context.getString(R.string.km));
            return;
		} 		
		else if(key.equals(SettingsActivity.KEY_PREF_SEARCH_CURR_DAY)){
			boolean bPref = sharedPreferences.getBoolean(key, false); 
			newVal = bPref ? context.getString(R.string.stSearchCurrentDayOn) : context.getString(R.string.stSearchCurrentDayOff);
			
            SharedPreferences.Editor editor = screen.getSharedPreferences().edit();
            editor.putBoolean(key, bPref);
            editor.commit();
            
			SharedPreferences mySharedPreferences = context.getSharedPreferences(MainActivity.PREFERENCES, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor1 = mySharedPreferences.edit();
            editor1.putBoolean(key, bPref);
            editor1.commit();
		}
		else if(key.equals(SettingsActivity.KEY_PREF_SERVICE_BATT_SAVE) || key.equals(SettingsActivity.KEY_PREF_NOTIFY_LED) ||key.equals(SettingsActivity.KEY_PREF_NOTIFY_SOUND) ||key.equals(SettingsActivity.KEY_PREF_NOTIFY_VIBRO)){
			boolean bPref = sharedPreferences.getBoolean(key, false); 
			newVal = bPref ? context.getString(R.string.stOn) : context.getString(R.string.stOff);
			
            SharedPreferences.Editor editor = screen.getSharedPreferences().edit();
            editor.putBoolean(key, bPref);
            editor.commit();		
            
			SharedPreferences mySharedPreferences = context.getSharedPreferences(MainActivity.PREFERENCES, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor1 = mySharedPreferences.edit();
            editor1.putBoolean(key, bPref);
            editor1.commit();
		}		
		else if(key.equals(SettingsActivity.KEY_PREF_SRV_NASA) || key.equals(SettingsActivity.KEY_PREF_SRV_USER)){
			newVal = sharedPreferences.getString(key, "");
			String sURL = newVal.toString();
			if(sURL.startsWith("http://") || sURL.startsWith("HTTP://")){
				//ok
			}
			else{
				sURL = "http://" + sURL;
	    		Editor editor = screen.getSharedPreferences().edit();
	    		editor.putString(key, sURL);
	    		editor.commit();	    		
	    		
	    		newVal = sURL;
			}
			SharedPreferences mySharedPreferences = context.getSharedPreferences(MainActivity.PREFERENCES, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor1 = mySharedPreferences.edit();
            editor1.putString(key, sURL);
            editor1.commit();
		}
		else if(key.equals(SettingsActivity.KEY_PREF_SRV_NASA_USER) || key.equals(SettingsActivity.KEY_PREF_SRV_USER_USER) || key.equals(SettingsActivity.KEY_PREF_SRV_SCAN_USER)) {  
            newVal = sharedPreferences.getString(key, "");
            
			SharedPreferences mySharedPreferences = context.getSharedPreferences(MainActivity.PREFERENCES, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor1 = mySharedPreferences.edit();
            editor1.putString(key, newVal.toString());
            editor1.commit();
		}
		
		else if(key.equals(SettingsActivity.KEY_PREF_COORD_FORMAT)){
            newVal = sharedPreferences.getString(key, "");
            
			SharedPreferences mySharedPreferences = context.getSharedPreferences(MainActivity.PREFERENCES, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor1 = mySharedPreferences.edit();
            editor1.putString(key, newVal.toString());
            editor1.commit();			
		}
			
		if(newVal.length() > 0)
        	Pref.setSummary(newVal);		
	}
	
	public void registerListener(){
		screen.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}
	
	public void unregisterListener(){
		screen.getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);		
	}
}

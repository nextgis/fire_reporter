/*******************************************************************************
*
* FireReporter
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

import org.json.JSONException;
import org.json.JSONObject;

import com.nextgis.firereporter.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.widget.Toast;

public class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener{
    public static final String KEY_PREF_COORD_FORMAT = "coordinates_format";
    public static final String KEY_PREF_INTERVAL = "interval";
    public static final String KEY_PREF_ROW_COUNT = "row_count";
    public static final String KEY_PREF_SEARCH_CURR_DAY = "search_current_date";
    public static final String KEY_PREF_SEARCH_DAY_INTERVAL = "days_before_current";
	public static final String KEY_PREF_SEND_IN_SUSPEND = "send_data_in_suspend";
	public static final String KEY_PREF_FIRE_SEARCH_RADIUS = "fire_search_radius";
    
    public static final String KEY_PREF_SRV_NASA = "mod14_server";
    public static final String KEY_PREF_SRV_NASA_USER = "mod14_user";
    public static final String KEY_PREF_SRV_NASA_PASS = "mod14_password";
    public static final String KEY_PREF_SRV_NASA_CHECK_CONN = "check_mod14_connection";
    
    public static final String KEY_PREF_SRV_USER = "user_server";
    public static final String KEY_PREF_SRV_USER_USER = "user_user";
    public static final String KEY_PREF_SRV_USER_PASS = "user_password";
    public static final String KEY_PREF_SRV_USER_CHECK_CONN = "check_user_connection";
    
    public static final String KEY_PREF_SRV_SCAN = "scanex_server";
    public static final String KEY_PREF_SRV_SCAN_USER = "scanex_user";
    public static final String KEY_PREF_SRV_SCAN_PASS = "scanex_password";
    public static final String KEY_PREF_SRV_SCAN_CHECK_CONN = "check_scanex_connection";
    
    private ListPreference mSendDataIntervalPref;
    private CheckBoxPreference mSendDataInSuspendPref;    
    private EditTextPreference mRowCountPref;
    private EditTextPreference mDayIntervalPref;
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

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
        
        mSendDataIntervalPref = (ListPreference) findPreference(KEY_PREF_INTERVAL);
        mSendDataIntervalPref.setSummary((String) mSendDataIntervalPref.getValue()); 
        
        mSendDataInSuspendPref = (CheckBoxPreference) findPreference(KEY_PREF_SEND_IN_SUSPEND);
        mSendDataInSuspendPref.setSummary(mSendDataInSuspendPref.isChecked() ? R.string.sSendDataInSuspendOn : R.string.sSendDataInSuspendOff);	    
        
        mRowCountPref = (EditTextPreference) findPreference(KEY_PREF_ROW_COUNT);
        mRowCountPref.setSummary((String) mRowCountPref.getText());
        
        mFireSearchRadiusPref = (EditTextPreference) findPreference(KEY_PREF_FIRE_SEARCH_RADIUS);
        mFireSearchRadiusPref.setSummary((String) mFireSearchRadiusPref.getText());
        
        mDayIntervalPref = (EditTextPreference) findPreference(KEY_PREF_SEARCH_DAY_INTERVAL);
        mDayIntervalPref.setSummary((String) mDayIntervalPref.getText());
        
        mSearchCurrentDatePref = (CheckBoxPreference) findPreference(KEY_PREF_SEARCH_CURR_DAY);
        mSearchCurrentDatePref.setSummary(mSearchCurrentDatePref.isChecked() ? R.string.stSearchCurrentDayOn : R.string.stSearchCurrentDayOff);	    
        
        mNasaServerPref = (EditTextPreference) findPreference(KEY_PREF_SRV_NASA);
        mNasaServerPref.setSummary((String) mNasaServerPref.getText());
        mNasaServerUserPref = (EditTextPreference) findPreference(KEY_PREF_SRV_NASA_USER);
        mNasaServerUserPref.setSummary((String) mNasaServerUserPref.getText());
        
        mUserServerPref = (EditTextPreference) findPreference(KEY_PREF_SRV_USER);
        mUserServerPref.setSummary((String) mUserServerPref.getText());
        mUserServerUserPref = (EditTextPreference) findPreference(KEY_PREF_SRV_USER_USER);
        mUserServerUserPref.setSummary((String) mUserServerUserPref.getText());

        mScanServerUserPref = (EditTextPreference) findPreference(KEY_PREF_SRV_SCAN_USER);
        mScanServerUserPref.setSummary((String) mScanServerUserPref.getText());
        
        
        mNasaServerPassPref = (EditTextPreference) findPreference(KEY_PREF_SRV_NASA_PASS);
        mUserServerPassPref = (EditTextPreference) findPreference(KEY_PREF_SRV_USER_PASS);
        mScanServerPassPref = (EditTextPreference) findPreference(KEY_PREF_SRV_SCAN_PASS);
        
        final Preference checkNasaConnPref = (Preference) findPreference(KEY_PREF_SRV_NASA_CHECK_CONN);
        final Preference checkUserConnPref = (Preference) findPreference(KEY_PREF_SRV_USER_CHECK_CONN);
        final Preference checkScanConnPref = (Preference) findPreference(KEY_PREF_SRV_SCAN_CHECK_CONN);
        
        mReturnHandler = new Handler() {
            public void handleMessage(Message msg) {
            	Bundle resultData = msg.getData();
            	
            	boolean bHaveErr = resultData.getBoolean("error");
            	if(bHaveErr){
            		Toast.makeText(getActivity(), resultData.getString("err_msq"), Toast.LENGTH_LONG).show();
            	}
            	else{            		            	
	    			int nType = resultData.getInt("src");
	    			String sData = resultData.getString("json");
	    			if(nType == 1){//user
	    		    	JSONObject jsonMainObject;
						try {
							jsonMainObject = new JSONObject(sData);
		    		    	if(jsonMainObject.getBoolean("error")){
		    		    	  String sMsg = jsonMainObject.getString("msg");
		    		    	  Toast.makeText(getActivity(), sMsg, Toast.LENGTH_LONG).show();
		    		    	  checkUserConnPref.setSummary(R.string.sCheckDBConnSummary);
		    		    	}
		    		    	else {
		    		    		checkUserConnPref.setSummary(R.string.stConnectionSucceeded);
		    		    	}
						} catch (JSONException e) {
							Toast.makeText(getActivity(), e.toString(), Toast.LENGTH_LONG).show();
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
		    		    	  Toast.makeText(getActivity(), sMsg, Toast.LENGTH_LONG).show();
		    		    	  checkNasaConnPref.setSummary(R.string.sCheckDBConnSummary);
		    		    	}
		    		    	else {
		    		    		checkNasaConnPref.setSummary(R.string.stConnectionSucceeded);
		    		    	}
						} catch (JSONException e) {
							Toast.makeText(getActivity(), e.toString(), Toast.LENGTH_LONG).show();
							e.printStackTrace();
							checkNasaConnPref.setSummary(R.string.sCheckDBConnSummary);
						}
	    			}
	    			else if(nType == 3){//scanex
	    				if(sData.isEmpty()){
	    					String sMsg = "Connect failed";
	    					Toast.makeText(getActivity(), sMsg, Toast.LENGTH_LONG).show();
		    		    	checkScanConnPref.setSummary(R.string.sCheckDBConnSummary);
	    				}
	    				else{
	    					checkScanConnPref.setSummary(R.string.stConnectionSucceeded);
	    				}
	    			}
            	}
            };
        };        
        
        checkNasaConnPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
        	public boolean onPreferenceClick(Preference preference) {
        		String sURL = mNasaServerPref.getText() + "?function=test_conn_nasa&user=" + mNasaServerUserPref.getText() + "&pass=" + mNasaServerPassPref.getText();
        		new HttpGetter(getActivity(), 2, getResources().getString(R.string.stChecking), mReturnHandler, true).execute(sURL);
        		return true;
        	}
        });
        
        checkUserConnPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
           	     String sURL = mUserServerPref.getText() + "?function=test_conn_user&user=" + mUserServerUserPref.getText() + "&pass=" + mUserServerPassPref.getText();
        		 new HttpGetter(getActivity(), 1, getResources().getString(R.string.stChecking), mReturnHandler, true).execute(sURL);
				 return true;
            }
        });
        
        checkScanConnPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
            	//String sURL = mUserServerPref.getText() + "?function=test_conn_nasa&user=" + mUserServerUserPref.getText() + "&pass=" + mUserServerPassPref.getText();
        		new HttpScanexLogin(getActivity(), 3, getResources().getString(R.string.stChecking), mReturnHandler, true).execute(mScanServerUserPref.getText(), mScanServerPassPref.getText());
				return true;
            }
        });     
    }
	
    @Override
	public void onResume() {
        super.onResume();
        
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
	public void onPause() {
        super.onPause();

        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }
        
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		CharSequence newVal = "";
		Preference Pref = findPreference(key);
		if(key.equals(KEY_PREF_INTERVAL)){
			newVal = sharedPreferences.getString(key, "0 min");
        	String toLongStr = ((String) newVal).substring(0, newVal.length() - 4);
    		Editor editor = getPreferenceScreen().getSharedPreferences().edit();
    		editor.putLong(key + "_long", Long.parseLong(toLongStr) * 1000);
    		editor.commit();
    		
			SharedPreferences mySharedPreferences = getActivity().getSharedPreferences(FireReporter.PREFERENCES, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor1 = mySharedPreferences.edit();
            editor1.putLong(key + "_long", Long.parseLong(toLongStr) * 1000);
            editor1.commit();    		
		}
		else if(key.equals(KEY_PREF_ROW_COUNT) || key.equals(KEY_PREF_SEARCH_DAY_INTERVAL) || key.equals(KEY_PREF_FIRE_SEARCH_RADIUS)){
			newVal = sharedPreferences.getString(key, "");
			int nVal = Integer.parseInt((String) newVal);
    		Editor editor = getPreferenceScreen().getSharedPreferences().edit();
    		editor.putInt(key + "_int", nVal);
    		editor.commit();
		} 	
		else if(key.equals(KEY_PREF_SEARCH_CURR_DAY)){
			boolean bPref = sharedPreferences.getBoolean(key, false); 
			newVal = bPref ? getText(R.string.stSearchCurrentDayOn) : getText(R.string.stSearchCurrentDayOff);
			
            SharedPreferences.Editor editor = getPreferenceScreen().getSharedPreferences().edit();
            editor.putBoolean(key, bPref);
            editor.commit();			
		}
		else if(key.equals(KEY_PREF_SEND_IN_SUSPEND)){
			boolean bPref = sharedPreferences.getBoolean(key, false); 
			newVal = bPref ? getText(R.string.sSendDataInSuspendOn) : getText(R.string.sSendDataInSuspendOff);
			
            SharedPreferences.Editor editor = getPreferenceScreen().getSharedPreferences().edit();
            editor.putBoolean(key, bPref);
            editor.commit();			
		}		
		else if(key.equals(KEY_PREF_SRV_NASA) || key.equals(KEY_PREF_SRV_USER)){
			newVal = sharedPreferences.getString(key, "");
			String sURL = newVal.toString();
			if(sURL.startsWith("http://") || sURL.startsWith("HTTP://")){
				//ok
			}
			else{
				sURL = "http://" + sURL;
	    		Editor editor = getPreferenceScreen().getSharedPreferences().edit();
	    		editor.putString(key, sURL);
	    		editor.commit();
	    		
	    		newVal = sURL;
			}
		}
		else if(key.equals(KEY_PREF_SRV_NASA_USER) || key.equals(KEY_PREF_SRV_USER_USER) || key.equals(KEY_PREF_SRV_SCAN_USER)) {  
            newVal = sharedPreferences.getString(key, "");
		}
			
		if(newVal.length() > 0)
        	Pref.setSummary(newVal);
	}	
}

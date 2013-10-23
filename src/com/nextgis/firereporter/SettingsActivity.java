/*******************************************************************************
 * Project:  Fire reporter
 * Purpose:  Report and view fires
 * Author:   Dmitry Baryshnikov (aka Bishop), polimax@mail.ru
 ******************************************************************************
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

import java.util.List;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;

public class SettingsActivity extends SherlockPreferenceActivity {

	protected final static String ACTION_PREFS_GENERAL = "com.nextgis.firereporter.PREFS_GENERAL";
	protected final static String ACTION_PREFS_NASA = "com.nextgis.firereporter.PREFS_NASA";
	protected final static String ACTION_PREFS_USER = "com.nextgis.firereporter.PREFS_USER";
	protected final static String ACTION_PREFS_SCANEX = "com.nextgis.firereporter.PREFS_SCANEX";
	
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
    public static final String KEY_PREF_SRV_SCAN_USER_NAME = "scanex_user_name";
    public static final String KEY_PREF_SRV_SCAN_USER_PHONE = "scanex_user_phone";

    public static final String KEY_PREF_UPDATE_DATA_TIME = "updatedata_time";
    public static final String KEY_PREF_SERVICE_BATT_SAVE = "service_battary_save";
	
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        
        String action = getIntent().getAction();
        if (action != null && action.equals(ACTION_PREFS_GENERAL)) {
            addPreferencesFromResource(R.xml.preferences_general);
        }
        else if (action != null && action.equals(ACTION_PREFS_NASA)) {
            addPreferencesFromResource(R.xml.preferences_nasa);
        }
        else if (action != null && action.equals(ACTION_PREFS_USER)) {
            addPreferencesFromResource(R.xml.preferences_user);
        }
        else if (action != null && action.equals(ACTION_PREFS_SCANEX)) {
            addPreferencesFromResource(R.xml.preferences_scanex);
        }
        else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            // Load the legacy preferences headers
            addPreferencesFromResource(R.xml.preference_headers_legacy);
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
            	finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    //TODO: add 2.3 support for check db connections
    
    @SuppressLint("NewApi")
	@Override
    public void onBuildHeaders(List<Header> target) {
       loadHeadersFromResource(R.xml.preference_headers, target);
    }
}

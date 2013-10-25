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

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceFragment;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class SettingsFragment extends PreferenceFragment{

	private SettingsSupport support;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        
        String settings = getArguments().getString("settings");
        if ("general".equals(settings)) {
            addPreferencesFromResource(R.xml.preferences_general);
        } else if ("nasa".equals(settings)) {
            addPreferencesFromResource(R.xml.preferences_nasa);
        } else if ("user".equals(settings)) {
            addPreferencesFromResource(R.xml.preferences_user);
        } else if ("scanex".equals(settings)) {
            addPreferencesFromResource(R.xml.preferences_scanex);
        }
        
        support = new SettingsSupport(getActivity(), this.getPreferenceScreen());
  
    }
	
    @Override
	public void onResume() {
        super.onResume();
        
        if(support != null)
        	support.registerListener();
    }

    @Override
	public void onPause() {
        super.onPause();
        if(support != null)
        	support.unregisterListener();
    } 
}

/*******************************************************************************
 * Project:  Fire reporter
 * Purpose:  Report and view fires
 * Author:   Dmitry Baryshnikov (aka Bishop), polimax@mail.ru
 ******************************************************************************
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.nextgis.firereporter.NeighborFiresDataFragment.FireItemComparator;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

public class ScanexDataFragment extends SherlockFragment implements FiresResultReceiver.Receiver {
	protected FiresResultReceiver mReceiver;
    protected SubscbesListAdapter mListAdapter;
    protected ListView mListFireInfo;
    protected List <SubscriptionItem> mSubscbesList;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		setHasOptionsMenu(true);
		
		//get subscriprions http://fires.kosmosnimki.ru/SAPI/Subscribe/Get/?CallBackName=44
		//get subscritption by id http://fires.kosmosnimki.ru/SAPI/Subscribe/Get/6?CallBackName=44
		//add subscription http://fires.kosmosnimki.ru/SAPI/Subscribe/Add?tItle=«‡„ÓÎÓ‚ÓÍ&typeReport=1&layerName=»Ãﬂ_—ÀŒﬂ&wkt=√≈ŒÃ≈“–»ﬂ_WKT&CallBackName=44 
		//update subscription http://fires.kosmosnimki.ru/SAPI/Subscribe/Update/?CallBackName=44?id=6
		//delete subscriprion http://fires.kosmosnimki.ru/SAPI/Subscribe/Delete/6?CallBackName=44
		
		/*
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getSherlockActivity());
		mnFilter = prefs.getInt(MainActivity.PREF_CURRENT_FILTER, MainActivity.SRC_NASA | MainActivity.SRC_USER);
		
        
        */
		
		mSubscbesList = new ArrayList<SubscriptionItem>();
		mReceiver = new FiresResultReceiver(new Handler());
        mReceiver.setReceiver(this);
        
        StartService();		
		
		super.onCreate(savedInstanceState);
	}
    
    @Override
	public void onPause() {
    	mReceiver.setReceiver(null); // clear receiver so no leaks.
		super.onPause();
	}

	@Override
	public void onDestroy() {
		StopService();
		super.onDestroy();
	}

	@Override
	public void onResume() {

        mReceiver.setReceiver(this);

        final Intent intent = new Intent(MainActivity.INTENT_NAME, null, getSherlockActivity(), GetFiresService.class);
        intent.putExtra("receiver_scanex", mReceiver);
        intent.putExtra("command", GetFiresService.SERVICE_SCANEXDATA);
        
        getSherlockActivity().startService(intent);

		super.onResume();
	}
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	
    	this.setRetainInstance(true);

    	View view = inflater.inflate(R.layout.scanexfragment, container, false);
     	
    	// load list
    	mListFireInfo = (ListView)view.findViewById(R.id.Mainlist);
    	// create new adapter
    	mListAdapter = new SubscbesListAdapter(getSherlockActivity(), mSubscbesList);
    	// set adapter to list view
    	mListFireInfo.setAdapter(mListAdapter);


    	// implement event when an item on list view is selected
    	mListFireInfo.setOnItemClickListener(new OnItemClickListener(){
    		public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
    			// get the list adapter
    			SubscbesListAdapter appListAdapter = (SubscbesListAdapter)parent.getAdapter();
    			// get selected item on the list
    			SubscriptionItem item = (SubscriptionItem)appListAdapter.getItem(pos);
    			// TODO: open notes fragment     			
    		}

    	});

    	return view;
    }

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		//add subscription
		//delete subscription
		//read all
		
		/*menu.add(com.actionbarsherlock.view.Menu.NONE, MainActivity.MENU_PLACE, com.actionbarsherlock.view.Menu.NONE, R.string.sPlace)
		.setIcon(R.drawable.ic_location_place)
		.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		*/
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
        case MainActivity.MENU_REFRESH:
        	StartService();
        	
        	return true;
        }
       
		return super.onOptionsItemSelected(item);
	}

	public void onReceiveResult(int resultCode, Bundle resultData) {
		switch (resultCode) {
		case GetFiresService.SERVICE_SCANEXSTART:
			((MainActivity)getSherlockActivity()).refresh();
			break;
		case GetFiresService.SERVICE_SCANEXDATA:
			mSubscbesList.add((SubscriptionItem) resultData.getParcelable("item"));		
		    mListAdapter.notifyDataSetChanged();
			break;
		case GetFiresService.SERVICE_STOP:
			((MainActivity)getSherlockActivity()).completeRefresh();
			break;
		case GetFiresService.SERVICE_ERROR:
			Toast.makeText(getSherlockActivity(), resultData.getString("err_msq"), Toast.LENGTH_LONG).show();
			break;
		}		
	}
	
	protected void StartService(){
        final Intent intent = new Intent(MainActivity.INTENT_NAME, null, getSherlockActivity(), GetFiresService.class);
        intent.putExtra("receiver_scanex", mReceiver);
        intent.putExtra("command", GetFiresService.SERVICE_SCANEXSTART);
        
        getSherlockActivity().startService(intent);
	}
	
	protected void StopService(){
        final Intent intent = new Intent(MainActivity.INTENT_NAME, null, getSherlockActivity(), GetFiresService.class);
        intent.putExtra("command", GetFiresService.SERVICE_STOP);
        
        getSherlockActivity().startService(intent);
	}
	
	
/*	protected void GetScanexData(){
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
		String sLogin = prefs.getString(SettingsActivity.KEY_PREF_SRV_SCAN_USER, "new@kosmosnimki.ru");
		String sPass = prefs.getString(SettingsActivity.KEY_PREF_SRV_SCAN_PASS, "test123");
		//new HttpScanexLogin(MainActivity.this, 3, getResources().getString(R.string.stChecking), mFillDataHandler, true).execute(sLogin, sPass);
		
		mFireList.add(new FireItem(getBaseContext(), 3, 0, Calendar.getInstance().getTime(), 37, 55, -1, R.drawable.ic_scan, "http://ya.ru"));
		mListAdapter.notifyDataSetChanged();
	}
*/
}

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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

public class FiresDataFragment extends SherlockFragment implements FiresResultReceiver.Receiver {

    protected FireListAdapter mListAdapter;
    protected ListView mListFireInfo;
    protected List <FireItem> mFireList;
    
    protected FiresResultReceiver mReceiver;
    
    protected int mnFilter;
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		setHasOptionsMenu(true);
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getSherlockActivity());
		mnFilter = prefs.getInt(MainActivity.PREF_CURRENT_FILTER, MainActivity.SRC_NASA | MainActivity.SRC_USER);
		
		mReceiver = new FiresResultReceiver(new Handler());
        mReceiver.setReceiver(this);
        
        mFireList = new ArrayList<FireItem>();
        
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
//		mReceiver = new FiresResultReceiver(new Handler());
        mReceiver.setReceiver(this);
        mFireList.clear();

        final Intent intent = new Intent(MainActivity.INTENT_NAME, null, getSherlockActivity(), GetFiresService.class);
        intent.putExtra(GetFiresService.RECEIVER, mReceiver);
        intent.putExtra(GetFiresService.COMMAND, GetFiresService.SERVICE_DATA);
        
        getSherlockActivity().startService(intent);
        
		super.onResume();
	}
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	
    	this.setRetainInstance(true);

    	View view = inflater.inflate(R.layout.neighbourfragment, container, false);
    
    	// load list
    	mListFireInfo = (ListView)view.findViewById(R.id.Mainlist);
    	// create new adapter
    	mListAdapter = new FireListAdapter(getSherlockActivity(), mFireList);
    	// set adapter to list view
    	mListFireInfo.setAdapter(mListAdapter);


    	//TODO:
    	// implement event when an item on list view is selected
    	/*
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
    	*/
    	return view;
    }

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		
		menu.add(com.actionbarsherlock.view.Menu.NONE, MainActivity.MENU_PLACE, com.actionbarsherlock.view.Menu.NONE, R.string.sPlace)
		.setIcon(R.drawable.ic_location_place)
		.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MainActivity.MENU_PLACE:
            Intent intentSendReport = new Intent(getSherlockActivity(), SendReportActivity.class);
            intentSendReport.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intentSendReport);
        	return true;
        case MainActivity.MENU_REFRESH:
        	StartService();
        	
        	return true;
        }
		return super.onOptionsItemSelected(item);
	}
	
	protected void StartService(){
        final Intent intent = new Intent(MainActivity.INTENT_NAME, null, getSherlockActivity(), GetFiresService.class);
        intent.putExtra(GetFiresService.RECEIVER, mReceiver);
        intent.putExtra(GetFiresService.COMMAND, GetFiresService.SERVICE_START);
        intent.putExtra(GetFiresService.SOURCE, mnFilter);
        
        getSherlockActivity().startService(intent);
	}
	
	protected void StopService(){
        final Intent intent = new Intent(MainActivity.INTENT_NAME, null, getSherlockActivity(), GetFiresService.class);
        intent.putExtra(GetFiresService.COMMAND, GetFiresService.SERVICE_STOP);
        
        getSherlockActivity().startService(intent);
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
	

	public void onReceiveResult(int resultCode, Bundle resultData) {
		if((resultCode & GetFiresService.SERVICE_START) !=0 ){
			((MainActivity)getSherlockActivity()).refresh();			
		}

		if((resultCode & GetFiresService.SERVICE_DATA) !=0 ){
  		  	mFireList.add((FireItem) resultData.getParcelable(GetFiresService.ITEM));		
		    Collections.sort(mFireList, new FireItemComparator());
		    mListAdapter.notifyDataSetChanged();
		}

		if((resultCode & GetFiresService.SERVICE_STOP) !=0 ){
			((MainActivity)getSherlockActivity()).completeRefresh();
		}
		
		if((resultCode & GetFiresService.SERVICE_ERROR) !=0 ){
			Toast.makeText(getSherlockActivity(), resultData.getString(GetFiresService.ERR_MSG), Toast.LENGTH_LONG).show();
		}
	}
}
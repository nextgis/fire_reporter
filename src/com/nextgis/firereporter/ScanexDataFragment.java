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

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentTransaction;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

public class ScanexDataFragment extends SherlockFragment implements FiresResultReceiver.Receiver {
	protected FiresResultReceiver mReceiver;
	protected boolean mbTwoPanelMode;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		setHasOptionsMenu(true);
		
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
	public void onResume() {

        mReceiver.setReceiver(this);
        
		ScanexSubscibesFragment ListFragment = (ScanexSubscibesFragment) getChildFragmentManager().findFragmentByTag("LIST");
        if(ListFragment != null){
        	ListFragment.clear();        

        	final Intent intent = new Intent(MainActivity.INTENT_NAME, null, getSherlockActivity(), GetFiresService.class);
        	intent.putExtra(GetFiresService.RECEIVER_SCANEX, mReceiver);
        	intent.putExtra(GetFiresService.COMMAND, GetFiresService.SERVICE_SCANEXDATA);
        
        	getSherlockActivity().startService(intent);
        }

		super.onResume();
	}
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	
    	this.setRetainInstance(true);
    	
    	View view = inflater.inflate(R.layout.scanexview, container, false);
   	
        FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
   	
    	DisplayMetrics metrics = new DisplayMetrics();
    	getSherlockActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
    	
    	if(getChildFragmentManager().findFragmentByTag("LIST") == null){
    		fragmentTransaction.add(R.id.scanex_container_list, new ScanexSubscibesFragment(), "LIST");
    	}
    	
   		ScanexNotificationsFragment NotesFr = (ScanexNotificationsFragment) getChildFragmentManager().findFragmentByTag("DETAILES");

    	if(metrics.widthPixels > 1000){
    		mbTwoPanelMode = true;

            if(NotesFr == null){
            	fragmentTransaction.add(R.id.scanex_container_detailes, new ScanexNotificationsFragment(), "DETAILES");
            }
            //else{
            //	fragmentTransaction.show(NotesFr);
            //}  
    	}
    	else
    	{
    		mbTwoPanelMode = false;
            if(NotesFr != null){
            	fragmentTransaction.remove(NotesFr);
            	//fragmentTransaction.hide(NotesFr);
            }
    	}
 	
        fragmentTransaction.commit(); 
        //getChildFragmentManager().executePendingTransactions();
        
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
			ScanexSubscibesFragment ListFragment = (ScanexSubscibesFragment) getChildFragmentManager().findFragmentByTag("LIST");
			if(ListFragment != null){				
				int nType = resultData.getInt(GetFiresService.TYPE);
				if(nType == GetFiresService.SCANEX_SUBSCRIPTION){
					ListFragment.add((ScanexSubscriptionItem) resultData.getParcelable(GetFiresService.ITEM));
				}
				else if(nType == GetFiresService.SCANEX_NOTIFICATION){
					long nSubID = resultData.getLong(GetFiresService.SUBSCRIPTION_ID);
					//long nNoteID = resultData.getLong("note_id");
					ScanexNotificationItem item = (ScanexNotificationItem) resultData.getParcelable(GetFiresService.ITEM);
					ListFragment.add(nSubID, item);
					
					ScanexNotificationsFragment NotesFragment = (ScanexNotificationsFragment) getChildFragmentManager().findFragmentByTag("DETAILES");
					if(NotesFragment != null){
						NotesFragment.add(item);
					}
				}
			}
			break;
		case GetFiresService.SERVICE_STOP:
			((MainActivity)getSherlockActivity()).completeRefresh();
			break;
		case GetFiresService.SERVICE_ERROR:
			Toast.makeText(getSherlockActivity(), resultData.getString(GetFiresService.ERR_MSG), Toast.LENGTH_LONG).show();
			break;
		}		
	}
	
	protected void StartService(){
        final Intent intent = new Intent(MainActivity.INTENT_NAME, null, getSherlockActivity(), GetFiresService.class);
        intent.putExtra(GetFiresService.RECEIVER_SCANEX, mReceiver);
        intent.putExtra(GetFiresService.COMMAND, GetFiresService.SERVICE_SCANEXSTART);
        
        getSherlockActivity().startService(intent);
	}
	
	public void onSelectSubscribe(ScanexSubscriptionItem item) {
		if(mbTwoPanelMode){
			ScanexNotificationsFragment NotesFragment = (ScanexNotificationsFragment) getChildFragmentManager().findFragmentByTag("DETAILES");
			if(NotesFragment != null){
				NotesFragment.addSubscriptionId(item.GetId());
				NotesFragment.add(item.GetItems());
			}
		}
		else{			
            Intent intent = new Intent(getSherlockActivity(), ScanexNotificationsActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra(GetFiresService.ITEM, item);
            intent.putExtra(GetFiresService.SUBSCRIPTION_ID, item.GetId());
            startActivity(intent);
		}
		
	}
	
}

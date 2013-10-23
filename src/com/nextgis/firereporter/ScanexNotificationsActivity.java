/*******************************************************************************
 * Project:  Fire reporter
 * Purpose:  Report and view fires
 * Author:   Dmitry Baryshnikov (aka Bishop), polimax@mail.ru
 *******************************************************************************
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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class ScanexNotificationsActivity  extends SherlockFragmentActivity implements FiresResultReceiver.Receiver {
	protected FiresResultReceiver mReceiver;
	protected ScanexNotificationsFragment mNotesFragment;
    private MenuItem refreshItem;    
    protected boolean mbRefreshing;
    protected long mnSubscriptionId;
    
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
 
		mReceiver = new FiresResultReceiver(new Handler());
        mReceiver.setReceiver(this);
       
        setContentView(R.layout.scanexnotesactivity);
        
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        mNotesFragment = new ScanexNotificationsFragment();
  		fragmentTransaction.add(R.id.scanex_notes_container, mNotesFragment, "DETAILES");
        fragmentTransaction.commit();  
        
        getSupportFragmentManager().executePendingTransactions();

        //
	    Bundle extras = getIntent().getExtras(); 
	    if(extras != null) {
	    	ScanexSubscriptionItem item = extras.getParcelable(GetFiresService.ITEM);
	    	mNotesFragment.add(item.GetItems());
	    	mnSubscriptionId = extras.getLong(GetFiresService.SUBSCRIPTION_ID);
	    	mNotesFragment.addSubscriptionId(mnSubscriptionId);
	    }
	    
       	getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        
        StartService();	
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    
			menu.add(com.actionbarsherlock.view.Menu.NONE, MainActivity.MENU_SETTINGS, com.actionbarsherlock.view.Menu.NONE, R.string.tabSettings)
	       .setIcon(R.drawable.ic_action_settings)
	       .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);		
			
			menu.add(com.actionbarsherlock.view.Menu.NONE, MainActivity.MENU_ABOUT, com.actionbarsherlock.view.Menu.NONE, R.string.tabAbout)
			.setIcon(R.drawable.ic_action_about)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);	
			
			refreshItem = menu.add(com.actionbarsherlock.view.Menu.NONE, MainActivity.MENU_REFRESH, com.actionbarsherlock.view.Menu.NONE, R.string.sRefresh)
			.setIcon(R.drawable.ic_navigation_refresh);
			refreshItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
			
			return true;
	    
	  }

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
	        case android.R.id.home:
	            // app icon in action bar clicked; go home
	        	finish();
	            return true;
	        case MainActivity.MENU_SETTINGS:
	            // app icon in action bar clicked; go home
	            Intent intentSet = new Intent(this, SettingsActivity.class);
	            intentSet.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
	            startActivity(intentSet);
	            return true;
	        case MainActivity.MENU_ABOUT:
	            // app icon in action bar clicked; go home
	            Intent intentAbout = new Intent(this, AboutActivity.class);
	            intentAbout.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
	            startActivity(intentAbout);
	            return true;
	        case MainActivity.MENU_REFRESH:
	        	StartService();	        	
	        	return true;
	        }
		
		return super.onOptionsItemSelected(item);
		
	}
	
    @Override
	public void onPause() {
    	mReceiver.setReceiver(null); // clear receiver so no leaks.
		super.onPause();
	}

	@Override
	public void onResume() {

        mReceiver.setReceiver(this);        
		super.onResume();
	}
	
	public void refresh() {
		if(refreshItem == null || mbRefreshing)
			return;

	     LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	     ImageView iv = (ImageView) inflater.inflate(R.layout.refresh_action_view, null);

	     Animation rotation = AnimationUtils.loadAnimation(this, R.anim.clockwise_refresh);
	     rotation.setRepeatCount(Animation.INFINITE);
	     iv.startAnimation(rotation);

	     refreshItem.setActionView(iv);
	     
	     mbRefreshing = true;
	}

	public void completeRefresh() {
		if(refreshItem == null || refreshItem.getActionView() == null)
			return;
		refreshItem.getActionView().clearAnimation();
		refreshItem.setActionView(null);
		
		mbRefreshing = false;
	}
	
	public void onReceiveResult(int resultCode, Bundle resultData) {
		switch (resultCode) {
		case GetFiresService.SERVICE_SCANEXSTART:
			refresh();
			break;
		case GetFiresService.SERVICE_SCANEXDATA:
			ScanexNotificationsFragment NotesFragment = (ScanexNotificationsFragment) getSupportFragmentManager().findFragmentByTag("DETAILES");
			if(NotesFragment != null){				
				int nType = resultData.getInt(GetFiresService.TYPE);
				long nSubID = resultData.getLong(GetFiresService.SUBSCRIPTION_ID);
				if(nType == GetFiresService.SCANEX_NOTIFICATION && nSubID == mnSubscriptionId){					
					//long nNoteID = resultData.getLong("note_id");
					ScanexNotificationItem item = (ScanexNotificationItem) resultData.getParcelable(GetFiresService.ITEM);
					mNotesFragment.add(item);
				}
			}
			break;
		case GetFiresService.SERVICE_STOP:
			completeRefresh();
			break;
		case GetFiresService.SERVICE_ERROR:
			Toast.makeText(this, resultData.getString(GetFiresService.ERR_MSG), Toast.LENGTH_LONG).show();
			break;
		}		
	}
	
	protected void StartService(){
        final Intent intent = new Intent(MainActivity.INTENT_NAME, null, this, GetFiresService.class);
        intent.putExtra(GetFiresService.RECEIVER_SCANEX, mReceiver);
        intent.putExtra(GetFiresService.COMMAND, GetFiresService.SERVICE_SCANEXSTART);
        
        startService(intent);
	}
}

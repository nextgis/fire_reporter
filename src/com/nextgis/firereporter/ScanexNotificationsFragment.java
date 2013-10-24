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
import java.util.Map;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockFragment;

public class ScanexNotificationsFragment extends SherlockFragment {
    protected ScanexNotificationListAdapter mListAdapter;
    protected ListView mListFireInfo;
    protected List <ScanexNotificationItem> mFireList;
    protected long mnSubscriptionId;
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
        
        mFireList = new ArrayList<ScanexNotificationItem>();
        
 		super.onCreate(savedInstanceState);
	}    
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	
    	View view = inflater.inflate(R.layout.neighbourfragment, container, false);
        
    	// load list
    	mListFireInfo = (ListView)view.findViewById(R.id.Mainlist);
    	// create new adapter
    	mListAdapter = new ScanexNotificationListAdapter(getSherlockActivity(), mFireList);
    	// set adapter to list view
    	mListFireInfo.setAdapter(mListAdapter);

    	// implement event when an item on list view is selected

    	mListFireInfo.setOnItemClickListener(new OnItemClickListener(){
    		public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
    			// get the list adapter
    			ScanexNotificationListAdapter appFireListAdapter = (ScanexNotificationListAdapter)parent.getAdapter();
    			// get selected item on the list
    			ScanexNotificationItem item = (ScanexNotificationItem)appFireListAdapter.getItem(pos);
    			item.setWatched(true);
    			mListAdapter.notifyDataSetInvalidated();
    			// launch the selected application
    			
                Intent intent = new Intent(getSherlockActivity(), ScanexNotificationActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra(GetFiresService.ITEM, item);
                startActivity(intent);
                
                onItemWatched(mnSubscriptionId, item.GetId());
    		}

    	});

    	return view;
    }

	public void add(Map<Long, ScanexNotificationItem> getItems) {
		mFireList.clear();
		for(ScanexNotificationItem item : getItems.values()){
			mFireList.add(item);
		}
	    Collections.sort(mFireList, new FireItemComparator());
	    if(mListAdapter != null)
	    	mListAdapter.notifyDataSetChanged();
	}
	
	public void add(ScanexNotificationItem item) {
		for(ScanexNotificationItem storedItem : mFireList){
			if(storedItem.GetId() == item.GetId()){
				return;
			}
		}

		mFireList.add(item);
	    Collections.sort(mFireList, new FireItemComparator());
	    if(mListAdapter != null)
	    	mListAdapter.notifyDataSetChanged();
		
	}
	
	public class FireItemComparator implements Comparator<ScanexNotificationItem>
	{
	    public int compare(ScanexNotificationItem left, ScanexNotificationItem right) {
	    	long nInterval = left.GetDate().getTime() - right.GetDate().getTime();
	    	if(nInterval == 0){
	    		nInterval = (long) (left.GetConfidence() - right.GetConfidence());
	    	}
	    	
	    	if (nInterval < 0)
	    		return 1;
	    	else if (nInterval > 0)
	    		return -1;
	    	else
	    		return 0;
	    }
	}

	public void addSubscriptionId(long nSubscriptionId) {
		mnSubscriptionId = nSubscriptionId;
	}
	
	protected void onItemWatched(long nSubscriptionId, long nNotificationId){
        final Intent intent = new Intent(MainActivity.INTENT_NAME, null, getSherlockActivity(), GetFiresService.class);
        intent.putExtra(GetFiresService.COMMAND, GetFiresService.SERVICE_SCANEXDATAUPDATE);
        intent.putExtra(GetFiresService.SUBSCRIPTION_ID, nSubscriptionId);
        intent.putExtra(GetFiresService.NOTIFICATION_ID, nNotificationId);
        
        getSherlockActivity().startService(intent);
	}
}

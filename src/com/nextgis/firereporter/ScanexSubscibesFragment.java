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
import java.util.List;

import com.actionbarsherlock.app.SherlockFragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class ScanexSubscibesFragment extends SherlockFragment {
	protected FiresResultReceiver mReceiver;
    protected ScanexSubscbesListAdapter mListAdapter;
    protected ListView mListFireInfo;
    protected List<ScanexSubscriptionItem> mSubscbesList;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		setHasOptionsMenu(true);

		mSubscbesList = new ArrayList<ScanexSubscriptionItem>();
		
		super.onCreate(savedInstanceState);
	}
    
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	
    	View view = inflater.inflate(R.layout.scanexfragment, container, false);
     	
    	// load list
    	mListFireInfo = (ListView)view.findViewById(R.id.Mainlist);
    	// create new adapter
    	mListAdapter = new ScanexSubscbesListAdapter(getSherlockActivity(), mSubscbesList);
    	// set adapter to list view
    	mListFireInfo.setAdapter(mListAdapter);


    	// implement event when an item on list view is selected
    	mListFireInfo.setOnItemClickListener(new OnItemClickListener(){
    		public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
    			// get the list adapter
    			ScanexSubscbesListAdapter appListAdapter = (ScanexSubscbesListAdapter)parent.getAdapter();
    			// get selected item on the list
    			ScanexSubscriptionItem item = (ScanexSubscriptionItem)appListAdapter.getItem(pos);
    			item.setHasNews(false);
    			mListAdapter.notifyDataSetInvalidated();
    			//open notes fragment
    			final ScanexDataFragment pParentFragment = (ScanexDataFragment)getParentFragment();
    			pParentFragment.onSelectSubscribe(item);
    		}

    	});
    	return view;
    }

	public void clear() {
		if(mSubscbesList != null){
			mSubscbesList.clear();	
		}
	}

	public void add(ScanexSubscriptionItem parcelable) {
		if(mSubscbesList != null){
			for(ScanexSubscriptionItem item : mSubscbesList){
				if(item.GetId() == parcelable.GetId())
					return;
			}
		}
		mSubscbesList.add(parcelable);		
		mListAdapter.notifyDataSetChanged();
	}

	public void add(long nSubID, ScanexNotificationItem parcelable) {
		if(mSubscbesList != null){
			for(ScanexSubscriptionItem item : mSubscbesList){
				if(item.GetId() == nSubID){
					item.add(parcelable);
					mListAdapter.notifyDataSetInvalidated();
				}
			}
		}
	}	
}

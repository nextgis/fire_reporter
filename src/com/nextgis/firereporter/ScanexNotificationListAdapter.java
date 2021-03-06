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

import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ScanexNotificationListAdapter extends BaseAdapter {
	private Context mContext;
	private List <ScanexNotificationItem> mListFireInfo;
	
	public ScanexNotificationListAdapter(Context c, List<ScanexNotificationItem> mFireList) {
		mContext = c;
		mListFireInfo = mFireList;
	}
	

	public int getCount() {
		return mListFireInfo.size();
	}

	public Object getItem(int arg0) {
		return mListFireInfo.get(arg0);
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		// get the selected entry
		ScanexNotificationItem entry = mListFireInfo.get(position);

    	// inflate new layout if null
		if(convertView == null) {
			LayoutInflater inflater = LayoutInflater.from(mContext);
			convertView = inflater.inflate(R.layout.rowlayout, null);
			ImageView ivIcon = (ImageView)convertView.findViewById(R.id.ivIcon);
			ivIcon.setImageDrawable(convertView.getResources().getDrawable(entry.GetIconId()));

		}

		// load controls from layout resources
		TextView tvText1 = (TextView)convertView.findViewById(R.id.tvText1);
		TextView tvText2 = (TextView)convertView.findViewById(R.id.tvText2);

		// set data to display
		tvText1.setText(entry.GetDateAsString());
		tvText2.setText(entry.GetCoordinates());
		
		if(entry.isWatched()){
			tvText1.setTypeface(null, Typeface.NORMAL);
			tvText1.setTextColor(Color.GRAY);
			tvText2.setTypeface(null, Typeface.NORMAL);
			tvText2.setTextColor(Color.GRAY);
		}
		else{
			tvText1.setTypeface(null, Typeface.BOLD);
			tvText1.setTextColor(Color.WHITE);
			tvText2.setTypeface(null, Typeface.BOLD);
			tvText2.setTextColor(Color.WHITE);
		}

		return convertView;

	}
}

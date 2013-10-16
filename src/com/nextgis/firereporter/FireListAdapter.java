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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class FireListAdapter extends BaseAdapter {
	private Context mContext;
	private List <FireItem> mListFireInfo;
	
	public FireListAdapter(Context c, List<FireItem> mFireList) {
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
		FireItem entry = mListFireInfo.get(position);

		// reference to convertView
		View v = convertView;

		// inflate new layout if null
		if(v == null) {
			LayoutInflater inflater = LayoutInflater.from(mContext);
			v = inflater.inflate(R.layout.rowlayout, null);
		}

		// load controls from layout resources
		ImageView ivIcon = (ImageView)v.findViewById(R.id.ivIcon);
		TextView tvText1 = (TextView)v.findViewById(R.id.tvText1);
		TextView tvText2 = (TextView)v.findViewById(R.id.tvText2);

		// set data to display
		ivIcon.setImageDrawable(v.getResources().getDrawable(entry.GetIconId()));
		tvText1.setText(entry.GetDateAsString());
		tvText2.setText(entry.GetCoordinates());

		return v;

	}
	
}

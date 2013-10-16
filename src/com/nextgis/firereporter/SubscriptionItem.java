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

import java.util.HashMap;

import android.os.Parcel;
import android.os.Parcelable;

public class SubscriptionItem implements Parcelable {
	protected long nID;
	protected String sTitle;
	protected String sLayerName;
	protected String sWKT;
	protected boolean bSMSEnable;
	
	public SubscriptionItem(long nID, String sTitle, String sLayerName, String sWKT, boolean bSMSEnable) {
		this.nID = nID;
		this.sTitle = sTitle;
		this.sLayerName = sLayerName;
		this.sWKT = sWKT;
		this.bSMSEnable = bSMSEnable;
	}		

	public String GetTitle(){
		return sTitle;
	}
	
	public long GetId(){
		return nID;
	}
	
	public String GetLayerName(){
		return sLayerName;
	}
	
	public boolean GetIsSMSEnabled(){
		return bSMSEnable;
	}

	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	public void writeToParcel(Parcel out, int flags) {
		out.writeLong(nID);
		out.writeString(sTitle);
		out.writeString(sLayerName);
		out.writeString(sWKT);
		out.writeInt(bSMSEnable == true ? 1 : 0);
		//
		/*out.writeInt(maoPortals.size());
		for(PortalItem it : maoPortals.values()){
			out.writeValue(it);
		}*/
	}	
	
	public static final Parcelable.Creator<SubscriptionItem> CREATOR
    = new Parcelable.Creator<SubscriptionItem>() {
	    public SubscriptionItem createFromParcel(Parcel in) {
	        return new SubscriptionItem(in);
	    }
	
	    public SubscriptionItem[] newArray(int size) {
	        return new SubscriptionItem[size];
	    }
	};
	
	private SubscriptionItem(Parcel in) {
		nID = in.readLong();
		sTitle = in.readString();
		sLayerName = in.readString();
		sWKT = in.readString();
		bSMSEnable = in.readInt() == 1 ? true : false;
		
		/*maoPortals = new HashMap<Integer, PortalItem>();
		int nSize = in.readInt();
		for(int i = 0; i < nSize; i++){
			PortalItem it = (PortalItem) in.readValue(PortalItem.class.getClassLoader());
			maoPortals.put(it.GetId(), it);
		}*/
	}

	@Override
	public String toString() {
		return "" + nID;
	}
}

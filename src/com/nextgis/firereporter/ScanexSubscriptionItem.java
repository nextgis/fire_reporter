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

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.Log;

public class ScanexSubscriptionItem implements Parcelable {
	protected long nID;
	protected String sTitle;
	protected String sLayerName;
	protected String sWKT;
	protected boolean bSMSEnable;
	protected boolean bHasNews;
	protected Handler mFillDataHandler; 
	protected Context c;
	
	protected final static int PREV_DAYS = 3;
	protected int mPrevDays;
	
	protected Map<Long, ScanexNotificationItem> mmoItems;

	
	public ScanexSubscriptionItem(Context c, long nID, String sTitle, String sLayerName, String sWKT, boolean bSMSEnable) {
		Prepare(c);
		
		this.nID = nID;
		this.sTitle = sTitle;
		this.sLayerName = sLayerName;
		this.sWKT = sWKT;
		this.bSMSEnable = bSMSEnable;
	}		
	
	public ScanexSubscriptionItem(Context c, JSONObject object){
		Prepare(c);
		try {
			this.nID = object.getLong("id");
			this.sTitle = object.getString("title");
			this.sLayerName = object.getString("layer_name");
			this.sWKT = object.getString("wkt");
			this.bSMSEnable = object.getBoolean("sms_enabled");
			
			JSONArray jsonArray = object.getJSONArray("items");
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject jsonObject = jsonArray.getJSONObject(i);
				
				ScanexNotificationItem Item = new ScanexNotificationItem(c, jsonObject);
				if(Item.GetId() != -1){
					mmoItems.put(Item.GetId(), Item);
				}
			}
			
		} catch (JSONException e) {
			SendError(e.getLocalizedMessage());
		}
	}
	
	protected void Prepare(Context c){
		this.c = c;
		nID = -1;
		bHasNews = false;
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
		mPrevDays = prefs.getInt(SettingsActivity.KEY_PREF_SEARCH_DAY_INTERVAL + "_int", PREV_DAYS);			
		
    	mFillDataHandler = new Handler() {
    	    public void handleMessage(Message msg) {
    	    	
    	    	Bundle resultData = msg.getData();
    	    	boolean bHaveErr = resultData.getBoolean(GetFiresService.ERROR);
    	    	if(bHaveErr){
    	    		SendError(resultData.getString(GetFiresService.ERR_MSG));
    	    	}
    	    	else{
    	    		int nType = resultData.getInt(GetFiresService.SOURCE);
	    			String sData = resultData.getString(GetFiresService.JSON);
    	    		switch(nType){
    	    		case 5:
    	    			FillData(nType, sData);
    	    			break;
    	    		default:
    	    			break;
    	    		}
    	    	}
    	    };
    	};	
    	
    	mmoItems = new HashMap<Long, ScanexNotificationItem>();
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
		out.writeInt(bHasNews == true ? 1 : 0);
		out.writeInt(mPrevDays);
		//
		out.writeInt(mmoItems.size());
		for(ScanexNotificationItem it : mmoItems.values()){
			out.writeValue(it);
		}
	}	
	
	public static final Parcelable.Creator<ScanexSubscriptionItem> CREATOR
    = new Parcelable.Creator<ScanexSubscriptionItem>() {
	    public ScanexSubscriptionItem createFromParcel(Parcel in) {
	        return new ScanexSubscriptionItem(in);
	    }
	
	    public ScanexSubscriptionItem[] newArray(int size) {
	        return new ScanexSubscriptionItem[size];
	    }
	};
	
	private ScanexSubscriptionItem(Parcel in) {
		nID = in.readLong();
		sTitle = in.readString();
		sLayerName = in.readString();
		sWKT = in.readString();
		bSMSEnable = in.readInt() == 1 ? true : false;
		bHasNews = in.readInt() == 1 ? true : false;
		mPrevDays = in.readInt();
		
		mmoItems = new HashMap<Long, ScanexNotificationItem>();
		int nSize = in.readInt();
		for(int i = 0; i < nSize; i++){
			ScanexNotificationItem it = (ScanexNotificationItem) in.readValue(ScanexNotificationItem.class.getClassLoader());
			mmoItems.put(it.GetId(), it);
		}
	}

	@Override
	public String toString() {
		return "" + nID;
	}
	
	public boolean HasNews(){
		return bHasNews;
	}
	
	public void setHasNews(boolean bHasNews){
		this.bHasNews = bHasNews;
	}
	
	public void UpdateFromRemote(Context c, String sCookie){
		//get notifications http://fires.kosmosnimki.ru/SAPI/Subscribe/GetData/55?dt=2013-08-06&CallBackName=44
    	Calendar calendar = Calendar.getInstance();
        
        for(int i = 0; i < PREV_DAYS; i++){
        	String sDate = String.format("%04d-%02d-%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH));
        	Log.d(MainActivity.TAG, sDate);
        	String sURL = GetFiresService.SCANEX_API + "/Subscribe/GetData/" + nID + "?dt=" + sDate + "&CallBackName=" + GetFiresService.USER_ID;
        	Log.d(MainActivity.TAG, sURL);
            new HttpGetter(c, 5, "", mFillDataHandler, false).execute(sURL, sCookie);

        	calendar.roll(Calendar.DAY_OF_MONTH, false);
        }
	}
	
	protected void FillData(int nType, String sData){
		String sCleanData = GetFiresService.removeJsonT(sData);
		try {
			JSONObject object = new JSONObject(sCleanData);
			String sStatus = object.getString("Status");
			if(sStatus.equals("OK")){
				JSONArray jsonArray = object.getJSONArray("Result");
				for(int i = 0; i < jsonArray.length(); i++) {
					JSONArray jsonSubArray = jsonArray.getJSONArray(i);
					long nID = jsonSubArray.getLong(0);
					String sPtCoord = jsonSubArray.getString(1);
					int nConfidence = jsonSubArray.getInt(2);
					int nPower = jsonSubArray.getInt(3);					
					String sURL1 = jsonSubArray.getString(4);
					String sURL2 = jsonSubArray.getString(5);
					String sType = jsonSubArray.getString(6);
					String sPlace = jsonSubArray.getString(7);
					String sDate = jsonSubArray.getString(8);
					String sMap = jsonSubArray.getString(9);
				
					ScanexNotificationItem Item = new ScanexNotificationItem(c, nID, sPtCoord, nConfidence, nPower, sURL1, sURL2, sType, sPlace, sDate, sMap, R.drawable.ic_scan);
					if(!mmoItems.containsKey(Item.GetId())){
						bHasNews = true;
						mmoItems.put(Item.GetId(), Item);
					}					
				}
			}
			else{
				SendError( object.getString("ErrorInfo") );			
			}
		} catch (JSONException e) {
			SendError(e.getLocalizedMessage());
		}
		
		/*44({
			"Status": "OK",
			"ErrorInfo": "",
			"Result": [[1306601,
			"61.917, 63.090",
			68,
			27.4,
			"\u003ca href=\u0027http://maps.kosmosnimki.ru/TileService.ashx/apikeyV6IAK16QRG/mapT42E9?SERVICE=WMS&request=GetMap&version=1.3&layers=C7B2E6510209444E80673F3C37519F7E,FFE60CFA7DAF498381F811C08A5E8CF5,T42E9.A78AC25E0D924258B5AF40048C21F7E7_dt04102013&styles=&crs=EPSG:3395&transparent=FALSE&format=image/jpeg&width=460&height=460&bbox=6987987,8766592,7058307,8836912\u0027\u003e \u003cimg src=\u0027http://maps.kosmosnimki.ru/TileService.ashx/apikeyV6IAK16QRG/mapT42E9?SERVICE=WMS&request=GetMap&version=1.3&layers=C7B2E6510209444E80673F3C37519F7E,FFE60CFA7DAF498381F811C08A5E8CF5,T42E9.A78AC25E0D924258B5AF40048C21F7E7_dt04102013&styles=&crs=EPSG:3395&transparent=FALSE&format=image/jpeg&width=100&height=100&bbox=6987987,8766592,7058307,8836912\u0027 width=\u0027{4}\u0027 height=\u0027{5}\u0027 /\u003e\u003c/a\u003e",
			"\u003ca href=\u0027http://fires.kosmosnimki.ru/?x=63.09&y=61.917&z=11&dt=04.10.2013\u0027 target=\"_blank\"\u003eView on the map\u003c/a\u003e",
			"Fire",
			"Агириш",
			"\/Date(1380859500000)\/",
			"http://maps.kosmosnimki.ru/TileService.ashx/apikeyV6IAK16QRG/mapT42E9?SERVICE=WMS&request=GetMap&version=1.3&layers=C7B2E6510209444E80673F3C37519F7E,FFE60CFA7DAF498381F811C08A5E8CF5,T42E9.A78AC25E0D924258B5AF40048C21F7E7_dt04102013&styles=&crs=EPSG:3395&transparent=FALSE&format=image/jpeg&width=460&height=460&bbox=6987987,8766592,7058307,8836912",
			null]]
		})*/
	}
	
	public JSONObject getAsJSON(){
		JSONObject object = new JSONObject();
		try {
			object.put("id", nID);
			object.put("title", sTitle);
			object.put("layer_name", sLayerName);
			object.put("wkt", sWKT);
			object.put("sms_enabled", bSMSEnable);
			
			JSONArray oJSONItems = new JSONArray();
			object.put("items", oJSONItems);
			for(ScanexNotificationItem Item : mmoItems.values()){
				oJSONItems.put(Item.getAsJSON());
			}
			
		} catch (JSONException e) {
			SendError(e.getLocalizedMessage());
		}
		return object;
	}
	
	protected void SendError(String sErr){
		Log.d(MainActivity.TAG, sErr);
	}
	
	public Map<Long, ScanexNotificationItem> GetItems(){
		return mmoItems;
	}

	public void add(ScanexNotificationItem parcelable) {
		if(mmoItems != null && !mmoItems.containsKey(parcelable.GetId())){
			mmoItems.put(parcelable.GetId(), parcelable);
			setHasNews(true);
		}		
	}
}

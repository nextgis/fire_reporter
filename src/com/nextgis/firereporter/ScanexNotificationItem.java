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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Date;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.Log;

public class ScanexNotificationItem implements Parcelable {
	public static final char DEGREE_CHAR = (char) 0x00B0;
	private long nID;
	private Date dt;
	private int nIconId;
	private double X,Y;
	private int nConfidence;
	private int nPower;					
	private String sURL1;
	private String sURL2;
	private String sType;
	private String sPlace;
	private String sMap;
	private int nFormat;
	private String sCoordLat;
	private String sCoordLon;
	private String sN, sS, sW, sE;
	boolean mbWatched;
	
	public ScanexNotificationItem(Context c, long nID, String sPtCoord, int nConfidence, int nPower, String sURL1, String sURL2, String sType, String sPlace, String sDate, String sMap, int nIconId) {
		Prepare(c);		
		
		this.nID = nID;
		
		//transform coords
		String[] sCoords = sPtCoord.split(",");
		if(sCoords.length == 2){
			X = Double.parseDouble(sCoords[0]);
			Y = Double.parseDouble(sCoords[1]);
		}
		else
		{
			X = 0;
			Y = 0;
		}

		this.nConfidence = nConfidence;
		this.nPower = nPower;
		this.sURL1 = clearURL(sURL1);
		this.sURL2 = clearURL(sURL2);
		this.sType = sType;
		this.sPlace = sPlace;
		
		//transform coords
		String sSubDate = sDate.substring(6, sDate.length() - 2);
		this.dt = new Date(Long.parseLong(sSubDate));
		this.sMap = sMap;
		this.nIconId = nIconId;

    	mbWatched = false;
	}
	
	private String clearURL(String sURL) {
		int nStart = sURL.indexOf("'");
		sURL = sURL.substring(nStart + 1);
		int nEnd = sURL.indexOf("'");
		sURL = sURL.substring(0,nEnd - 1);
		return sURL;
	}

	public ScanexNotificationItem(Context c, JSONObject object) {
		Prepare(c);
		try {
			this.nID = object.getLong("id");
			this.dt = new Date(object.getLong("date"));
			this.X = object.getDouble("X");
			this.Y = object.getDouble("Y");
			this.nConfidence = object.getInt("confidence");
			this.nPower = object.getInt("power");
			this.sURL1 = object.getString("URL1");
			this.sURL2 = object.getString("URL2");
			this.sType = object.getString("type");
			this.sPlace = object.getString("place");
			this.sMap = object.getString("map");
			this.nIconId = object.getInt("iconId");
			this.mbWatched = object.getBoolean("watched");

		} catch (JSONException e) {
			SendError(e.getLocalizedMessage());
		}
		
	}
	
	protected void Prepare(Context c){
		this.nID = -1;
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
    	nFormat = prefs.getInt(SettingsActivity.KEY_PREF_COORD_FORMAT + "_int", Location.FORMAT_SECONDS);
    	sCoordLat = (String) c.getResources().getText(R.string.coord_lat);
    	sCoordLon = (String) c.getResources().getText(R.string.coord_lon);
    	
    	sN = (String) c.getResources().getText(R.string.compas_N);
    	sS = (String) c.getResources().getText(R.string.compas_S);
    	sW = (String) c.getResources().getText(R.string.compas_W);
    	sE = (String) c.getResources().getText(R.string.compas_E);		
	}
	
	public String GetCoordinates(){
    	String sOut;
    	sOut = formatLat(Y, nFormat) + sCoordLat;
    	sOut += " ";
    	sOut += formatLng(X, nFormat) + sCoordLon;			

		return sOut;
	}
	
	public long GetId(){
		return nID;
	}
	public String GetDateAsString(){
		return java.text.DateFormat.getDateTimeInstance().format(dt);
	}
	
	public Date GetDate(){
		return dt;
	}		
	
	public String formatLat(double lat, int outputType) {

		String direction = sN;
		if (lat < 0) {
			direction = sS;
			lat = -lat;
		}

		return formatCoord(lat, outputType) + direction;

	}

	public String formatLat(double lat) {
		return formatLat(lat, Location.FORMAT_DEGREES);
	}

	public String formatLng(double lng, int outputType) {

		String direction = sE;
		if (lng < 0) {
			direction = sW;
			lng = -lng;
		}

		return formatCoord(lng, outputType) + direction;

	}

	public String formatLng(double lng) {
		return formatLng(lng, Location.FORMAT_DEGREES);
	}
	
	/**
	 * Formats coordinate value to string based on output type (modified version
	 * from Android API)
	 */
	public static String formatCoord(double coordinate, int outputType) {

		StringBuilder sb = new StringBuilder();
		char endChar = DEGREE_CHAR;

		DecimalFormat df = new DecimalFormat("###.######");
		if (outputType == Location.FORMAT_MINUTES || outputType == Location.FORMAT_SECONDS) {

			df = new DecimalFormat("##.###");

			int degrees = (int) Math.floor(coordinate);
			sb.append(degrees);
			sb.append(DEGREE_CHAR); // degrees sign
			endChar = '\''; // minutes sign
			coordinate -= degrees;
			coordinate *= 60.0;

			if (outputType == Location.FORMAT_SECONDS) {

				df = new DecimalFormat("##.##");

				int minutes = (int) Math.floor(coordinate);
				sb.append(minutes);
				sb.append('\''); // minutes sign
				endChar = '\"'; // seconds sign
				coordinate -= minutes;
				coordinate *= 60.0;
			}
		}

		sb.append(df.format(coordinate));
		sb.append(endChar);

		return sb.toString();
	}

	/**
	 * Simple coordinate decimal formatter
	 * 
	 * @param coord
	 * @return
	 */
	public static String formatCoord(double coord) {
		DecimalFormat df = new DecimalFormat("###.######");
		df.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
		return df.format(coord);
	}

	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	public void writeToParcel(Parcel out, int flags) {
		out.writeSerializable(dt);		//	private Date dt;
		out.writeDouble(X);
		out.writeDouble(Y);				//  private double X,Y;
		out.writeInt(nIconId);      	//	private nIconId;
		out.writeLong(nID);				//  private long nFid;
		out.writeInt(nConfidence);		//	private int nConfidence;
		out.writeInt(nPower);			//	private int nPower;
		out.writeString(sURL1);			//	private String sURL1
		out.writeString(sURL2);			//	private String sURL2
		out.writeString(sType);			//	private String sType
		out.writeString(sPlace);		//	private String sPlace
		out.writeString(sMap);			//	private String sMap
		out.writeInt(mbWatched == true ? 1 : 0);			//	private boolean mbWatched
		
	//
		out.writeInt(nFormat);
		out.writeString(sCoordLat);
		out.writeString(sCoordLon);
		out.writeString(sN);
		out.writeString(sS);
		out.writeString(sW);
		out.writeString(sE);
	}	
	
	public static final Parcelable.Creator<ScanexNotificationItem> CREATOR
    = new Parcelable.Creator<ScanexNotificationItem>() {
	    public ScanexNotificationItem createFromParcel(Parcel in) {
	        return new ScanexNotificationItem(in);
	    }
	
	    public ScanexNotificationItem[] newArray(int size) {
	        return new ScanexNotificationItem[size];
	    }
	};
	
	private ScanexNotificationItem(Parcel in) {
		dt = (Date) in.readSerializable();
		X = in.readDouble();
		Y = in.readDouble();
		nIconId = in.readInt();
		nID = in.readLong();
		nConfidence = in.readInt();
		nPower = in.readInt();
		sURL1 = in.readString();
		sURL2 = in.readString();
		sType = in.readString();
		sPlace = in.readString();
		sMap = in.readString();
		mbWatched = in.readInt() == 1 ? true : false;

		//
		nFormat = in.readInt();
		sCoordLat = in.readString();
		sCoordLon = in.readString();
		
		sN = in.readString();
		sS = in.readString();
		sW = in.readString();
		sE = in.readString();
	}

	public boolean isWatched() {
		return mbWatched;
	}

	public void setWatched(boolean mbWatched) {
		this.mbWatched = mbWatched;
	}

	public JSONObject getAsJSON() {
		JSONObject object = new JSONObject();
		try {
			object.put("id", nID);
			object.put("date", dt.getTime());
			object.put("X", X);
			object.put("Y", Y);
			object.put("iconId", nIconId);
			object.put("confidence", nConfidence);
			object.put("power", nPower);
			object.put("URL1", sURL1);
			object.put("URL2", sURL2);
			object.put("type", sType);
			object.put("place", sPlace);
			object.put("map", sMap);
			object.put("watched", mbWatched);
			
			
		} catch (JSONException e) {
			SendError(e.getLocalizedMessage());
		}
		return object;
	}	
	
	protected void SendError(String sErr){
		Log.d(MainActivity.TAG, sErr);
	}

	public int GetIconId() {
		return nIconId;
	}

	public int GetConfidence() {
		return nConfidence;
	}

	public String GetMapURL() {		
		return sMap;
	}

	public int GetPower() {
		return nPower;
	}

	public String GetType() {
		return sType;
	}

	public String GetPlace() {
		return sPlace;
	}

	public String GetSiteURL() {
		return sURL2;
	}

	public String GetShortCoordinates() {
		return String.format("%.2f,  $.2f", Y, X);
	}

}

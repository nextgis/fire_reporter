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

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;

public class FireItem implements Parcelable{
	public static final char DEGREE_CHAR = (char) 0x00B0;
	private Date dt;
	private double X,Y;
	private int nIconId;
	private long nFid;
	private int nType;
	private double dfDist;
	private int nFormat;
	private String sCoordLat;
	private String sCoordLon;
	private String sN, sS, sW, sE;
	public FireItem(Context c, int nType, long nFid, Date dt, double X, double Y, double dfDist, int nIconId) {
		this.dt = dt;
		this.X = X;
		this.Y = Y;
		this.nIconId = nIconId;
		this.nFid = nFid;
		this.nType = nType;
		this.dfDist = dfDist;
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
    	nFormat = prefs.getInt(SettingsActivity.KEY_PREF_COORD_FORMAT + "_int", Location.FORMAT_SECONDS);
    	sCoordLat = (String) c.getResources().getText(R.string.coord_lat);
    	sCoordLon = (String) c.getResources().getText(R.string.coord_lon);
    	
    	sN = (String) c.getResources().getText(R.string.compas_N);
    	sS = (String) c.getResources().getText(R.string.compas_S);
    	sW = (String) c.getResources().getText(R.string.compas_W);
    	sE = (String) c.getResources().getText(R.string.compas_E);
	}		
	
	public int GetIconId(){
		return nIconId;
	}
	
	public String GetCoordinates(){
    	String sOut;
    	sOut = formatLat(Y, nFormat) + sCoordLat;
    	sOut += " ";
    	sOut += formatLng(X, nFormat) + sCoordLon;			

		return sOut;
	}

	public String GetShortCoordinates(){
    	return String.format("%.2f,  %.2f", Y, X);
	}
	
	public int GetType(){
		return nType;
	}
	
	public long GetId(){
		return nFid;
	}
	
	public double GetDistance(){
		return dfDist;
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
		out.writeSerializable(dt);			//	private Date dt;
		out.writeDouble(X);
		out.writeDouble(Y);			//  private double X,Y;
		out.writeInt(nIconId);      //	private nIconId;
		out.writeLong(nFid);		//  private long nFid;
		out.writeInt(nType);		//	private int nType;
		out.writeDouble(dfDist);	//	private double dfDist;
		
		//
		out.writeInt(nFormat);
		out.writeString(sCoordLat);
		out.writeString(sCoordLon);
		out.writeString(sN);
		out.writeString(sS);
		out.writeString(sW);
		out.writeString(sE);
	}	
	
	public static final Parcelable.Creator<FireItem> CREATOR
    = new Parcelable.Creator<FireItem>() {
	    public FireItem createFromParcel(Parcel in) {
	        return new FireItem(in);
	    }
	
	    public FireItem[] newArray(int size) {
	        return new FireItem[size];
	    }
	};
	
	private FireItem(Parcel in) {
		dt = (Date) in.readSerializable();
		X = in.readDouble();
		Y = in.readDouble();
		nIconId = in.readInt();
		nFid = in.readLong();
		nType = in.readInt();
		dfDist = in.readDouble();
		//
		nFormat = in.readInt();
		sCoordLat = in.readString();
		sCoordLon = in.readString();
		
		sN = in.readString();
		sS = in.readString();
		sW = in.readString();
		sE = in.readString();
	}
}


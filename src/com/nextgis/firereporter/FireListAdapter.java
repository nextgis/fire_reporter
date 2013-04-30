/*******************************************************************************
*
* FireReporter
* ---------------------------------------------------------
* Report and view fires
*
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
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import com.nextgis.firereporter.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class FireListAdapter extends BaseAdapter {
	private Context mContext;
	private ArrayList <FireItem> mListFireInfo;
	
	public FireListAdapter(Context c, ArrayList <FireItem> list) {
		mContext = c;
		mListFireInfo = list;
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
	
	public static class FireItem implements Parcelable{
		public static final char DEGREE_CHAR = (char) 0x00B0;
		private Date dt;
		private double X,Y;
		private int nIconId;
		private String sURL;
		private long nFid;
		private int nType;
		private double dfDist;
		private int nFormat;
		private String sCoordLat;
		private String sCoordLon;
		private String sN, sS, sW, sE;
		public FireItem(Context c, int nType, long nFid, Date dt, double X, double Y, double dfDist, int nIconId, String sURL) {
			this.dt = dt;
			this.X = X;
			this.Y = Y;
			this.nIconId = nIconId;
			this.sURL = sURL;
			this.nFid = nFid;
			this.nType = nType;
			this.dfDist = dfDist;
			
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        	nFormat = prefs.getInt(SettingsFragment.KEY_PREF_COORD_FORMAT + "_int", Location.FORMAT_SECONDS);
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
		
		public String GetUrl(){
			return sURL;
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
			out.writeString(sURL);		//  private String sURL;
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
			sURL = in.readString();
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
}

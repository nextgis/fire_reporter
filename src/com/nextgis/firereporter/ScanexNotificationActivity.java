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

import java.io.InputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;

public class ScanexNotificationActivity extends SherlockActivity {

	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
	    setContentView(R.layout.scanex_notification_activity);
	    
	    Bundle extras = getIntent().getExtras(); 
	    if(extras != null) {
	    	ScanexNotificationItem item = extras.getParcelable(GetFiresService.ITEM);
	    	TextView vID = (TextView) findViewById(R.id.textViewIdVal);
	    	if(vID != null){
	    		vID.setText(""+ item.GetId());
	    	}
	    	TextView vCoordinate = (TextView) findViewById(R.id.textViewCoordinateVal);
	    	if(vCoordinate != null){
	    		vCoordinate.setText(item.GetCoordinates());
	    	}
	    	TextView vConfidence = (TextView) findViewById(R.id.textViewConfidenceVal);
	    	if(vConfidence != null){
	    		vConfidence.setText("" + item.GetConfidence());
	    	}
	    	TextView vPower = (TextView) findViewById(R.id.textViewPowerVal);
	    	if(vPower != null){
	    		vPower.setText("" + item.GetPower());
	    	}
	    	TextView vMap = (TextView) findViewById(R.id.textViewMapVal);
	    	if(vMap != null){
	    		vMap.setText(item.GetMapURL());
	    	}
	    	TextView vType = (TextView) findViewById(R.id.textViewTypeVal);
	    	if(vType != null){
	    		vType.setText(item.GetType());
	    	}
	    	TextView vPlace = (TextView) findViewById(R.id.textViewPlaceVal);
	    	if(vPlace != null){
	    		vPlace.setText(item.GetPlace());
	    	}
	    	TextView vDate = (TextView) findViewById(R.id.textViewDateVal);
	    	if(vDate != null){
	    		vDate.setText(item.GetDateAsString());
	    	}	
	    	TextView vShownOnSite = (TextView) findViewById(R.id.textViewShownOnSiteVal);
	    	if(vShownOnSite != null){
	    		vShownOnSite.setText(item.GetSiteURL());
	    	}	    	
	    	// show The Image
	        new DownloadImageTask((ImageView) findViewById(R.id.imageView)).execute(item.GetMapURL());
	    }
        
       	getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}
	
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e(MainActivity.TAG, e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
        	if(bmImage != null)
        		bmImage.setImageBitmap(result);
        }
    }
}

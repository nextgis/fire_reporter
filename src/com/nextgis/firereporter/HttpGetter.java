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

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.util.Log;


public class HttpGetter extends AsyncTask<String, Void, Void> {
    private String mContent;
    private Context mContext;
    private String mError = null;
    private ProgressDialog mDownloadDialog;
    private String mDownloadDialogMsg;
    private int mnType;
    private Handler mEventReceiver;
    private boolean mbShowProgress;
    private HttpGet httpget;
	
    public HttpGetter(Context c, int nType, String sMsg, Handler eventReceiver, boolean bShowProgress) {        
        super();
        mbShowProgress = bShowProgress;
        mContext = c;
       	mDownloadDialog = null;
        mnType = nType;  
        mEventReceiver = eventReceiver;
        mDownloadDialogMsg = sMsg;  
        httpget = null;
    }
    
    @Override
    protected void onPreExecute() {
    	super.onPreExecute();
    	if(mbShowProgress){
    		mDownloadDialog = new ProgressDialog(mContext);
    		mDownloadDialog.setMessage(mDownloadDialogMsg);
    		mDownloadDialog.show();
    	}
    } 

    @Override
    protected Void doInBackground(String... urls) {
        if(IsNetworkAvailible(mContext))
        {    	
	        try {
	        	String sURL = urls[0];
	        	
	        	httpget = new HttpGet(sURL);
	            
	            Log.d("MainActivity", "HTTPGet URL " + sURL);
	        	
	        	if(urls.length > 1){
	        		httpget.setHeader("Cookie", urls[1]);
	        	}
	        	
	            HttpClient Client = new DefaultHttpClient();
	            HttpResponse response = Client.execute(httpget);	            
	            //ResponseHandler<String> responseHandler = new BasicResponseHandler();
	            //mContent = Client.execute(httpget, responseHandler);
	            HttpEntity entity = response.getEntity();
	                        
	            Bundle bundle = new Bundle();
				if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
					bundle.putBoolean("error", false);
					mContent = EntityUtils.toString(entity);
		            bundle.putString("json", mContent);
				}
				else{
					bundle.putBoolean("error", true);
				}				
				
	            bundle.putInt("src", mnType);
	            
	            Message msg = new Message();
	            msg.setData(bundle);
	            if(mEventReceiver != null){
	            	mEventReceiver.sendMessage(msg);
	            }
	            
	        } catch (ClientProtocolException e) {
	        	mError = e.getMessage();
	            cancel(true);
	        } catch (IOException e) {
	        	mError = e.getMessage();
	            cancel(true);
	        }
        }
        else {
            Bundle bundle = new Bundle();
            bundle.putBoolean("error", true);
            bundle.putString("err_msq", mContext.getString(R.string.stNetworkUnreach));
            bundle.putInt("src", mnType);
            
            Message msg = new Message();
            msg.setData(bundle);
            if(mEventReceiver != null){
            	mEventReceiver.sendMessage(msg);
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void unused) {
    	super.onPostExecute(unused);
    	DismissDowloadDialog();
        if (mError != null) {
        	Bundle bundle = new Bundle();
            bundle.putBoolean("error", true);
            bundle.putString("err_msq", mError);
            bundle.putInt("src", mnType);
            
            Message msg = new Message();
            msg.setData(bundle);
            
            if(mEventReceiver != null){
            	mEventReceiver.sendMessage(msg);
            }
        } else {
            //Toast.makeText(MainActivity.this, "Source: " + Content, Toast.LENGTH_LONG).show();
        }
    }
/*
	@Override
	protected void onCancelled() {		
		super.onCancelled();
		if(httpget != null)
			httpget.abort();
		
		mEventReceiver = null;
		DismissDowloadDialog();
	}

	@Override
	protected void onCancelled(Void result) {
		super.onCancelled(result);
		if(httpget != null)
			httpget.abort();
		
		mEventReceiver = null;
		DismissDowloadDialog();
	}
*/	
	public void DismissDowloadDialog(){
		if(mDownloadDialog != null){
			mDownloadDialog.dismiss();
			//mDownloadDialog = null;
		}	
	}
	
	public void Abort(){
		if(httpget != null)
			httpget.abort();		
		DismissDowloadDialog();
		this.cancel(true);
	}
	
	static boolean IsNetworkAvailible(Context c)
	{
		ConnectivityManager cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
		TelephonyManager tm = (TelephonyManager) c.getSystemService(Context.TELEPHONY_SERVICE);  
        
        NetworkInfo info = cm.getActiveNetworkInfo();
		if (info == null /*|| !cm.getBackgroundDataSetting()*/) 
			return false;
		
		int netType = info.getType();
		//int netSubtype = info.getSubtype();
		if (netType == ConnectivityManager.TYPE_WIFI) {
			return info.isConnected();
		} 
		else if (netType == ConnectivityManager.TYPE_MOBILE
		&& /*netSubtype == TelephonyManager.NETWORK_TYPE_UMTS
		&&*/ !tm.isNetworkRoaming()) {
			return info.isConnected();
		} 
		else {
			return false;
		}	
	}	
}

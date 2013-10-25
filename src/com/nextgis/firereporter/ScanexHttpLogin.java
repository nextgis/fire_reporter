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
import java.util.ArrayList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpParams;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class ScanexHttpLogin extends AsyncTask<String, Void, Void> {
    private Context mContext;
    private String mError = null;
    private ProgressDialog mDownloadDialog = null;
    private String mDownloadDialogMsg;
    private int mnType;
    private Handler mEventReceiver;
    private boolean mbShowProgress;
	
    public ScanexHttpLogin(Context c, int nType, String sMsg, Handler eventReceiver, boolean bShowProgress) {        
        super();
        mbShowProgress = bShowProgress;
        mContext = c;
        if(mbShowProgress){
        	mDownloadDialog = new ProgressDialog(mContext);
        }
        mnType = nType;  
        mEventReceiver = eventReceiver;
        mDownloadDialogMsg = sMsg;      
    }
    
    @Override
    protected void onPreExecute() {
    	super.onPreExecute();
    	if(mbShowProgress){
    		mDownloadDialog.setMessage(mDownloadDialogMsg);
    		mDownloadDialog.show();
    	}
    } 

    @Override
    protected Void doInBackground(String... urls) {
    	if(HttpGetter.IsNetworkAvailible(mContext))
    	{    	
    		String sUser = urls[0];
    		String sPass = urls[1];

    		try {

    			// Create a new HttpClient
    			HttpClient httpclient = new DefaultHttpClient();

    			// step 1. open login dialog
    			String sRedirect = "http://fires.kosmosnimki.ru/SAPI/oAuthCallback.html&authServer=MyKosmosnimki";
    			String sURL = "http://fires.kosmosnimki.ru/SAPI/LoginDialog.ashx?redirect_uri=" + Uri.encode(sRedirect);
    			HttpGet httpget = new HttpGet(sURL);
    			HttpParams params = httpget.getParams();
    			params.setParameter(ClientPNames.HANDLE_REDIRECTS, Boolean.FALSE);
    			httpget.setParams(params);
    			HttpResponse response = httpclient.execute(httpget);

    			//2 get cookie and params            
    			Header head = response.getFirstHeader("Set-Cookie");
    			String sCookie = head.getValue();

    			head = response.getFirstHeader("Location");
    			String sLoc = head.getValue();

    			Uri uri = Uri.parse(sLoc);
    			String sClientId = uri.getQueryParameter("client_id");
    			String sScope = uri.getQueryParameter("scope");
    			String sState = uri.getQueryParameter("state");

    			String sPostUri = "http://my.kosmosnimki.ru/Account/LoginDialog?redirect_uri=" + Uri.encode(sRedirect) + "&client_id=" + sClientId + "&scope=" + sScope + "&state=" + sState;

    			HttpPost httppost = new HttpPost(sPostUri);     

    			params = httppost.getParams();
    			params.setParameter(ClientPNames.HANDLE_REDIRECTS, Boolean.FALSE);
    			httppost.setHeader("Cookie", sCookie);

    			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
    			nameValuePairs.add(new BasicNameValuePair("email", sUser));
    			nameValuePairs.add(new BasicNameValuePair("password", sPass));
    			nameValuePairs.add(new BasicNameValuePair("IsApproved", "true"));
    			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));                

    			response = httpclient.execute(httppost);
    			head = response.getFirstHeader("Set-Cookie");
    			
    			if(head == null){
    				mError = mContext.getString(R.string.noNetwork);
    				return null;
    			}
    			
    			sCookie += "; " + head.getValue();
    			head = response.getFirstHeader("Location");
    			sLoc = head.getValue();

    			uri = Uri.parse(sLoc);
    			String sCode = uri.getQueryParameter("code");
    			sState = uri.getQueryParameter("state");

    			//3 get 
    			String sGetUri = "http://fires.kosmosnimki.ru/SAPI/Account/logon/?authServer=MyKosmosnimki&code=" + sCode + "&state=" + sState;
    			httpget = new HttpGet(sGetUri);
    			httpget.setHeader("Cookie", sCookie);
    			params = httpget.getParams();
    			params.setParameter(ClientPNames.HANDLE_REDIRECTS, Boolean.FALSE);
    			httpget.setParams(params);
    			response = httpclient.execute(httpget);

    			head = response.getFirstHeader("Set-Cookie");
    			if(head == null){
    				mError = mContext.getString(R.string.noNetwork);
    				return null;
    			}
    			sCookie += "; " + head.getValue();


    			Bundle bundle = new Bundle();
    			if(sCookie.length() > 0){
    				//if(bGetCookie){
    				bundle.putBoolean(GetFiresService.ERROR, false);
    				bundle.putString(GetFiresService.JSON, sCookie);
    				//bundle.putString("json", mContent);
    			}
    			else{
    				bundle.putBoolean(GetFiresService.ERROR, true);
    			}

    			bundle.putInt(GetFiresService.SOURCE, mnType);
    			Message msg = new Message();
    			msg.setData(bundle);
    			if(mEventReceiver != null){
    				mEventReceiver.sendMessage(msg);
    			}

    		} catch (ClientProtocolException e) {
    			mError = e.getMessage();
    			return null;
    		} catch (IOException e) {
    			mError = e.getMessage();
    			return null;
    		} catch (Exception e){
    			mError = e.getMessage();
    			return null;
    		}
    	}
    	else {
    		Bundle bundle = new Bundle();
    		bundle.putBoolean(GetFiresService.ERROR, true);
    		bundle.putString(GetFiresService.ERR_MSG, mContext.getString(R.string.noNetwork));
    		bundle.putInt(GetFiresService.SOURCE, mnType);

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
    	if(mbShowProgress){
    		mDownloadDialog.dismiss();
    	}
        if (mError != null) {
        	Bundle bundle = new Bundle();
            bundle.putBoolean(GetFiresService.ERROR, true);
            bundle.putString(GetFiresService.ERR_MSG, mError);
            bundle.putInt(GetFiresService.SOURCE, mnType);
            
            Message msg = new Message();
            msg.setData(bundle);
            if(mEventReceiver != null){
            	mEventReceiver.sendMessage(msg);
            }
        }
    }

	@Override
	protected void onCancelled() {		
		super.onCancelled();
		if(mbShowProgress){
			mDownloadDialog.dismiss();
		}
	}
}

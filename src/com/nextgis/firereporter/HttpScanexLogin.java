/*******************************************************************************
*
* MainActivity
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class HttpScanexLogin extends AsyncTask<String, Void, Void> {
    private String mContent;
    private Context mContext;
    private String mError = null;
    private ProgressDialog mDownloadDialog = null;
    private String mDownloadDialogMsg;
    private int mnType;
    private Handler mEventReceiver;
    private boolean mbShowProgress;
	
    public HttpScanexLogin(Context c, int nType, String sMsg, Handler eventReceiver, boolean bShowProgress) {        
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
        	
        	//http://my.kosmosnimki.ru/Account/Login
        	//email=new@kosmosnimki.ru&password=test123
        	
        	// Create a new HttpClient and Post Header
            HttpClient httpclient = new DefaultHttpClient();
            
            HttpPost httppost = new HttpPost("http://my.kosmosnimki.ru/Account/Login");
            
            HttpParams params = httppost.getParams();
            params.setParameter(ClientPNames.HANDLE_REDIRECTS, Boolean.FALSE);
            
            HttpContext localContext = new BasicHttpContext();
            // Create a local instance of cookie store
            CookieStore cookieStore = new BasicCookieStore();

            // Bind custom cookie store to the local context
            localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);

            try {
                // Add your data
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                nameValuePairs.add(new BasicNameValuePair("email", sUser));
                nameValuePairs.add(new BasicNameValuePair("password", sPass));
                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));                

                boolean bGetCookie = false;
                // Execute HTTP Post Request
                HttpResponse response = httpclient.execute(httppost, localContext);
                
                List<Cookie> cl = cookieStore.getCookies();
                for(int i = 0; i < cl.size(); i++){
                	if(cl.get(i).getName().contentEquals(".MYKOSMOSNIMKIAUTH")){
                		bGetCookie = true;
                		mContent = cl.get(i).getName() + "=" + cl.get(i).getValue();
                		break;
                	}
                }
                
                Bundle bundle = new Bundle();
                if(bGetCookie){
                	bundle.putBoolean("error", false);
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
    	if(mbShowProgress){
    		mDownloadDialog.dismiss();
    	}
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

	@Override
	protected void onCancelled() {		
		super.onCancelled();
		if(mbShowProgress){
			mDownloadDialog.dismiss();
		}
	}
}

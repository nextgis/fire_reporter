/*******************************************************************************
*
* FireReporter
* ---------------------------------------------------------
* Report and view fires
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

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;

import android.annotation.SuppressLint;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;

import android.view.View;

import android.widget.TextView;
import android.widget.ImageView;

public class AboutReporter extends SherlockActivity {
    private TextView txtVersion;
    private TextView txtDescription;
    private ImageView imgLogo;

    private String versionName = "unknown";
    private String versionCode = "unknown";

	@SuppressLint("NewApi")
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.about);

        txtVersion = (TextView) findViewById(R.id.txtVersion);
        txtDescription = (TextView) findViewById(R.id.txtDescription);
        imgLogo = (ImageView) findViewById(R.id.imgLogo);

        imgLogo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onLogoClicked();
            }
        });

        String pkgName = this.getPackageName();
        try {
            PackageManager pm = this.getPackageManager();
            versionName = pm.getPackageInfo(pkgName, 0).versionName;
            versionCode = Integer.toString(pm.getPackageInfo(this.getPackageName(), 0).versionCode);
        } catch (NameNotFoundException e) {
        }

        txtVersion.setText("v. " + versionName + " (rev. " + versionCode + ")");
        
       	getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void onLogoClicked() {
        Intent browserIntent = new Intent("android.intent.action.VIEW",
                Uri.parse("http://nextgis.ru"));
        startActivity(browserIntent);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // app icon in action bar clicked; go home
                Intent intent = new Intent(this, FireReporter.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}

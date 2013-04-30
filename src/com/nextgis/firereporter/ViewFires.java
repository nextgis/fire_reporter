/*******************************************************************************
*
* FireReporter
* ---------------------------------------------------------
* Report and view fires
*
* Copyright (C) 2011 NextGIS (http://nextgis.ru)
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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;

import android.content.Context;
import android.content.SharedPreferences;

import android.graphics.Typeface;

import android.location.Location;
import android.location.LocationManager;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import android.os.Bundle;

import android.view.View;

import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class ViewFires extends Activity {
    private static final String SELECT_QUERY_P1 = "SELECT fire14.ogc_fid AS gid, date, latitude, longitude, round(ST_Distance_Sphere(ST_PointFromText(";
    private static final String SELECT_QUERY_P2 = ", fire14.wkb_geometry)) AS distance FROM fire14 ";
    private static final String SELECT_QUERY_P3 = " ORDER BY distance LIMIT ";

    private Button btnSearch;
    private TableLayout tblResults;
    private TextView lblRows;

    private String pgHost;
    private String pgPort;
    private String pgDatabase;
    private String pgUser;
    private String pgPassword;
    private boolean useSSL;
    private String fetchRows;
    private String pgURL;
    private boolean searchByDate;

    private Connection conn = null;
    private Statement stmt;
    private ResultSet res;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view);

        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            Toast.makeText(ViewFires.this, e.toString(),
                           Toast.LENGTH_LONG).show();
            return;
        }

        tblResults = (TableLayout) findViewById(R.id.tblResults);
        lblRows = (TextView) findViewById(R.id.lblRows);
        btnSearch = (Button) findViewById(R.id.btnSearch);

        btnSearch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onSearchButtonClicked();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences settings = getSharedPreferences(FireReporter.PREFERENCES, 0);
        pgHost = settings.getString("mod14_server", "gis-lab.info");
        pgPort = settings.getString("mod14_port", "5432");
        pgDatabase = settings.getString("mod14_database", "mod14");
        pgUser = settings.getString("mod14_user", "fire_usr");
        pgPassword = settings.getString("mod14_password", "J59DY");
        useSSL = settings.getBoolean("mod14_use_ssl", false);
        fetchRows = settings.getString("fetch_rows", "5");
        searchByDate = settings.getBoolean("search_by_date",false);

        pgURL = "jdbc:postgresql://";
        pgURL += pgHost;

        if (pgPort.length() > 0) {
            pgURL += ":" + pgPort;
        }

        pgURL += "/";
        pgURL += pgDatabase;

        if (useSSL) {
            pgURL += "?ssl=true&sslfactory=org.postgresql.ssl.NonValidatingFactory";
        }
    }

    public void onSearchButtonClicked() {
        ConnectivityManager mConnectivity = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if ((mConnectivity.getNetworkInfo(0).getState() == NetworkInfo.State.CONNECTED)
                ||( mConnectivity.getNetworkInfo(1).getState() == NetworkInfo.State.CONNECTED)) {
            // TODO: use requestRouteToHost to check if server available?

            // network is up, we can request server for data
            findFires();
        } else {
            // oops... seems that there is no connection
            Toast.makeText(ViewFires.this, getString(R.string.noNetwork),
                           Toast.LENGTH_LONG).show();
        }
    }

    public void displayQueryResult(String lat, String lon) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        String dt = dateFormat.format(date);

        try {
            conn = DriverManager.getConnection(pgURL, pgUser, pgPassword);
            stmt = conn.createStatement();

            String sql = SELECT_QUERY_P1 + "'POINT(";
            sql += lon + " ";
            sql += lat +")', 4326)";
            sql += SELECT_QUERY_P2;
            if (searchByDate) {
                sql += "WHERE CAST(date as date) = '";
                sql += dt + "'";
            }
            sql += SELECT_QUERY_P3 + fetchRows + ";";

            res = stmt.executeQuery(sql);

            ResultSetMetaData rsMetadata = res.getMetaData();
            tblResults.removeAllViews();
            int cols = rsMetadata.getColumnCount();

            TableRow header = new TableRow(this);
            TableLayout.LayoutParams tableParams = new TableLayout.LayoutParams();
            tblResults.addView(header, tableParams);
            String colName;
            for (int i = 1; i <= cols; i++) {
                colName = rsMetadata.getColumnName(i);
                TextView txtColName = new TextView(this);
                txtColName.setText(colName);
                txtColName.setPadding(3, 3, 3, 3);
                txtColName.setTypeface(Typeface.DEFAULT_BOLD, 1); // bold
                txtColName.setTextColor(-16777216);
                txtColName.setBackgroundColor(-5592406);
                TableRow.LayoutParams params = new TableRow.LayoutParams();
                header.addView(txtColName, params);
            }

            int recordsFetched = 1;
            int color;
            while(res.next()) {
                TableRow row = new TableRow(this);
                tblResults.addView(row, tableParams);

                color = -3355444 + (1118481 * (recordsFetched % 2));

                for(int i = 1; i <= cols; i++) {
                    colName = res.getString(i);
                    TextView txtColName = new TextView(this);
                    txtColName.setText(colName);
                    txtColName.setPadding(4, 3, 4, 3);
                    txtColName.setTextColor(-16777216);
                    txtColName.setBackgroundColor(color);
                    TableRow.LayoutParams params = new TableRow.LayoutParams();
                    row.addView(txtColName, params);
                }
                recordsFetched += 1;
            }

            lblRows.setText(getString(R.string.sRows) + Integer.toString(recordsFetched - 1));

            res.close();
            stmt.close();
        } catch (SQLException e) {
            Toast.makeText(ViewFires.this, e.toString(),
                           Toast.LENGTH_LONG).show();
            return;
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                }
            }
        }
    } // displayQueryResult

    private void findFires() {
        LocationManager locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location currentLocation = locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        if (currentLocation != null) {
            String lat = Double.toString(currentLocation.getLatitude());
            String lon = Double.toString(currentLocation.getLongitude());

            displayQueryResult(lat, lon);
        } else {
            Toast.makeText(ViewFires.this, getString(R.string.noLocation),
                           Toast.LENGTH_LONG).show();
            return;
        }
    }
}

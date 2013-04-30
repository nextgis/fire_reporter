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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;

import android.hardware.Sensor;
import android.hardware.SensorManager;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;

import android.view.Menu;
import android.view.MenuItem;

import android.widget.EditText;
import android.widget.Toast;

public class SendReport extends Activity {
    private EditText edLatitude;
    private EditText edLongitude;
    private EditText edAzimuth;
    private EditText edDistance;
    private EditText edComment;
    private CompassFragment frCompass;

    private LocationManager mLocationManager;
    private LocationListener mLocationListener;

    private SensorManager mSensorManager;

    Sensor mAccelerometer;
    Sensor mMagnetometer;

    float[] mGravity;
    float[] mGeomagnetic;
    float mAzimuth;
    float mOldAzimuth = 0;

    private boolean gpsAvailable = false;
    private boolean compassAvailable = false;
    
	private SQLiteDatabase ReportsDB;
	private ReportsDatabase dbHelper;

    @SuppressLint("NewApi")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.report);
        
        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentapiVersion >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH){
        	getActionBar().setHomeButtonEnabled(true);
        }
	    getActionBar().setDisplayHomeAsUpEnabled(true);       
        

        edLatitude = (EditText) findViewById(R.id.edLatitude);
        edLongitude = (EditText) findViewById(R.id.edLongitude);
        edAzimuth = (EditText) findViewById(R.id.edAzimuth);
        edDistance = (EditText) findViewById(R.id.edDistance);
        edComment = (EditText) findViewById(R.id.edComment);
        
        edDistance.setText("100");

        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if(mLocationManager != null){

	        for (String aProvider : mLocationManager.getProviders(false)) {
	            if (aProvider.equals(LocationManager.GPS_PROVIDER)) {
	                gpsAvailable = true;
	            }
	        }
	
	        if (gpsAvailable) {
	            Location location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
	
	            if (location != null) {
	                float lat = (float) (location.getLatitude());
	                float lon = (float) (location.getLongitude());
	                edLatitude.setText(Float.toString(lat));
	                edLongitude.setText(Float.toString(lon));
	            } else {
	                edLatitude.setText(getString(R.string.noLocation));
	                edLongitude.setText(getString(R.string.noLocation));
	            }
	
	            mLocationListener = new LocationListener() {
	                public void onStatusChanged(String provider, int status,
	                        Bundle extras) {
	                    switch (status) {
	                      case LocationProvider.OUT_OF_SERVICE:
	                          break;
	                      case LocationProvider.TEMPORARILY_UNAVAILABLE:
	                          break;
	                      case LocationProvider.AVAILABLE:
	                          break;
	                    }
	                }
	
	                public void onProviderEnabled(String provider) {
	                }
	
	                public void onProviderDisabled(String provider) {
	                }
	
	                public void onLocationChanged(Location location) {
	                    float lat = (float) (location.getLatitude());
	                    float lon = (float) (location.getLongitude());
	                    edLatitude.setText(Float.toString(lat));
	                    edLongitude.setText(Float.toString(lon));
	
	                    // FIXME: also need to calculate declination?
	                }
	            }; // location listener
	        } else {
	            edLatitude.setText(getString(R.string.noGPS));
	            edLongitude.setText(getString(R.string.noGPS));
	            edLatitude.setEnabled(false);
	            edLongitude.setEnabled(false);
	        }
        }
        
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        boolean accelerometerAvailable = false;
        boolean magnetometerAvailable = false;
        for (Sensor aSensor : mSensorManager.getSensorList(Sensor.TYPE_ALL)) {
            if (aSensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                accelerometerAvailable = true;
            } else if (aSensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                magnetometerAvailable = true;
            }
        }

        compassAvailable = accelerometerAvailable && magnetometerAvailable;

        if (compassAvailable) {
            frCompass = (CompassFragment) getFragmentManager().findFragmentById(R.id.compass_fragment);
            frCompass.SetAzimuthCtrl(edAzimuth);
        } else {
            edAzimuth.setText(getString(R.string.noCompass));
            edAzimuth.setEnabled(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

    } // onResume

    @Override
    protected void onPause() {
        super.onPause();
    }
/*
    private void onSendButtonClicked() {
        try {
            float lat = Float.valueOf(edLatitude.getText().toString()).floatValue();
            float lon = Float.valueOf(edLongitude.getText().toString()).floatValue();
            sendReport();
        } catch (NumberFormatException e) {
            AlertDialog.Builder builder = new AlertDialog.Builder(SendReport.this);
            builder.setMessage(getString(R.string.confirmSend))
                   .setCancelable(false)
                   .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                       public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                            SendReport.this.sendReport();
                       }
                   })
                   .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                       public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                       }
                   });
            AlertDialog alert = builder.create();
            alert.show();
        }
    }

    private void sendReport() {
        ConnectivityManager mConnectivity = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if ((mConnectivity.getNetworkInfo(0).getState() == NetworkInfo.State.CONNECTED)
                ||( mConnectivity.getNetworkInfo(1).getState() == NetworkInfo.State.CONNECTED)) {
            // TODO: use requestRouteToHost to check if server available?

            // network is up, we can send data directly to the server
            sendDataToServer();
        } else {
            // network is down, save report in local database
            saveDataLocally();
            Toast.makeText(SendReport.this, getString(R.string.reportStored),
                           Toast.LENGTH_LONG).show();

            // schedule alarm for delayed send
            AlarmManager mgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent i = new Intent(this, OnAlarmReceiver.class);
            PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 0);
            mgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + sendInterval * 60000, pi);
        }

        if (gpsAvailable && compassAvailable) {
            edLatitude.setText("");
            edLongitude.setText("");
            edAzimuth.setText("");
            edDistance.setText("");
            edComment.setText("");
        } else {
              edDistance.setText("");
              edComment.setText("");
        }
    } // onSendButtonClicked

    private void sendDataToServer() {
    	
        float lat;
        float lon;
        try {
            lat = Float.valueOf(edLatitude.getText().toString()).floatValue();
            lon = Float.valueOf(edLongitude.getText().toString()).floatValue();
        } catch (NumberFormatException e) {
            lat = -999.0f;
            lon = -999.0f;
        }

        GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        Timestamp ts = new Timestamp(cal.getTimeInMillis());

        try {
            conn = DriverManager.getConnection(pgURL, pgUser, pgPassword);

            String sql = INSERT_QUERY;
            if (gpsAvailable && lat != -999.0 && lon !=-999.0) {
                sql += "(ST_GeomFromText('POINT(";
                sql += edLongitude.getText().toString() + " ";
                sql += edLatitude.getText().toString() + ")', 4326),?,?,?,?,?,?)";
                stmt = conn.prepareStatement(sql);
                stmt.setFloat(1, lon);
                stmt.setFloat(2, lat);
            } else {
                sql += "(NULL,?,?,?,?,?,?)";
                stmt = conn.prepareStatement(sql);
                stmt.setNull(1, java.sql.Types.FLOAT);
                stmt.setNull(2, java.sql.Types.FLOAT);
            }

            if (compassAvailable ) {
                stmt.setFloat(3, Float.valueOf(edAzimuth.getText().toString()).floatValue());
            } else {
                stmt.setNull(3, java.sql.Types.FLOAT);
            }

            if (edDistance.getText().toString().equals("")) {
                stmt.setNull(4, java.sql.Types.FLOAT);
            } else {
                stmt.setFloat(4, Float.valueOf(edDistance.getText().toString()).floatValue());
            }
            stmt.setTimestamp(5, ts);
            stmt.setString(6, edComment.getText().toString());

            int rows = stmt.executeUpdate();
            stmt.close();
            conn.close();
            Toast.makeText(SendReport.this, getString(R.string.reportSend),
                           Toast.LENGTH_SHORT).show();
        } catch (SQLException e) {
            Toast.makeText(SendReport.this, e.toString(),
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
    } // sendDataToServer

    private void saveDataLocally() {
    	
        ContentValues values = new ContentValues();

        float lat;
        float lon;
        try {
            lat = Float.valueOf(edLatitude.getText().toString()).floatValue();
            lon = Float.valueOf(edLongitude.getText().toString()).floatValue();
        } catch (NumberFormatException e) {
            lat = -999.0f;
            lon = -999.0f;
        }

        if (gpsAvailable && lat != -999.0 && lon != -999.0) {
            values.put(ReportsDbAdapter.KEY_LATITUDE, lat);
            values.put(ReportsDbAdapter.KEY_LONGITUDE, lon);
        } else {
            values.putNull(ReportsDbAdapter.KEY_LATITUDE);
            values.putNull(ReportsDbAdapter.KEY_LONGITUDE);
        }

        if (compassAvailable) {
            values.put(ReportsDbAdapter.KEY_AZIMUTH,
                Float.valueOf(edAzimuth.getText().toString()).floatValue());
        } else {
            values.putNull(ReportsDbAdapter.KEY_AZIMUTH);
        }

        if (edDistance.getText().toString().equals("")) {
            values.putNull(ReportsDbAdapter.KEY_DISTANCE);
        } else {
            values.put(ReportsDbAdapter.KEY_DISTANCE,
                Float.valueOf(edDistance.getText().toString()).floatValue());
        }
        values.put(ReportsDbAdapter.KEY_COMMENT, edComment.getText().toString());

        GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        Timestamp ts = new Timestamp(cal.getTimeInMillis());
        values.put(ReportsDbAdapter.KEY_DATETIME, ts.toString());

        ReportSaver task = new ReportSaver();
        task.execute(values);
    }

    private class ReportSaver extends AsyncTask<ContentValues, Void, Void> {
        protected void onPreExecute() {
        }

        protected Void doInBackground(ContentValues... vals) {
            ReportsDbAdapter dbAdapter = new ReportsDbAdapter(SendReport.this);
            dbAdapter.open();
            long res = dbAdapter.saveReport(vals[0]);
            dbAdapter.close();
            return null;
        }

        protected void onPostExecute(Void unused) {
        }
    }*/

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // app icon in action bar clicked; go home
                Intent intent = new Intent(this, FireReporter.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
	        case R.id.settings:
	            // app icon in action bar clicked; go home
	            Intent intentSet = new Intent(this, SettingsMain.class);
	            intentSet.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
	            startActivity(intentSet);
	            return true;
	        case R.id.about:
	            // app icon in action bar clicked; go home
	            Intent intentAbout = new Intent(this, AboutReporter.class);
	            intentAbout.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
	            startActivity(intentAbout);
	            return true;	
	        case R.id.report:
	        	onReport();
	            return true;   
	         default:
                return super.onOptionsItemSelected(item);
        }
    }
 
	  @Override
	  public boolean onCreateOptionsMenu(Menu menu) {
	    getMenuInflater().inflate(R.menu.report, menu);
	    return true;
	  }
	  
	  public void onReport(){
        try {
            double dfLat = Double.valueOf(edLatitude.getText().toString()).doubleValue();
            double dfLon = Double.valueOf(edLongitude.getText().toString()).doubleValue();
            double dfAz = frCompass.getOrientation();
            //double dfAz = Double.valueOf(edAzimuth.getText().toString()).doubleValue();
            double dfDist = Double.valueOf(edDistance.getText().toString()).doubleValue();
            String sComment = edComment.getText().toString();
            
    		dbHelper = new ReportsDatabase(this.getApplicationContext());
    		ReportsDB = dbHelper.getWritableDatabase();

			ContentValues values = new ContentValues();
			values.put(ReportsDatabase.COLUMN_LAT, dfLat);
			values.put(ReportsDatabase.COLUMN_LON, dfLon);
			values.put(ReportsDatabase.COLUMN_AZIMUTH, dfAz);
			values.put(ReportsDatabase.COLUMN_DISTANCE, dfDist);
			values.put(ReportsDatabase.COLUMN_COMMENT, sComment);
			long nRowId = ReportsDB.insert(ReportsDatabase.TABLE_POS, null, values);
			
			ReportsDB.close();

			dbHelper.close();			
			
			if(nRowId == -1){
				Toast.makeText(SendReport.this, getString(R.string.reportStoredFailed), Toast.LENGTH_LONG).show();
	        	finish();
	        	return;				
			}

        } catch (NumberFormatException e) {
        	Toast.makeText(SendReport.this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        	finish();
        	return;
        }
        //start or restart service
        startService(new Intent(ReporterService.ACTION_START));
        
        finish();
	  }
}

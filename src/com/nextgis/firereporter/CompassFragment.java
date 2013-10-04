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

import java.text.NumberFormat;

import com.nextgis.firereporter.R;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.widget.EditText;

/**
 * @author Bishop
 *
 */

public class CompassFragment extends Fragment implements OnTouchListener {
	
	final static String TAG = "firereporter";
	
	protected Location currentLocation;
	private float downX, downY, upX, upY;
	protected float declination;
	protected CompassImage compass;
	protected float m_dfAzimuth;
	
	protected Vibrator vibrator;

	private boolean vibrationOn;
	
	public static final String ACTION_COMPASS_UPDATES = "com.nextgis.firereporter.ACTION_COMPASS_UPDATES";
	public static final char DEGREE_CHAR = (char) 0x00B0;
	
	private SensorManager sensorManager = null;		
	
	private EditText edAzimuth = null;
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
		
		View view = inflater.inflate(R.layout.compassfragment, container, false);
        
		// magnetic north compass
		if (view.findViewById(R.id.compass) != null) {

			compass = (CompassImage) view.findViewById(R.id.compass);

			compass.setOnTouchListener(this);
		}
        
		return view;
    }
	
	public void SetAzimuthCtrl(EditText edAzimuth){
		this.edAzimuth = edAzimuth;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        
		// reference to vibrator service
		vibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);

		// vibrate or not?
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
		vibrationOn = prefs.getBoolean("compass_vibration", true);

		LocationManager locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
		
		if(currentLocation == null)
		{
			currentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			if (currentLocation == null){
				currentLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			}
		}
		
		long now = System.currentTimeMillis();
		declination = getDeclination(currentLocation, now);

		
		sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
		if(sensorManager != null){
			sensorManager.registerListener(sensorListener, sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_NORMAL);
		}
		
		getActivity().registerReceiver(compassBroadcastReceiver, new IntentFilter(ACTION_COMPASS_UPDATES));	
		
        Log.d(TAG, "CompassActivity: onCreate");	
	}

	@Override
	public void onDestroy() {
		if(sensorManager != null){
			sensorManager.unregisterListener(sensorListener);
		}
		super.onDestroy();
	}

	@Override
	public void onPause() {
		getActivity().unregisterReceiver(compassBroadcastReceiver);

		super.onPause();
	}	
	
	@Override
	public void onResume() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
		if (getView().findViewById(R.id.compassView) != null) {
			getView().findViewById(R.id.compassView).setKeepScreenOn(prefs.getBoolean("compass_wake_lock", true));
		}

		// registering receiver for compass updates
		getActivity().registerReceiver(compassBroadcastReceiver, new IntentFilter(ACTION_COMPASS_UPDATES));		

		super.onResume();
	}

	public boolean onTouch(View v, MotionEvent event) {
		
		switch (event.getAction()) {

		case MotionEvent.ACTION_DOWN:
			downX = event.getX();
			downY = event.getY();
			return true;

		case MotionEvent.ACTION_MOVE:

			upX = event.getX();
			upY = event.getY();

			double downR = Math.atan2(v.getHeight() / 2 - downY, downX - v.getWidth() / 2);
			int angle1 = (int) Math.toDegrees(downR);

			double upR = Math.atan2(v.getHeight() / 2 - upY, upX - v.getWidth() / 2);
			int angle2 = (int) Math.toDegrees(upR);

			this.rotateCompass(angle1 - angle2);

			if (vibrationOn) {
				vibrator.vibrate(5);
			}

			// update starting point for next move event
			downX = upX;
			downY = upY;

			return true;
		}
		return false;
	}
	
	protected void rotateCompass(float angle) {
		// magnetic north compass
		double dfOrinetAngle = 0;
		if (compass != null) {
			compass.setAngle(compass.getAngle() + angle);
			compass.invalidate();
			dfOrinetAngle = getAzimuth(m_dfAzimuth + compass.getAngle() + angle);
		}		
		
		if(edAzimuth != null){
			String sAz = formatNumber(dfOrinetAngle, 0, 0) + DEGREE_CHAR + " " + getDirectionCode((float) dfOrinetAngle, getResources());
			edAzimuth.setText(sAz);
		}		
	}
	
	public void updateCompass(float azimuth) {
				
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
		boolean trueNorth = prefs.getBoolean("compass_true_north", false);
		boolean showMagnetic = prefs.getBoolean("compass_show_magnetic", true);

		float rotation = 0;

		// are we taking declination into account?
		if (!trueNorth || currentLocation == null) {
			declination = 0;
		}

		// magnetic north to true north, compensate for device's physical rotation 
		rotation = getAzimuth(azimuth + declination + getDeviceRotation());
		m_dfAzimuth = rotation;

		//	((TextView) getView().findViewById(R.id.azimuth)).setText(formatNumber(rotation, 0, 0) + DEGREE_CHAR + " " + getDirectionCode(rotation, getResources()));

		// true north compass
		if (getView().findViewById(R.id.compassNeedle) != null) {

			CompassImage compassNeedle = (CompassImage) getView().findViewById(R.id.compassNeedle);

			if (compassNeedle.getVisibility() == View.VISIBLE) {
				compassNeedle.setAngle(360 - rotation);
				compassNeedle.invalidate();
			}
		}

		// magnetic north compass
		if (getView().findViewById(R.id.compassNeedleMagnetic) != null) {

			CompassImage compassNeedleMagnetic = (CompassImage) getView().findViewById(R.id.compassNeedleMagnetic);

			if (showMagnetic) {

				if (compassNeedleMagnetic.getVisibility() != View.VISIBLE) {
					compassNeedleMagnetic.setVisibility(View.VISIBLE);
				}

				compassNeedleMagnetic.setAngle(360 - rotation + declination);
				compassNeedleMagnetic.getDrawable().setAlpha(50);
				compassNeedleMagnetic.invalidate();

			} else {
				compassNeedleMagnetic.setVisibility(View.INVISIBLE);
			}

		}
		
		double dfOrinetAngle = 0;
		if (compass != null) {
			dfOrinetAngle = getAzimuth(m_dfAzimuth + compass.getAngle());
		}	
		
		if(edAzimuth != null){
			edAzimuth.setText(formatNumber(dfOrinetAngle, 0, 0) + DEGREE_CHAR + " " + getDirectionCode((float) dfOrinetAngle, getResources()));
		}
	}
	
	public double getOrientation(){
		if (compass != null) {
			return getAzimuth(m_dfAzimuth + compass.getAngle());			
		}
		else{
			return m_dfAzimuth;
		}
	}
	
	public static String getDirectionCode(float azimuth, Resources res) {	
		int nIndex = Math.round(azimuth / 45);

		String directionCodes[] = { 
				(String) res.getText(R.string.compas_N), 
				(String) res.getText(R.string.compas_NE),
				(String) res.getText(R.string.compas_E),
				(String) res.getText(R.string.compas_SE),
				(String) res.getText(R.string.compas_S),
				(String) res.getText(R.string.compas_SW),
				(String) res.getText(R.string.compas_W),
				(String) res.getText(R.string.compas_NW),
				(String) res.getText(R.string.compas_N) };
		if(nIndex > 8 || nIndex < 0){
			return directionCodes[0];
		}		
		else {
			return directionCodes[nIndex];
		}
	}		
	
	public static String formatNumber(Object value, int max, int min) {

		NumberFormat f = NumberFormat.getInstance();
		f.setMaximumFractionDigits(max);
		f.setMinimumFractionDigits(min);
		f.setGroupingUsed(false);

		try {
			return f.format(value);
		} catch (IllegalArgumentException e) {
			return "err";
		}

	}	

	protected float getAzimuth(float az) {
		
		if(az < 0)
			return az + 360;	
		if(az > 360)
			return az - 360;

		return az;

	}	
	
	protected SensorEventListener sensorListener = new SensorEventListener() {

		public void onSensorChanged(SensorEvent event) {
			// let's broadcast compass data to any activity waiting for updates
			Intent intent = new Intent(ACTION_COMPASS_UPDATES);

			// packing azimuth value into bundle
			Bundle bundle = new Bundle();
			bundle.putFloat("azimuth", event.values[0]);
			bundle.putFloat("pitch", event.values[1]);
			bundle.putFloat("roll", event.values[2]);

			intent.putExtras(bundle);

			// broadcasting compass updates
			if(getActivity() != null)
				getActivity().sendBroadcast(intent);
		}

		public void onAccuracyChanged(Sensor arg0, int arg1) {			
		}
	};
		
	protected OnLongClickListener resetCompass = new OnLongClickListener() {

		public boolean onLongClick(View v) {

			if (compass != null) {
				compass.setAngle(0);
				compass.invalidate();
			}

			return true;
		}

	};
	
	/**
	 * Get current magnetic declination
	 * 
	 * @param location
	 * @param timestamp
	 * @return
	 */
	public static float getDeclination(Location location, long timestamp) {

		GeomagneticField field = new GeomagneticField((float) location.getLatitude(), (float) location.getLongitude(),
				(float) location.getAltitude(), timestamp);

		return field.getDeclination();
	}	
	
	public int getDeviceRotation() {

		Display display = getActivity().getWindowManager().getDefaultDisplay();

		final int rotation = display.getRotation();

		if (rotation == Surface.ROTATION_90) {
			return 90;
		} else if (rotation == Surface.ROTATION_180) {
			return 180;
		} else if (rotation == Surface.ROTATION_270) { 
			return 270; 
		}

		return 0;
	}	

	protected BroadcastReceiver compassBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			Bundle bundle = intent.getExtras();

			updateCompass(bundle.getFloat("azimuth"));
		}
	};	
}



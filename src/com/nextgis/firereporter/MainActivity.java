/*******************************************************************************
 * Project:  Fire reporter
 * Purpose:  Report and view fires
 * Author:   Dmitry Baryshnikov (aka Bishop), polimax@mail.ru
 *******************************************************************************
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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class MainActivity extends SherlockFragmentActivity{
    public final static String PREFERENCES = "FireReporter";
    public final static String TAG = "FireReporter";
    public final static String INTENT_NAME = "com.nextgis.firereporter.intent.action.SYNC";
    
    public final static String PREF_CURRENT_TAB = "CURRENT_TAB";
    public final static String PREF_CURRENT_FILTER = "CURRENT_FILTER";

    private static final int NUM_ITEMS = 2;
    
	public final static int MENU_REPORT = 1;
	public final static int MENU_PLACE = 2;
	public final static int MENU_REFRESH = 3;
	public final static int MENU_SETTINGS = 4;
	public final static int MENU_ABOUT = 5;

	public final static int SRC_NASA = 1 << 0; // 1
	public final static int SRC_USER = 1 << 1; // 2
	public final static int SRC_SCANEX = 1 << 2; // 4
	
	
	private FragmentRollAdapter mAdapter;
    private ViewPager mPager;
    
	private int mPosition;
    private MenuItem refreshItem;
    
    protected boolean mbRefreshing;
    
	@Override
	  public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    mbRefreshing = false;
	    
        // initialize the default settings
        PreferenceManager.setDefaultValues(this, R.xml.preferences_general, false);
        PreferenceManager.setDefaultValues(this, R.xml.preferences_user, false);
        PreferenceManager.setDefaultValues(this, R.xml.preferences_nasa, false);
        PreferenceManager.setDefaultValues(this, R.xml.preferences_scanex, false);

	    setContentView(R.layout.main);
	    
	    final ActionBar actionBar = getSupportActionBar();
	    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        
        mAdapter = new FragmentRollAdapter(getSupportFragmentManager());
    	mAdapter.setActionBar(actionBar);
        mPager = (ViewPager)findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);
        
        mPager.setOnPageChangeListener(new OnPageChangeListener() {

			public void onPageScrollStateChanged(int arg0) {
				}

			public void onPageScrolled(int arg0, float arg1, int arg2) {
				}

			public void onPageSelected(int arg0) {
				Log.d(TAG, "onPageSelected: " + arg0);
				
				actionBar.getTabAt(arg0).select();
				
				Editor editor = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit();
				editor.putInt(PREF_CURRENT_TAB, arg0);
				editor.commit();
			}
        } );        
        
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        mPosition = prefs.getInt(PREF_CURRENT_TAB, 0);
        
        Tab tab = actionBar.newTab()
                .setText(R.string.tabNotification)
                .setTabListener(new TabListener<SherlockFragment>(0 + "", mPager));
        actionBar.addTab(tab);
        
        tab = actionBar.newTab()
            .setText(R.string.tabSubscription)
            .setTabListener(new TabListener<SherlockFragment>(1 + "", mPager));
        actionBar.addTab(tab); 
        
        actionBar.getTabAt(mPosition).select();
        
	  }

	  @Override
	  public boolean onCreateOptionsMenu(Menu menu) {
	    
			menu.add(com.actionbarsherlock.view.Menu.NONE, MENU_SETTINGS, com.actionbarsherlock.view.Menu.NONE, R.string.tabSettings)
	       .setIcon(R.drawable.ic_action_settings)
	       .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);		
			
			menu.add(com.actionbarsherlock.view.Menu.NONE, MENU_ABOUT, com.actionbarsherlock.view.Menu.NONE, R.string.tabAbout)
			.setIcon(R.drawable.ic_action_about)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);	

			boolean wasRefreshing = false;
			if(mbRefreshing){
				wasRefreshing = true;
				completeRefresh();
			}
			refreshItem = menu.add(com.actionbarsherlock.view.Menu.NONE, MENU_REFRESH, com.actionbarsherlock.view.Menu.NONE, R.string.sRefresh)
			.setIcon(R.drawable.ic_navigation_refresh);
			refreshItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
			
			if(wasRefreshing){
				refresh();
			}
			
			return true;
	    
	  }

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
	        case android.R.id.home:
	            // app icon in action bar clicked; go home
	            //Intent intentMain = new Intent(this, MainActivity.class);
	            //intentMain.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	            //startActivity(intentMain);
	            return true;
	        case MENU_SETTINGS:
	            // app icon in action bar clicked; go home
	            Intent intentSet = new Intent(this, SettingsActivity.class);
	            intentSet.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
	            startActivity(intentSet);
	            return true;
	        case MENU_ABOUT:
	            // app icon in action bar clicked; go home
	            Intent intentAbout = new Intent(this, AboutActivity.class);
	            intentAbout.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
	            startActivity(intentAbout);
	            return true;	  
	    }
		
		return super.onOptionsItemSelected(item);
		
	}
	
	public void refresh() {
		if(refreshItem == null || mbRefreshing)
			return;

	     LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	     ImageView iv = (ImageView) inflater.inflate(R.layout.refresh_action_view, null);

	     Animation rotation = AnimationUtils.loadAnimation(this, R.anim.clockwise_refresh);
	     rotation.setRepeatCount(Animation.INFINITE);
	     iv.startAnimation(rotation);

	     refreshItem.setActionView(iv);
	     
	     mbRefreshing = true;
	}

	public void completeRefresh() {
		if(refreshItem == null || refreshItem.getActionView() == null)
			return;
		refreshItem.getActionView().clearAnimation();
		refreshItem.setActionView(null);
		
		mbRefreshing = false;
	}

	public static class TabListener<T extends SherlockFragment> implements ActionBar.TabListener {
		private final String mTag;
		private ViewPager mPager;

		public TabListener(String tag, ViewPager pager) {
			mTag = tag;
			mPager = pager;
		}

		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			int nTag = Integer.parseInt(mTag);
			mPager.setCurrentItem(nTag);
		}

		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
		}

		public void onTabReselected(Tab tab, FragmentTransaction ft) {
		}
	}	

	public static class FragmentRollAdapter extends FragmentPagerAdapter {
		ActionBar mActionBar;

		public FragmentRollAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public int getCount() {
			return NUM_ITEMS;
		}

		public void setActionBar( ActionBar bar ) {
			mActionBar = bar;
		}

		@Override
		public SherlockFragment getItem(int arg0) {
			switch(arg0)
			{
			case 0:
				FiresDataFragment frag0 = new FiresDataFragment();
				return (SherlockFragment) frag0;
			case 1:
				ScanexDataFragment frag1 = new ScanexDataFragment();
				return (SherlockFragment) frag1;
			default:
				return null;
			}
		}
	} 
	
	protected void StopService(){
        final Intent intent = new Intent(MainActivity.INTENT_NAME, null, this, GetFiresService.class);
        intent.putExtra(GetFiresService.COMMAND, GetFiresService.SERVICE_STOP);
        
        startService(intent);
	}

	@Override
	protected void onDestroy() {
//		StopService();
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		if(mbRefreshing)
			completeRefresh();
		super.onPause();
	}
}



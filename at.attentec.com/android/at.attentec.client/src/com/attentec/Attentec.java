/***
 * Copyright (c) 2010 Attentec AB, http://www.attentec.se
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.attentec;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TabHost;
import android.widget.Toast;

/**
 * Main class for the application.
 * @author David Granqvist
 * @author Malte Lenz
 * @author Johannes Nordkvist
 *
 */
public class Attentec extends TabActivity {

	/**
	 * Tag used for logging.
	 */
	private static final String TAG = "Attentec";

	private static final int LOCAL_FIRST_MENU_ITEM = 0;
	private static final int LOCAL_SECOND_MENU_ITEM = 1;
	private static final int LOCAL_THIRD_MENU_ITEM = 2;
	private static final int LOCAL_FOURTH_MENU_ITEM = 3;

	/** menu identifier for stopping the service. */
	private static final int STOP_SERVICE_ID = Menu.FIRST + LOCAL_FIRST_MENU_ITEM;

	/** menu identifier for starting the service. */
	private static final int START_SERVICE_ID = Menu.FIRST + LOCAL_SECOND_MENU_ITEM;

	/** menu identifier for starting the service. */
	private static final int SHOW_PREFERENCES_ID = Menu.FIRST + LOCAL_THIRD_MENU_ITEM;
	private static final int CONTACT_RADIUS_ID = Menu.FIRST + LOCAL_FOURTH_MENU_ITEM;

	/** Nr of items in the menu from this file. */
	public static final int NR_MENU_ITEMS = 3;

	/**
	 * Called when the activity is first created.
	 * @param savedInstanceState the saved instance
	 */
	@Override
	public final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent givenintent = getIntent();
		boolean startService = givenintent.getBooleanExtra("service", true);
		if (startService) {
			//Log.d(TAG, "Pre start service");
			Intent service = new Intent(this, AttentecService.class);
			startService(service);
			//Log.d(TAG, "Post start service");
		} else {
			//login error, show that we are not starting the service
			Toast.makeText(this, R.string.login_error_starting_without_service, Toast.LENGTH_LONG).show();
		}
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		updateView();
	}

	/**
	 * This prepares a general menu which holds for all tabs in the application.
	 * This will always be on top of the menu in each tab and the classes for each tab
	 * activity will extend this menu when there is a need for a menu.
	 *
	 * @param menu the menu that shows start/stop service.
	 * @return Always true of some strange reason.
	 */
	@Override
	public final boolean onPrepareOptionsMenu(final Menu menu) {
		menu.clear();
		if (AttentecService.getIsAlive()) {
			menu.add(0, STOP_SERVICE_ID, 0, R.string.menu_stop_service).setIcon(android.R.drawable.ic_menu_close_clear_cancel);
		} else {
			menu.add(0, START_SERVICE_ID, 0, R.string.menu_start_service).setIcon(android.R.drawable.ic_menu_add);
		}
		menu.add(0, SHOW_PREFERENCES_ID, 0, R.string.menu_show_preferences).setIcon(android.R.drawable.ic_menu_preferences);
		menu.add(0, CONTACT_RADIUS_ID, 0, R.string.menu_contact_radius).setIcon(android.R.drawable.ic_menu_search);
		super.onPrepareOptionsMenu(menu);
		return true;
	}

	@Override
	public final boolean onOptionsItemSelected(final MenuItem item) {
		switch(item.getItemId()) {
		case STOP_SERVICE_ID:
			Log.d(TAG, "Killing service");
			Intent stopService = new Intent(this, AttentecService.class);
			stopService(stopService);
			//Log.d(TAG, "Has killed service");
			return true;
		case START_SERVICE_ID:
			Log.d(TAG, "Starting service");
			Intent startService = new Intent(this, AttentecService.class);
			startService(startService);
			return true;
		case SHOW_PREFERENCES_ID:
			Intent settingsActivity = new Intent(getBaseContext(), Preferences.class);
			startActivity(settingsActivity);
			return true;
		case CONTACT_RADIUS_ID:
			Intent radiusActivity = new Intent(getBaseContext(), ContactInRadiusActivity.class);
			startActivity(radiusActivity);
			return true;
		default:
			Log.d(TAG, "Attentec->onOptionsItemSelected->case->default happens if something from a 'child' activity was selected.");
			break;
		}
		return super.onOptionsItemSelected(item);
	}



	@Override
	protected final void onDestroy() {
		super.onDestroy();
		Intent service = new Intent(this, AttentecService.class);
		stopService(service);
	}


	/**
	 * Updates the tabs in the application. This method is preferred to be called as part of a synchronization.
	 */
	public final void updateView() {
		Resources res = getResources();

		TabHost mTabHost = getTabHost();

		TabHost.TabSpec spec;

		Intent intent;

		//Create intent for tab ContactsActivity
		intent = new Intent().setClass(this, ContactsActivity.class);



		//Get picture for tab icon, will only be used for the first tab
		Drawable attentec = res.getDrawable(R.drawable.logo_tab_icon);

		//Specify tab to be added
		spec = mTabHost.newTabSpec("contacts").setIndicator(res.getString(R.string.contact_list), attentec).setContent(intent);

		//Add tab to tabhost
		mTabHost.addTab(spec);


		//Get picture for tab icon for closetoyou
		Drawable closetoyou = res.getDrawable(android.R.drawable.ic_dialog_map);

		//Create intent for tab ContactsActivity
		intent = new Intent().setClass(this, CloseToYou.class);

		//Specify tab to be added
		spec = mTabHost.newTabSpec("close_to_you").setIndicator(res.getString(R.string.close_to_you), closetoyou).setContent(intent);

		//Add tab to tabhost
		mTabHost.addTab(spec);

		//Set start tab to the first tab specified
		mTabHost.setCurrentTab(0);
		//Log.d(TAG, "Post updateView");
	}
}

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

import java.util.Date;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Fetches information from SharedPreferences.
 * @author David Granqvist
 * @author Malte Lenz
 * @author Johannes Nordkvist
 *
 */
public final class PreferencesHelper {
	/** Unused constructor. */
	private PreferencesHelper() { }

	private static SharedPreferences sp = null;

	/** Default timeout in seconds before giving up on GPS. */
	private static final String DEFAULT_GPS_TIMEOUT = "60";

	/** Default gps accuracy limit in meters before giving up. */
	private static final String DEFAULT_GPS_ACCURACY_LIMIT = "50";

	/** Default interval between fetching contacts, in minutes. */
	private static final String DEFAULT_CONTACTS_UPDATE_INTERVAL = "600";

	/** Default interval between fetching others locations, in minutes. */
	private static final String DEFAULT_LOCATIONS_UPDATE_INTERVAL = "3";

	/** Default interval between fetching own location, in minutes. */
	private static final String DEFAULT_LOCATIONS_UPDATE_OWN_INTERVAL = "10";

	/** Defaulting to sending own location to server. */
	private static final boolean DEFAULT_LOCATIONS_UPDATE_ENABLED = false;

	/** When to start sending location to server. */
	public static final String DEFAULT_LOCATIONS_UPDATE_START_TIME = "08:00";

	/** When to stop sending location to server. */
	public static final String DEFAULT_LOCATIONS_UPDATE_END_TIME = "17:00";

	//private static final String TAG = "Attentec->PreferencesHelper";




	/**
	 * Initialize our connection to SharedPreferences.
	 * @param ctx calling context
	 */
	static void initPreferences(final Context ctx) {
		if (sp == null) {
			sp = PreferenceManager.getDefaultSharedPreferences(ctx);
		}
	}

	/**
	 * Fetch set gps timeout and converto to milliseconds.
	 * @param ctx calling context
	 * @return timeout in milliseconds
	 */
	static Integer getGPSTimeout(final Context ctx) {
		initPreferences(ctx);
		return Integer.parseInt(sp.getString(ctx.getString(R.string.settings_gps_timeout_key), DEFAULT_GPS_TIMEOUT))
		* DevelopmentSettings.MILLISECONDS_IN_SECOND;
	}

	/**
	 * Fetch set GPS distance limit in meters.
	 * @param ctx calling context
	 * @return distance in meters
	 */
	static Integer getGPSAccuracyLimit(final Context ctx) {
		initPreferences(ctx);
		return Integer.parseInt(sp.getString(ctx.getString(R.string.settings_gps_distance_key), DEFAULT_GPS_ACCURACY_LIMIT));
	}

	/**
	 * Fetch set interval between contact updates, in milliseconds.
	 * @param ctx calling context
	 * @return interval in milliseconds
	 */
	static Integer getContactsUpdateInterval(final Context ctx) {
		initPreferences(ctx);
		return Integer.parseInt(
					sp.getString(ctx.getString(R.string.settings_contact_sync_interval_key), DEFAULT_CONTACTS_UPDATE_INTERVAL)
				) * DevelopmentSettings.MILLISECONDS_IN_MINUTE;
	}

	/**
	 * Fetch set interval between fetching others location updates, in milliseconds.
	 * @param ctx calling context
	 * @return interval in milliseconds
	 */
	static Integer getLocationsUpdateInterval(final Context ctx) {
		initPreferences(ctx);
		return Integer.parseInt(
					sp.getString(ctx.getString(R.string.settings_locations_update_interval_key), DEFAULT_LOCATIONS_UPDATE_INTERVAL)
				) * DevelopmentSettings.MILLISECONDS_IN_MINUTE;
	}

	/**
	 * Fetch set interval between own location updates, in milliseconds.
	 * @param ctx calling context
	 * @return interval in milliseconds
	 */
	static Integer getLocationsUpdateOwnInterval(final Context ctx) {
		initPreferences(ctx);
		return Integer.parseInt(
					sp.getString(ctx.getString(R.string.settings_locations_update_own_interval_key), DEFAULT_LOCATIONS_UPDATE_OWN_INTERVAL)
				) * DevelopmentSettings.MILLISECONDS_IN_MINUTE;
	}

	/**
	 * Fetch if application should send own position to server.
	 * Checks both the "enabled" setting and the time limits.
	 * @param ctx calling context
	 * @return if application should send own position to server
	 */
	static Boolean getLocationUpdateEnabled(final Context ctx) {
		initPreferences(ctx);
		//first get the interval in which sending position is enabled.
		String startTime = sp.getString(ctx.getString(R.string.settings_locations_update_start_time_key), DEFAULT_LOCATIONS_UPDATE_START_TIME);
		String endTime = sp.getString(ctx.getString(R.string.settings_locations_update_end_time_key), DEFAULT_LOCATIONS_UPDATE_END_TIME);
		if (startTime.equals("0:0")) {
			startTime = DEFAULT_LOCATIONS_UPDATE_START_TIME;
		}
		if (endTime.equals("0:0")) {
			endTime = DEFAULT_LOCATIONS_UPDATE_END_TIME;
		}
		//Log.d(TAG, "Real Start time: " + startTime + " Real End time: " + endTime);
		//split them up and calculate offset since 00:00
		String[] splitStartTime = startTime.split(":");
		int startTimeOffset = Integer.parseInt(splitStartTime[0]) * DevelopmentSettings.MINUTES_IN_HOUR + Integer.parseInt(splitStartTime[1]);
		String[] splitEndTime = endTime.split(":");
		int endTimeOffset = Integer.parseInt(splitEndTime[0]) * DevelopmentSettings.MINUTES_IN_HOUR + Integer.parseInt(splitEndTime[1]);

		//get offset since 00:00 now DOES NOT WORK, UTC TIME from Date()
		long offsetNow = ((new Date()).getTime() / DevelopmentSettings.MILLISECONDS_IN_MINUTE % DevelopmentSettings.MINUTES_IN_DAY)
		- (new Date()).getTimezoneOffset();

		//Log.d(TAG, "Start time: " + startTimeOffset + " End time: " + endTimeOffset + " time now: " + offsetNow);
		//and check if the offset now is in between
		if (offsetNow >= startTimeOffset && offsetNow <= endTimeOffset) {
			//we are inside the allowed time interval
			return sp.getBoolean(ctx.getString(R.string.settings_locations_update_enabled_key), DEFAULT_LOCATIONS_UPDATE_ENABLED);
		}
		return false;
	}

	/**
	 * Returns what server url to use.
	 * @param ctx calling context
	 * @return the base url of the server
	 */
	static String getServerUrlbase(final Context ctx) {
		initPreferences(ctx);
		String prefUrlbase = sp.getString(ctx.getString(R.string.settings_server_urlbase_key), DevelopmentSettings.SERVER_URLBASE);
		if (prefUrlbase != null && !prefUrlbase.equals("")) {
			return prefUrlbase;
		}
		return DevelopmentSettings.SERVER_URLBASE;
	}
}

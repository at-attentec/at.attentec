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

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

/**
 * Interfaces with GPS and returns a location through callback.
 * @author David Granqvist
 * @author Malte Lenz
 * @author Johannes Nordkvist
 */
public class LocationHelper {

	/** Tag for logging. */
	private static final String TAG = "LocationHelper";

	/** Timer for waiting on GPS fix. */
	private Timer timeoutTimer;

	/** Location manager. */
	private LocationManager lm;

	/** Interface for sending back result. */
	private LocationResult locationResult;

	/** Save calling context for later use when location changes. */
	private Context mContext;

	/**
	 * Starts fetching a location.
	 * @param context	calling activity context
	 * @param result	interface for callback
	 */
	public final void getLocation(final Context context, final LocationResult result) {
		mContext = context;
		Log.d(TAG, "getLocation");

		//this must be initialized before we we create the listeners,
		//or NullPointerExceptions will occur when the GPS or network is fast.
		timeoutTimer = new Timer();
		timeoutTimer.schedule(new GetLastLocation(),  PreferencesHelper.getGPSTimeout(context));

		locationResult = result;
		if (lm == null) {
			lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		}

		try {
			Log.d(TAG, "Is GPS enabled: " + lm.isProviderEnabled(LocationManager.GPS_PROVIDER));
		} catch (Exception ex) {
			Log.w(TAG, "GPS is not available");
		}

		try {
			Log.d(TAG, "Is Network enabled: " + lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
		} catch (Exception ex) {
			Log.w(TAG, "Network is not available");
		}

		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListenerGPS, Looper.getMainLooper());

		lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListenerNetwork, Looper.getMainLooper());

		return;
	}

	/**
	 * Listener for GPS updates.
	 */
	private LocationListener locationListenerGPS = new LocationListener() {
		public void onLocationChanged(final Location location) {
			Log.d(TAG, "onLocationChanged GPS");
			if (location != null) {
				if (location.getAccuracy() < PreferencesHelper.getGPSAccuracyLimit(mContext)) {
					timeoutTimer.cancel();
					locationResult.gotLocation(location);
					lm.removeUpdates(locationListenerGPS);
					lm.removeUpdates(locationListenerNetwork);
				}
			}
		}

		public void onProviderDisabled(final String arg0) {
		}

		public void onProviderEnabled(final String arg0) {
		}

		public void onStatusChanged(final String arg0, final int arg1, final Bundle arg2) {
		}
	};

	/**
	 * Listener for Network updates.
	 */
	private LocationListener locationListenerNetwork = new LocationListener() {
		public void onLocationChanged(final Location location) {
			Log.d(TAG, "onLocationChanged network");
			if (location != null) {
				if (location.getAccuracy() < PreferencesHelper.getGPSAccuracyLimit(mContext)) {
					timeoutTimer.cancel();
					locationResult.gotLocation(location);
					lm.removeUpdates(locationListenerGPS);
					lm.removeUpdates(locationListenerNetwork);
				}
			}
		}

		public void onProviderDisabled(final String arg0) {
		}

		public void onProviderEnabled(final String arg0) {
		}

		public void onStatusChanged(final String arg0, final int arg1, final Bundle arg2) {
		}
	};

	/**
	 * Fetches last known location (timeout has happened)
	 * and return it.
	 * @author sommarjobb
	 *
	 */
	class GetLastLocation extends TimerTask {
		@Override
		public void run() {
			Log.d(TAG, "getLastLocation");
			lm.removeUpdates(locationListenerGPS);
			lm.removeUpdates(locationListenerNetwork);

			Location gpsLoc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);

			Location netLoc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

			if (gpsLoc != null) {
				locationResult.gotLocation(gpsLoc);
			} else if (netLoc != null && netLoc.getAccuracy() < DevelopmentSettings.NETWORK_ACCURACY_LIMIT) {
				locationResult.gotLocation(netLoc);
			}
		}
	}

	/**
	 * Interface for callback with result.
	 * @author sommarjobb
	 */
	public abstract static class LocationResult {
		/**
		 * Call back with the found location.
		 * @param location	found location
		 */
		public abstract void gotLocation(Location location);
	}

	@Override
	protected final void finalize() throws Throwable {
		Log.d(TAG, "finalize, removing updates");
		lm.removeUpdates(locationListenerGPS);
		lm.removeUpdates(locationListenerNetwork);
		super.finalize();
	}
}

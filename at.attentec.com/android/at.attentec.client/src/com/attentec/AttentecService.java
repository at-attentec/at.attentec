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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.ByteArrayBuffer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.location.Location;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.attentec.LocationHelper.LocationResult;
import com.attentec.Login.LoginException;

/**
 * Service that runs in the background, fetching new locations from
 * other people, updating the contact list, as well as uploading own
 * location to server.
 * @author David Granqvist
 * @author Malte Lenz
 * @author Johannes Nordkvist
 *
 */
public class AttentecService extends Service implements OnSharedPreferenceChangeListener {
	/** Intent for broadcasting that we have started or stopped. */
	public static final String COM_ATTENTEC_SERVICE_CHANGED_STATUS = "com.attentec.serviceChangedStatus";

	/** The length of datetime of format YYYY-MM-DD HH:MM:SS. */
	private static final int DATETIME_STRING_LENGTH = 19;

	/** Size of the input buffer that reads photos from server. */
	private static final int INPUT_BUFFER_SIZE = 128;

	/** Tag used for logging. */
	private static final String TAG = "AttentecService";

	/** Helper for database connection. */
	private DatabaseAdapter dbh;

	/** Timer for location updates from server. */
	private Timer locationTimer;

	/** Timer for contacts updates from server. */
	private Timer contactsTimer;

	/** Timer for fetching own location updates from GPS. */
	private Timer ownLocationTimer;

	/** Time in milliseconds between fetching remote contacts. */
	private static long contactsUpdateInterval;

	/** Connector for callback functions to contact list activity. */
	private static ServiceUpdateUIListener contactsUIUpdateListener;

	/** Connector for callback functions to close to you activity. */
	private static ServiceUpdateUIListener closeToYouUIUpdateListener;

	/** Tells if the service is currently started. */
	private static boolean isAlive = false;

	/** Manager for notifications in notification bar. */
	private NotificationManager mNM;

	/** Timestamp for last location sent to server. */
	private long lastUpdatedToServer;

	/** Format for parsing dates. */
	private SimpleDateFormat dfm = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	/** Listener for location updates (ourselves). */
	private Context ctx;

	/** Helper for fetching our own location. */
	private LocationHelper locationHelper;

	private Boolean previousLocationUpdateEnabled;

	/**
	 * Method run from ContactsActivity for connecting callbacks.
	 * @param l	listener from ContactsActivity
	 */
	public static void setContactsUpdateListener(final ServiceUpdateUIListener l) {
		contactsUIUpdateListener = l;
	}

	/**
	 * Method run from CloseToYou for connecting callbacks.
	 * @param l	listener from CloseToYou
	 */
	public static void setCloseToYouUpdateListener(final ServiceUpdateUIListener l) {
		closeToYouUIUpdateListener = l;
	}


	@Override
	public final void onCreate() {
		super.onCreate();
		Log.d(TAG, "onCreate");

		ctx = this;
		locationHelper = new LocationHelper();

		//register for changes in the preferences
		PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

		previousLocationUpdateEnabled = PreferencesHelper.getLocationUpdateEnabled(ctx);

		startService();
		//Set status to saved status
		SharedPreferences sp = getSharedPreferences("attentec_preferences", MODE_PRIVATE);
		Status myStatus = new Status();
		myStatus.load(sp);
		myStatus.sendToServer(sp, this);
	}

	@Override
	public final void onDestroy() {
		super.onDestroy();

		//Set status to offline
		SharedPreferences sp = getSharedPreferences("attentec_preferences", MODE_PRIVATE);
		Status myStatus = new Status();
		myStatus.load(sp);
		myStatus.updateStatus(Status.STATUS_OFFLINE, null);
		myStatus.sendToServer(sp, this);

		PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
		shutdownService();
	}

	/**
	 * Start recurring fetches of contacts and locations,
	 * and also update our own location at intervals.
	 */
	private void startService() {
		Log.d(TAG, "startService");
		//fetch the interval for getting contacts
		contactsUpdateInterval = PreferencesHelper.getContactsUpdateInterval(this);

		//set the time updated to server to a long time ago
		lastUpdatedToServer = new Date().getTime() - DevelopmentSettings.MILLISECONDS_IN_MINUTE * DevelopmentSettings.MINUTES_IN_DAY;

		//Display a notification about us starting.  We put an icon in the status bar.
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		showNotification();

		//initialize database contact
		dbh = new DatabaseAdapter(this);
		dbh.open();
		//get contacts every contactsUpdateInterval
		contactsTimer = new Timer();
		contactsTimer.scheduleAtFixedRate(
				new TimerTask() {
					public void run() {
						getContacts();
					}
				},
				0,
				contactsUpdateInterval);

		locationTimer = new Timer();
		locationTimer.scheduleAtFixedRate(
				new TimerTask() {
					public void run() {
						//get others locations
						getLocations();
					}
				},
				0,
				PreferencesHelper.getLocationsUpdateInterval(ctx));

		ownLocationTimer = new Timer();
		ownLocationTimer.scheduleAtFixedRate(
				new TimerTask() {
					public void run() {
						if (PreferencesHelper.getLocationUpdateEnabled(ctx)) {
							//get own location
							locationHelper.getLocation(ctx, locationResult);
							//check if we need to update the notification
						}
						updateNotificationIfNeeded();
					}
				},
				0,
				PreferencesHelper.getLocationsUpdateOwnInterval(ctx));

		isAlive = true;
		Intent serviceStartedIntent = new Intent(COM_ATTENTEC_SERVICE_CHANGED_STATUS);
		sendBroadcast(serviceStartedIntent);
	}

	/**
	 * Updates the notification if we went
	 * over a time limit for sending location and
	 * need to show something different.
	 */
	protected final void updateNotificationIfNeeded() {
		Log.d(TAG, "Old locUpEn: " + previousLocationUpdateEnabled + " new locUpEn: " + PreferencesHelper.getLocationUpdateEnabled(ctx));
		if (PreferencesHelper.getLocationUpdateEnabled(ctx) != previousLocationUpdateEnabled) {
			//remove the old notifications
			mNM.cancelAll();
			//show the new one
			showNotification();
		}
		previousLocationUpdateEnabled = PreferencesHelper.getLocationUpdateEnabled(ctx);
	}

	/**
	 * Stop all recurring tasks and location updates.
	 */
	private void shutdownService() {
		//close database connection
		dbh.close();
		//stop the syncing of contacts and location fetches
		locationTimer.cancel();
		ownLocationTimer.cancel();
		contactsTimer.cancel();
		//remove all notifications
		mNM.cancelAll();
		isAlive = false;
		Intent serviceStoppedIntent = new Intent(COM_ATTENTEC_SERVICE_CHANGED_STATUS);
		sendBroadcast(serviceStoppedIntent);
	}

	/**
	 * Callback connector for new location from GPS.
	 */
	private LocationResult locationResult = new LocationResult() {
		@Override
		public void gotLocation(final Location location) {
			//got location, handle it
			handleNewLocation(location);
		}
	};

	/**
	 * Saves and sends a new (our own) location to server.
	 * @param location new location
	 */
	private void handleNewLocation(final Location location) {
		Log.d(TAG, "handleNewLocation");
		if (location != null) {

			Double lat = location.getLatitude();
			Double lng = location.getLongitude();

			//save the location in preferences
			SharedPreferences sp = getSharedPreferences("attentec_preferences", MODE_PRIVATE);

			String username = sp.getString("username", "");
			String phoneKey = sp.getString("phone_key", "");

			Date d = new Date();

			SharedPreferences.Editor editor = sp.edit();
			editor.putFloat("latitude", lat.floatValue());
			editor.putFloat("longitude", lng.floatValue());
			editor.putLong("location_updated_at", d.getTime() / DevelopmentSettings.MILLISECONDS_IN_SECOND);
			editor.commit();

			//check if we need to update the server
			if (d.getTime() - lastUpdatedToServer < PreferencesHelper.getLocationsUpdateInterval(ctx) / 2) {
				Log.d(TAG, "Not updating location to server, to soon since last time: " + (d.getTime() - lastUpdatedToServer));
				return;
			}

			Log.d(TAG, "Updating location to server");
			//get logindata for server contact
			Hashtable<String, List<NameValuePair>> postdata = ServerContact.getLogin(username, phoneKey);

			//add location to POST data
			List<NameValuePair> locationdata = new ArrayList<NameValuePair>();
			locationdata.add(new BasicNameValuePair("latitude", lat.toString()));
			locationdata.add(new BasicNameValuePair("longitude", lng.toString()));
			postdata.put("location", locationdata);

			//send location to server
			String url = "/app/app_update_user_info.json";

			try {
				ServerContact.postJSON(postdata, url, ctx);
			} catch (LoginException e) {
				//Login was wrong, so close activity
				endAllActivities();
				return;
			}
			lastUpdatedToServer = new Date().getTime();
		}
	}

	/**
	 * get users Locations from server.
	 * @return true on success
	 */
	@SuppressWarnings("unchecked")
	public final boolean getLocations() {
		Log.d(TAG, "getLocations");

		//fetch locations from server
		SharedPreferences sp = getSharedPreferences("attentec_preferences", MODE_PRIVATE);
		String username = sp.getString("username", "");
		Hashtable<String, List<NameValuePair>> postdata = ServerContact.getLogin(username, sp.getString("phone_key", ""));

		JSONArray users = null;
		JSONObject js = null;
		ContentValues cv;

		try {
			js = ServerContact.postJSON(postdata, "/app/user_locations_and_status.json", ctx);
		} catch (LoginException e) {
			//wrong login, close activity and go back to login
			endAllActivities();
			return false;
		}
		if (js == null) {
			/**
			 * the contact with server failed for one of the following reasons:
			 * * Could not encode the posted data
			 * * HTTP request failed (IOException)
			 * * Could not decode the response
			 */
			return false;
		}


		try {
			users = js.getJSONArray("users");
		} catch (JSONException e) {
			Log.e(TAG, "Could not parse users from JSONstring");
			return false;
		}
		Log.d(TAG, "Have fetched nr of users: " + users.length());
		//go through all users to save them to database
		for (int i = 0; i < users.length(); i++) {
			cv = new ContentValues();
			Long id = null;
			try {
				JSONObject user = users.getJSONObject(i).getJSONObject("user");
				Iterator<String> it = user.keys();
				while (it.hasNext()) {
					String key = it.next();
					String value = user.getString(key);
					//Log.d(TAG, "user info, " + key + " => " + value);
					if (key.equals("location_updated_at") || key.equals("connected_at")) {
						value = cleanDate(value);
					}
					if (key.equals("id")) {
						id = Long.parseLong(value);
					} else {
						cv.put(key, value);
					}
				}
			} catch (JSONException e) {
				Log.e(TAG, "Could not parse user from JSONstring");
				return false;
			}

			//if nothing weird happened in the decode, save user to database,
			// but only if the user exists (may not have updated user list)
			if (id != null && dbh.userExists(id)) {
				dbh.updateUser(cv, id);
			} else {
				Log.e(TAG, "Could not add user location, server response contains no id, or user does not exist.");
			}
		}
		if (closeToYouUIUpdateListener != null) {
			closeToYouUIUpdateListener.updateUI();
		}
		if (contactsUIUpdateListener != null) {
			contactsUIUpdateListener.updateUI();
		}
		return true;
	}

	/**
	 * Fetch image from server.
	 * @param path to the image
	 * @return return png image as an ByteArray
	 * @throws GetImageException on failure of any kind
	 */
	private byte[] getImage(final String path) throws GetImageException {
		URL url;
		try {
			url = new URL(PreferencesHelper.getServerUrlbase(ctx) + path.replaceAll(" ", "%20"));
		} catch (MalformedURLException e) {
			throw new GetImageException("Could not parse URL: " + e.getMessage());
		}
		Log.d(TAG, "Getting image with URL: " + url.toString());

		//Open connection
		URLConnection ucon;
		try {
			ucon = url.openConnection();
		} catch (IOException e) {
			throw new GetImageException("Could not open URL-connection: " + e.getMessage());
		}

		//Get Image
		InputStream is;
		try {
			is = ucon.getInputStream();
		} catch (IOException e) {
			throw new GetImageException("Could not get InputStream: " + e.getMessage());
		}
		//Setup buffers
		BufferedInputStream bis = new BufferedInputStream(is, INPUT_BUFFER_SIZE);
		ByteArrayBuffer baf = new ByteArrayBuffer(INPUT_BUFFER_SIZE);

		//Put content into bytearray
		int current = 0;
		try {
			while ((current = bis.read()) != -1) {
				baf.append((byte) current);
			}
		} catch (IOException e) {
			throw new GetImageException("Could not read from BufferedInputStream(bis): " + e.getMessage());
		}

		return baf.toByteArray();
	}

	/**
	 * GetImageException is raised when a getImage failed.
	 * @author David Granqvist
	 * @author Malte Lenz
	 * @author Johannes Nordkvist
	 */
	public static class GetImageException extends Exception {
		/** Used for serialization. */
		private static final long serialVersionUID = 1L;

		/** Exception for when getting image from server fails. */
		public GetImageException() {

		}

		/**
		 * Exception for when getting image from server fails with description.
		 * @param description of the error
		 */
		public GetImageException(final String description) {
			super(description);
		}
	}

	/**
	 * Parse one user from JSONObject and save to database.
	 * @param user the JSONObject to parse
	 * @param username the username of ourselves
	 * @return which userid whas updated
	 * @throws JSONException if parsing failed
	 */
	@SuppressWarnings("unchecked")
	private String parseJSONUser(final JSONObject user, final String username) throws JSONException {
		//Log.d(TAG, "parsing JSON user");
		ContentValues cv = new ContentValues();
		String updated = new String();
		//go over all fields
		Iterator<String> it = user.keys();
		boolean isSelf = false;
		while (it.hasNext()) {
			String key = it.next();
			String value = null;
			try {
				value = user.getString(key);
			} catch (JSONException e) {
				Log.e(TAG, "JSONException user field: " + key);
				throw e;
			}

			if (key.equals("id")) {
				key = "_id";
				updated = value;
			} else if (key.equals("photo_url")) {
				//check if there is a valid photo
				String newPhotoUpdatedAt = null;
				try {
					newPhotoUpdatedAt = cleanDate(user.getString("photo_updated_at"));
				} catch (JSONException e) {
					Log.e(TAG, "Could not parse photo_updated_at.");
					continue;
				}

				if (newPhotoUpdatedAt == null) {
					//no photo exists, so we don't need to save one
					//Log.d(TAG, "No photo exists");
					continue;
				}

				Long userId = null;
				try {
					userId = Long.parseLong(user.getString("id"));
				} catch (NumberFormatException e) {
					Log.e(TAG, "Could not parse user id from server.");
					continue;
				} catch (JSONException e3) {
					Log.e(TAG, "Could not parse user id from server.");
					continue;
				}

				//check if the user already exists
				if (!dbh.userExists(userId)) {
					//this is a new user, we can get a new image
					try {
						cv.put(DatabaseAdapter.KEY_PHOTO, getImage(value));
					} catch (GetImageException e) {
						Log.e(TAG, "Could not get image: " + e.getMessage());
						continue;
					}
				}

				//we want to check if this photo is newer than the one we have now

				//parse the date
				dfm.setLenient(true);
				Date newTimestamp = new Date();
				try {
					newTimestamp = dfm.parse(newPhotoUpdatedAt);
				} catch (ParseException e1) {
					Log.e(TAG, "Could not parse newPhotoUpdatedAt: " + newPhotoUpdatedAt);
					continue;
				}

				String oldPhotoUpdatedAt = dbh.getContactPhotoUpdatedAt(userId);

				Date oldTimestamp = new Date();
				if (oldPhotoUpdatedAt != null) {
					//parse the date
					try {
						oldTimestamp = (dfm.parse(oldPhotoUpdatedAt));
					} catch (ParseException e) {
						Log.e(TAG, "Could not parse oldPhotoUpdatedAt: " + oldPhotoUpdatedAt);
					}
				}

				if ((oldTimestamp.getTime() != newTimestamp.getTime())) {
					Log.d(TAG, "There is a new photo, downloading...");
					try {
						cv.put(DatabaseAdapter.KEY_PHOTO, getImage(user.get("photo_url").toString()));
					} catch (JSONException e) {
						Log.e(TAG, "Could not parse photo_url.");
					} catch (GetImageException e) {
						Log.e(TAG, "Could not get image: " + e.getMessage());
					}
				}
			} else if (key.equals("username") && value.equals(username)) {
				//save that this is ourself, we don't want to save ourselves
				isSelf = true;
				updated = null;
				break;
			} else if (key.equals(DatabaseAdapter.KEY_LOCATION_UPDATED_AT)
					|| key.equals(DatabaseAdapter.KEY_CONNECTED_AT)
					|| key.equals(DatabaseAdapter.KEY_PHOTO_UPDATED_AT)) {
				value = cleanDate(value);
			}

			if (key != "photo_url") {
				cv.put(key, value);
			}
		}

		//check if we are ourselves
		if (!isSelf) {
			//Log.d(TAG, "Saving user: " + cv.toString());
			Long existingrowId = cv.getAsLong(DatabaseAdapter.KEY_ROWID);
			cv.remove("photo_url");
			if (!dbh.userExists(existingrowId)) {
				//save the user to the database
				dbh.addUser(cv);
			} else {
				//update the user in the database
				dbh.updateUser(cv, existingrowId);
			}
		}
		return updated;
	}

	/**
	 * Fetches contacts from server.
	 * @return true if successful, false otherwise
	 */
	public final boolean getContacts() {
		//get login data for server contact
		SharedPreferences sp = getSharedPreferences("attentec_preferences", MODE_PRIVATE);
		String username = sp.getString("username", "");
		Hashtable<String, List<NameValuePair>> postdata = ServerContact.getLogin(username, sp.getString("phone_key", ""));

		JSONObject js = null;
		try {
			js = ServerContact.postJSON(postdata, "/app/contactlist.json", ctx);
		} catch (LoginException e) {
			//wrong login, close application
			endAllActivities();
			return false;
		}
		if (js == null) {
			/**
			 * the contact with server failed for one of the following reasons:
			 * * Could not encode the posted data
			 * * HTTP request failed (IOException)
			 * * Could not decode the response
			 */
			return false;
		}
		//fetch the users object from the response
		JSONArray users = null;
		try {
			users = js.getJSONArray("users");
		} catch (JSONException e) {
			Log.e(TAG, "Error in JSON decode of users array");
			return false;
		}

		//update all users in database
		ArrayList<String> updated = new ArrayList<String>();
		for (int i = 0; i < users.length(); i++) {
			JSONObject user;
			try {
				user = users.getJSONObject(i).getJSONObject("user");
			} catch (JSONException e) {
				Log.e(TAG, "JSONException on decoding user object");
				return false;
			}

			String updatedId = null;
			try {
				updatedId = parseJSONUser(user, username);
			} catch (JSONException e) {
				//Failed to parse a user
				Log.e(TAG, "Failed to parse a user.");
				return false;
			}

			if (updatedId != null) {
				updated.add(updatedId);
			}
		}

		//remove all entries in the database that were not updated (people quitting)
		ArrayList<String> existing = dbh.getUserIds();
		Iterator<String> it = existing.iterator();
		while (it.hasNext()) {
			String key = it.next();
			if (key != null && !updated.contains(key)) {
				dbh.deleteUser(Long.parseLong(key));
			}
		}

		//call callback in main thread to refresh the list
		if (contactsUIUpdateListener != null) {
			contactsUIUpdateListener.updateUI();
		}
		return true;
	}

	/**
	 * Removes T and ending +2:00 from time fetched from server.
	 * @param date that should be cleaned
	 * @return cleaned date
	 */
	private String cleanDate(final String date) {
		String newDate = null;
		if (!date.equals("null") && !date.equals("")) {
			newDate = date.replace('T', ' ');
			newDate = newDate.substring(0, DATETIME_STRING_LENGTH);
		}
		return newDate;
	}

	@Override
	public final IBinder onBind(final Intent arg0) {
		return null;
	}

	/**
	 * This is the interface we use for callbacks to the activity.
	 * @author David Granqvist
	 * @author Malte Lenz
	 * @author Johannes Nordkvist
	 */
	public interface ServiceUpdateUIListener {
		/** Tell UI to update. */
		void updateUI();

		/** End UI Activity. */
		void endActivity();
	}

	/**
	 * End all activities.
	 */
	private void endAllActivities() {
		if (contactsUIUpdateListener != null) {
			contactsUIUpdateListener.endActivity();
		}
		if (closeToYouUIUpdateListener != null) {
			closeToYouUIUpdateListener.endActivity();
		}
	}

	/**
	 * shows a notification in the status bar.
	 */
	private void showNotification() {
		//text for display in status bar
		CharSequence text;
		if (PreferencesHelper.getLocationUpdateEnabled(ctx)) {
			text = getText(R.string.service_started);
		} else {
			text = getText(R.string.service_started_no_position);
		}

		//get icon for display status bar
		Notification notification = new Notification(R.drawable.statusbar_icon, text,
				System.currentTimeMillis());

		//Intent to launch on click in notification list
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, Attentec.class), 0);

		//set notification settings
		notification.setLatestEventInfo(this, getText(R.string.service_label),
				text, contentIntent);

		//Send notification
		if (PreferencesHelper.getLocationUpdateEnabled(ctx)) {
			//Log.d(TAG, "Showing notifications: with locations");
			mNM.notify(R.string.service_started, notification);
		} else {
			//Log.d(TAG, "Showing notifications: no locations");
			mNM.notify(R.string.service_started_no_position, notification);
		}
	}

	/**
	 * Check if service is alive.
	 * @return true if service is alive
	 */
	public static boolean getIsAlive() {
		return isAlive;
	}

	/**
	 * Gets called when a preference is changed. We need to restart the service then.
	 * @param sp preferences
	 * @param key key that was changed
	 */
	public final void onSharedPreferenceChanged(final SharedPreferences sp, final String key) {
		Log.d(TAG, "Preferences changed, restarting service");
		shutdownService();
		startService();
	}
}

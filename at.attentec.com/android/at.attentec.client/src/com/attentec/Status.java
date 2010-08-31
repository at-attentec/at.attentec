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

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.attentec.Login.LoginException;

/**
 * Handles the status of the user.
 * @author David Granqvist
 * @author Malte Lenz
 * @author Johannes Nordkvist
 *
 */
public class Status extends Observable implements Cloneable {

	private static final String STATUS_CUSTOM_MESSAGE = "status_custom_message";
	private static final String STATUS = "status";

	/** Tag used for logging. */
	private static final String TAG = "Attentec->Status";

	private int mStatus;
	private String mCustomMessage = null;

	/** The maximum length allowed for custom messages. */
	public static final int CUSTOM_MESSAGE_MAXIMUM_LENGTH = 25;

	/** Status for being offline. */
	public static final int STATUS_OFFLINE = 1;
	/** Status for being online. */
	public static final int STATUS_ONLINE = 2;
	/** Status for being online. */
	public static final int STATUS_DO_NOT_DISTURB = 3;
	/** Status for not being available. */
	public static final int STATUS_NOT_AVAILABLE = 4;
	/** Status for being away. */
	public static final int STATUS_AWAY = 5;
	/** Status for being invisible, not implemented yet. */
	public static final int STATUS_INVISIBLE = 6;

	/** Status for being offline. */
	public static final String STATUS_OFFLINE_STRING = "offline";
	/** Status for being online. */
	public static final String STATUS_ONLINE_STRING = "online";
	/** Status for being online. */
	public static final String STATUS_DO_NOT_DISTURB_STRING = "do_not_disturb";
	/** Status for not being available. */
	public static final String STATUS_NOT_AVAILABLE_STRING = "not_available";
	/** Status for being away. */
	public static final String STATUS_AWAY_STRING = "away";
	/** Status for being invisible, not implemented yet.*/
	public static final String STATUS_INVISIBLE_STRING = "invisible";

	/**
	 * Create a Status object.
	 */
	public Status() { }

	/**
	 * Copy Constructor, skips observables.
	 * @param myStatus To to be copied from.
	 */
	private Status(final Status myStatus) {
		this.mStatus = myStatus.mStatus;
	}


	/**
	 * Update status and status custom message.
	 * @param status can be all of the available status constants i.e. MyStatus.STATUS_AWAY etc.
	 * @param customMessage Custom message of a maximum of 15 letters.
	 * @return true on Success
	 */
	public final boolean updateStatus(final Integer status, final String customMessage) {
		boolean statusChanged = false;
		if (validateStatus(status)) {
			mStatus = status;
			statusChanged = true;
		}
		if (validateStatusCustomMessage(customMessage)) {
			mCustomMessage = customMessage;
			statusChanged = true;
		}
		if (statusChanged) {
			notifyObservers();
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Validate custom message is of correct format.
	 * @param customMessage the custom message
	 * @return true on success
	 */
	private boolean validateStatusCustomMessage(final String customMessage) {
		return (customMessage != null)
			&& (customMessage.length() <= CUSTOM_MESSAGE_MAXIMUM_LENGTH);
	}

	@Override
	public final void notifyObservers() {
		setChanged();
		super.notifyObservers();
	}

	/**
	 * Validate that status is of correct format.
	 * @param status the status
	 * @return true on success
	 */
	private boolean validateStatus(final Integer status) {
		return (status != null)
			&& (status >= STATUS_OFFLINE)
			&& (status <= STATUS_INVISIBLE);
	}

	/**
	 * Get the current set status as int.
	 * @return the current status
	 */
	public final int getStatus() {
		return mStatus;
	}

	/**
	 * Get the current set status as string.
	 * @return the current status
	 */
	public final String getStatusAsString() {
		switch (mStatus) {
		case STATUS_OFFLINE:
			return STATUS_OFFLINE_STRING;

		case STATUS_ONLINE:
			return STATUS_ONLINE_STRING;

		case STATUS_DO_NOT_DISTURB:
			return STATUS_DO_NOT_DISTURB_STRING;

		case STATUS_NOT_AVAILABLE:
			return STATUS_NOT_AVAILABLE_STRING;

		case STATUS_AWAY:
			return STATUS_AWAY_STRING;

		case STATUS_INVISIBLE:
			return STATUS_INVISIBLE_STRING;

		default:
			break;
		}
		return "getStatusAsStringFailure";
	}

	/**
	 * Creates an Status object with the status given by the statusString.
	 * statusString should be one of the Status.STATUS_*_STRING constants.
	 * @param statusString the status as a string
	 * @return a New status instance with status given by the string string.
	 */
	public static Status buildStatusFromString(final String statusString) {
		Status status = new Status();
		if (statusString.equals(STATUS_ONLINE_STRING)) {
			status.updateStatus(STATUS_ONLINE, null);
		} else if (statusString.equals(STATUS_DO_NOT_DISTURB_STRING)) {
			status.updateStatus(STATUS_DO_NOT_DISTURB, null);
		} else if (statusString.equals(STATUS_NOT_AVAILABLE_STRING)) {
			status.updateStatus(STATUS_NOT_AVAILABLE, null);
		} else if (statusString.equals(STATUS_AWAY_STRING)) {
			status.updateStatus(STATUS_AWAY, null);
		} else if (statusString.equals(STATUS_INVISIBLE_STRING)) {
			status.updateStatus(STATUS_INVISIBLE, null);
		} else { //Offline
			status.updateStatus(STATUS_OFFLINE, null);
		}
		return status;
	}

	/**
	 * Get the correct resource ID for circle that with a color that represents the current status.
	 * @return Resource ID for status circle.
	 */
	public final int getStatusCircleResourceId() {
		int status;
		switch (mStatus) {
		case STATUS_ONLINE:
			status =  R.drawable.status_online_circle;
			break;

		case STATUS_DO_NOT_DISTURB:
			status = R.drawable.status_do_not_disturb_circle;
			break;


		case STATUS_NOT_AVAILABLE:
			status = R.drawable.status_not_available_circle;
			break;

		case STATUS_AWAY:
			status = R.drawable.status_away_circle;
			break;

		case STATUS_INVISIBLE:
			status = R.drawable.status_invisible_circle;
			break;

		case STATUS_OFFLINE:
		default:
			status = R.drawable.status_offline_circle;
			break;
		}

//		return mCtx.getResources().getDrawable(status);
		return status;
	}


	/**
	 * Get the Resource ID for the current set status as a string for humans (translation to current language).
	 * @return translated status resource ID
	 */
	public final int getStatusAsHumanStringResourceId() {
		int status = 0;
		switch (mStatus) {
		case STATUS_ONLINE:
			status = R.string.status_online;
			break;

		case STATUS_DO_NOT_DISTURB:
			status = R.string.status_do_not_disturb;
			break;


		case STATUS_NOT_AVAILABLE:
			status = R.string.status_not_available;
			break;

		case STATUS_AWAY:
			status = R.string.status_away;
			break;

		case STATUS_INVISIBLE:
			status = R.string.status_invisible;
			break;

		case STATUS_OFFLINE:
		default:
			status = R.string.status_offline;
			break;
		}
//		return mCtx.getResources().getString(status);
		return status;
	}

	/**
	 * Get the custom message if set.
	 * @return custom message
	 */
	public final String getCustomMessage() {
		return mCustomMessage;
	}

	/**
	 * Save Status to preferences and server.
	 * @param sp Shared Preferences to save to.
	 * @param ctx calling context
	 * @return true on success.
	 */
	public final boolean save(final SharedPreferences sp, final Context ctx) {
		if (sp == null) {
			Log.e(TAG, "SharedPreferences is null in save.");
			return false;
		}

		// save the status in preferences
		sp.edit().putString(STATUS_CUSTOM_MESSAGE, mCustomMessage).putInt(STATUS, mStatus).commit();

		//Sent to server
		return sendToServer(sp, ctx);
	}

	/**
	 * Send status to server.
	 * @param sp Shared Preferences where username and phonekey are stored.
	 * @param ctx calling context
	 * @return true on success
	 */
	public final boolean sendToServer(final SharedPreferences sp, final Context ctx) {
		//Save to server.
		//get logindata for server contact
		String username = sp.getString("username", "");
		String phoneKey = sp.getString("phone_key", "");
		Hashtable<String, List<NameValuePair>> postdata = ServerContact.getLogin(username, phoneKey);

		//add location to POST data
		List<NameValuePair> statusData = new ArrayList<NameValuePair>();
		statusData.add(new BasicNameValuePair("status", this.getStatusAsString()));
		statusData.add(new BasicNameValuePair("status_custom_message", this.mCustomMessage));
		postdata.put("status", statusData);

		//send location to server
		String url = "/app/app_update_user_info.json";
		try {
			ServerContact.postJSON(postdata, url, ctx);
		} catch (LoginException e) {
			Log.e(TAG, "Could not send status to server");
			return false;
		}
		return true;
	}

	/**
	 * Load Status from preferences.
	 * @param sp SharedPreferences to load from
	 * @return true on success
	 */
	public final boolean load(final SharedPreferences sp) {
		if (sp == null) {
			Log.e(TAG, "SharedPreferences is null in load.");
			return false;
		}
		//Load the status from preferences
		mCustomMessage = sp.getString(STATUS_CUSTOM_MESSAGE, "");
		mStatus = sp.getInt(STATUS, Status.STATUS_ONLINE);
		notifyObservers();
		return true;
	}

	@Override
	public final Status clone() throws CloneNotSupportedException {
		return new Status(this);
	}

	/**
	 * Create an iterator that can iterate through all status types.
	 * @return an iterator
	 */
	public final StatusIterator getIterator() {
		return new StatusIterator(this);
	}

	/**
	 * Iterator for iterating through Status type.
	 * @author David Granqvist
	 * @author Malte Lenz
	 * @author Johannes Nordkvist
	 *
	 */
	class StatusIterator implements Iterator<Status> {
		//Start on online instead of offline since it is weird to set you own status to offline when you are online.
		private int current = STATUS_ONLINE;
		private Status status;

		/**
		 * Create an iterator.
		 * @param myStatus the status that should be cloned for iterating through.
		 */
		public StatusIterator(final Status myStatus) {
			try {
				status = myStatus.clone();
			} catch (CloneNotSupportedException e) {
				Log.e(TAG, "StatusIterator can't clone MyStatus:" + e.getMessage());
			}
		}

		/**
		 * Checks if there are any more status type to fetch.
		 * @return true if has next
		 */
		public boolean hasNext() {
			return current <= Status.STATUS_AWAY;
		}

		/**
		 * Get the next status.
		 * @return next status
		 */
		public Status next() {
			if (hasNext()) {
				status.updateStatus(current++, "");
				return status;
			} else {
				return null;
			}
		}

		/**
		 * Is not implemented.
		 */
		public void remove() {
			//DO nothing
		}
	}

}

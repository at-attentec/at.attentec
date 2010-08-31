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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.Semaphore;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.location.LocationManager;
import android.text.format.DateFormat;
import android.util.Log;

/**
 * Handles connections to the SQLite database.
 * @author David Granqvist
 * @author Malte Lenz
 * @author Johannes Nordkvist
 *
 */
public class DatabaseAdapter {
	/**
	 * Tag used for logging.
	 */
	private static final String TAG = "Attentec->DatabaseAdapter";

	/** Column name for id. */
	public static final String KEY_ROWID = "_id";

	/** Column name for username. */
	public static final String KEY_USERNAME = "username";
	/** Column name for first name. */
	public static final String KEY_FIRST_NAME = "first_name";
	/** Column name for last name. */
	public static final String KEY_LAST_NAME = "last_name";
	/** Column name for address. */
	public static final String KEY_ADDRESS = "address";
	/** Column name for zipcode. */
	public static final String KEY_ZIPCODE = "zipcode";
	/** Column name for city. */
	public static final String KEY_CITY = "city";
	/** Column name for phone. */
	public static final String KEY_PHONE = "phone";
	/** Column name for email. */
	public static final String KEY_EMAIL = "email";
	/** Column name for latitude. */
	public static final String KEY_LATITUDE = "latitude";
	/** Column name for longitude. */
	public static final String KEY_LONGITUDE = "longitude";
	/** Column name for when location is updated. */
	public static final String KEY_LOCATION_UPDATED_AT = "location_updated_at";
	/** Column name for scholar degree. */
	public static final String KEY_DEGREE = "degree";
	/** Column name for title in the company. */
	public static final String KEY_TITLE = "title";
	/** Column name for URL to linkedin. */
	public static final String KEY_LINKEDIN = "linkedin_url";
	/** Column name for current client. */
	public static final String KEY_CLIENT = "client";
	/** Column name for last time contacted at timestamp. */
	public static final String KEY_CONTACTED_AT = "contacted_at";
	/** Column name for distance from this cellphone. */
	public static final String KEY_DISTANCE = "distance";
	/** Column name for photo of the user. */
	public static final String KEY_PHOTO = "photo";
	/** Column name for when photo was updated. */
	public static final String KEY_PHOTO_UPDATED_AT = "photo_updated_at";
	/** Column name for user status. */
	public static final String KEY_STATUS = "status";
	/** Column name for user custom message status. */
	public static final String KEY_CUSTOM_STATUS = "status_custom_message";
	/** Column name for users last contact time to server. */
	public static final String KEY_CONNECTED_AT = "connected_at";

	//sorting of the contact list
	/** Constant for ordering contact list by last contact time. */
	public static final int ORDER_BY_CONTACTED_AT = 0;
	/** Constant for ordering contact list by first name. */
	public static final int ORDER_BY_FIRST_NAME = 1;
	/** Constant for ordering contact list by last name. */
	public static final int ORDER_BY_LAST_NAME = 2;
	/** Constant for ordering contact list by distance from this cellphone to other user. */
	public static final int ORDER_BY_DISTANCE = 3;

	/** Database helper instance. */
	private DatabaseHelper mDbHelper;
	/** SQLlite helper instance. */
	private SQLiteDatabase mDb;

	/** Database creation SQL statements. */
	private static final String DATABASE_CREATE_USERS =
		"create table users ("
		+ KEY_ROWID                  + " integer primary key, "
		+ KEY_USERNAME               + " text not null, "
		+ KEY_FIRST_NAME             + " text not null, "
		+ KEY_LAST_NAME              + " text not null, "
		+ KEY_ADDRESS                + " text, "
		+ KEY_ZIPCODE                + " integer, "
		+ KEY_CITY                   + " text, "
		+ KEY_PHONE                  + " text, "
		+ KEY_EMAIL                  + " text not null, "
		+ KEY_DEGREE                 + " text, "
		+ KEY_TITLE                  + " text, "
		+ KEY_LINKEDIN               + " text, "
		+ KEY_CLIENT                 + " text, "
		+ KEY_LATITUDE               + " float, "
		+ KEY_LOCATION_UPDATED_AT    + " datetime, "
		+ KEY_LONGITUDE              + " float,"
		+ KEY_CONTACTED_AT           + " text,"
		+ KEY_PHOTO                  + " blob,"
		+ KEY_PHOTO_UPDATED_AT       + " datetime,"
		+ KEY_STATUS                 + " text,"
		+ KEY_CUSTOM_STATUS          + " text,"
		+ KEY_DISTANCE               + " float,"
		+ KEY_CONNECTED_AT           + " datetime"
		+ ");";

	/** The default name of the database. */
	private static final String DATABASE_NAME = "data";
	/** The name of the table that stores the users.*/
	private static final String DATABASE_TABLE_USERS = "users";

	/** Increase this number when changing the database schema! */
	private static final int DATABASE_VERSION = 22;

	/** The context that the databaseAdapter is used in.*/
	private final Context mCtx;
	/** The name of the database. */
	private String databaseName = DATABASE_NAME;

	private boolean databaseIsOpen = false;

	/**
	 * Use this semaphore everywhere the database is accessed.
	 * Assures both that we don't access a closed database,
	 * and that only one at a time can access the database.
	 */
	private static final Semaphore USE_DB = new Semaphore(1, true);

	/** Date format used for parsing. */
	public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

//	private static final int NR_OF_TRIES_FOR_LOCK = 5;

//	private static final long MILLISECONDS_BETWEEN_TRIES = 100;

	/**
	 * Database helper that is used to communcate with SQLite database.
	 * @author David Granqvist
	 * @author Malte Lenz
	 * @author Johannes Nordkvist
	 *
	 */
	private static class DatabaseHelper extends SQLiteOpenHelper {

		/**
		 * Create a new DatabaseHelper with a certain database.
		 * @param context the context
		 * @param databaseName name of the database
		 */
		public DatabaseHelper(final Context context, final String databaseName) {
			super(context, databaseName, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(final SQLiteDatabase db) {
			db.execSQL(DATABASE_CREATE_USERS);
		}

		/**
		 * Upgrade the database.
		 * @param db the sqLite database
		 */
		private void upgrade(final SQLiteDatabase db) {
			db.execSQL("DROP TABLE IF EXISTS users");
			onCreate(db);
		}

		/**
		 * Clear the database for test purpose.
		 */
		public void clear() {
			upgrade(this.getWritableDatabase());
		}

		@Override
		public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
					+ newVersion + ", which will destroy all old data");
			upgrade(db);
		}
	}

	/**
	 * Constructor - takes the context to allow the database to be
	 * opened/created.
	 *
	 * @param ctx the Context within which to work
	 */
	public DatabaseAdapter(final Context ctx) {
		this.mCtx = ctx;
	}

	/**
	 * Creates a DatabaseAdapter with a custom database (used for testing).
	 * @param ctx Context
	 * @param inDatabaseName name of the database that should be used.
	 */
	public DatabaseAdapter(final Context ctx, final String inDatabaseName) {
		this.mCtx = ctx;
		this.databaseName = inDatabaseName;
	}

	/**
	 * Clear the database for test purposes.
	 */
	public final void clear() {
		this.mDbHelper.clear();
	}

	/**
	 * Create a new instance of the database.
	 *
	 * @return this (self reference, allowing this to be chained in an
	 *         initialization call)
	 */
	public final DatabaseAdapter open() {
		try {
			USE_DB.acquire();
		} catch (InterruptedException e) {
			Log.w(TAG, "Could not acquire database access.");
			return this;
		}
		mDbHelper = new DatabaseHelper(mCtx, databaseName);
		mDb = mDbHelper.getWritableDatabase();
		databaseIsOpen  = true;
		USE_DB.release();
		return this;
	}

	/**
	 * Closes the database.
	 */
	public final void close() {
		try {
			USE_DB.acquire();
		} catch (InterruptedException e) {
			Log.w(TAG, "Could not acquire database access.");
			return;
		}
		databaseIsOpen = false;
		mDbHelper.close();
		USE_DB.release();
	}

	/**
	 * Acquires a lock on the database if it is open.
	 * @return if lock was acquired
	 */
	private boolean acquireDbLock() {
		try {
			USE_DB.acquire();
		} catch (InterruptedException e) {
			Log.w(TAG, "Could not acquire database access.");
			return false;
		}
		if (!databaseIsOpen) {
			USE_DB.release();
			return false;
		}
		return true;
	}

	/**
	 * Deletes a user with the specified id.
	 * @param id row id to delete
	 * @return if successful
	 */
	public final boolean deleteUser(final long id) {
		if (!acquireDbLock()) {
			return false;
		}
		boolean ret = mDb.delete(DATABASE_TABLE_USERS, KEY_ROWID + "=" + id, null) == 1;
		USE_DB.release();
		return ret;
	}

	/**
	 * Returns a Cursor with the Content of the database.
	 * @param orderBy order indentifer
	 * @return cursor with users
	 */
	public final Cursor getContent(final Integer orderBy) {

		String s = new String();

		int mOrderBy = 2;
		if (orderBy != null) {
			mOrderBy = orderBy;
		}
		switch (mOrderBy) {

		case ORDER_BY_CONTACTED_AT:
			s = KEY_CONTACTED_AT + " DESC";
			break;
		case ORDER_BY_FIRST_NAME:
			s = KEY_FIRST_NAME + " ASC";
			break;
		case ORDER_BY_LAST_NAME:
			s = KEY_LAST_NAME + " ASC";
			break;
		case ORDER_BY_DISTANCE:
			//the ISNULL check is to get ones with null after ones with a distance
			s = KEY_DISTANCE + " ISNULL, " + KEY_DISTANCE + " ASC";
			break;
		default:
			break;

		}

		if (!acquireDbLock()) {
			return null;
		}
		Cursor ret = mDb.query(
				DATABASE_TABLE_USERS,
				new String[] {
						KEY_ROWID,
						KEY_USERNAME,
						KEY_FIRST_NAME,
						KEY_LAST_NAME,
						KEY_ADDRESS,
						KEY_ZIPCODE,
						KEY_CITY,
						KEY_PHONE,
						KEY_EMAIL,
						KEY_DEGREE,
						KEY_TITLE,
						KEY_LINKEDIN,
						KEY_CLIENT,
						KEY_LATITUDE,
						KEY_LONGITUDE,
						KEY_LOCATION_UPDATED_AT,
						KEY_CONTACTED_AT,
						KEY_PHOTO,
						KEY_PHOTO_UPDATED_AT,
						KEY_STATUS,
						KEY_CUSTOM_STATUS,
						KEY_CONNECTED_AT,
						KEY_DISTANCE},
						null, null, null, null, s);
		USE_DB.release();
		return ret;
	}

	/**
	 * Returns all contacts in a radius of m meters.
	 * @param m meters distance
	 * @return cursor with contacts
	 */
	public final Cursor getContactsInsideRadius(final Integer m) {
		//Log.d(TAG, "getting with radius: " + m);
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MILLISECOND, -ContactsActivity.TIME_INTERVAL_ONLINE_MILLISECONDS);

		String whereClause = KEY_DISTANCE + " < " + m + " AND "
				+ KEY_LATITUDE + " IS NOT NULL AND "
				+ KEY_LONGITUDE + " IS NOT NULL AND "
				+ KEY_LATITUDE + "!='' AND "
				+ KEY_LONGITUDE + "!='' AND "
				+ KEY_LATITUDE + "!='null' AND "
				+ KEY_LONGITUDE + "!='null' AND "
				+ KEY_LOCATION_UPDATED_AT + " IS NOT NULL AND "
				+ KEY_LOCATION_UPDATED_AT + "> Datetime('" + DATE_FORMAT.format(cal.getTime()) + "')";

		if (!acquireDbLock()) {
			return null;
		}

		Cursor ret = mDb.query(
				DATABASE_TABLE_USERS,
				new String[] {
						KEY_ROWID,
						KEY_USERNAME,
						KEY_FIRST_NAME,
						KEY_LAST_NAME,
						KEY_ADDRESS,
						KEY_ZIPCODE,
						KEY_CITY,
						KEY_PHONE,
						KEY_EMAIL,
						KEY_DEGREE,
						KEY_TITLE,
						KEY_LINKEDIN,
						KEY_CLIENT,
						KEY_LATITUDE,
						KEY_LONGITUDE,
						KEY_LOCATION_UPDATED_AT,
						KEY_CONTACTED_AT,
						KEY_PHOTO,
						KEY_PHOTO_UPDATED_AT,
						KEY_STATUS,
						KEY_CUSTOM_STATUS,
						KEY_CONNECTED_AT,
						KEY_DISTANCE},
						whereClause ,
						null, null, null, KEY_DISTANCE + " ASC");
		USE_DB.release();
		return ret;
	}


	/**
	 * Returns a cursor for a specified users locations.
	 * @param rowId
	 * @return cursor to handle locations
	 */
	public final Cursor getLocations() {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MILLISECOND, -ContactsActivity.TIME_INTERVAL_ONLINE_MILLISECONDS);

		if (!acquireDbLock()) {
			return null;
		}

		//Ordered By latitude to get balloon to overlay each other from north to south.
		Cursor ret = mDb.query(
				DATABASE_TABLE_USERS,
				new String[] {KEY_ROWID,
						KEY_USERNAME,
						KEY_FIRST_NAME,
						KEY_LAST_NAME,
						KEY_PHONE,
						KEY_EMAIL,
						KEY_DEGREE,
						KEY_TITLE,
						KEY_LINKEDIN,
						KEY_CLIENT,
						KEY_LATITUDE,
						KEY_LONGITUDE,
						KEY_PHOTO,
						KEY_STATUS,
						KEY_PHOTO_UPDATED_AT,
						KEY_CONNECTED_AT,
						KEY_LOCATION_UPDATED_AT},
						KEY_LATITUDE + " IS NOT NULL AND "
						+ KEY_LONGITUDE + " IS NOT NULL AND "
						+ KEY_LATITUDE + "!='' AND "
						+ KEY_LONGITUDE + "!='' AND "
						+ KEY_LATITUDE + "!='null' AND "
						+ KEY_LONGITUDE + "!='null' AND "
						+ KEY_LOCATION_UPDATED_AT + " IS NOT NULL AND "
						+ KEY_LOCATION_UPDATED_AT + "> Datetime('" + DATE_FORMAT.format(cal.getTime()) + "')",
						null,
						null,
						null,
						KEY_LATITUDE + " DESC");
		USE_DB.release();
		return ret;

	}


	/**
	 * Compare my location with contacts locations and update the distance between me and the contacts,
	 * when my location is present.
	 * @param lng Longitude of my location
	 * @param lat Latitude of my location
	 * @throws ParseException if not possible to parse contact coordinates
	 */
	public final void updateDistances(final double lng, final double lat) throws ParseException {
		if (lat == 0 && lng == 0) {
			return;
		}
		//Create my location
		Location myLocation = new Location(LocationManager.GPS_PROVIDER);
		myLocation.setLatitude(lat);
		myLocation.setLongitude(lng);

		Cursor l = getLocations();
		l.moveToFirst();
		for (int i = 0; i < l.getCount(); i++) {
			ContentValues cv = new ContentValues();

			//Create contact location
			double contactLng = l.getDouble(l.getColumnIndex(KEY_LONGITUDE));
			double contactLat = l.getDouble(l.getColumnIndex(KEY_LATITUDE));
			Location contactLocation = new Location(LocationManager.GPS_PROVIDER);
			contactLocation.setLatitude(contactLat);
			contactLocation.setLongitude(contactLng);

			cv.put(KEY_DISTANCE, myLocation.distanceTo(contactLocation));

			if (!acquireDbLock()) {
				continue;
			}
			mDb.update(DATABASE_TABLE_USERS, cv, KEY_ROWID + "=" + l.getInt(l.getColumnIndex(KEY_ROWID)), null);
			USE_DB.release();
		}
	}


	/**
	 * Returns a Cursor for a specified user.
	 * @param id of the user
	 * @return cursor for user
	 */
	public final Cursor getUser(final long id) {
		if (!acquireDbLock()) {
			return null;
		}

		Cursor ret = mDb.query(
				DATABASE_TABLE_USERS,
				new String[] {KEY_ROWID,
						KEY_USERNAME,
						KEY_FIRST_NAME,
						KEY_LAST_NAME,
						KEY_ADDRESS,
						KEY_ZIPCODE,
						KEY_CITY,
						KEY_PHONE,
						KEY_EMAIL,
						KEY_DEGREE,
						KEY_TITLE,
						KEY_STATUS,
						KEY_PHOTO_UPDATED_AT,
						KEY_PHOTO,
						KEY_LINKEDIN,
						KEY_CLIENT,
						KEY_LATITUDE,
						KEY_LONGITUDE,
						KEY_CONNECTED_AT,
						KEY_LOCATION_UPDATED_AT},
						KEY_ROWID + "=" + id, null, null, null, null);
		USE_DB.release();
		return ret;
	}

	/**
	 * Returns an ArrayList of user IDs.
	 * @return ArrayList of user ids
	 */
	public final ArrayList<String> getUserIds() {
		if (!acquireDbLock()) {
			return null;
		}

		Cursor c = mDb.query(DATABASE_TABLE_USERS, new String[] {KEY_ROWID}, null, null, null, null, null);

		USE_DB.release();

		ArrayList<String> l = new ArrayList<String>();
		c.moveToFirst();
		while (!c.isAfterLast()) {
			l.add(c.getString(0));
			c.moveToNext();
		}
		c.close();
		return l;
	}


	/**
	 * Update a user row in the database.
	 * @param cv User data that should be added to the database.
	 * @param id of the user that should be updated.
	 * @return id of user (long)
	 */
	public final Long updateUser(final ContentValues cv, final long id) {
		if (!acquireDbLock()) {
			return null;
		}

		long res = mDb.update(DATABASE_TABLE_USERS, cv, KEY_ROWID + "=" + id, null);
		USE_DB.release();
		return res;
	}

	/**
	 * Update the Contacted at column to current timestamp.
	 * @param id of the user
	 */
	public final void updateContactedAt(final long id) {
		ContentValues cv = new ContentValues();
		cv.put(KEY_CONTACTED_AT, (String) DateFormat.format("yyyy-MM-dd kk:mm:ss", new Date()));

		if (!acquireDbLock()) {
			return;
		}

		mDb.update(DATABASE_TABLE_USERS, cv, KEY_ROWID + "=" + id, null);

		USE_DB.release();
	}


	/**
	 * Add new user to the database (replace if a current user with the same id already exists).
	 * @param cv User that should be added to the database
	 * @return id of user (long)
	 */
	public final Long addUser(final ContentValues cv) {
		if (!acquireDbLock()) {
			return null;
		}

		long res = mDb.replace(DATABASE_TABLE_USERS, null, cv);
		USE_DB.release();
		return res;
	}

	/**
	 * Check if user exists.
	 * @param id of the user
	 * @return Succesful
	 */
	public final boolean userExists(final long id) {
		String username = getColumn(id, KEY_USERNAME);
		if (username == null) {
			Log.d(TAG, "User Does Not Exist");
			return false;
		}
		return true;
	}


	/**
	 * Saves location for a specified user in database.
	 * @param lat Latitude of the user
	 * @param lng Longitude of the user
	 * @param username the username of the user
	 */
	public final void saveLocation(final Double lat, final Double lng, final String username) {
		ContentValues args = new ContentValues();
		args.put(KEY_LATITUDE, lat);
		args.put(KEY_LONGITUDE, lat);
		args.put(KEY_LOCATION_UPDATED_AT, (String) DateFormat.format("yyyy-MM-dd kk:mm:ss", new Date()));
		if (!acquireDbLock()) {
			return;
		}

		mDb.update(DATABASE_TABLE_USERS, args, KEY_USERNAME + "=" + DatabaseUtils.sqlEscapeString(username), null);
		USE_DB.release();
	}


	/**
	 * Check if a location fresh to use. i.e. was updated since a predefined time ago.
	 * @param id of the user
	 * @return if users location is fresh
	 */
	public final boolean isLocationFresh(final long id) {
		Cursor c = getUser(id);
		if (c == null) {
			return false;
		}
		c.moveToFirst();

		SimpleDateFormat dfm = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		dfm.setLenient(true);
		Date oldTimestamp = new Date();
		String dateString = c.getString(c.getColumnIndex(KEY_LOCATION_UPDATED_AT));
		c.close();
		if (dateString == null || dateString.equals("") || dateString.equals("null")) {
			return false;
		}
		try {
			oldTimestamp = dfm.parse(dateString);
			if (((new Date()).getTime() - oldTimestamp.getTime()) > ContactsActivity.TIME_INTERVAL_ONLINE_MILLISECONDS) {
				return false;
			}
		} catch (ParseException e) {
			Log.e(TAG, e.getMessage());
		}
		return true;
	}

	/**
	 * Get phone number for contact (user).
	 * @param id of the user
	 * @return phone number
	 */
	public final String getContactPhone(final long id) {
		return getColumn(id, KEY_PHONE);
	}

	/**
	 * Get longitude for contact (user).
	 * @param id of the user
	 * @return longitude
	 */
	public final String getContactLongitude(final long id) {
		return getColumn(id, KEY_LONGITUDE);
	}

	/**
	 * Get latitude for contact (user).
	 * @param id of the user
	 * @return latitude
	 */
	public final String getContactLatitude(final long id) {
		return getColumn(id, KEY_LATITUDE);
	}

	/**
	 * Get email for contact (user).
	 * @param id of the user
	 * @return email
	 */
	public final String getContactEmail(final long id) {
		return getColumn(id, KEY_EMAIL);
	}

	/**
	 * Get first name for contact (user).
	 * @param id of the user
	 * @return first name
	 */
	public final String getContactName(final long id) {
		return getColumn(id, KEY_FIRST_NAME);
	}

	/**
	 * Get photo updated at for contact (user).
	 * @param id of the user
	 * @return timestamp for when photo was updated
	 */
	public final String getContactPhotoUpdatedAt(final long id) {
		return getColumn(id, KEY_PHOTO_UPDATED_AT);
	}

	/**
	 * Get status for contact (user).
	 * @param id of the user
	 * @return status
	 */
	public final String getStatus(final long id) {
		return getColumn(id, KEY_STATUS);
	}

	/**
	 * Get a column named columnKey from the user table.
	 * @param id of the user
	 * @param columnKey name of the column
	 * @return first name
	 */
	private String getColumn(final long id, final String columnKey) {
		Cursor c = getUser(id);
		if (c == null) {
			return null;
		}
		c.moveToFirst();
		if (c.isAfterLast()) {
			//Log.d(TAG, "getColumn no hits");
			return null;
		}
		String str =  c.getString(c.getColumnIndex(columnKey));
		c.close();
		return str;
	}

}

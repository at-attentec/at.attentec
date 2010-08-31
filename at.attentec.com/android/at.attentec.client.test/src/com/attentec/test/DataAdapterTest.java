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
package com.attentec.test;

import java.util.ArrayList;

import android.content.ContentValues;
import android.database.Cursor;
import android.test.AndroidTestCase;
import android.util.Log;

import com.attentec.DatabaseAdapter;

/**
 * Tests for DatabaseAtapter.
 * @author David Granqvist
 *
 */
public class DataAdapterTest extends AndroidTestCase {

	/**
	 * The Database Adapter that is used for testing.
	 */
	private DatabaseAdapter dbh;
	/**
	 * ContentValues that should contain data to shuffle into the test database.
	 */
	private ContentValues cv;
	/**
	 * The name of the test database.
	 */
	private static final String DATABASE_NAME = "sql_test_db";

	@Override
	protected final void setUp() throws Exception {
		super.setUp();
		dbh = new DatabaseAdapter(mContext, DATABASE_NAME);
		dbh.open();
		dbh.clear();
	}

	@Override
	protected final void tearDown() throws Exception {
		super.tearDown();
		dbh.close();
	}

	/**
	 * Logs a message.
	 * @param str the message
	 */
	protected final void log(final String str) {
		Log.w("TestAttentecDBHelper", str);
	}

	/**
	 * Create correct user data.
	 * @return ContentValues containing correct user data
	 */
	protected final ContentValues getCorrectUser() {
		cv = new ContentValues();
		cv.put(DatabaseAdapter.KEY_ROWID, "1");
		cv.put(DatabaseAdapter.KEY_USERNAME, "username");
		cv.put(DatabaseAdapter.KEY_FIRST_NAME, "first_name");
		cv.put(DatabaseAdapter.KEY_LAST_NAME, "last_name");
		cv.put(DatabaseAdapter.KEY_ADDRESS, "address");
		cv.put(DatabaseAdapter.KEY_ZIPCODE, "zipcode");
		cv.put(DatabaseAdapter.KEY_CITY, "city");
		cv.put(DatabaseAdapter.KEY_PHONE, "phone");
		cv.put(DatabaseAdapter.KEY_EMAIL, "email");
		cv.put(DatabaseAdapter.KEY_LATITUDE, "latitude");
		cv.put(DatabaseAdapter.KEY_LONGITUDE, "longitude");
		return cv;
	}

	/**
	 * Create bad user data.
	 * @return ContentValues containing bad user data
	 */
	protected final ContentValues getIncorrectUser() {
		cv = new ContentValues();
		cv.put(DatabaseAdapter.KEY_ROWID, "1");
		cv.put(DatabaseAdapter.KEY_USERNAME, "username");
		cv.put(DatabaseAdapter.KEY_FIRST_NAME, "first_name");
		cv.put(DatabaseAdapter.KEY_LAST_NAME, "last_name");
		cv.put(DatabaseAdapter.KEY_ADDRESS, "address");
		cv.put(DatabaseAdapter.KEY_ZIPCODE, "zipcode");
		cv.put(DatabaseAdapter.KEY_CITY, "city");
		cv.put(DatabaseAdapter.KEY_PHONE, "phone");
		//email cant be null;
		return cv;
	}

	/**
	 * Get id from the ContentValues that is used for testing.
	 * @return The id of the contentValue instance
	 */
	protected final Long cvId() {
		return cv.getAsLong(DatabaseAdapter.KEY_ROWID);
	}


	/*
	 * Tests
	 */

	/**
	 * Test preconditions.
	 * @throws Exception on error
	 */
	public final void testPreconditions() throws Exception {
		assertNotNull(mContext);
		assertNotNull(dbh);
	}

	/**
	 * Test that database is empty from the beginning.
	 * @throws Exception on error
	 */
	public final void testIsEmptyDatabase() throws Exception {
		dbh.clear();
		Cursor c = dbh.getContent(null);
		assertEquals(0, c.getCount());
	}

	/**
	 * Test that a new correct user is added .
	 * @throws Exception on error
	 */
	public final void testAddCorrectUser() throws Exception {
		getCorrectUser();
		assertEquals(cvId(), dbh.addUser(cv));
		assertEquals(1, dbh.getContent(null).getCount());
	}

	/**
	 * Test that a new incorrect user is not added .
	 * @throws Exception on error
	 */
	public final void testAddIncorrectUser() throws Exception {
		getIncorrectUser();
		assertEquals(new Long(-1), dbh.addUser(cv));
		assertEquals(0, dbh.getContent(null).getCount());
	}

	/**
	 * Test that a new correct user and then remove the same again.
	 * @throws Exception on error
	 */
	public final void testAddDeleteUser() throws Exception {
		getCorrectUser();
		assertEquals(cvId(), dbh.addUser(cv));
		assertEquals(1, dbh.getContent(null).getCount());
		assertEquals(true, dbh.deleteUser(cvId()));
		assertEquals(0, dbh.getContent(null).getCount());
	}

	/**
	 * Try to delete a user that does not exist.
	 * @throws Exception on error
	 */
	public final void testDeleteNonexistingUser() throws Exception {
		assertEquals(false, dbh.deleteUser(1));
	}

	/**
	 * Try to update a user.
	 * @throws Exception on error
	 */
	public final void testUpdateUser() throws Exception {
		String newUserName = "bob";
		getCorrectUser();
		assertEquals(cvId(), dbh.addUser(cv));
		assertEquals(1, dbh.getContent(null).getCount());

		cv.put(DatabaseAdapter.KEY_USERNAME, newUserName);

		assertEquals(new Long(1), dbh.addUser(cv));
		assertEquals(1, dbh.getContent(null).getCount());
		Cursor user = dbh.getUser(cvId());
		user.moveToFirst();
		assertEquals(newUserName, user.getString(user.getColumnIndex(DatabaseAdapter.KEY_USERNAME)));
	}

	/**
	 * Test the getUserIds method.
	 * @throws Exception on error
	 */
	public final void testGetUserIds() throws Exception {
		getCorrectUser();
		assertEquals(cvId(), dbh.addUser(cv));
		assertEquals(1, dbh.getContent(null).getCount());
		ArrayList<String> compareTo = new ArrayList<String>();
		compareTo.add("1");
		assertEquals(compareTo, dbh.getUserIds());

		cv.put(DatabaseAdapter.KEY_ROWID, "3");
		compareTo.add("3");
		assertEquals(cvId(), dbh.addUser(cv));
		assertEquals(compareTo, dbh.getUserIds());
	}

}

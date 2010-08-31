/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 * Modified by:
 * Malte Lenz
 */

package com.attentec;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.Settings;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.util.Log;

/**
 * Class for managing contacts sync related mOperations.
 */
public final class SyncContactManager {
	/** Utility class constructor should not be available. */
	private SyncContactManager() { }

	/** Number of operations before submitting batch. */
	private static final int BATCH_OPERATION_SIZE_LIMIT = 50;

	/** Name of the account type. */
	public static final String ACCOUNT_TYPE = "Attentec";

	/** Tag used for logging. */
	private static final String TAG = "SyncContactManager";

	private static DatabaseAdapter dbh;

	/**
	 * Synchronize raw contacts.
	 *
	 * @param context The context of Authenticator Activity
	 * @param account The username for the account
	 */
	public static synchronized void syncContacts(final Context context, final String account) {
		int userId;
		long rawId = 0;
		final ContentResolver resolver = context.getContentResolver();
		final SyncBatchOperation batchOperation =
			new SyncBatchOperation(context, resolver);
		//Log.d(TAG, "In SyncContacts for account name: " + account);

		// Load the local contacts
		dbh = new DatabaseAdapter(context);
		dbh.open();
		Cursor c1 = dbh.getContent(null);

		while (c1.moveToNext()) {
			userId = c1.getInt(c1.getColumnIndex(DatabaseAdapter.KEY_ROWID));
			rawId = lookupRawContact(resolver, userId);

			String firstName = c1.getString(c1.getColumnIndex(DatabaseAdapter.KEY_FIRST_NAME));
			String lastName = c1.getString(c1.getColumnIndex(DatabaseAdapter.KEY_LAST_NAME));
			String phoneNumber = c1.getString(c1.getColumnIndex(DatabaseAdapter.KEY_PHONE));
			String email = c1.getString(c1.getColumnIndex(DatabaseAdapter.KEY_EMAIL));
			String street = c1.getString(c1.getColumnIndex(DatabaseAdapter.KEY_ADDRESS));
			String postCode = c1.getString(c1.getColumnIndex(DatabaseAdapter.KEY_ZIPCODE));
			String city = c1.getString(c1.getColumnIndex(DatabaseAdapter.KEY_CITY));
			if (rawId != 0) {
				// update contact
				updateContact(context, resolver, account,  userId, firstName, lastName, email, phoneNumber,
						street, postCode, city,
						rawId, batchOperation);
			} else {
				addContact(context, account, userId, firstName, lastName, email, phoneNumber, street, postCode, city, batchOperation);
			}
			// A sync adapter should batch operations on multiple contacts,
			// because it will make a dramatic performance difference.
			if (batchOperation.size() >= BATCH_OPERATION_SIZE_LIMIT) {
				batchOperation.execute();
			}
		}
		c1.close();
		dbh.close();
		batchOperation.execute();
		ContentProviderClient client = resolver.acquireContentProviderClient(ContactsContract.AUTHORITY_URI);
		ContentValues cv = new ContentValues();
		cv.put(Groups.ACCOUNT_NAME, account);
		cv.put(Groups.ACCOUNT_TYPE, ACCOUNT_TYPE);
		cv.put(Settings.UNGROUPED_VISIBLE, true);
		try {
			client.insert(
					Settings.CONTENT_URI.buildUpon().appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true").build(),
					cv);
		} catch (RemoteException e) {
			Log.e(TAG, "Could not make ungrouped visible.");
		}
	}

	/**
	 * Adds a single contact to the platform contacts provider.
	 * @param context the Authenticator Activity context
	 * @param accountName the account the contact belongs to
	 * @param userId the sample SyncAdapter User id
	 * @param firstName first name of user to add
	 * @param lastName last name of user to add
	 * @param email email of user to add
	 * @param phoneNumber phone number of user to add
	 * @param city where the contact lives
	 * @param zipCode where the contact lives
	 * @param street where the contact lives
	 * @param batchOperation batch operation object to use
	 */
	private static void addContact(final Context context,
			final String accountName,
			final int userId,
			final String firstName,
			final String lastName,
			final String email,
			final String phoneNumber,
			final String street,
			final String zipCode,
			final String city,
			final SyncBatchOperation batchOperation) {
		//Log.d(TAG, "Adding: " + firstName + " " + email + " with user id: " + userId);
		// Put the data in the contacts provider
		final SyncContactOperations contactOp =
			SyncContactOperations.createNewContact(context, userId,
					accountName, batchOperation);
		contactOp.addName(firstName, lastName).addEmail(email).addPhone(phoneNumber).addAddress(street, city, zipCode);
	}

	/**
	 * Updates a single contact to the platform contacts provider.
	 * @param context the Authenticator Activity context
	 * @param resolver the ContentResolver to use
	 * @param accountName the account the contact belongs to
	 * @param userId the sample SyncAdapter contact id.
	 * @param firstName of user to update
	 * @param lastName of user to update
	 * @param email of user to update
	 * @param phoneNumber of user to update
	 * @param city where the contact lives
	 * @param zipCode where the contact lives
	 * @param street where the contact lives
	 * @param rawContactId the unique Id for this rawContact in contacts
	 *        provider
	 * @param batchOperation object to use
	 */
	private static void updateContact(final Context context,
			final ContentResolver resolver,
			final String accountName,
			final int userId,
			final String firstName,
			final String lastName,
			final String email,
			final String phoneNumber,
			final String street,
			final String zipCode,
			final String city,
			final long rawContactId,
			final SyncBatchOperation batchOperation) {
		//Log.d(TAG, "Updating: " + firstName + " " + email);
		Uri uri;
		String oPhone = null;
		String oEmail = null;
		String oStreet = null;
		String oZipCode = null;
		String oCity = null;

		final Cursor c =
			resolver.query(Data.CONTENT_URI, DataQuery.PROJECTION,
					DataQuery.SELECTION,
					new String[] {String.valueOf(rawContactId)}, null);
		final SyncContactOperations contactOp =
			SyncContactOperations.updateExistingContact(context, rawContactId,
					batchOperation);
		//Log.d(TAG, "Length of cursor: " + c.getCount());
		try {
			while (c.moveToNext()) {
				final long id = c.getLong(DataQuery.COLUMN_ID);
				final String mimeType = c.getString(DataQuery.COLUMN_MIMETYPE);
				uri = ContentUris.withAppendedId(Data.CONTENT_URI, id);
				Log.d(TAG, "Mimetype: " + mimeType + " Searching for: " + StructuredName.CONTENT_ITEM_TYPE);
				if (mimeType.equals(StructuredName.CONTENT_ITEM_TYPE)) {
					Log.d(TAG, "Found mimetype name");
					final String oLastName =
						c.getString(DataQuery.COLUMN_FAMILY_NAME);
					final String oFirstName =
						c.getString(DataQuery.COLUMN_GIVEN_NAME);
					contactOp.updateName(uri, oFirstName, oLastName, firstName, lastName);
				} else if (mimeType.equals(Phone.CONTENT_ITEM_TYPE)) {
					Log.d(TAG, "Found mimetype phone");
					oPhone = c.getString(DataQuery.COLUMN_PHONE_NUMBER);
					contactOp.updatePhone(oPhone, phoneNumber,
							uri);

				} else if (mimeType.equals(Email.CONTENT_ITEM_TYPE)) {
					Log.d(TAG, "Found mimetype email");
					oEmail = c.getString(DataQuery.COLUMN_EMAIL_ADDRESS);
					Log.d(TAG, "Email old: " + oEmail + " Email new: " + email);
					contactOp.updateEmail(email, oEmail, uri);
				} else if (mimeType.equals(StructuredPostal.CONTENT_ITEM_TYPE)) {
					Log.d(TAG, "Found mimetype structuredPostal");
					oStreet = c.getString(DataQuery.COLUMN_STREET);
					oZipCode = c.getString(DataQuery.COLUMN_ZIP_CODE);
					oCity = c.getString(DataQuery.COLUMN_CITY);
					contactOp.updateAddress(street, oStreet, city, oCity, zipCode, oZipCode, uri);
				} else {
					Log.w(TAG, "mimetype not handled: " + mimeType);
				}
			} // while
		} finally {
			c.close();
		}

		// Add the cell phone, if present and not updated above
		if (phoneNumber != null && !phoneNumber.equals("null") && oPhone == null) {
			//Log.d(TAG, "Adding phone number: " + phoneNumber);
			contactOp.addPhone(phoneNumber);
		}

		// Add the email address, if present and not updated above
		if (email != null && !email.equals("null") && oEmail == null) {
			contactOp.addEmail(email);
		}

		// Add the address, if present and not updated above
		if (street != null && !street.equals("null") && oStreet == null
				&& city != null && !city.equals("null") && oCity == null
				&& zipCode != null && !zipCode.equals("null") && oZipCode == null) {
			contactOp.addAddress(street, city, zipCode);
		}
	}

	/**
	 * Deletes a contact from the platform contacts provider.
	 *
	 * @param context the Authenticator Activity context
	 * @param rawContactId the unique Id for this rawContact in contacts
	 *        provider
	 */
	//    private static void deleteContact(Context context, long rawContactId,
	//        SyncBatchOperation batchOperation) {
	//        batchOperation.add(SyncContactOperations.newDeleteCpo(
	//            ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId),
	//            true).build());
	//    }

	/**
	 * Returns the RawContact id for a sample SyncAdapter contact, or 0 if the
	 * sample SyncAdapter user isn't found.
	 *
	 * @param resolver to use for fetching info
	 * @param userId the sample SyncAdapter user ID to lookup
	 * @return the RawContact id, or 0 if not found
	 */
	private static long lookupRawContact(final ContentResolver resolver, final long userId) {
		long authorId = 0;
		final Cursor c =
			resolver.query(RawContacts.CONTENT_URI, UserIdQuery.PROJECTION,
					UserIdQuery.SELECTION, new String[] {String.valueOf(userId)},
					null);
		try {
			if (c.moveToFirst()) {
				authorId = c.getLong(UserIdQuery.COLUMN_ID);
			}
		} finally {
			if (c != null) {
				c.close();
			}
		}
		return authorId;
	}

	/**
	 * Constants for a query to find a contact given a sample SyncAdapter user
	 * ID.
	 */
	private static final class UserIdQuery {
		/** Utility class constructor should not be available. */
		private UserIdQuery() { }

		public static final String[] PROJECTION =
			new String[] {RawContacts._ID};

		public static final int COLUMN_ID = 0;

		public static final String SELECTION =
			RawContacts.ACCOUNT_TYPE + "='" + ACCOUNT_TYPE + "' AND "
			+ RawContacts.SOURCE_ID + "=?";
	}

	/**
	 * Constants for a query to get contact data for a given rawContactId.
	 */
	private static final class DataQuery {
		/** Utility class constructor should not be available. */
		private DataQuery() { }

		public static final String[] PROJECTION =
			new String[] {Data._ID, Data.MIMETYPE, Data.DATA1, Data.DATA2,
			Data.DATA3, Data.DATA4, Data.DATA7, Data.DATA9 };

		public static final int COLUMN_ID = 0;
		public static final int COLUMN_MIMETYPE = 1;
		public static final int COLUMN_DATA1 = 2;
		public static final int COLUMN_DATA2 = 3;
		public static final int COLUMN_DATA3 = 4;
		public static final int COLUMN_DATA4 = 5;
		public static final int COLUMN_DATA7 = 6;
		public static final int COLUMN_DATA9 = 7;
		public static final int COLUMN_PHONE_NUMBER = COLUMN_DATA1;
		public static final int COLUMN_EMAIL_ADDRESS = COLUMN_DATA1;
		public static final int COLUMN_GIVEN_NAME = COLUMN_DATA2;
		public static final int COLUMN_FAMILY_NAME = COLUMN_DATA3;
		public static final int COLUMN_STREET = COLUMN_DATA4;
		public static final int COLUMN_CITY = COLUMN_DATA7;
		public static final int COLUMN_ZIP_CODE = COLUMN_DATA9;

		public static final String SELECTION = Data.RAW_CONTACT_ID + "=?";
	}
}

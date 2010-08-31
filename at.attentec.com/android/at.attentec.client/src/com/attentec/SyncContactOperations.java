/**
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
 */
package com.attentec;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.text.TextUtils;

/**
 * Helper class for storing data in the platform content providers.
 */
public class SyncContactOperations {

	/**
	 * Tag used for logging.
	 */
	//private static final String TAG = "SyncContactOperations";
	private final ContentValues mValues;
	private ContentProviderOperation.Builder mBuilder;
	private final SyncBatchOperation mBatchOperation;
	private boolean mYield;
	private long mRawContactId;
	private int mBackReference;
	private boolean mIsNewContact;

	/**
	 * Returns an instance of SyncContactOperations instance for adding new contact
	 * to the platform contacts provider.
	 *
	 * @param context the Authenticator Activity context
	 * @param userId the userId of the sample SyncAdapter user object
	 * @param accountName the username of the current login
	 * @param batchOperation batch operation to use
	 * @return instance of SyncContactOperations
	 */
	public static SyncContactOperations createNewContact(final Context context,
			final int userId, final String accountName, final SyncBatchOperation batchOperation) {
		return new SyncContactOperations(context, userId, accountName,
				batchOperation);
	}

	/**
	 * Returns an instance of SyncContactOperations for updating existing contact in
	 * the platform contacts provider.
	 *
	 * @param context the Authenticator Activity context
	 * @param rawContactId the unique Id of the existing rawContact
	 * @param batchOperation batch operation to use
	 * @return instance of SyncContactOperations
	 */
	public static SyncContactOperations updateExistingContact(final Context context, final long rawContactId, final SyncBatchOperation batchOperation) {
		return new SyncContactOperations(context, rawContactId, batchOperation);
	}

	/**
	 * Initialize a new object.
	 * @param context calling context
	 * @param batchOperation handler of batches of user info to submit
	 */
	public SyncContactOperations(final Context context, final SyncBatchOperation batchOperation) {
		mValues = new ContentValues();
		mYield = true;
		mBatchOperation = batchOperation;
	}

	/**
	 * Initialize for a new contact.
	 * @param context calling context
	 * @param userId id of the user to insert
	 * @param accountName name of the calling account
	 * @param batchOperation handler of batches of user info to submit
	 */
	public SyncContactOperations(final Context context, final int userId, final String accountName,
			final SyncBatchOperation batchOperation) {
		this(context, batchOperation);
		mBackReference = mBatchOperation.size();
		mIsNewContact = true;
		mValues.put(RawContacts.SOURCE_ID, userId);
		mValues.put(RawContacts.ACCOUNT_TYPE, SyncContactManager.ACCOUNT_TYPE);
		mValues.put(RawContacts.ACCOUNT_NAME, accountName);
		mBuilder =
			newInsertCpo(RawContacts.CONTENT_URI, true).withValues(mValues);
		mBatchOperation.add(mBuilder.build());
	}

	/**
	 * Updating an old user.
	 * @param context calling context
	 * @param rawContactId id of the existing RawContact
	 * @param batchOperation handler of batches of user info to submit
	 */
	public SyncContactOperations(final Context context, final long rawContactId,
			final SyncBatchOperation batchOperation) {
		this(context, batchOperation);
		//Log.d(TAG, "Is not new contact, with raw id: " + rawContactId);
		mIsNewContact = false;
		mRawContactId = rawContactId;
	}

	/**
	 * Adds a contact name.
	 *
	 * @param firstName Fist name of contact
	 * @param lastName Last name of contact
	 * @return instance of SyncContactOperations
	 */
	public final SyncContactOperations addName(final String firstName, final String lastName) {
		//Log.d(TAG, "adding Name: " + firstName + ", " + lastName);
		mValues.clear();
		if (!TextUtils.isEmpty(firstName) && !firstName.equals("null")) {
			mValues.put(StructuredName.GIVEN_NAME, firstName);
			mValues.put(StructuredName.MIMETYPE,
					StructuredName.CONTENT_ITEM_TYPE);
		}
		if (!TextUtils.isEmpty(lastName) && !lastName.equals("null")) {
			mValues.put(StructuredName.FAMILY_NAME, lastName);
			mValues.put(StructuredName.MIMETYPE,
					StructuredName.CONTENT_ITEM_TYPE);
		}
		if (mValues.size() > 0) {
			addInsertOp();
		}
		return this;
	}

	/**
	 * Add contacts to a group.
	 * ** UNTESTED AND UNUSED **
	 * @return itself to enable chaining
	 */
	//    public final SyncContactOperations addGroup() {
	//    	Log.d(TAG, "addGroup");
	//        mValues.clear();
	//        mValues.put(GroupMembership.MIMETYPE, GroupMembership.CONTENT_ITEM_TYPE);
	//        mValues.put(GroupMembership.GROUP_SOURCE_ID, "Attentec");
	//        addInsertOp();
	//        return this;
	//    }

	/**
	 * Adds an email.
	 *
	 * @param email for user
	 * @return itself to enable chaining
	 */
	public final SyncContactOperations addEmail(final String email) {
		//Log.d(TAG, "addEmail");
		mValues.clear();
		if (!TextUtils.isEmpty(email) && !email.equals("null")) {
			mValues.put(Email.DATA, email);
			mValues.put(Email.TYPE, Email.TYPE_OTHER);
			mValues.put(Email.MIMETYPE, Email.CONTENT_ITEM_TYPE);
			addInsertOp();
		}
		return this;
	}

	/**
	 * Adds a phone number.
	 *
	 * @param phone new phone number for the contact
	 * @return instance of SyncContactOperations
	 */
	public final SyncContactOperations addPhone(final String phone) {
		//Log.d(TAG, "addPhone: " + phone);
		mValues.clear();
		if (!TextUtils.isEmpty(phone) && !phone.equals("null")) {
			mValues.put(Phone.NUMBER, phone);
			mValues.put(Phone.TYPE, Phone.TYPE_WORK);
			mValues.put(Phone.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
			addInsertOp();
		}
		return this;
	}

	/**
	 * Saves a new address.
	 * @param street street
	 * @param city which city
	 * @param zipCode what zipcode/postcode
	 * @return itself to allow for chaining
	 */
	public final SyncContactOperations addAddress(final String street, final String city, final String zipCode) {
		mValues.clear();
		if (!TextUtils.isEmpty(street) && !street.equals("null")
				&& !TextUtils.isEmpty(city) && !city.equals("null")
				&& !TextUtils.isEmpty(zipCode) && !zipCode.equals("null")) {
			mValues.put(StructuredPostal.MIMETYPE, StructuredPostal.CONTENT_ITEM_TYPE);
			mValues.put(StructuredPostal.STREET, street);
			mValues.put(StructuredPostal.CITY, city);
			mValues.put(StructuredPostal.POSTCODE, zipCode);
		}
		if (mValues.size() > 0) {
			addInsertOp();
		}
		return this;
	}

	/**
	 * Updates contacts email.
	 *
	 * @param email new email to save
	 * @param existingEmail old email to replace
	 * @param uri Uri for the existing raw contact to be updated
	 * @return instance of SyncContactOperations
	 */
	public final SyncContactOperations updateEmail(final String email, final String existingEmail, final Uri uri) {
		if (!TextUtils.equals(existingEmail, email)) {
			mValues.clear();
			mValues.put(Email.DATA, email);
			addUpdateOp(uri);
		}
		return this;
	}

	/**
	 * Updates contacts name.
	 *
	 * @param firstName New first name of contact
	 * @param existingFirstName FirstName of contact stored in provider
	 * @param lastName New last name of contact
	 * @param existingLastName LastName of contact stored in provider
	 * @param uri Uri for the existing raw contact to be updated
	 * @return instance of SyncContactOperations
	 */
	public final SyncContactOperations updateName(final Uri uri, final String existingFirstName,
			final String existingLastName, final String firstName, final String lastName) {
		//Log.i(TAG, "updateName ef =" + existingFirstName + "el = "
		//    + existingLastName + " f = " + firstName + " l = " + lastName);
		mValues.clear();
		if (!TextUtils.equals(existingFirstName, firstName)) {
			mValues.put(StructuredName.GIVEN_NAME, firstName);
		}
		if (!TextUtils.equals(existingLastName, lastName)) {
			mValues.put(StructuredName.FAMILY_NAME, lastName);
		}
		if (mValues.size() > 0) {
			addUpdateOp(uri);
		}
		return this;
	}

	/**
	 * Updates contacts phone.
	 *
	 * @param existingNumber phone number stored in contacts provider
	 * @param phone new phone number for the contact
	 * @param uri Uri for the existing raw contact to be updated
	 * @return instance of SyncContactOperations
	 */
	public final SyncContactOperations updatePhone(final String existingNumber, final String phone, final Uri uri) {
		if (!TextUtils.equals(phone, existingNumber)) {
			mValues.clear();
			mValues.put(Phone.NUMBER, phone);
			addUpdateOp(uri);
		}
		return this;
	}

	/**
	 * Updates contacts address.
	 * @param street street address
	 * @param oStreet old street address
	 * @param city new city
	 * @param oCity old city
	 * @param zipCode new zipcode
	 * @param oZipCode old zipcode
	 * @param uri where to save
	 * @return itself to allow for chaining
	 */
	public final SyncContactOperations updateAddress(
			final String street,
			final String oStreet,
			final String city,
			final String oCity,
			final String zipCode,
			final String oZipCode,
			final Uri uri) {
		mValues.clear();
		if (!TextUtils.equals(oStreet, street)) {
			mValues.put(StructuredPostal.STREET, street);
		}
		if (!TextUtils.equals(oCity, city)) {
			mValues.put(StructuredPostal.CITY, city);
		}
		if (!TextUtils.equals(oZipCode, zipCode)) {
			mValues.put(StructuredPostal.POSTCODE, zipCode);
		}
		if (mValues.size() > 0) {
			addUpdateOp(uri);
		}
		return this;
	}

	/**
	 * Updates contact's profile action.
	 *
	 * @param userId sample SyncAdapter user id
	 * @param uri Uri for the existing raw contact to be updated
	 * @return instance of SyncContactOperations
	 */
	//    public SyncContactOperations updateProfileAction(Integer userId, Uri uri) {
	//        mValues.clear();
	//        mValues.put(SampleSyncAdapterColumns.DATA_PID, userId);
	//        addUpdateOp(uri);
	//        return this;
	//    }

	/**
	 * Adds an insert operation into the batch.
	 */
	private void addInsertOp() {
		if (!mIsNewContact) {
			mValues.put(Phone.RAW_CONTACT_ID, mRawContactId);
		}
		mBuilder =
			newInsertCpo(addCallerIsSyncAdapterParameter(Data.CONTENT_URI),
					mYield);
		mBuilder.withValues(mValues);
		if (mIsNewContact) {
			mBuilder
			.withValueBackReference(Data.RAW_CONTACT_ID, mBackReference);
		}
		mYield = false;
		mBatchOperation.add(mBuilder.build());
	}

	/**
	 * Adds an update operation into the batch.
	 * @param uri where to update
	 */
	private void addUpdateOp(final Uri uri) {
		mBuilder = newUpdateCpo(uri, mYield).withValues(mValues);
		mYield = false;
		mBatchOperation.add(mBuilder.build());
	}

	/**
	 * Create a new insert object.
	 * @param uri where to save stuff
	 * @param yield .
	 * @return an object of itself
	 */
	public static ContentProviderOperation.Builder newInsertCpo(final Uri uri, final boolean yield) {
		return ContentProviderOperation.newInsert(
				addCallerIsSyncAdapterParameter(uri)).withYieldAllowed(yield);
	}

	/**
	 * Create a new update object.
	 * @param uri where to save stuff
	 * @param yield .
	 * @return an object of itself
	 */
	public static ContentProviderOperation.Builder newUpdateCpo(final Uri uri, final boolean yield) {
		return ContentProviderOperation.newUpdate(
				addCallerIsSyncAdapterParameter(uri)).withYieldAllowed(yield);
	}

	/**
	 * Create a new delete object.
	 * @param uri where to delete stuff
	 * @param yield .
	 * @return an object of itself
	 */
	public static ContentProviderOperation.Builder newDeleteCpo(final Uri uri, final boolean yield) {
		return ContentProviderOperation.newDelete(
				addCallerIsSyncAdapterParameter(uri)).withYieldAllowed(yield);

	}

	/**
	 * Add a parameter that tells it is a syncadapter.
	 * @param uri where to save stuff
	 * @return an object of itself
	 */
	private static Uri addCallerIsSyncAdapterParameter(final Uri uri) {
		return uri.buildUpon().appendQueryParameter(
				ContactsContract.CALLER_IS_SYNCADAPTER, "true").build();
	}
}

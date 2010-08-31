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
 */

package com.attentec;

import java.util.ArrayList;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.OperationApplicationException;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.util.Log;

/**
 * This class handles execution of batch mOperations on Contacts provider.
 */
public class SyncBatchOperation {
    private static final String TAG = "SyncBatchOperation";

    private final ContentResolver mResolver;
    // List for storing the batch mOperations
    private ArrayList<ContentProviderOperation> mOperations;

    /**
     * initialize an empty batch.
     * @param context calling context
     * @param resolver accessor for saving
     */
    public SyncBatchOperation(final Context context, final ContentResolver resolver) {
        mResolver = resolver;
        mOperations = new ArrayList<ContentProviderOperation>();
    }

    /**
     * Number of operations.
     * @return number of operations
     */
    public final int size() {
        return mOperations.size();
    }

    /**
     * Add a piece of info to save.
     * @param cpo what to save
     */
    public final void add(final ContentProviderOperation cpo) {
    	//Log.d(TAG, "adding cpo: " + cpo.toString());
        mOperations.add(cpo);
    }

    /**
     * Apply the whole batch.
     */
    public final void execute() {
        if (mOperations.size() == 0 || mOperations == null || mResolver == null) {
            return;
        }
        // Apply the mOperations to the content provider
        try {
            mResolver.applyBatch(ContactsContract.AUTHORITY, mOperations);
        } catch (final OperationApplicationException e1) {
            Log.e(TAG, "storing contact data failed", e1);
        } catch (final RemoteException e2) {
            Log.e(TAG, "storing contact data failed", e2);
        }
        mOperations.clear();
    }

}

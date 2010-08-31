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

import android.accounts.Account;
import android.accounts.OperationCanceledException;
import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

/**
 * Adapter between android contacts and our contact sync.
 * @author David Granqvist
 * @author Malte Lenz
 * @author Johannes Nordkvist
 *
 */
public class SyncContactsSyncAdapterService extends Service {
	private static final String TAG = "SyncContactsSyncAdapterService";
	private static SyncAdapterImpl sSyncAdapter = null;

	/**
	 * Implementation of the SyncAdapter.
	 * @author David Granqvist
	 * @author Malte Lenz
	 * @author Johannes Nordkvist
	 *
	 */
	private static class SyncAdapterImpl extends AbstractThreadedSyncAdapter {
		private Context mContext;

		/**
		 * Construct the SyncAdapterImpl.
		 * @param context calling context
		 */
		public SyncAdapterImpl(final Context context) {
			super(context, true);
			mContext = context;
		}

		@Override
		public void onPerformSync(final Account account, final Bundle extras,
				final String authority, final ContentProviderClient provider, final SyncResult syncResult) {
			try {
				SyncContactsSyncAdapterService.performSync(mContext, account, extras, authority, provider, syncResult);
			} catch (OperationCanceledException e) {
				Log.e(TAG, "Syncing contacts failed in onPerformSync");
			}
		}
	}

	@Override
	public final IBinder onBind(final Intent ntent) {
		IBinder ret = null;
		ret = getSyncAdapter().getSyncAdapterBinder();
		return ret;
	}

	/**
	 * Fetch the implementation of the SyncAdapter.
	 * @return SyncAdapterImpl-ementation
	 */
	private SyncAdapterImpl getSyncAdapter() {
		if (sSyncAdapter == null) {
			sSyncAdapter = new SyncAdapterImpl(this);
		}
		return sSyncAdapter;
	}

	/**
	 * Perform a sync between our contact list and the phone contact list.
	 * @param context calling context
	 * @param account account to sync
	 * @param extras unused
	 * @param authority unused
	 * @param provider unused
	 * @param syncResult unused
	 * @throws OperationCanceledException if operation is canceled.
	 */
	private static void performSync(final Context context, final Account account, final Bundle extras,
			final String authority, final ContentProviderClient provider, final SyncResult syncResult)
	throws OperationCanceledException {
		//Log.i(TAG, "performSync: " + account.toString());

		SyncContactManager.syncContacts(context, account.name);
	}

}

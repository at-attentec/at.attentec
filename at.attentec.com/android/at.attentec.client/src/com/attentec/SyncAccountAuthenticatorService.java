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

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

/**
 * Authenticator service that returns a subclass of AbstractAccountAuthenticator in onBind().
 */
public class SyncAccountAuthenticatorService extends Service {

	/** Used to check if it can login with our account. */
	private static AccountAuthenticatorImpl sAccountAuthenticator = null;

	/**
	 * Contstructor. Does nothing but is required.
	 */
	public SyncAccountAuthenticatorService() {
		super();
	}

	/**
	 * Binds authenticator to AccountManager.
	 * @param intent the calling intent
	 * @return IBinder our authenticator binder
	 */
	public final IBinder onBind(final Intent intent) {
		IBinder ret = null;
		if (intent.getAction().equals(android.accounts.AccountManager.ACTION_AUTHENTICATOR_INTENT)) {
			ret = getAuthenticator().getIBinder();
		}
		return ret;
	}

	/**
	 * Getter for sAccountAuthenticator, creates a new one if it is null.
	 * @return sAccountAuthenticator
	 */
	private AccountAuthenticatorImpl getAuthenticator() {
		if (sAccountAuthenticator == null) {
			sAccountAuthenticator = new AccountAuthenticatorImpl(this);
		}
		return sAccountAuthenticator;
	}

	/**
	 * Handles Account Authentification.
	 * @author David Granqvist
	 * @author Malte Lenz
	 * @author Johannes Nordkvist
	 *
	 */
	private static class AccountAuthenticatorImpl extends AbstractAccountAuthenticator {
		/** The context. */
		private Context mContext;

		/**
		 * Sets up a new AccountAuthenticatorImpl with context.
		 * @param context to be used within this class
		 */
		public AccountAuthenticatorImpl(final Context context) {
			super(context);
			mContext = context;
		}

		/*
		 *  The user has requested to add a new account to the system.  We return an intent that will launch our login screen if the user has not logged in yet,
		 *  otherwise our activity will just pass the user's credentials on to the account manager.
		 */
		@Override
		public Bundle addAccount(final AccountAuthenticatorResponse response, final String accountType,
				final String authTokenType, final String[] requiredFeatures, final Bundle options)
		throws NetworkErrorException {
			Bundle reply = new Bundle();

			Intent i = new Intent(mContext, Login.class);
			i.putExtra("account_create", true);
			i.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
			reply.putParcelable(AccountManager.KEY_INTENT, i);

			return reply;
		}

		@Override
		public Bundle confirmCredentials(final AccountAuthenticatorResponse response, final Account account, final Bundle options) {
			return null;
		}

		@Override
		public Bundle editProperties(final AccountAuthenticatorResponse response, final String accountType) {
			return null;
		}

		@Override
		public Bundle getAuthToken(final AccountAuthenticatorResponse response, final Account account,
				final String authTokenType, final Bundle options) throws NetworkErrorException {
			return null;
		}

		@Override
		public String getAuthTokenLabel(final String authTokenType) {
			return null;
		}

		@Override
		public Bundle hasFeatures(final AccountAuthenticatorResponse response, final Account account, final String[] features) throws NetworkErrorException {
			return null;
		}

		@Override
		public Bundle updateCredentials(final AccountAuthenticatorResponse response, final Account account, final String authTokenType, final Bundle options) {
			return null;
		}
	}
}

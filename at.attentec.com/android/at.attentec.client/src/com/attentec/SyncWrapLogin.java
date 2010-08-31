package com.attentec;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.content.Context;
import android.os.Bundle;

/**
 * This class is a wrapper around the callback from the login class.
 * It is needed to disable the contact sync with the phones contact list
 * in android 1.6.
 * @author David Granqvist
 * @author Malte Lenz
 * @author Johannes Nordkvist
 *
 */
final class SyncWrapLogin {
	//private static final String TAG = "Attentec->SyncWrapLogin";

	static {
		try {
			Class.forName("android.accounts.Account");
		} catch (Exception e) {
			//Does not have 2.1
			throw new RuntimeException(e);
		}
	}

	/**
	 * Empty function that is called to check if the Account class (above) is available.
	 */
	public static void checkAvailable() { }

	/**
	 * Creates an Account and passes it to the account manager.
	 * @param username of the user
	 * @param accountType string for the account name
	 * @param ctx calling context
	 * @param key phone key for authorization
	 * @param extras bundle with extras.
	 */
	public static void passResponse(final String username, final String accountType, final Context ctx, final String key, final Bundle extras) {
		Account account = new Account(username, accountType);
		AccountManager am = AccountManager.get(ctx);
		boolean accountCreated = am.addAccountExplicitly(account, key, null);

		if (accountCreated) {  //Pass the new account back to the account manager
			AccountAuthenticatorResponse response = extras.getParcelable(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
			Bundle result = new Bundle();
			result.putString(AccountManager.KEY_ACCOUNT_NAME, username);
			result.putString(AccountManager.KEY_ACCOUNT_TYPE, accountType);
			response.onResult(result);
		}
	}

	/**
	 * Empty default unused constructor.
	 */
	private SyncWrapLogin() { }
}

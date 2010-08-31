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
import java.util.List;

import org.apache.http.NameValuePair;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Shows a login screen, and performs the authentication with the server.
 * @author David Granqvist
 * @author Malte Lenz
 * @author Johannes Nordkvist
 *
 */
public class Login extends Activity {

	/**
	 * Tag used for logging.
	 */
	private static final String TAG = "Attentec->Login";

	/**
	 * Dialog while contacting server.
	 */
	private ProgressDialog pd;

	/**
	 * Identifier for contacting server dialog.
	 */
	private static final int LOGIN_DIALOG = 1;

	/** Flag if we are using an android version that has phone sync classes. */
	private static boolean mIsTwoPointOneOrHigher;

	/**
	 * Resources item.
	 */
	private Resources res;

	/** constant that matches the request for a phone_key and the resulting code from the QR scanner. */
	private static final int REQUEST_PHONE_KEY_SCANNER_REQUEST_CODE = 0;

	/** Check what version of android we are running */
	static {
		try {
			SyncWrapLogin.checkAvailable();
			mIsTwoPointOneOrHigher = true;
		} catch (Throwable e) {
			mIsTwoPointOneOrHigher = false;
		}
	}

	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login);
		makeButton();

		Button scanButton = (Button) findViewById(R.id.scan_button);
		scanButton.setOnClickListener(new Button.OnClickListener() {
			public void onClick(final View v) {
				//save username so we don't lose it while reading qr code
				EditText usernameEditText = (EditText) findViewById(R.id.txt_username);
				saveUserName(usernameEditText.getText().toString());

				//read qr code
				Intent intent = new Intent("com.google.zxing.client.android.SCAN");
				intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
				try {
					startActivityForResult(intent, REQUEST_PHONE_KEY_SCANNER_REQUEST_CODE);
				} catch (ActivityNotFoundException e) {
					showFailedScan();
				}
			}
		});

		if (!readKey().equals("")) {
			login(readKey(), readUserName());
		}
	}

	/**
	 * Show text that scanning the phone key failed.
	 */
	protected final void showFailedScan() {
		Toast.makeText(this, R.string.scan_fail, Toast.LENGTH_LONG).show();
	}

	/**
	 * Handle the result of scanning a phone key QR code with the barcode scanner app.
	 * @param requestCode A code that tells which activity the request is bound to.
	 * @param resultCode The resultet code after QR code scan.
	 * @param intent dummy, never used
	 */
	@Override
	public final void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
		if (requestCode == REQUEST_PHONE_KEY_SCANNER_REQUEST_CODE) {
			if (resultCode == RESULT_OK) {
				String contents = intent.getStringExtra("SCAN_RESULT");
				//Handle successful scan
				saveKey(contents);
				if (!readUserName().equals("")) {
					login(readKey(), readUserName());
				} else {
					EditText passwordEditText = (EditText) findViewById(R.id.txt_password);
					passwordEditText.setText(readKey());
				}
			} /*else if (resultCode == RESULT_CANCELED) {
				//If user cancels, we don't need to do anything
			}*/
		}
	}

	@Override
	protected final void onDestroy() {
		Log.d(TAG, "Login onDestroy");
		super.onDestroy();
	}

	@Override
	protected final void onPause() {
		Log.d(TAG, "Login onpause");
		super.onPause();
	}

	@Override
	protected final void onStart() {
		EditText usernameEditText = (EditText) findViewById(R.id.txt_username);
		usernameEditText.setText(readUserName());
		EditText passwordEditText = (EditText) findViewById(R.id.txt_password);
		passwordEditText.setText(readKey());
		super.onRestart();
	}

	@Override
	protected final void onResume() {
		super.onResume();
	}




	/**
	 * Saves login key for future use for automatic login.
	 * @param k phone key to save
	 */
	public final void saveKey(final String k) {
		SharedPreferences sp = getSharedPreferences("attentec_preferences", MODE_PRIVATE);
		SharedPreferences.Editor editor = sp.edit();
		editor.putString("phone_key", k);
		editor.commit();
	}


	/**
	 * Saves login username for future use for automatic login.
	 * @param u	username to save
	 */
	public final void saveUserName(final String u) {
		SharedPreferences sp = getSharedPreferences("attentec_preferences", MODE_PRIVATE);
		SharedPreferences.Editor editor = sp.edit();
		editor.putString("username", u);
		editor.commit();
	}

	/**
	 * Returns a previously saved key.
	 * @return the saved phone key
	 */
	public final String readKey() {
		SharedPreferences sp = getSharedPreferences("attentec_preferences", MODE_PRIVATE);

		return sp.getString("phone_key", "");
	}

	/**
	 * Returns a previosly saved username.
	 * @return	the saved username
	 */
	public final String readUserName() {
		SharedPreferences sp = getSharedPreferences("attentec_preferences", MODE_PRIVATE);

		return sp.getString("username", "");
	}

	/**
	 * Posts login information to server and the server verifies.
	 * @param localKey	key to try logging in with
	 * @param userName	username to try logging in with
	 */
	@SuppressWarnings("unchecked")
	public final void login(final String localKey, final String userName) {
		Log.d(TAG, "Login Function");
		res = getResources();

		ArrayList<String> l = new ArrayList<String>();
		l.add(localKey);
		l.add(userName);

		showDialog(LOGIN_DIALOG);
		new VerifyLogin().execute(l);

		return;
	}

	/**
	 * Starts the application. This method is called when login was successful.
	 * @return
	 */
	public final void loginSuccess() {
		Log.d(TAG, "Login Success");
		//check if Login was launched for creating an account for synching
		// with phone contact list
		Bundle extras = getIntent().getExtras();
		if (extras != null && extras.getBoolean("account_create")) {
			//this means that we are creating an account for synching
			// with the phones contact list

			//We need to check if we are running a high enough version to do contact sync.
			if (mIsTwoPointOneOrHigher) {
				SyncWrapLogin.passResponse(readUserName(), getString(R.string.ACCOUNT_TYPE), this, readKey(), extras);
			} else {
				Log.e(TAG, "Adding account but below 2.1, not good. Quitting...");
			}
			finish();
			return;
		}
		//Log.d(TAG, "Login end, creating intent");

		Intent myIntent = new Intent(this, Attentec.class);
		startActivity(myIntent);
		finish();
	}

	/**
	 * Starts the application but without the update service.
	 * This method is called when there was an error on login.
	 */
	public final void loginError() {
		Log.d(TAG, "Login Error");
		Intent myIntent = new Intent(this, Attentec.class);
		//login failed, so we do not want to start the service automatically
		myIntent.putExtra("service", false);
		startActivity(myIntent);
	}

	/**
	 * Run on a failed login.
	 * Failed login means the server did not answer or answered incorrectly.
	 */
	public final void loginFail() {
		Log.w(TAG, "Login Fail");
		Toast.makeText(this, R.string.login_fail, Toast.LENGTH_LONG).show();
		saveKey("");
	}

	/**
	 * Make the login button clickable.
	 */
	private void makeButton() {
		Log.d(TAG, "Making new button");
		Button login = (Button) findViewById(R.id.login_button);

		//Start listening for a click on the login button
		login.setOnClickListener(new OnClickListener() {
			public void onClick(final View viewParam) {
				Log.d(TAG, "Clicked the button");
				//this gets the resources in the xml file and assigns it to a local variable of type EditText
				EditText usernameEditText = (EditText) findViewById(R.id.txt_username);
				EditText passwordEditText = (EditText) findViewById(R.id.txt_password);
				//the getText() gets the current value of the text box
				//the toString() converts the value to String data type
				//then assigns it to a variable of type String
				String sUserName = usernameEditText.getText().toString();
				String sPassword = passwordEditText.getText().toString();

				//Resets key-field upon a failed login attempt

				passwordEditText.setText("");
				//Save the login data for future use in authentication. This will enable the user to login without
				//entering login information again.
				saveKey(sPassword);
				saveUserName(sUserName);

				login(readKey(), readUserName());

			}
		});
	}

	@Override
	protected final Dialog onCreateDialog(final int id) {
		if (id == LOGIN_DIALOG) {
			pd = new ProgressDialog(this);
			pd.setMessage(res.getString(R.string.logging_in));
			pd.setTitle(res.getString(R.string.contacting_server));
		}
		return pd;
	}

	/**
	 * Asynchronous task that wait for Login to get verified.
	 * @author David Granqvist
	 * @author Malte Lenz
	 * @author Johannes Nordkvist
	 *
	 */
	private class VerifyLogin extends AsyncTask<List<String>, Void, Boolean> {
		/** URL-path to login for the app. */
		private String url = "/app/login.json";

		@Override
		protected Boolean doInBackground(final List<String>... args) {

			Hashtable<String, List<NameValuePair>> postdata = ServerContact.getLogin(args[0].get(1), args[0].get(0));

			JSONObject result;
			try {
				result = ServerContact.postJSON(postdata, url, getBaseContext());
			} catch (LoginException e) {
				//Wrong login
				return false;
			}
			if (result != null) {
				//correct login
				return true;
			}
			//the result was null, so contact with the server failed
			return null;
		}

		@Override
		protected void onPostExecute(final Boolean result) {
			super.onPostExecute(result);
			dismissDialog(LOGIN_DIALOG);
			if (result == null) {
				//contact with server failed, let people in but no service
				Log.w(TAG, "Login fail on server contact");
				loginError();
			} else if (result) {
				Log.d(TAG, "Login Success");
				loginSuccess();
			} else {
				Log.d(TAG, "Login Fail");
				loginFail();
			}
		}
	}
	/**
	 * LoginException is raised when a login failed.
	 * @author David Granqvist
	 * @author Malte Lenz
	 * @author Johannes Nordkvist
	 */
	public static class LoginException extends Exception {
		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		/**
		 * No message constructor.
		 */
		public LoginException() {

		}

		/**
		 * Message constructor.
		 * @param description	description of exception
		 */
		public LoginException(final String description) {
			super(description);
		}
	}
}

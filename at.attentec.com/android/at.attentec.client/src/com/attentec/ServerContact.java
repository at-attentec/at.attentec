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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.util.Log;



/**
 * Used to login and talk to the server.
 * @author David Granqvist
 * @author Malte Lenz
 * @author Johannes Nordkvist
 *
 */
public class ServerContact extends Activity {

	/**
	 * Tag used for logging.
	 */
	private static final String TAG = "Attentec->ServerContact";

	/**
	 * urlbase that is used to contact the server.
	 */
	private static String urlbase = null;

	/**
	 * Http connection timeout in milliseconds.
	 */
	private static final int CONNECTION_TIMEOUT = 10000;

	/**
	 * Posts POST request to url with data that from values.
	 * Values are put together to a json object before they are sent to server.
	 * postname = { subvar_1_name => subvar_1_value,
	 * 				subvar_2_name => subvar_2_value
	 * 				...}
	 * postname_2 = {...}
	 * ...
	 * @param values hashtable with structure:
	 * @param url path on the server to call
	 * @param ctx calling context
	 * @return JSONObject with data from rails server.
	 * @throws Login.LoginException when login is wrong
	 */
	public static JSONObject postJSON(final Hashtable<String, List<NameValuePair>> values, final String url, final Context ctx) throws Login.LoginException {
		//fetch the urlbase
		urlbase = PreferencesHelper.getServerUrlbase(ctx);

		//create a JSONObject of the hashtable
		List<NameValuePair> pairs = new ArrayList<NameValuePair>();
		Enumeration<String> postnames = values.keys();

		String postname;
		List<NameValuePair> postvalues;
		JSONObject postdata;

		while (postnames.hasMoreElements()) {
			postname = (String) postnames.nextElement();
			postvalues = values.get(postname);
			postdata = new JSONObject();
			for (int i = 0; i < postvalues.size(); i++) {
				try {
					postdata.put(postvalues.get(i).getName(), postvalues.get(i).getValue());
				} catch (JSONException e) {
					Log.w(TAG, "JSON fail");
					return null;
				}
			}
			pairs.add(new BasicNameValuePair(postname, postdata.toString()));
		}

		//prepare the http call
		HttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, CONNECTION_TIMEOUT);
        HttpConnectionParams.setSoTimeout(httpParams, CONNECTION_TIMEOUT);

		HttpClient client = new DefaultHttpClient(httpParams);

		HttpPost post = new HttpPost(urlbase + url);
		//Log.d(TAG, "contacting url: " + post.getURI());
		try {
			post.setEntity(new UrlEncodedFormEntity(pairs, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			return null;
		}

		//call the server
		String response = null;
		ResponseHandler<String> responseHandler = new BasicResponseHandler();
		try {
			response = client.execute(post, responseHandler);
		} catch (IOException e) {
			Log.e(TAG, "Failed in HTTP request: " + e.toString());
			return null;
		}
		//Log.d(TAG, "Have contacted url success: " + post.getURI());
		//read response
		JSONObject jsonresponse;
		try {

			jsonresponse = new JSONObject(response);
		} catch (JSONException e) {
			Log.e(TAG, "Incorrect response from server" + e.toString());
			return null;
		}
		String responsestatus;
		try {
			responsestatus = jsonresponse.getString("Responsestatus");
		} catch (JSONException e1) {
			return null;
		}
		if (!responsestatus.equals("Wrong login")) {
			return jsonresponse;
		} else {
			Log.w(TAG, "Wrong login");
			throw new Login.LoginException("Wrong login");
		}
	}


	/**
	 * Puts user name and phone key together to hashTable with key phone_auth.
	 * @param username The name of the user that is suppose to login.
	 * @param key the key to authenticate the user.
	 * @return Hashtable with user name and phone key.
	 */
	public static Hashtable<String, List<NameValuePair>> getLogin(final String username, final String key) {


		Hashtable<String, List<NameValuePair>> postdata = new Hashtable<String, List<NameValuePair>>();

		List<NameValuePair> logindata = new ArrayList<NameValuePair>();

		logindata.add(new BasicNameValuePair("username", username));
		logindata.add(new BasicNameValuePair("phone_key", key));

		postdata.put("phone_auth", logindata);
		return postdata;

	}

}

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

import java.io.ByteArrayInputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;

import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

/**
 *
 * @author David Granqvist
 * @author Malte Lenz
 * @author Johannes Nordkvist
 *
 */
public class ContactInRadiusActivity extends ListActivity implements SeekBar.OnSeekBarChangeListener {
	private static final int BREAKPOINT_FOR_FASTER_GROWTH = 25;

	private static final double SLOW_GROWTH_FACTOR = 2.15;

	private static final int VALUE_AT_BREAKPOINT = ((Double) Math.pow(BREAKPOINT_FOR_FASTER_GROWTH, SLOW_GROWTH_FACTOR)).intValue();

	private static final String TAG = "Attentec->ContactInRadiusActivity";

	private static final int MINIMUM_DISTANCE = 20;

	private static final int SEEKBAR_MAX = 100;

	private static final double FAST_GROWTH_FACTOR = 3;

	private Resources res;
	private DatabaseAdapter dbh;

	private SeekBar seekBar;

	private TextView progressView;

	private Button emailButton;

	private Button smsButton;

	private Cursor users;


	private SimpleCursorAdapter adapter;

	/** List of KEY_ROWID-s for the users that are selected. */
	private ArrayList<Long> selectedItems = new ArrayList<Long>();


	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		res = getResources();
		setContentView(R.layout.contactinradiusactivity);

		seekBar = (SeekBar) findViewById(R.id.seek_radius);
		seekBar.setOnSeekBarChangeListener(this);

		progressView = (TextView) findViewById(R.id.progress);

		emailButton = (Button) findViewById(R.id.email_button);

		smsButton = (Button) findViewById(R.id.sms_button);

		emailButton.setOnClickListener(new Button.OnClickListener() {
			public void onClick(final View v) {
				//send email

				ArrayList<String> emails = new ArrayList<String>();
				emails.add("");
				//add all users that are both on the cursor and selectedItems
				users.moveToFirst();
				for (int i = 0; i < users.getCount(); i++) {
					if (selectedItems.contains(users.getLong(users.getColumnIndex(DatabaseAdapter.KEY_ROWID)))) {
						emails.add(users.getString(users.getColumnIndex(DatabaseAdapter.KEY_EMAIL)));
					}
					users.moveToNext();
				}

				Intent emailIntent = new Intent(Intent.ACTION_SEND);
				emailIntent.setType("message/rfc822");
				//this is java black magic. It does basically:
				//String[] emailsArr = (String[]) emails.toArray();
				String[] emailsArr = new String[emails.size()];
				emails.toArray(emailsArr);
				emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, (String[]) emailsArr);
				ContactInRadiusActivity.this.startActivity(Intent.createChooser(emailIntent, "Send mail with"));
			}
		});

		smsButton.setOnClickListener(new Button.OnClickListener() {
			public void onClick(final View v) {
				//send sms
				String phoneNumbers = "";
				//check all users in the list (cursor) against the selectedItems list,
				// and add all that are on both.
				users.moveToFirst();

				boolean first = true;
				for (int i = 0; i < users.getCount(); i++) {

					if (selectedItems.indexOf(users.getLong(users.getColumnIndex(DatabaseAdapter.KEY_ROWID))) != -1
							&& !users.getString(users.getColumnIndex(DatabaseAdapter.KEY_PHONE)).equals("")) {

							if (!first) {
									phoneNumbers += ";";
							}
							phoneNumbers += users.getString(users.getColumnIndex(DatabaseAdapter.KEY_PHONE));
							first = false;
					}
					users.moveToNext();

				}
				Log.d(TAG, "adding phone numbers: " + phoneNumbers);
				Intent sendIntent = new Intent(Intent.ACTION_VIEW);

				sendIntent.putExtra("address", phoneNumbers);
				sendIntent.setType("vnd.android-dir/mms-sms");
				ContactInRadiusActivity.this.startActivity(sendIntent);
			}
		});

		dbh = new DatabaseAdapter(this);
		dbh.open();

		users = dbh.getContactsInsideRadius(calculateDistance(seekBar.getProgress()));

		startManagingCursor(users);

		updateText(seekBar.getProgress());
		updateList(seekBar);
	}

	/**
	 * Creates an adapter for the user display.
	 * @return an adapter
	 */
	private SimpleCursorAdapter createAdapter() {
		return new SimpleCursorAdapter(
				this,
				R.layout.contact_list_item_radius, //Specify the row template to use (here, two columns bound to the two retrieved cursor rows).
				users, //Pass in the cursor to bind to .
				new String[] {DatabaseAdapter.KEY_FIRST_NAME,
						DatabaseAdapter.KEY_LAST_NAME,
						DatabaseAdapter.KEY_PHONE,
						DatabaseAdapter.KEY_PHOTO_UPDATED_AT,
						DatabaseAdapter.KEY_CONNECTED_AT,
						DatabaseAdapter.KEY_CONNECTED_AT,
						DatabaseAdapter.KEY_DISTANCE}, //Array of cursor columns to bind to.
						new int[] {R.id.first_name_field,
						R.id.last_name_field,
						R.id.phone_number_field,
						R.id.photo,
						R.id.status_text,
						R.id.status_icon,
						R.id.status_custom_message}) {
			@Override
			public void setViewText(final TextView v, final String text) {
				String finalText = text;
				switch (v.getId()) {
				case R.id.status_text:
					Date connectedAt = null;
					try {
						connectedAt = DatabaseAdapter.DATE_FORMAT.parse(text);
					} catch (ParseException e) {
						//This will happen if location_updated_at is null on server.
						Log.i(TAG, "Could not parse date in setViewText in ContactsActivity.");
						connectedAt = new Date(0); //"null"-date = January 1, 1970, 00:00:00 GM
					}
					if (new Date().getTime() - connectedAt.getTime() < ContactsActivity.TIME_INTERVAL_ONLINE_MILLISECONDS) {
						Integer index = this.getCursor().getColumnIndex(DatabaseAdapter.KEY_STATUS);
						String theStatus = this.getCursor().getString(index);
						theStatus = res.getString(Status.buildStatusFromString(theStatus).getStatusAsHumanStringResourceId());
						finalText = theStatus; //res.getString(R.string.status_online);
					} else {
						finalText = res.getString(R.string.status_offline);
					}
					break;

				case R.id.status_custom_message: //Distance
					if (text.equals("null")) {
						finalText = "";
					}
					try {
						Float f = Float.parseFloat(text);
						finalText = Utilities.meterFormat(f.intValue(), res);
					} catch (Exception e) {
						Log.e(TAG, "Could not parse float, " + text + ", " + e.getMessage());
					}
					break;
				case R.id.phone_number_field:
				default:
					//A filter to empty strings with content null for phone number field.
					if ((text.equals("") || text.equals("null")) && v.getId() == R.id.phone_number_field) {
						finalText = "-";
					}
					break;
				}

				//update text with new filtered content
				super.setViewText(v, finalText);
			}

			@Override
			public void setViewImage(final ImageView v, final String value) {
				// This is a little bit of an ugly hack, but it works.
				if (v.getId() == R.id.photo) {
					if (value.equals("null") || value.equals("") || value == null) {
						v.setImageDrawable(res.getDrawable(R.drawable.missing));
						return;
					}
					int index = this.getCursor().getColumnIndex(DatabaseAdapter.KEY_PHOTO);
					byte[] imageByteArray = this.getCursor().getBlob(index);
					if (imageByteArray != null) {
						ByteArrayInputStream imageStream = new ByteArrayInputStream(imageByteArray);
						Bitmap myImage = BitmapFactory.decodeStream(imageStream);
						v.setImageBitmap(myImage);
					}
				} else if (v.getId() == R.id.status_icon) {
					Date connectionAt = null;
					try {
						connectionAt = DatabaseAdapter.DATE_FORMAT.parse(value);
					} catch (ParseException e) {
						Log.i(TAG, "Could not parse date in setViewImage in ContactsActivity.");
						connectionAt = new Date(0); //"null"-date = January 1, 1970, 00:00:00 GM
					}
					if (new Date().getTime() - connectionAt.getTime() < ContactsActivity.TIME_INTERVAL_ONLINE_MILLISECONDS) {
						//online
						int index = this.getCursor().getColumnIndex(DatabaseAdapter.KEY_STATUS);
						//Log.d(TAG, "Setting image to online");
						v.setImageDrawable(res.getDrawable(Status.buildStatusFromString(this.getCursor().getString(index)).getStatusCircleResourceId()));
					} else {
						//offline
						//Log.d(TAG, "Setting image to offline");
						v.setImageDrawable(res.getDrawable(R.drawable.status_offline_circle));
					}
				}
			}
		};
	}

	/**
	 * Gets called when the list item itself is clicked.
	 * We switch the state of the checkbox, and also flip the state in selectedItems list.
	 * @param l the listview that was clicked
	 * @param v the view that was clicked
	 * @param position the position in the list clicked
	 * @param id the id (KEY_ROWID) of the user
	 */
	@Override
	protected final void onListItemClick(final ListView l, final View v, final int position, final long id) {
		super.onListItemClick(l, v, position, id);
		//add or remove from selectedItems
		flip(id);
		handleButtonState();
		//switch checked state of the checkbox
		CheckBox view = (CheckBox) v.findViewById(R.id.checkbox);
		view.setChecked(!view.isChecked());
	}

	/**
	 * Flips the state of a user/item in the selectedItems list.
	 * @param id KEY_ROWID of the user
	 */
	private void flip(final long id) {
		if (selectedItems.contains(id)) {
			selectedItems.remove(id);
		} else {
			selectedItems.add(id);
		}
	}

	@Override
	protected final void onDestroy() {
		dbh.close();
		super.onDestroy();
	}


	/**
	 * Called whenever the slider is changed (during the slide action).
	 * @param sBar the slidden seekbar
	 * @param progress the distance selected (nr between 0 and 100)
	 * @param fromTouch if this was produced by the user touching the slider
	 */
	public final void onProgressChanged(final SeekBar sBar, final int progress, final boolean fromTouch) {
		updateText(progress);
	}


	/**
	 * Updates the field displaying the selected radius.
	 * @param progress the progress used for calculating radius
	 */
	private void updateText(final int progress) {
		String formattedDistance = Utilities.meterFormat(calculateDistance(progress), res);
		progressView.setText(formattedDistance);
	}

	/**
	 * Converts a number between 0 and 100 to a distance in meters.
	 * @param progress number between 0 and 100
	 * @return string of distance in meters
	 */
	private Integer calculateDistance(final int progress) {
		if (progress == SEEKBAR_MAX) {
			return Utilities.DISTANCE_INFINITY;
		} else if (progress <= BREAKPOINT_FOR_FASTER_GROWTH) {
			return ((Double) Math.pow(progress, SLOW_GROWTH_FACTOR)).intValue() + MINIMUM_DISTANCE;
		} else {
			return VALUE_AT_BREAKPOINT + MINIMUM_DISTANCE
			+ ((Double) Math.pow(progress - BREAKPOINT_FOR_FASTER_GROWTH, FAST_GROWTH_FACTOR)).intValue();
		}
	}

	/**
	 * Called when the user starts sliding.
	 * @param sBar the seekbar slidden
	 */
	public void onStartTrackingTouch(final SeekBar sBar) {
	}

	/**
	 * Called when the user stops sliding,
	 * which is when we want to update the list of users.
	 * @param sBar the seekbar slidden
	 */
	public final void onStopTrackingTouch(final SeekBar sBar) {
		//update list of people
		updateList(sBar);
	}

	/**
	 * Updates the cursor with the new radius, and updates the adapter/list.
	 * @param sBar seekbar with data on radius selected
	 */
	private void updateList(final SeekBar sBar) {
		users = dbh.getContactsInsideRadius(calculateDistance(sBar.getProgress()));
		adapter = createAdapter();

		setListAdapter(adapter);
		users.moveToFirst();

		for (int i = 0; i < users.getCount(); i++) {
			selectedItems.add(users.getLong(users.getColumnIndex(DatabaseAdapter.KEY_ROWID)));
			users.moveToNext();
		}
		handleButtonState();

		//Show a message if our location is old
		SharedPreferences sp = getSharedPreferences("attentec_preferences", MODE_PRIVATE);
		long ownLocationUpdated = sp.getLong("location_updated_at", 0) * DevelopmentSettings.MILLISECONDS_IN_SECOND;
		TextView noOwnLocationView = (TextView) findViewById(R.id.no_own_location);
		if (new Date().getTime() - ownLocationUpdated > ContactsActivity.TIME_INTERVAL_ONLINE_MILLISECONDS) {
			noOwnLocationView.setVisibility(View.VISIBLE);
		} else {
			noOwnLocationView.setVisibility(View.GONE);
		}

	}

	/**
	 * Handle if the contacting buttons should be active.
	 */
	public final void handleButtonState() {

		boolean state;
		if (users.getCount() == 0 || selectedItems.size() == 0) {
			state = false;
		} else {
			state = true;
		}

		emailButton.setEnabled(state);
		smsButton.setEnabled(state);
	}


}

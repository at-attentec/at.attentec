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
import java.util.Date;
import java.util.Observable;
import java.util.Observer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.attentec.AttentecService.ServiceUpdateUIListener;

/**
 * Activity that shows a contact list.
 * @author David Granqvist
 * @author Malte Lenz
 * @author Johannes Nordkvist
 *
 */
public class ContactsActivity extends ListActivity implements Observer {

	/**
	 * Tag used for logging.
	 */
	private static final String TAG = "Attentec->ContactsActivity";

	private DatabaseAdapter dbh;

	//	private Context mContext = null;

	private long dialogid = 0;

	private Resources res;

	private Cursor contactCursor;

	private ContactDialogCreator cdc = null;

	private static final int LOCAL_FIRST_MENU_ITEM = 1;
	private static final int LOCAL_SECOND_MENU_ITEM = 2;
	private static final int LOCAL_THIRD_MENU_ITEM = 3;
	private static final int LOCAL_FOURTH_MENU_ITEM = 4;

	private static final int ORDER_BY_CONTACTED_AT = Attentec.NR_MENU_ITEMS + Menu.FIRST + LOCAL_FIRST_MENU_ITEM;
	private static final int ORDER_BY_FIRST_NAME   = Attentec.NR_MENU_ITEMS + Menu.FIRST + LOCAL_SECOND_MENU_ITEM;
	private static final int ORDER_BY_LAST_NAME    = Attentec.NR_MENU_ITEMS + Menu.FIRST + LOCAL_THIRD_MENU_ITEM;
	private static final int ORDER_BY_DISTANCE     = Attentec.NR_MENU_ITEMS + Menu.FIRST + LOCAL_FOURTH_MENU_ITEM;

	private int orderMode = DatabaseAdapter.ORDER_BY_FIRST_NAME;

	private ListAdapter adapter;

	private Status status;

	/** Time until someone is seen as offline since last update. */
	public static final int TIME_INTERVAL_ONLINE_MILLISECONDS = 60 * 60 * 1000;

	/** Minimum length for contact info (email/phone). */
	private static final int MINIMUM_ACCEPTED_CONTACT_INFO_LENGTH = 4;

	/**
	 * The two kinds of dialogs used.
	 */
	private enum DialogType { CONTACT_COMMUNICATION_DIALOG, SET_STATUS_DIALOG }
	private DialogType dialogType = null;

	private StatusDialogCreator sdc;

	private TextView statusBarStatus;
	private TextView statusBarCustomMessage;
	private ImageView statusBarIcon;

	private SharedPreferences sp;

	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.contactsactivity);

		sp = getSharedPreferences("attentec_preferences", MODE_PRIVATE);
		res = getResources();

		//show the correct version of the status bar (enabled/disabled)
		updateStatusBarVisibility();
		//Setup status bar
		final RelativeLayout statusBar = (RelativeLayout) findViewById(R.id.statusbar);
		statusBarIcon = (ImageView) findViewById(R.id.statusbar_icon);
		statusBarStatus = (TextView) findViewById(R.id.statusbar_status);
		statusBarCustomMessage = (TextView) findViewById(R.id.statusbar_custom_message);
		status = new Status();
		status.addObserver(this);
		status.load(sp);
		status.save(sp, this); //Save so that it updates against server.


		//fetch the statusbar blocks
		final LinearLayout statusBarStatusBlock = (LinearLayout) findViewById(R.id.statusbar_status_block);
		final LinearLayout statusBarCustomMessageBlock = (LinearLayout) findViewById(R.id.statusbar_custom_message_block);

		//make the statusbar status block change color on touch
		statusBarStatusBlock.setOnTouchListener(new OnTouchListener() {
			public boolean onTouch(final View view, final MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_UP) {
					statusBarStatusBlock.setBackgroundDrawable(statusBar.getBackground());
				} else {
					statusBarStatusBlock.setBackgroundResource(android.R.color.darker_gray);
				}
				return false;
			}
		});

		//make the statusbar custom message block change color on touch
		statusBarCustomMessageBlock.setOnTouchListener(new OnTouchListener() {
			public boolean onTouch(final View view, final MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_UP) {
					statusBarCustomMessageBlock.setBackgroundDrawable(statusBar.getBackground());
				} else {
					statusBarCustomMessageBlock.setBackgroundResource(android.R.color.darker_gray);
				}
				return false;
			}
		});

		//Make the full Status and  blocks as clickable.
		statusBarStatusBlock.setOnClickListener(new OnClickListener() {
			public void onClick(final View view) {
				showSetStatus();
			}
		});

		statusBarCustomMessageBlock.setOnClickListener(new OnClickListener() {
			public void onClick(final View view) {
				showSetCustomStatus();
			}

		});

		//Setup database
		dbh = new DatabaseAdapter(this);
		dbh.open();

		contactCursor = dbh.getContent(orderMode);

		startManagingCursor(contactCursor);

		//Now create a new list adapter bound to the cursor.
		adapter = createAdapter();

		setListAdapter(adapter);

		ListView contactlist = (ListView) findViewById(android.R.id.list);

		contactlist.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(final AdapterView<?> parent, final View v, final int position, final long id) {
				showContact(id);
			}
		});

		AttentecService.setContactsUpdateListener(new ServiceUpdateUIListener() {
			public void updateUI() {
				//update the list of contacts
				updateView();
			}

			public void endActivity() {
				//Close activity (go back to login screen)
				finish();
			}
		});

		cdc = new ContactDialogCreator(this, res, 0, dbh);
	}

	@Override
	protected final void onResume() {
		registerReceiver(receiver, new IntentFilter(AttentecService.COM_ATTENTEC_SERVICE_CHANGED_STATUS));
		updateStatusBarVisibility();
		super.onResume();
	}

	@Override
	protected final void onPause() {
		unregisterReceiver(receiver);
		super.onPause();
	}

	/** Receives broadcasts from the service when it starts or stops. */
	private BroadcastReceiver receiver = new BroadcastReceiver() {
		public void onReceive(final Context context, final Intent intent) {
			updateStatusBarVisibility();
		}
	};

	/** Enables or disables the fields for setting status. */
	protected final void updateStatusBarVisibility() {
		RelativeLayout statusBar = (RelativeLayout) findViewById(R.id.statusbar);
		RelativeLayout disabledStatusBar = (RelativeLayout) findViewById(R.id.disabled_statusbar);
		if (AttentecService.getIsAlive()) {
			statusBar.setVisibility(View.VISIBLE);
			disabledStatusBar.setVisibility(View.GONE);
			Log.d(TAG, "Service is alive");
		} else {
			statusBar.setVisibility(View.GONE);
			disabledStatusBar.setVisibility(View.VISIBLE);
			Log.d(TAG, "Service is not alive");
		}
	}


	/**
	 * Show dialog for setting custom status message.
	 */
	private void showSetCustomStatus() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle(res.getString(R.string.status_custom_message_dialog_title));
		//Get translation for maximum message limit
		String limitMessage = res.getString(R.string.status_custom_message_dialog_message);
		//And replace # with the actual limit.
		limitMessage = limitMessage.replace("#", Integer.toString(Status.CUSTOM_MESSAGE_MAXIMUM_LENGTH));
		alert.setMessage(limitMessage);
		alert.setIcon(android.R.color.transparent);

		// Set an EditText view to get user input
		final EditText input = new EditText(this);
		//Set the text to the previous message.
		input.setText(status.getCustomMessage());
		//Single line, means no new lines.
		input.setSingleLine();
		//Set maximum length of input
		InputFilter[] filterArray = new InputFilter[1];
		filterArray[0] = new InputFilter.LengthFilter(Status.CUSTOM_MESSAGE_MAXIMUM_LENGTH);
		input.setFilters(filterArray);
		//Add the text input to the alert dialog.
		alert.setView(input);

		alert.setPositiveButton(res.getString(R.string.ok), new DialogInterface.OnClickListener() {
			public void onClick(final DialogInterface dialog, final int whichButton) {
				String customMessage = input.getText().toString();
				status.updateStatus(null, customMessage);
			}
		});

		alert.setNegativeButton(res.getString(R.string.cancel), new DialogInterface.OnClickListener() {
			public void onClick(final DialogInterface dialog, final int whichButton) {
				// Canceled.
			}
		});
		alert.show();
	}

	/**
	 * Show the different kind of status a user can set.
	 */
	protected final void showSetStatus() {
		dialogType = DialogType.SET_STATUS_DIALOG;
		showDialog(0);
	}

	/**
	 * Show contact information.
	 * @param id of the dialog
	 */
	protected final void showContact(final long id) {
//		Log.d(TAG, "showContact: " + id);
		dialogid = id;
		int hasThings = 0;
		if (dbh.getContactPhone(dialogid).length() > MINIMUM_ACCEPTED_CONTACT_INFO_LENGTH) {
			hasThings += ContactDialogCreator.DIALOG_HAS_PHONE;
		}
		if (dbh.getContactEmail(dialogid).length() > MINIMUM_ACCEPTED_CONTACT_INFO_LENGTH) {
			hasThings += ContactDialogCreator.DIALOG_HAS_EMAIL;
		}
		if (dbh.isLocationFresh(dialogid)) {
			String lat = dbh.getContactLatitude(dialogid);
			if (lat != null && !lat.equals("null") && !lat.equals("")) {
				hasThings += ContactDialogCreator.DIALOG_HAS_LOCATION;
			}
		}

		dialogType = DialogType.CONTACT_COMMUNICATION_DIALOG;
		showDialog(hasThings);
	}

	@Override
	protected final void onPrepareDialog(final int id, final Dialog dialog) {
//		Log.d(TAG, "onPrepareDialog: " + dialogid);
		if (dialogType == DialogType.CONTACT_COMMUNICATION_DIALOG) {
			dialog.setTitle(res.getString(R.string.contact) + " " + dbh.getContactName(dialogid) + ":");
			cdc.setDialogId(dialogid);
		} /* else if (dialogType == DialogType.SET_STATUS_DIALOG) {
		//Do nothing
		} */
	}

	@Override
	protected final Dialog onCreateDialog(final int id) {
//		Log.d(TAG, "onCreateDialog");
		if (dialogType == DialogType.CONTACT_COMMUNICATION_DIALOG) {
			cdc.setDialogType(id);
			return cdc.getDialog();
		} else if (dialogType == DialogType.SET_STATUS_DIALOG) {
			sdc = new StatusDialogCreator(this, status);
			return sdc.getDialog();
		}
		return null;
	}

	@Override
	public final boolean onOptionsItemSelected(final MenuItem item) {
		int i = item.getItemId();
		switch(i) {
		case ORDER_BY_CONTACTED_AT:
			orderMode = DatabaseAdapter.ORDER_BY_CONTACTED_AT;
			break;
		case ORDER_BY_FIRST_NAME:
			orderMode = DatabaseAdapter.ORDER_BY_FIRST_NAME;
			break;
		case ORDER_BY_LAST_NAME:
			orderMode = DatabaseAdapter.ORDER_BY_LAST_NAME;
			break;
		case ORDER_BY_DISTANCE:
			orderMode = DatabaseAdapter.ORDER_BY_DISTANCE;
			break;
		default:
			break;
		}
		if (i == ORDER_BY_CONTACTED_AT || i == ORDER_BY_FIRST_NAME || i == ORDER_BY_LAST_NAME || i == ORDER_BY_DISTANCE) {
			setNewOrderMode();
		}
		Log.d(TAG, "End of clicked menu, order_mode is: " + orderMode);
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Get the contacts in order "order_mode" and rebuild the list.
	 */
	protected final void setNewOrderMode() {
		Log.d(TAG, "Updating list with order_mode: " + orderMode);
		contactCursor = dbh.getContent(orderMode);
		startManagingCursor(contactCursor);

		//rebuild the list
		adapter = createAdapter();

		setListAdapter(adapter);
	}

	/**
	 * Attentec.java creates the basic menu which contains a button to stop/start service.
	 * This method extends that method in Attentec.java  with additional
	 * functionality specific for this tab.
	 * The first part of the menu from Attentec.java is still preserved.
	 * @param menu to which items should be added
	 * @return always true
	 */
	@Override
	public final boolean onPrepareOptionsMenu(final Menu menu) {
		super.onPrepareOptionsMenu(menu);
		menu.add(Menu.NONE, ORDER_BY_CONTACTED_AT, Menu.NONE, R.string.menu_order_by_contacted_at).setIcon(android.R.drawable.ic_menu_recent_history);
		menu.add(Menu.NONE, ORDER_BY_FIRST_NAME, Menu.NONE, R.string.menu_order_by_first_name).setIcon(android.R.drawable.ic_menu_sort_alphabetically);
		menu.add(Menu.NONE, ORDER_BY_LAST_NAME, Menu.NONE, R.string.menu_order_by_last_name).setIcon(android.R.drawable.ic_menu_sort_alphabetically);
		menu.add(Menu.NONE, ORDER_BY_DISTANCE, Menu.NONE, R.string.menu_order_by_distance).setIcon(android.R.drawable.ic_menu_compass);
		return true;
	}

	/**
	 * Create an adapter to handle the contact list items.
	 * @return ListAdapter
	 */
	private ListAdapter createAdapter() {
		SimpleCursorAdapter simpleCursorAdapter = new SimpleCursorAdapter(
				this,
				R.layout.contact_list_item, //Specify the row template to use (here, two columns bound to the two retrieved cursor rows).
				contactCursor, //Pass in the cursor to bind to .
				new String[] {DatabaseAdapter.KEY_FIRST_NAME,
						DatabaseAdapter.KEY_LAST_NAME,
						DatabaseAdapter.KEY_PHONE,
						DatabaseAdapter.KEY_PHOTO_UPDATED_AT,
						DatabaseAdapter.KEY_CONNECTED_AT,
						DatabaseAdapter.KEY_CONNECTED_AT,
						DatabaseAdapter.KEY_CUSTOM_STATUS}, //Array of cursor columns to bind to.
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
					if (new Date().getTime() - connectedAt.getTime() < TIME_INTERVAL_ONLINE_MILLISECONDS) {
						Integer index = this.getCursor().getColumnIndex(DatabaseAdapter.KEY_STATUS);
						String theStatus = this.getCursor().getString(index);
						theStatus = res.getString(Status.buildStatusFromString(theStatus).getStatusAsHumanStringResourceId());
						finalText = theStatus; //res.getString(R.string.status_online);
					} else {
						finalText = res.getString(R.string.status_offline);
					}
					break;

				case R.id.status_custom_message:
					if (text.equals("null")) {
						finalText = "";
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
					if (new Date().getTime() - connectionAt.getTime() < TIME_INTERVAL_ONLINE_MILLISECONDS) {
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

		return simpleCursorAdapter;
	}

	/**
	 * Update distances and resort the contact list. Called by AttentecService.
	 */
	public final void updateView() {
		//Log.d(TAG, "updateView");
		runOnUiThread(new Runnable() {

			public void run() {
				double lng = sp.getFloat("longitude", 0);
				double lat = sp.getFloat("latitude", 0);
				try {
					//Log.d(TAG, "Updating Distances");
					dbh.updateDistances(lng, lat);
				} catch (ParseException e) {
					Log.e(TAG, "Could not update distances");
				}
//				setNewOrderMode();
				contactCursor.requery();
			}
		});
	}

	/**
	 * Stop UI Thread. Called by AttentectService.
	 */
	public final void endActivity() {
		runOnUiThread(new Runnable() {
			public void run() {
				//finish activity
				finish();
			}
		});
	}

	@Override
	protected final void onDestroy() {
		cdc = null;
		AttentecService.setContactsUpdateListener(null);
		dbh.close();
		super.onDestroy();
	}

	/**
	 * Updates the Status text above contactlist.
	 * @param observable unused
	 * @param arg1 unused
	 */
	public final void update(final Observable observable, final Object arg1) {
		statusBarIcon.setImageDrawable(res.getDrawable(status.getStatusCircleResourceId()));
		statusBarStatus.setText(res.getString(status.getStatusAsHumanStringResourceId()));
		String customMessage = status.getCustomMessage();

		//Save status to server and preferences.
		status.save(sp, this);
		if (!customMessage.equals("")) {
			statusBarCustomMessage.setText(customMessage);
		} else { //Show that no status message is set.
			statusBarCustomMessage.setText(res.getString(R.string.no_custom_status));
		}
	}

}

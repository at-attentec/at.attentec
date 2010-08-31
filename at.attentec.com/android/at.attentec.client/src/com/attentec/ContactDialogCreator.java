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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Creates Contact dialog.
 * @author David Granqvist
 * @author Malte Lenz
 * @author Johannes Nordkvist
 *
 */
public class ContactDialogCreator {
	//These are used as a bitfield, therefore need to be powers of 2
	/** Constant for telling that the dialog has email. */
	public static final int DIALOG_HAS_EMAIL = 1;
	/** Constant for telling that the dialog has phone. */
	public static final int DIALOG_HAS_PHONE = 2;
	/** Constant for telling that the dialog has location. */
	public static final int DIALOG_HAS_LOCATION = 4;

//	private static final String TAG = "Attentec->ContactDialogCreator";

	private Context mContext;
	private Integer mItems;
	private Resources mRes;
	private DatabaseAdapter mDbh;
	private long mDialogid;
	private boolean hasEmail;
	private boolean hasPhone;
	private boolean hasLocation;

	/**
	 * Constructor for creating contact info dialog.
	 * @param ctx the contect
	 * @param res resources fro fetching images.
	 * @param items that should be added.
	 * @param dbh database adapter
	 */
	public ContactDialogCreator(final Context ctx, final Resources res, final Integer items, final DatabaseAdapter dbh) {
		mItems = items;
		mContext = ctx;
		mRes = res;

		mDbh = dbh;
	}

	/**
	 * Sets the type of dialog.
	 * @param items a bitfield of what items to show
	 */
	public final void setDialogType(final Integer items) {
		mItems = items;
	}

	@Override
	protected final void finalize() throws Throwable {
		super.finalize();
	}

	/**
	 * Get dialog id.
	 * @param id of the dialog
	 */
	public final void setDialogId(final long id) {
		mDialogid = id;
	}

	/** Get the id of the user in the dialog.
	 *
	 * @return id of user in dialog
	 */
	private long getDialogid() {
		return mDialogid;
	}


	/**
	 * Create the dialog that should be show and return it.
	 * @return the dialog
	 */
	public final Dialog getDialog() {
		Dialog dialog;
		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		builder.setIcon(android.R.drawable.sym_contact_card);
		builder.setTitle(mRes.getString(R.string.contact));


		ArrayList<String> items = new ArrayList<String>();
		IconicAdapter arrayAdapter = null;
		hasPhone = false;
		hasEmail = false;

		builder.setNegativeButton(mContext.getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
			public void onClick(final DialogInterface dialog, final int whichButton) {
				dialog.cancel();
			}
		});

		items.add(mRes.getString(R.string.view_detailed_info));

		if ((mItems & DIALOG_HAS_PHONE) != 0) {
			items.add(mRes.getString(R.string.call));
			items.add(mRes.getString(R.string.send_message));
			hasPhone = true;
		}
		if ((mItems & DIALOG_HAS_EMAIL) != 0) {
			items.add(mRes.getString(R.string.send_email));
			hasEmail = true;
		}
		if ((mItems & DIALOG_HAS_LOCATION) != 0) {
			items.add(mRes.getString(R.string.navigate_to));
			hasLocation = true;
		}


		arrayAdapter = new IconicAdapter(mContext, items);
		builder.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {


			public void onClick(final DialogInterface dialog, final int item) {
				final int firstMenuItem = 0, secondMenuItem = 1, thirdMenuItem = 2, fourthMenuItem = 3, fifthMenuItem = 4;
				mDbh.updateContactedAt(getDialogid());
				if (item == firstMenuItem) {
					//Detailed Info
					Intent intent = new Intent(mContext, DetailedInfoActivity.class);
					intent.putExtra(DetailedInfoActivity.KEY_CONTACT_ID, getDialogid());
					mContext.startActivity(intent);
				} else if (item == secondMenuItem && hasPhone) {
					//Call
					Intent intent = new Intent(Intent.ACTION_CALL);
					intent.setData(Uri.parse("tel:" + mDbh.getContactPhone(getDialogid())));
					mContext.startActivity(intent);
				} else if (item == thirdMenuItem && hasPhone) {
					//send sms
					Intent sendIntent = new Intent(Intent.ACTION_VIEW);
					sendIntent.putExtra("address", mDbh.getContactPhone(getDialogid()));
					sendIntent.setType("vnd.android-dir/mms-sms");
					mContext.startActivity(sendIntent);
				} else if ((item == fourthMenuItem && hasPhone) || (item == secondMenuItem && !hasPhone && hasEmail)) {
					//send email
					Intent emailIntent = new Intent(Intent.ACTION_SEND);
					emailIntent.setType("message/rfc822");
					emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{mDbh.getContactEmail(getDialogid()), ""});
					mContext.startActivity(Intent.createChooser(emailIntent, "Send mail with"));
				} else if (hasLocation
						&&
						((item == secondMenuItem && !hasPhone && !hasEmail)
						|| (item == thirdMenuItem && !hasPhone && hasEmail)
						|| (item == fourthMenuItem && hasPhone && !hasEmail)
						|| (item == fifthMenuItem && hasPhone && hasEmail))) {
					//navigate to person
					Intent navigateIntent = new Intent(Intent.ACTION_VIEW,
							Uri.parse("http://maps.google.com/?daddr="
									+ mDbh.getContactLatitude(getDialogid()) + "," + mDbh.getContactLongitude(getDialogid())));
					mContext.startActivity(navigateIntent);
				}
			}
		});
		dialog = builder.create();
		return dialog;
	}

	/**
	 * Adds an icon to a contact method, this is done once per menu row.
	 * @author David Granqvist
	 * @author Malte Lenz
	 * @author Johannes Nordkvist
	 *
	 */
	class IconicAdapter extends ArrayAdapter<String> {
		private Context context;

		private ArrayList<String> items = null;

		/**
		 * Create an IconicAdapter.
		 * @param inContext the context
		 * @param sitems dialog items
		 */
		IconicAdapter(final Context inContext, final ArrayList<String> sitems) {
			super(inContext, R.layout.menu_layout, sitems);
			this.items = sitems;
			this.context = inContext;
		}

		/**
		 * Sets the icon in the view.
		 * @param position of the item in the dialog
		 * @param convertView not used
		 * @param parent not used
		 * @return a View with icon
		 */
		public final View getView(final int position, final View convertView, final ViewGroup parent) {

			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View row = inflater.inflate(R.layout.menu_layout, null);
			TextView label = (TextView) row.findViewById(R.id.menutext);

			label.setText(items.get(position));

			ImageView icon = (ImageView) row.findViewById(R.id.menuicon);
			if (items.get(position) == mRes.getString(R.string.call)) {
				icon.setImageResource(android.R.drawable.sym_action_call);
			} else if (items.get(position) == mRes.getString(R.string.send_message)) {
				icon.setImageResource(android.R.drawable.sym_action_chat);
			} else if (items.get(position) == mRes.getString(R.string.send_email)) {
				icon.setImageResource(android.R.drawable.sym_action_email);
			} else if (items.get(position) == mRes.getString(R.string.view_detailed_info)) {
				icon.setImageResource(android.R.drawable.ic_menu_info_details);
			} else if (items.get(position) == mRes.getString(R.string.navigate_to)) {
				icon.setImageResource(android.R.drawable.ic_menu_compass);
			}

			return row;
		}
	}
}

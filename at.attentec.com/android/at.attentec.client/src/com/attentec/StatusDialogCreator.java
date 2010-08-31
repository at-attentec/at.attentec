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
import android.content.res.Resources;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.attentec.Status.StatusIterator;

/**
 * Creates Status dialog.
 * @author David Granqvist
 * @author Malte Lenz
 * @author Johannes Nordkvist
 *
 */
public class StatusDialogCreator {
		/** Tag used for logging. */
		private static final String TAG = "Attentec->StatusDialogCreator";

		private Context mContext;
		private Status mStatus;

		/**
		 * Constructor for creating status dialog.
		 * @param ctx the contect
		 * @param myStatus The my status object that should be changed.
		 */
		public StatusDialogCreator(final Context ctx, final Status myStatus) {
			mContext = ctx;
			mStatus = myStatus;
		}

		/**
		 * Create the dialog that should be shown and return it.
		 * @return the dialog
		 */
		public final Dialog getDialog() {
			Dialog dialog;
			AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
			Resources mRes = mContext.getResources();
			builder.setIcon(android.R.color.transparent);
			builder.setTitle(mRes.getString(R.string.my_status));

			builder.setNegativeButton(mContext.getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
				public void onClick(final DialogInterface dialog, final int whichButton) {
					dialog.cancel();
				}
			});

			final ArrayList<Status> items = new ArrayList<Status>();

			StatusIterator statusIterator = mStatus.getIterator();
			while (statusIterator.hasNext()) {
				Status status = statusIterator.next();
				try {
					items.add(status.clone());
				} catch (CloneNotSupportedException e) {
					Log.e(TAG, "getDialog -> " + e.getMessage());
				}
			}

			StatusAdapter arrayAdapter = null;

			arrayAdapter = new StatusAdapter(mContext, items);
			builder.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
				public void onClick(final DialogInterface dialog, final int item) {
					mStatus.updateStatus(items.get(item).getStatus(), null);
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
		class StatusAdapter extends ArrayAdapter<Status> {
			private Context context;

			private ArrayList<Status> items = null;

			/**
			 * Create a StatusAdapter.
			 * @param inContext the context
			 * @param sitems dialog items
			 */
			StatusAdapter(final Context inContext, final ArrayList<Status> sitems) {
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

				label.setText(mContext.getResources().getString(items.get(position).getStatusAsHumanStringResourceId()));
				ImageView icon = (ImageView) row.findViewById(R.id.menuicon);
				icon.setImageDrawable(mContext.getResources().getDrawable(items.get(position).getStatusCircleResourceId()));

				return row;
			}
		}
	}

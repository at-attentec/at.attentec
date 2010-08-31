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

import android.app.Dialog;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;

import com.attentec.AttentecService.ServiceUpdateUIListener;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;

/**
 * Shows a map with contacts at their location, with
 * clickable markers for easy contact.
 *
 * @author David Granqvist
 * @author Malte Lenz
 * @author Johannes Nordkvist
 *
 */
public class CloseToYou extends MapActivity {



	/**
	 * Tag used for logging.
	 */
	private static final String TAG = "Attentec";

	/** Minimum distance to show on screen, so defines a limit on max zoom-in. */
	private static final int MAX_ZOOM = 3000;

	/** Maximum distance to show on screen. */
	private static final int MIN_ZOOM = 3000000;

	// Declares map and its controller.
	/** Instance of the map view. */
	private MapView mapView;
	/** Instance of the map controller. */
	private MapController mc;

	/** Database adapter. */
	private DatabaseAdapter dbh;

	/** Overlay that should show my location. */
	private MyLocationOverlay mlo;

	/** Database cursor for handling locations. */
	private Cursor locationcursor;

	/** Used to show contact info dialog. */
	private long dialogid;

	/** Used to show contact info dialog. */
	private ContactDialogCreator cdc;

	/** Resources . */
	private Resources res;

	/** Maximum latitude that should be showed on the map. */
	private int maxLat = Integer.MIN_VALUE;
	/** Maximum longitude that should be showed on the map. */
	private int maxLng = Integer.MIN_VALUE;
	/** Minimum latitude that should be showed on the map. */
	private int minLat = Integer.MAX_VALUE;
	/** Minimum longitude that should be showed on the map. */
	private int minLng = Integer.MAX_VALUE;

	/** Vertical padding for map bounds. */
	private final Float vpadding = 0.1f;
	/** Bottom padding for map bounds. */
	private final Float hpaddingBottom = 0.1f;
	/** Top padding for map bounds. */
	private final Float hpaddingTop = 0.4f;

	/** Used for converting to GeoPoint integer. */
	private static final int ONE_MILLION = 1000000;
	/** Minimum length for contact info (email/phone). */
	private static final int MINIMUM_ACCEPTED_CONTACT_INFO_LENGTH = 4;

	private ContactItemizedOverlay itemizedOverlay;

	/* Called when the activity is first created. */
	@Override
	public final void onCreate(final Bundle savedInstanceState) {
		Log.d(TAG, "CloseToYou onCreate");

		super.onCreate(savedInstanceState);
		setContentView(R.layout.closetoyou);

		res = getResources();

		//Setup database connection
		dbh = new DatabaseAdapter(this);
		dbh.open();

		locationcursor = dbh.getLocations();
		locationcursor.moveToFirst();
		startManagingCursor(locationcursor);

		//Setup the map
		mapView = (MapView) findViewById(R.id.mapView);
		//Create ContactItemizedOverlay for mapView
		itemizedOverlay = new ContactItemizedOverlay(new ColorDrawable(), mapView);
		mapView.setBuiltInZoomControls(true);
		mapView.displayZoomControls(true);
		mapView.setSatellite(true);
		mc = mapView.getController();

		AttentecService.setCloseToYouUpdateListener(new ServiceUpdateUIListener() {
			public void updateUI() {
				//update the list of contacts
				updateLocations();
			}

			public void endActivity() {
				//Close activity (go back to login screen)
				finish();
			}
		});

		cdc = new ContactDialogCreator(this, res, 0, dbh);
	}

	@Override
	protected final void onPause() {
		//disable own location, as it will keep the GPS alive
		mlo.disableMyLocation();
		super.onPause();
	}

	@Override
	protected final void onResume() {
		mlo = new MyLocationOverlay(this, mapView);
		mlo.enableMyLocation();

		updateLocations();

		maxLat += (int) ((maxLat - minLat) * hpaddingTop);
		minLat -= (int) ((maxLat - minLat) * hpaddingBottom);
		maxLng += (int) ((maxLng - minLng) * vpadding);
		minLng -= (int) ((maxLng - minLng) * vpadding);

		mc.zoomToSpan(Math.max(Math.abs(maxLat - minLat), MAX_ZOOM), Math.abs(maxLng - minLng));

		mc.animateTo(new GeoPoint((maxLat + minLat) / 2, (maxLng + minLng) / 2));

		super.onResume();
	}

	/**
	 * Paint all contacts on map.
	 */
	protected final void updateLocations() {
		this.runOnUiThread(new Runnable() {
			public void run() {
				if (locationcursor.isClosed()) {
					Log.w(TAG, "locationcursor was closed when we tried accessing");
					return;
				}

				maxLat = Integer.MIN_VALUE;
				maxLng = Integer.MIN_VALUE;
				minLat = Integer.MAX_VALUE;
				minLng = Integer.MAX_VALUE;

				//add own location into the calculation for zoom
				SharedPreferences sp = getSharedPreferences("attentec_preferences", MODE_PRIVATE);
				int ownLat = (int) (sp.getFloat("latitude", DevelopmentSettings.DEFAULT_LATITUDE) * ONE_MILLION);
				int ownLng = (int) (sp.getFloat("longitude", DevelopmentSettings.DEFAULT_LONGITUDE) * ONE_MILLION);

				boolean haveOwnLocation = true;
				if (ownLat == DevelopmentSettings.DEFAULT_LATITUDE * ONE_MILLION
						&& ownLng == DevelopmentSettings.DEFAULT_LONGITUDE * ONE_MILLION) {
					haveOwnLocation = false;
				}

				maxLat = Math.max(ownLat, maxLat);
				minLat = Math.min(ownLat, minLat);
				maxLng = Math.max(ownLng, maxLng);
				minLng = Math.min(ownLng, minLng);

				//Add own position
				mapView.getOverlays().clear();
				mapView.getOverlays().add(mlo);

				mapView.invalidate();

				locationcursor.requery();

				if (locationcursor.getCount() <= 0) {
					Log.d(TAG, "Close to you, locationcursor is empty");
					return;
				}
				locationcursor.moveToFirst();

				//Observe that Views has to be removed since the balloons are views.
				mapView.removeAllViews();
				itemizedOverlay.clear();

				int lat, lng;
				do {

					lat = (int) (Double.valueOf(locationcursor.getString(locationcursor.getColumnIndex(DatabaseAdapter.KEY_LATITUDE))) * ONE_MILLION);
					lng = (int) (Double.valueOf(locationcursor.getString(locationcursor.getColumnIndex(DatabaseAdapter.KEY_LONGITUDE))) * ONE_MILLION);

					maxLat = Math.max(lat, maxLat);
					minLat = Math.min(lat, minLat);
					maxLng = Math.max(lng, maxLng);
					minLng = Math.min(lng, minLng);
					if (haveOwnLocation) {
						maxLat = Math.min(maxLat, ownLat + MIN_ZOOM);
						minLat = Math.max(minLat, ownLat - MIN_ZOOM);
						maxLng = Math.min(maxLng, ownLng + MIN_ZOOM);
						minLng = Math.max(minLng, ownLng - MIN_ZOOM);
					}

					String firstName = locationcursor.getString(locationcursor.getColumnIndex(DatabaseAdapter.KEY_FIRST_NAME));
					String lastName = locationcursor.getString(locationcursor.getColumnIndex(DatabaseAdapter.KEY_LAST_NAME));
					String name = firstName + " " + lastName.charAt(0) + ".";
					Long contactId = locationcursor.getLong(locationcursor.getColumnIndex(DatabaseAdapter.KEY_ROWID));
					byte[] photo = locationcursor.getBlob(locationcursor.getColumnIndex(DatabaseAdapter.KEY_PHOTO));
					Bitmap photoBitmap;
					if (photo != null) {
						ByteArrayInputStream imageStream = new ByteArrayInputStream(photo);
						photoBitmap = BitmapFactory.decodeStream(imageStream);
					}  else {
						photoBitmap = null;
					}
					GeoPoint point = new GeoPoint(lat, lng);
					Drawable statusCircle;


					String connectedAt = locationcursor.getString(locationcursor.getColumnIndex(DatabaseAdapter.KEY_CONNECTED_AT));


					Date d = new Date(0); //long time ago
					if (connectedAt != null) {
						try {
							d = DatabaseAdapter.DATE_FORMAT.parse(connectedAt);
						} catch (ParseException e) {
							Log.e(TAG, "Could not parse locationUpdatedAt: " + e);
						}
					}

					//Log.d(TAG, "Old:" + (d.getTime()));
					//Log.d(TAG, "New:" + (new Date().getTime()));
					//Log.d(TAG, "Difference" + (new Date().getTime() - d.getTime()));
					long offset = new Date().getTime() - d.getTime();
					if (offset < ContactsActivity.TIME_INTERVAL_ONLINE_MILLISECONDS) {
						String statusString = locationcursor.getString(locationcursor.getColumnIndex(DatabaseAdapter.KEY_STATUS));
						statusCircle = res.getDrawable(Status.buildStatusFromString(statusString).getStatusCircleResourceId());
					} else {
						Log.d(TAG, "OFFLINE");
						statusCircle = res.getDrawable(Status.buildStatusFromString(Status.STATUS_OFFLINE_STRING).getStatusCircleResourceId());
					}

					ContactOverlayItem overlayItem = new ContactOverlayItem(point, name, "", contactId, photoBitmap, statusCircle);
					itemizedOverlay.addOverlay(overlayItem);
				} while(locationcursor.moveToNext());

				mapView.invalidate();
			}
		});

	}

	/**
	 * Show contact dialog for one contact.
	 * @param id row for user in database
	 */
	protected final void showContact(final long id) {
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
		showDialog(hasThings);
	}

	@Override
	protected final void onPrepareDialog(final int id, final Dialog dialog) {
		dialog.setTitle(res.getString(R.string.contact) + " " + dbh.getContactName(dialogid) + ":");
		cdc.setDialogId(dialogid);
	}

	@Override
	protected final Dialog onCreateDialog(final int id) {
		cdc.setDialogType(id);
		return cdc.getDialog();
	}

	@Override
	protected final void onDestroy() {
		cdc = null;
		AttentecService.setCloseToYouUpdateListener(null);
		dbh.close();
		Log.d(TAG, "CloseToYou onDestroy");
		super.onDestroy();
	}

	@Override
	protected final boolean isRouteDisplayed() {
		return false;
	}
}

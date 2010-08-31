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

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.util.Linkify;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;


/**
 *
 * @author Sommarjobb2
 *
 */
public class DetailedInfoActivity extends Activity implements SensorEventListener {
	/**
	 *  KEY_CONTACT_ID is used as an identifier for the detailed info.
	 */
	public static final String KEY_CONTACT_ID = "contactId";

	private static final String TAG = "Attentec->DetailedInfoActivity";

	private Resources res;
	private Cursor c;
	private DatabaseAdapter dbd;

	private SensorManager mSensorManager;
	private CompassView mView;

	private boolean haveLocation;
	private static float[] mValues;

	private static Location theirLocation;
	private static Location myLocation;

	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.detailedinfoactivity);

		long id = getIntent().getExtras().getLong(KEY_CONTACT_ID);

		ImageView v = (ImageView) findViewById(R.id.detailed_photo);

		res = getResources();

		dbd = new DatabaseAdapter(this);
		dbd.open();
		c = dbd.getUser(id);
		startManagingCursor(c);
		c.moveToFirst();

		//Fetch own and contacts location for compass.
		SharedPreferences sp = getSharedPreferences("attentec_preferences", MODE_PRIVATE);
		long ownLocationUpdated = sp.getLong("location_updated_at", 0) * DevelopmentSettings.MILLISECONDS_IN_SECOND;
		//check if our own and contacts location is new enough
		if (new Date().getTime() - ownLocationUpdated > ContactsActivity.TIME_INTERVAL_ONLINE_MILLISECONDS
				|| !dbd.isLocationFresh(id)) {
			//Have no own location or contact location, so show no compass
			haveLocation = false;
			mView = (CompassView) findViewById(R.id.compass);
			mView.setVisibility(View.GONE);
			mView = null;
		} else {
			//both locations are new enough, show compass and distance
			haveLocation = true;
			mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
			mView = (CompassView) findViewById(R.id.compass);

			//get my own location
			Float latitude = (sp.getFloat("latitude", DevelopmentSettings.DEFAULT_LATITUDE));
			Float longitude = (sp.getFloat("longitude", DevelopmentSettings.DEFAULT_LONGITUDE));
			myLocation = new Location(LocationManager.GPS_PROVIDER);
			myLocation.setLatitude(latitude);
			myLocation.setLongitude(longitude);

			//get contacts location
			theirLocation = new Location(LocationManager.GPS_PROVIDER);
			theirLocation.setLatitude(c.getFloat(c.getColumnIndex(DatabaseAdapter.KEY_LATITUDE)));
			theirLocation.setLongitude(c.getFloat(c.getColumnIndex(DatabaseAdapter.KEY_LONGITUDE)));

			//show the distance between us
			TextView distance = (TextView) findViewById(R.id.distance_field);
			distance.setText(Utilities.meterFormat((int) myLocation.distanceTo(theirLocation), res));
		}

		int index = c.getColumnIndex(DatabaseAdapter.KEY_PHOTO);
		byte[] imageByteArray = c.getBlob(index);
		if (imageByteArray != null) {
			ByteArrayInputStream imageStream = new ByteArrayInputStream(imageByteArray);
			Bitmap myImage = BitmapFactory.decodeStream(imageStream);
			v.setImageBitmap(myImage);
		}
		TextView statusPhoneNumber = (TextView) findViewById(R.id.phone_number_field);
		index = c.getColumnIndex(DatabaseAdapter.KEY_PHONE);
		statusPhoneNumber.setText(cleanString(c.getString(index)));
		Linkify.addLinks(statusPhoneNumber, Linkify.PHONE_NUMBERS);

		TextView firstName = (TextView) findViewById(R.id.first_name_field);
		index = c.getColumnIndex(DatabaseAdapter.KEY_FIRST_NAME);
		firstName.setText(cleanString(c.getString(index)));

		TextView lastName = (TextView) findViewById(R.id.last_name_field);
		index = c.getColumnIndex(DatabaseAdapter.KEY_LAST_NAME);
		lastName.setText(cleanString(c.getString(index)));

		TextView address = (TextView) findViewById(R.id.address_field);
		index = c.getColumnIndex(DatabaseAdapter.KEY_ADDRESS);
		address.setText(cleanString(c.getString(index)));

		TextView zipcode = (TextView) findViewById(R.id.zipcode_field);
		index = c.getColumnIndex(DatabaseAdapter.KEY_ZIPCODE);
		if (!address.getText().equals("-") || !cleanString(c.getString(index)).equals("-")) {
			zipcode.setText(cleanString(c.getString(index)));
		}

		TextView city = (TextView) findViewById(R.id.city_field);
		index = c.getColumnIndex(DatabaseAdapter.KEY_CITY);
		if (!zipcode.getText().equals("-") && !address.getText().equals("-") || !cleanString(c.getString(index)).equals("-")) {
			city.setText(cleanString(c.getString(index)));
		}

		TextView email = (TextView) findViewById(R.id.email_field);
		index = c.getColumnIndex(DatabaseAdapter.KEY_EMAIL);
		email.setText(cleanString(c.getString(index)));
		Linkify.addLinks(email, Linkify.EMAIL_ADDRESSES);

		TextView degree = (TextView) findViewById(R.id.degree_field);
		index = c.getColumnIndex(DatabaseAdapter.KEY_DEGREE);
		degree.setText(cleanString(c.getString(index)));

		TextView title = (TextView) findViewById(R.id.title_field);
		index = c.getColumnIndex(DatabaseAdapter.KEY_TITLE);
		title.setText(cleanString(c.getString(index)));

		TextView linkedin = (TextView) findViewById(R.id.linkedin_field);
		index = c.getColumnIndex(DatabaseAdapter.KEY_LINKEDIN);
		linkedin.setText(cleanString(c.getString(index)));
		// Recognize web URLs
		Linkify.addLinks(linkedin, Linkify.WEB_URLS);

		TextView client = (TextView) findViewById(R.id.client_field);
		index = c.getColumnIndex(DatabaseAdapter.KEY_CLIENT);
		client.setText(cleanString(c.getString(index)));

		ImageView statusCircle = (ImageView) findViewById(R.id.status_icon);
		Date connectedUpdated = null;
		int statusCircleId;

		String statusKey = c.getString(c.getColumnIndex(DatabaseAdapter.KEY_STATUS));

		TextView statusTextView = (TextView) findViewById(R.id.status_text);

		String connectedAt = c.getString(c.getColumnIndex(DatabaseAdapter.KEY_CONNECTED_AT));

		dbd.close();

		String statusText = res.getString(Status.buildStatusFromString(statusKey).getStatusAsHumanStringResourceId());

		statusCircleId = Status.buildStatusFromString(statusKey).getStatusCircleResourceId();

		if ((connectedAt != null) && !connectedAt.equals("null")) {
			try {

				connectedUpdated = DatabaseAdapter.DATE_FORMAT.parse(connectedAt);
			}  catch (ParseException e) {
				Log.e(TAG, "Date Exception:" + e);
			}
		} else { connectedUpdated = new Date(0); }

		if (new Date().getTime() - connectedUpdated.getTime() < ContactsActivity.TIME_INTERVAL_ONLINE_MILLISECONDS) {
			//online
			//Log.d(TAG, "Setting image to online");
			statusCircle.setImageDrawable(res.getDrawable(statusCircleId));

			statusTextView.setText(statusText);
		} else {
			//offline
			//Log.d(TAG, "Setting image to offline");
			statusCircle.setImageDrawable(res.getDrawable(R.drawable.status_offline_circle));
			statusTextView.setText(res.getString(R.string.status_offline));
		}

	}

	@Override
	protected final void onResume() {
        super.onResume();
        if (haveLocation) {
	        mSensorManager.registerListener(this,
	                        mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
	                        SensorManager.SENSOR_DELAY_GAME);
        }
    }

    /**
     *  View that implements a simple compass needle.
     */
	public static class CompassView extends View {
        private static final int CIRCLE_DEGREES = 360;
		private Paint   mPaint = new Paint();
        private Path    mPath = new Path();

        private static final int ARROW_TIP_Y = -30;
        private static final int ARROW_MIDDLE_X = 0;
        private static final int ARROW_LEFT_TAIL_X = -10;
        private static final int ARROW_OUTER_TAIL_Y = 40;
        private static final int ARROW_INNER_TAIL_Y = 30;
        private static final int ARROW_RIGHT_TAIL_X = 10;

        /**
         * Default constructor, needed for xml inflation to work.
         * @param context calling context
         * @param attrs layout attributes
         */
        public CompassView(final Context context, final AttributeSet attrs) {
            super(context, attrs);

            // Construct a wedge-shaped path
            mPath.moveTo(ARROW_MIDDLE_X, ARROW_TIP_Y);
            mPath.lineTo(ARROW_LEFT_TAIL_X, ARROW_OUTER_TAIL_Y);
            mPath.lineTo(ARROW_MIDDLE_X, ARROW_INNER_TAIL_Y);
            mPath.lineTo(ARROW_RIGHT_TAIL_X, ARROW_OUTER_TAIL_Y);
            mPath.close();
        }

        @Override
		protected final void onDraw(final Canvas canvas) {
            Paint paint = mPaint;

            canvas.drawColor(Color.WHITE);

            paint.setAntiAlias(true);
            paint.setColor(Color.GRAY);
            paint.setStyle(Paint.Style.FILL);

            //check how big our canvas is
            int w = getWidth();
            int h = getHeight();
            int cx = w / 2;
            int cy = h / 2;

            canvas.translate(cx, cy);
            if (mValues != null) {
                canvas.rotate(-(mValues[0] - myLocation.bearingTo(theirLocation)) % CIRCLE_DEGREES);
            }
            canvas.drawPath(mPath, mPaint);
        }
    }

	/**
	 * Returns the string or "-" if null.
	 * @param s the string
	 * @return the string or "-"
	 */
	private String cleanString(final String s) {
		if (s != null && !s.equals("null") && !s.equals("")) {
			return s;
		}
		return "-";
	}

	@Override
	protected final void onStop() {
		if (haveLocation) {
			mSensorManager.unregisterListener(this);
		}
		super.onStop();
	}

	@Override
	protected final void onDestroy() {
		super.onDestroy();
	}

	/**
	 * Required to listen to rotation sensor.
	 * @param arg0 -
	 * @param arg1 -
	 */
	public void onAccuracyChanged(final Sensor arg0, final int arg1) {
		//needed for SensorEventListener
	}

	/**
	 * Called when rotation changed.
	 * @param event data with what event happened
	 */
	public final void onSensorChanged(final SensorEvent event) {
        mValues = event.values;
        if (mView != null) {
        	Log.d(TAG, "mView not null: (" + event.values[0] + ", " + event.values[1] + ", " + event.values[2] + ")");
            mView.invalidate();
        }
	}



}

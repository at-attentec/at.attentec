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
package android.preference;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TimePicker;

import com.attentec.PreferencesHelper;
import com.attentec.R;

/**
 * A preference type that allows a user to choose a time.
 */
public class TimePickerPreference extends DialogPreference implements TimePicker.OnTimeChangedListener {

	/**
	 * The validation expression for this preference.
	 */
	private static final String VALIDATION_EXPRESSION = "[0-2]*[0-9]:[0-5]*[0-9]";

	//private static final String TAG = "Attentec->TimePickerPreference";

	/**
	 * The default value for this preference.
	 */
	private String defaultValue;

	private int mHour = 0;

	private int mMinute = 0;

	/**
	 * @param context calling context
	 * @param attrs attributes
	 */
	public TimePickerPreference(final Context context, final AttributeSet attrs) {
		super(context, attrs);
		initialize();
	}

	/**
	 * @param context calling context
	 * @param attrs attributes
	 * @param defStyle style
	 */
	public TimePickerPreference(final Context context, final AttributeSet attrs, final int defStyle) {
		super(context, attrs, defStyle);
		initialize();
	}

	/**
	 * Initialize this preference.
	 */
	private void initialize() {
		setPersistent(true);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see android.preference.DialogPreference#onCreateDialogView()
	 */
	@Override
	protected final View onCreateDialogView() {

		TimePicker tp = new TimePicker(getContext());
		tp.setIs24HourView(true);
		tp.setOnTimeChangedListener(this);

		int h = getHour();
		int m = getMinute();
		if (h >= 0) {
			tp.setCurrentHour(h);
		}
		if (m >= 0) {
			tp.setCurrentMinute(m);
		}

		return tp;
	}

	/**
	 * Called whenever time is changed.
	 * @param view the view that changed
	 * @param hour changed to
	 * @param minute changed to
	 */
	public final void onTimeChanged(final TimePicker view, final int hour, final int minute) {
		mHour = hour;
		mMinute = minute;
		//persistString(hour + ":" + minute);
	}

	@Override
	public final void onDialogClosed(final boolean positiveResult) {
		if (positiveResult) {
			persistString(mHour + ":" + mMinute);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see android.preference.Preference#setDefaultValue(java.lang.Object)
	 */
	@Override
	public final void setDefaultValue(final Object defValue) {
		//BUG this method is never called if you use the 'android:defaultValue'
		//	attribute in your XML preference file, not sure why it isn't
		super.setDefaultValue(defValue);

		if (!(defValue instanceof String)) {
			return;
		}

		if (!((String) defValue).matches(VALIDATION_EXPRESSION)) {
			return;
		}

		this.defaultValue = (String) defValue;
	}

	/**
	 * Get the hour value (in 24 hour time).
	 *
	 * @return The hour value, will be 0 to 23 (inclusive)
	 */
	private int getHour() {
		String time = getPersistedString(this.defaultValue);
		if (time == null || !time.matches(VALIDATION_EXPRESSION)) {
			if (getKey() == getContext().getResources().getString(R.string.settings_locations_update_end_time_key)) {
				time = PreferencesHelper.DEFAULT_LOCATIONS_UPDATE_END_TIME;
			} else if (getKey() == getContext().getResources().getString(R.string.settings_locations_update_start_time_key)) {
				time = PreferencesHelper.DEFAULT_LOCATIONS_UPDATE_START_TIME;
			} else {
				return -1;
			}
		}

		return Integer.valueOf(time.split(":")[0]);
	}

	/**
	 * Get the minute value.
	 *
	 * @return the minute value, will be 0 to 59 (inclusive)
	 */
	private int getMinute() {
		String time = getPersistedString(this.defaultValue);
		if (time == null || !time.matches(VALIDATION_EXPRESSION)) {
			if (time == null || !time.matches(VALIDATION_EXPRESSION)) {
				if (getKey() == getContext().getResources().getString(R.string.settings_locations_update_end_time_key)) {
					time = PreferencesHelper.DEFAULT_LOCATIONS_UPDATE_END_TIME;
				} else if (getKey() == getContext().getResources().getString(R.string.settings_locations_update_start_time_key)) {
					time = PreferencesHelper.DEFAULT_LOCATIONS_UPDATE_START_TIME;
				} else {
					return -1;
				}
			}
		}

		return Integer.valueOf(time.split(":")[1]);
	}
}
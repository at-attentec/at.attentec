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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import android.content.res.Resources;

/**
 * Contains utility functions.
 * @author David Granqvist
 * @author Malte Lenz
 * @author Johannes Nordkvist
 *
 */
public final class Utilities {
	/** The infinite distance. */
	public static final int DISTANCE_INFINITY = Integer.MAX_VALUE;

	/**
	 * Format meters as xxx m or yyy km if greater or equal to 1000 meters.
	 * @param meters that should be formatted
	 * @param res resources
	 * @return m or km with postfix unit.
	 */
	public static String meterFormat(final int meters, final Resources res) {
		final int kilometersPerMeter = 1000;
		String formattedLength;

		if (meters == DISTANCE_INFINITY) { //Format as infinity kilometers
			formattedLength = res.getString(R.string.infinity) + " " + res.getString(R.string.kilometers);
		} else if (meters >= kilometersPerMeter) { //Format as kilometers
			int kilometers = meters / kilometersPerMeter;

			//Create formatters
			DecimalFormat df = new DecimalFormat();
			DecimalFormatSymbols dfs = new DecimalFormatSymbols();

			//Set thousand separator to space.
			dfs.setGroupingSeparator(' ');
			df.setDecimalFormatSymbols(dfs);

			//Format string
			String formattedString = df.format(kilometers);
			formattedLength =  formattedString + " " + res.getString(R.string.kilometers);
		} else { //Format as meters
			formattedLength = Integer.toString(meters) + " " + res.getString(R.string.meters);
		}
		return formattedLength;
	}

	/**
	 * Unused default constructor.
	 */
	private Utilities() { }
}

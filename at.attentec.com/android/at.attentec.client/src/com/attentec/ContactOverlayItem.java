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

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;

/**
 * Overlay item that holds name and bitmap photo of contact.
 * @author David Granqvist
 * @author Malte Lenz
 * @author Johannes Nordkvist
 *
 */
public class ContactOverlayItem extends OverlayItem {

	private Long mId;
	private Bitmap mPhoto;
	private Drawable mStatusIndicator;

	/**
	 * Constructor for creating an contact overlay.
	 * @param point of the marker
	 * @param title Name of the contact
	 * @param snippet not used
	 * @param id of the user
	 * @param photoBm Bitmap of the photo
	 * @param statusIndicator Drawable for the status
	 */
	public ContactOverlayItem(final GeoPoint point, final String title, final String snippet,
			final Long id, final Bitmap photoBm, final Drawable statusIndicator) {
		super(point, title, snippet);
		mId = id;
		mPhoto = photoBm;
		mStatusIndicator = statusIndicator;
	}

	/**
	 * Get user id.
	 * @return is of the user
	 */
	public final Long getId() {
		return mId;
	}

	/**
	 * Get photo bitmap.
	 * @return Bitmap of the contact
	 */
	public final Bitmap getPhoto() {
		return mPhoto;
	}

	/**
	 *
	 * @return mStatusIndicator
	 */
	public final Drawable getStatusIndicator() {
		return mStatusIndicator;
	}
}

/***
 * Copyright (c) 2010 readyState Software Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Changes made by:
 * Malte Lenz
 * David Granqvist
 *
 */

package com.attentec;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.google.android.maps.MapView;

/**
 * Overlay handles all contact balloons that enables contact balloons to show contact methods by tapping.
 * @author David Granqvist
 * @author Malte Lenz
 * @author Johannes Nordkvist
 *
 */
	public class ContactItemizedOverlay extends BalloonItemizedOverlay<ContactOverlayItem> {

		private ArrayList<ContactOverlayItem> mOverlays = new ArrayList<ContactOverlayItem>();
		private Context c;

		/**
		 * Constructor for creating the ContactItemizedOverlay.
		 * @param defaultMarker Marker that should be drawn
		 * @param mapView the MapView to draw in
		 */
		public ContactItemizedOverlay(final Drawable defaultMarker, final MapView mapView) {
			super(boundCenter(defaultMarker), mapView);
			c = mapView.getContext();
		}

		/**
		 * Clear the overlays.
		 */
		public final void clear() {
			mOverlays.clear();
		}

		/**
		 * Add an balloon (overlay).
		 * @param overlay Balloon that show be added
		 */
		public final void addOverlay(final ContactOverlayItem overlay) {
			mOverlays.add(overlay);
			populate();
			//show the bubble
			this.showBubble(size() - 1);
		}

		@Override
		protected final ContactOverlayItem createItem(final int i) {
			return mOverlays.get(i);
		}

		@Override
		public final int size() {
			return mOverlays.size();
		}

		@Override
		protected final boolean onBalloonTap(final int index) {
			//c is the context of the mapActivity CloseToYou
			//createItem(index).getId() is the ROWID
			// of the user in the database
			((CloseToYou) c).showContact(createItem(index).getId());
			return true;
		}

	}

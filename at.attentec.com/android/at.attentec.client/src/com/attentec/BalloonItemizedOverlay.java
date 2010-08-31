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

import java.lang.reflect.Method;

import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

/**
 * An abstract extension of ItemizedOverlay for displaying an information balloon
 * upon screen-tap of each marker overlay.
 *
 * @author Jeff Gilfelt
 * @param <Item> no clue
 */
public abstract class BalloonItemizedOverlay<Item> extends ItemizedOverlay<OverlayItem> {

	/** Tag used for logging. */
	private static final String TAG = "BalloonItemizedOverlay";

	/** the mapView to draw in. */
	private MapView mapView;
	/** the region that should be clickable. */
	private View clickRegion;

	/** Offset of the balloon. */
	private int viewOffset;
//	/** the map controller. */
//	private final MapController mc;

	/**
	 * Create a new BalloonItemizedOverlay.
	 *
	 * @param defaultMarker - A bounded Drawable to be drawn on the map for each item in the overlay.
	 * @param inMapView - The view upon which the overlay items are to be drawn.
	 */
	public BalloonItemizedOverlay(final Drawable defaultMarker, final MapView inMapView) {
		super(defaultMarker);
		this.mapView = inMapView;
		viewOffset = 0;
//		mc = mapView.getController();
	}

	/**
	 * Set the horizontal distance between the marker and the bottom of the information
	 * balloon. The default is 0 which works well for center bounded markers. If your
	 * marker is center-bottom bounded, call this before adding overlay items to ensure
	 * the balloon hovers exactly above the marker.
	 *
	 * @param pixels - The padding between the center point and the bottom of the
	 * information balloon.
	 */
	public final void setBalloonBottomOffset(final int pixels) {
		viewOffset = pixels;
	}

	/**
	 * Override this method to handle a "tap" on a balloon. By default, does nothing
	 * and returns false.
	 *
	 * @param index - The index of the item whose balloon is tapped.
	 * @return true if you handled the tap, otherwise false.
	 */
	protected abstract boolean onBalloonTap(int index);

	/**
	 * Show the Information bubble with photo and name.
	 * @param index of bubble
	 * @return always true
	 */
	protected final boolean showBubble(final int index) {

		BalloonOverlayView balloonView;
		final int thisIndex;
		GeoPoint point;

		thisIndex = index;
		point = createItem(index).getPoint();

		balloonView = new BalloonOverlayView(mapView.getContext(), viewOffset);
		clickRegion = (View) balloonView.findViewById(R.id.balloon_inner_layout);

		balloonView.setData((ContactOverlayItem) createItem(index));

		MapView.LayoutParams params = new MapView.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, point,
				MapView.LayoutParams.BOTTOM_CENTER);
		params.mode = MapView.LayoutParams.MODE_MAP;

		setBalloonTouchListener(thisIndex);

		balloonView.setVisibility(View.VISIBLE);

		mapView.addView(balloonView, params);

		return true;
	}

	/**
	 * Sets the onTouchListener for the balloon being displayed, calling the
	 * overridden onBalloonTap if implemented.
	 *
	 * @param thisIndex - The index of the item whose balloon is tapped.
	 */
	private void setBalloonTouchListener(final int thisIndex) {

		try {
			@SuppressWarnings("unused")
			Method m = this.getClass().getDeclaredMethod("onBalloonTap", int.class);

			clickRegion.setOnTouchListener(new OnTouchListener() {
				public boolean onTouch(final View v, final MotionEvent event) {

					View l =  ((View) v.getParent()).findViewById(R.id.balloon_main_layout);
					Drawable d = l.getBackground();

					if (event.getAction() == MotionEvent.ACTION_DOWN) {
						int[] states = {android.R.attr.state_pressed};
						if (d.setState(states)) {
							d.invalidateSelf();
						}
						return true;
					} else if (event.getAction() == MotionEvent.ACTION_UP) {
						int[] newStates = {};
						if (d.setState(newStates)) {
							d.invalidateSelf();
						}
						// call overridden method
						onBalloonTap(thisIndex);
						return true;
					} else {
						return false;
					}

				}
			});

		} catch (SecurityException e) {
			Log.e(TAG, "setBalloonTouchListener reflection SecurityException");
			return;
		} catch (NoSuchMethodException e) {
			// method not overridden - do nothing
			return;
		}

	}

}

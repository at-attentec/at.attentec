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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * A view representing a MapView marker information balloon.
 * <p>
 * This class has a number of Android resource dependencies:
 * <ul>
 * <li>drawable/balloon_overlay_bg_selector.xml</li>
 * <li>drawable/balloon_overlay_close.png</li>
 * <li>drawable/balloon_overlay_focused.9.png</li>
 * <li>drawable/balloon_overlay_unfocused.9.png</li>
 * <li>layout/balloon_map_overlay.xml</li>
 * </ul>
 * </p>
 *
 * @author Jeff Gilfelt
 *
 * Changes made by:
 * Malte Lenz
 * David Granqvist
 *
 */
public class BalloonOverlayView extends FrameLayout {

	/** Layout. */
	private LinearLayout layout;
	/** Title (First name + initial). */
	private TextView title;

	/** Photo of the person. */
	private ImageView photo;

	/** Photo of the person. */
	private ImageView statusIndicator;

	/**
	 * Create a new BalloonOverlayView.
	 *
	 * @param context - The activity context.
	 * @param balloonBottomOffset - The bottom padding (in pixels) to be applied
	 * when rendering this view.
	 */
	public BalloonOverlayView(final Context context, final int balloonBottomOffset) {

		super(context);

		final int paddingLeftRight = 10;
		final int paddingTop = 0;

		setPadding(paddingLeftRight, paddingTop, paddingLeftRight, balloonBottomOffset);
		layout = new LinearLayout(context);
		layout.setVisibility(VISIBLE);

		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = inflater.inflate(R.layout.balloon_map_overlay, layout);
		title = (TextView) v.findViewById(R.id.balloon_item_title);
		photo = (ImageView) v.findViewById(R.id.balloon_photo);
		statusIndicator = (ImageView) v.findViewById(R.id.balloon_status_indicator);

		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params.gravity = Gravity.NO_GRAVITY;

		addView(layout, params);

	}

	/**
	 * Sets the view data from a given overlay item.
	 *
	 * @param item - The overlay item containing the relevant view data
	 * (title and snippet).
	 */
	public final void setData(final ContactOverlayItem item) {

		layout.setVisibility(VISIBLE);
		if (item.getTitle() != null) {
			title.setVisibility(VISIBLE);
			title.setText(item.getTitle());
		} else {
			title.setVisibility(GONE);
		}
		Bitmap photoBitmap = item.getPhoto();
		if (photoBitmap != null) {
			photo.setImageBitmap(photoBitmap);
		}
		Drawable statusCircle = item.getStatusIndicator();
		if (statusCircle != null) {
			statusIndicator.setImageDrawable(statusCircle);
		}
	}

}

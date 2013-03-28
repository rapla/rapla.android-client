/*--------------------------------------------------------------------------*
 | Copyright (C) 2012 Maximilian Lenkeit                                    |
 |                                                                          |
 | This program is free software; you can redistribute it and/or modify     |
 | it under the terms of the GNU General Public License as published by the |
 | Free Software Foundation. A copy of the license has been included with   |
 | these distribution in the COPYING file, if not go to www.fsf.org         |
 |                                                                          |
 | As a special exception, you are granted the permissions to link this     |
 | program with every library, which license fulfills the Open Source       |
 | Definition as published by the Open Source Initiative (OSI).             |
 *--------------------------------------------------------------------------*/

package org.rapla.mobile.android.widget;

import org.rapla.entities.dynamictype.Attribute;
import org.rapla.entities.dynamictype.AttributeType;

import android.content.Context;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * This class wraps a rapla dynamic type attribute of type integer and provides
 * the necessary interface for being a list item.
 * 
 * @author Maximilian Lenkeit <dev@lenki.com>
 * 
 */
public class RaplaAttributeInt extends RaplaAttribute<Long> {

	private EditText widget;
	private View cachedView;

	public RaplaAttributeInt(Context context, Attribute attribute) {
		super(context, attribute, AttributeType.INT);
	}

	@Override
	protected void setValueToWidget(Long value) {
		if (this.widget != null && value != null) {
			this.widget.setText(value.toString());
		}

	}

	@Override
	public View getListItemView() {
		// Check if list view has previously been created
		if (this.cachedView == null) {
			// Compose relative layout for list item
			RelativeLayout layout = new RelativeLayout(this.getContext());

			// Add label
			TextView label = this.getLabel();
			layout.addView(label, 0);

			// Compose attribute specific widget and add to view
			this.widget = new EditText(this.getContext());
			this.widget.setInputType(InputType.TYPE_CLASS_NUMBER);
			this.widget.setSingleLine();
			this.setValueToWidget(this.value);
			RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
					RelativeLayout.LayoutParams.FILL_PARENT,
					RelativeLayout.LayoutParams.WRAP_CONTENT);
			params.addRule(RelativeLayout.BELOW, label.getId());
			layout.addView(this.widget, 1, params);

			// Cache view
			this.cachedView = layout;
		}

		return this.cachedView;
	}

	@Override
	protected Long getValueFromWidget() {
		if (this.widget == null) {
			return this.value;
		} else {
			String value = this.widget.getText().toString();
			if(value == null || value.length() == 0) {
				value = "0";
			}
			return Long.parseLong(value);
		}
	}
}
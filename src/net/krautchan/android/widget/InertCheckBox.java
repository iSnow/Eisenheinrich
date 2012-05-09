package net.krautchan.android.widget;

/*
* Copyright (C) 2011 Johannes Jander (johannes@jandermail.de)
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

/*
* Lifted from: http://www.marvinlabs.com/2010/10/custom-listview-ability-check-items/
*/

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.CheckBox;

/**
 * CheckBox that does not react to any user event in order to let the container handle them.
 */
public class InertCheckBox extends CheckBox {

	// Provide the same constructors as the superclass
	public InertCheckBox(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	// Provide the same constructors as the superclass
	public InertCheckBox(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	// Provide the same constructors as the superclass
	public InertCheckBox(Context context) {
		super(context);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// Make the checkbox not respond to any user event
		int action = event.getActionMasked();
		if (action == MotionEvent.ACTION_DOWN) {
			boolean checked = this.isChecked();
			this.setChecked(!checked);
		}
		
		return false;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// Make the checkbox not respond to any user event
		return false;
	}

	@Override
	public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
		// Make the checkbox not respond to any user event
		return false;
	}

	@Override
	public boolean onKeyPreIme(int keyCode, KeyEvent event) {
		// Make the checkbox not respond to any user event
		return false;
	}

	@Override
	public boolean onKeyShortcut(int keyCode, KeyEvent event) {
		// Make the checkbox not respond to any user event
		return false;
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		// Make the checkbox not respond to any user event
		return false;
	}

	@Override
	public boolean onTrackballEvent(MotionEvent event) {
		// Make the checkbox not respond to any user event
		return false;
	}
}

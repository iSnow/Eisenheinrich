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

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.LinearLayout;

/*
 * Lifted from: http://www.marvinlabs.com/2010/10/custom-listview-ability-check-items/
 */
public class CheckableLinearLayout extends  LinearLayout implements Checkable{
	private boolean isChecked;
	private List<Checkable> checkableViews;

	public CheckableLinearLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		initialise(attrs);
	}

	public CheckableLinearLayout(Context context, int checkableId) {
		super(context);
		initialise(null);
	}
	

	@Override
	public boolean isChecked() {
		return isChecked;
	}


	/*
	 * @see android.widget.Checkable#setChecked(boolean)
	 */
	public void setChecked(boolean isChecked) {
		this.isChecked = isChecked;
		for (Checkable c : checkableViews) {
			c.setChecked(isChecked);
		}
	}

	/*
	 * @see android.widget.Checkable#toggle()
	 */
	public void toggle() {
		this.isChecked = !this.isChecked;
		for (Checkable c : checkableViews) {
			c.toggle();
		}
	}
	
	
	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();

		final int childCount = this.getChildCount();
		for (int i = 0; i < childCount; ++i) {
			findCheckableChildren(this.getChildAt(i));
		}
	}

	/**
	 * Add to our checkable list all the children of the view that implement the
	 * interface Checkable
	 */
	private void findCheckableChildren(View v) {
		if (null == checkableViews) {
			checkableViews = new ArrayList<Checkable>();
		}
		if (v instanceof Checkable) {
			checkableViews.add((Checkable) v);
		}

		if (v instanceof ViewGroup) {
			final ViewGroup vg = (ViewGroup) v;
			final int childCount = vg.getChildCount();
			for (int i = 0; i < childCount; ++i) {
				findCheckableChildren(vg.getChildAt(i));
			}
		}
	}
	
	/**
	 * Read the custom XML attributes
	 */
	private void initialise(AttributeSet attrs) {
		//this.isChecked = true;
		this.checkableViews = new ArrayList<Checkable>(5);
	}
}


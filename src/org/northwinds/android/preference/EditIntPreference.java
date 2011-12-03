/*
 * Copyright (c) 2010-2011, Loren M. Lang
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.northwinds.android.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.EditTextPreference;
import android.text.TextUtils;
import android.util.AttributeSet;


public class EditIntPreference extends EditTextPreference {
	private int mValue = 0;

	public EditIntPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		/* TODO: verify inputType for EditText */
	}

	public EditIntPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public EditIntPreference(Context context) {
		super(context);
	}

	public void setText(String text) {
		final boolean wasBlocking = shouldDisableDependents();

		if(TextUtils.isEmpty(text))
			text = "0";

		try {
			mValue = Integer.parseInt(text);
		} catch(NumberFormatException ex) {
			/* With a proper inputType on EditText, the worst that
			 * should happen is an empty or undefined text value
			 * which we will consider 0 */
			mValue = 0;
		}

		persistInt(mValue);

		final boolean isBlocking = shouldDisableDependents(); 
		if (isBlocking != wasBlocking)
			notifyDependencyChange(isBlocking);
	}

	public String getText() {
		return String.valueOf(mValue);
	}

	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		return a.getInt(index, 0);
	}

	@Override
	protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
		setText(String.valueOf(restoreValue ? getPersistedInt(mValue) : ((Integer)defaultValue)));
	}

	@Override
	public boolean shouldDisableDependents() {
		/* Should call up the class hierarchy, but mText in the parent
		 * class is never set causing it to always return true. */
		return mValue == 0 || !isEnabled();
	}
}

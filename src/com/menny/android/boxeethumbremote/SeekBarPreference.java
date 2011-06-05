package com.menny.android.boxeethumbremote;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

// http://android-journey.blogspot.com/2010/01/for-almost-any-application-we-need-to.html
public class SeekBarPreference extends Preference implements
		OnSeekBarChangeListener {
	private static final String TAG = "SeekBarPreference";
	
	private static final String ANDROIDNS = "http://schemas.android.com/apk/res/android";

	private int maximum = 100;
	private int minimum = 1;
	private int interval = 5;
	private float oldValue = 50;
	private String mTitle;
	private String mSummary;

	public SeekBarPreference(Context context) {
		super(context);
		//setLayoutResource(R.layout.preference_seek_bar);
	}

	public SeekBarPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		//setLayoutResource(R.layout.preference_seek_bar_widget);
		mTitle = attrs.getAttributeValue(ANDROIDNS, "title");
		mSummary = attrs.getAttributeValue(ANDROIDNS, "summary");
		oldValue = attrs.getAttributeIntValue(ANDROIDNS, "defaultValue", 50);
		maximum = attrs.getAttributeIntValue(null, "max", 100);
		minimum = attrs.getAttributeIntValue(null, "min", 1);
		interval = attrs.getAttributeIntValue(null, "interval", 5);
	}
	
	public SeekBarPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		//setLayoutResource(R.layout.preference_seek_bar);
	}
	

	@Override
	protected View onCreateView(ViewGroup parent) {
		Context context = getContext();

		// TODO: device independence
		
		LinearLayout layout = new LinearLayout(context);
		layout.setPadding(15, 5, 15, 5);
		layout.setGravity(Gravity.CENTER_VERTICAL);
		layout.setOrientation(LinearLayout.HORIZONTAL);
		int height = 64;
		layout.setMinimumHeight(height);

		LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		params1.gravity = Gravity.CENTER_VERTICAL;

		TextView view = new TextView(context);
		view.setText(getTitle());
		view.setTextSize(24);
		view.setTextColor(0xffffffff);
		view.setLayoutParams(params1);

		LinearLayout.LayoutParams params2 = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.FILL_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		params2.gravity = Gravity.CENTER_VERTICAL;
		params2.leftMargin = 15;

		SeekBar bar = new SeekBar(context);
		bar.setMax(maximum);
		bar.setProgress((int) this.oldValue);
		bar.setLayoutParams(params2);
		bar.setOnSeekBarChangeListener(this);

		layout.addView(view);
		layout.addView(bar);
		layout.setId(android.R.id.widget_frame);

		return layout;
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {

		progress = validateValue(progress);
		//validateValue does the round for us
		//progress = Math.round(((float) progress) / interval) * interval;

		if (!callChangeListener(progress)) {
			seekBar.setProgress((int) this.oldValue);
			return;
		}

		seekBar.setProgress(progress);
		this.oldValue = progress;
		updatePreference(progress);

		notifyChanged();
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
	}

	@Override
	protected Object onGetDefaultValue(TypedArray ta, int index) {

		int dValue = (int) ta.getInt(index, 50);

		return validateValue(dValue);
	}

	@Override
	protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {

		int temp = restoreValue ? getPersistedInt(50) : (Integer) defaultValue;

		if (!restoreValue)
			persistInt(temp);

		this.oldValue = temp;
	}

	private int validateValue(int value) {

		if (value > maximum)
			value = maximum;
		else if (value < minimum)
			value = minimum;
		else if (value % interval != 0)
			value = Math.round(((float) value) / interval) * interval;

		return value;
	}

	private void updatePreference(int newValue) {
		SharedPreferences.Editor editor = getEditor();
		editor.putInt(getKey(), newValue);
		editor.commit();
	}

}

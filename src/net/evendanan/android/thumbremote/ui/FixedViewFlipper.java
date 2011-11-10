package net.evendanan.android.thumbremote.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ViewFlipper;

/*
 * http://daniel-codes.blogspot.com/2010/05/viewflipper-receiver-not-registered.html
 */
public class FixedViewFlipper extends ViewFlipper {

	public FixedViewFlipper(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onDetachedFromWindow() {
	    try {
	        super.onDetachedFromWindow();
	    }
	    catch (IllegalArgumentException e) {
	        stopFlipping();
	    }
	}
}

/* The following code was written by Menny Even Danan
 * and is released under the APACHE 2.0 license
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package com.menny.android.thumbremote;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.content.Context;
import java.lang.UnsupportedOperationException;

public class ShakeListener implements SensorEventListener 
{
	private static final int SHAKE_SPEED_THRESHOLD = 500;
	private static final int TIME_THRESHOLD = 50;
	private static final int SHAKING_TIME = 300;
	private static final String TAG = ShakeListener.class.toString();

	private SensorManager mSensorMgr;
	
	private float mLastX=-1.0f, mLastY=-1.0f, mLastZ=-1.0f;
	private long mLastSampleTime;
	private long mShakeStartTime = -1;
	
	private OnShakeListener mShakeListener;
	private Context mContext;
	private Sensor mSensor;

  public interface OnShakeListener
  {
    public void onShake();
  }

  public ShakeListener(Context context) 
  { 
    mContext = context;
  }

  public void setOnShakeListener(OnShakeListener listener)
  {
    mShakeListener = listener;
  }

  public void resume() {
    mSensorMgr = (SensorManager)mContext.getSystemService(Context.SENSOR_SERVICE);
    if (mSensorMgr == null) {
      throw new UnsupportedOperationException("Sensors not supported");
    }
    mSensor = mSensorMgr.getDefaultSensor(SensorManager.SENSOR_ACCELEROMETER);
    
    boolean supported = mSensorMgr.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_GAME);
    if (!supported) {
      mSensorMgr.unregisterListener(this, mSensor);
      throw new UnsupportedOperationException("Accelerometer not supported");
    }
  }

  public void pause() {
    if (mSensorMgr != null) {
    	mSensorMgr.unregisterListener(this, mSensor);
      mSensorMgr = null;
      mSensor = null;
    }
  }

  @Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		
	}
  
  @Override
  public void onSensorChanged(SensorEvent event) { 
	  if (event.sensor.getType() != SensorManager.SENSOR_ACCELEROMETER) return;
	  float[] values = event.values;
  
	  final long curTime = System.currentTimeMillis();
	  // only allow one update every TIME_THRESHOLD ms.
	  final long diffTime = (curTime - mLastSampleTime);
	  
	  if (diffTime > TIME_THRESHOLD) {
		  Log.d(TAG, "Sampling..");
		  mLastSampleTime = curTime;
		  final float x = values[SensorManager.DATA_X];
		  final float y = values[SensorManager.DATA_Y];
		  final float z = values[SensorManager.DATA_Z];
		  Log.d(TAG, "X "+x+" Y "+y+" Z "+z);
		  float speed = Math.abs(x+y+z - mLastX - mLastY - mLastZ) / diffTime * 10000;
		  if (speed > SHAKE_SPEED_THRESHOLD) {
			  Log.d(TAG, "Passed shaking threshold.");
			  //OK something is happening
			  if (mShakeStartTime < 0)
				  mShakeStartTime = curTime;
			  if ((curTime - mShakeStartTime) > SHAKING_TIME)
			  {
				  Log.d(TAG, "Shaked enough! Notify!");
				  //yes, this is a shake action! Do something about it!
				  if (mShakeListener != null)
					  mShakeListener.onShake();
			  }
		  }
		  else
		  {
			  Log.d(TAG, "Too weak of a shake. Reseting state.");
			  mShakeStartTime = -1;
		  }
		  mLastX = x;
		  mLastY = y;
		  mLastZ = z;
	  }
  }

}

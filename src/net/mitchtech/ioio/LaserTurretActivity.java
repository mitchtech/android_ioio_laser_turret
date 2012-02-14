package net.mitchtech.ioio;

import ioio.lib.api.DigitalOutput;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.AbstractIOIOActivity;

import java.util.Timer;
import java.util.TimerTask;

import net.mitchtech.ioio.laserturret.R;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.ToggleButton;

public class LaserTurretActivity extends AbstractIOIOActivity {
	private static final int PAN_PIN = 3;
	private static final int TILT_PIN = 6;
	private static final int LASER_PIN1 = 34;
	private static final int LASER_PIN2 = 35;

	private static final int PWM_FREQ = 100;

	private SeekBar mPanSeekBar;
	private SeekBar mTiltSeekBar;

	private ToggleButton mLaserToggle1;
	private ToggleButton mLaserToggle2;

	private Button mLaserButton1;
	private Button mLaserButton2;
	private Button mLaserButton3;

	MediaPlayer mMediaPlayer = null;
	Timer mTimer = null;

	private boolean mLaser1State = false;
	private boolean mLaser2State = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mPanSeekBar = (SeekBar) findViewById(R.id.panSeekBar);
		mTiltSeekBar = (SeekBar) findViewById(R.id.tiltSeekBar);

		mLaserToggle1 = (ToggleButton) findViewById(R.id.ToggleButton1);
		mLaserToggle1.setTag(LASER_PIN1);
		mLaserToggle1.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					turnPinOn(LASER_PIN1);
					playLaserSound(1);
				} else {
					turnPinOff(LASER_PIN1);
				}
			}
		});

		mLaserToggle2 = (ToggleButton) findViewById(R.id.ToggleButton2);
		mLaserToggle2.setTag(LASER_PIN2);
		mLaserToggle2.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					turnPinOn(LASER_PIN2);
					playLaserSound(2);
				} else {
					turnPinOff(LASER_PIN2);
				}
			}
		});

		mLaserButton1 = (Button) findViewById(R.id.laser1);
		mLaserButton1.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				playLaserSound(1);
				pulsePin(LASER_PIN1);
			}
		});

		mLaserButton2 = (Button) findViewById(R.id.laser2);
		mLaserButton2.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				playLaserSound(2);
				pulsePin(LASER_PIN2);
			}
		});

		mLaserButton3 = (Button) findViewById(R.id.laser3);
		mLaserButton3.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				playLaserSound(3);
				pulsePin(LASER_PIN1);
				pulsePin(LASER_PIN2);
			}
		});

		enableUi(false);
	}

	class IOIOThread extends AbstractIOIOActivity.IOIOThread {
		private PwmOutput panPwmOutput, tiltPwmOutput;

		private DigitalOutput mLaser1, mLaser2;

		public void setup() throws ConnectionLostException {
			try {
				panPwmOutput = ioio_.openPwmOutput(PAN_PIN, PWM_FREQ);
				tiltPwmOutput = ioio_.openPwmOutput(TILT_PIN, PWM_FREQ);
				mLaser1 = ioio_.openDigitalOutput(LASER_PIN1, false);
				mLaser2 = ioio_.openDigitalOutput(LASER_PIN2, false);
				enableUi(true);
			} catch (ConnectionLostException e) {
				enableUi(false);
				throw e;
			}
		}

		public void loop() throws ConnectionLostException {
			try {
				panPwmOutput.setPulseWidth(500 + mPanSeekBar.getProgress() * 2);
				tiltPwmOutput.setPulseWidth(500 + mTiltSeekBar.getProgress() * 2);
				mLaser1.write(mLaser1State);
				mLaser2.write(mLaser2State);
				sleep(10);
			} catch (InterruptedException e) {
				ioio_.disconnect();
			} catch (ConnectionLostException e) {
				enableUi(false);
				throw e;
			}
		}
	}

	@Override
	protected AbstractIOIOActivity.IOIOThread createIOIOThread() {
		return new IOIOThread();
	}

	private void enableUi(final boolean enable) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mPanSeekBar.setEnabled(enable);
				mTiltSeekBar.setEnabled(enable);
				mLaserButton1.setEnabled(enable);
				mLaserButton2.setEnabled(enable);
				mLaserButton3.setEnabled(enable);
				mLaserToggle1.setEnabled(enable);
				mLaserToggle2.setEnabled(enable);
			}
		});
	}

	private void playLaserSound(int soundNum) {

		switch (soundNum) {
		case 1:
			mMediaPlayer = MediaPlayer.create(LaserTurretActivity.this, R.raw.laser1);
			break;
		case 2:
			mMediaPlayer = MediaPlayer.create(LaserTurretActivity.this, R.raw.laser2);
			break;
		case 3:
			mMediaPlayer = MediaPlayer.create(LaserTurretActivity.this, R.raw.laser3);
			break;
		default:
			mMediaPlayer = MediaPlayer.create(LaserTurretActivity.this, R.raw.laser1);
			break;
		}
		mMediaPlayer.start();
	}

	private void turnPinOn(int pin) {
		if (pin == LASER_PIN1) {
			mLaser1State = true;
		} else if (pin == LASER_PIN2) {
			mLaser2State = true;
		}
	}

	private void turnPinOff(int pin) {
		if (pin == LASER_PIN1) {
			mLaser1State = false;
		} else if (pin == LASER_PIN2) {
			mLaser2State = false;
		}
	}

	private void pulsePin(int pin) {
		turnPinOn(pin);
		mTimer = new Timer();
		mTimer.schedule(new PinOffTask(pin), 200);
	}

//	private class PinOnTask extends TimerTask {
//		int pin;
//
//		public PinOnTask(int pin) {
//			super();
//			this.pin = pin;
//		}
//
//		public void run() {
//			turnPinOn(pin);
//		}
//	}

	private class PinOffTask extends TimerTask {
		int pin;

		public PinOffTask(int pin) {
			super();
			this.pin = pin;
		}

		public void run() {
			turnPinOff(pin);
		}
	}

}
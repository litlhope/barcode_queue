package team.milkyway.study.bqueue;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import team.milkyway.study.bqueue.aoa.AOAMgr;
import team.milkyway.study.bqueue.util.ByteUtil;

public class MainActivity extends AppCompatActivity {
	private static final String TAG = "MainActivity";

	private AOAMgr mMgr = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		onNewIntent(getIntent());
		setContentView(R.layout.activity_main);

		init();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		Log.d(TAG, "onNewIntent - action: " + intent.getAction());
		if (mMgr == null) {
			mMgr = new AOAMgr(this, mCallback);
		}
		mMgr.onNewIntent(intent);

		super.onNewIntent(intent);
	}

	@Override
	protected void onDestroy() {
		mMgr.onDestroy();
		mMgr = null;

		super.onDestroy();
	}

	private void init() {
		PackageManager pm = getPackageManager();
		boolean isUSBAccessory = pm.hasSystemFeature(PackageManager.FEATURE_USB_ACCESSORY);

		if (isUSBAccessory) {
			Toast.makeText(this, "USB Accessory supported!", Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(this, "USB Accessory not supported!!!", Toast.LENGTH_SHORT).show();
			finish();
		}
	}

	private final AOAMgr.Callback mCallback = new AOAMgr.Callback() {
		@Override
		public void onConnectionEstablished() {
			Log.d(TAG, "USB Connected!");
		}

		@Override
		public void onDeviceDisconnected() {
			Log.d(TAG, "USB Disconnected!!");
		}

		@Override
		public void onConnectionClosed() {
			Log.d(TAG, "USB Connection closed!");
		}

		@Override
		public void onDataReceived(byte[] data, int num) {
			if (num > 0) {
				Log.i(TAG, "Received - data: " + ByteUtil.bytes2hex(data));
			}
		}
	};
}

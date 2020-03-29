package team.milkyway.study.bqueue.aoa;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class AOAMgr {
	private static final String TAG = "AOAMgr";

	private static final int BUFFER_SIZE = 1024;
	private final Context mCtx;
	private final UsbManager mUsbManager;
	private final Callback mCallback;

	private static final String ACTION_ACCESSORY_DETACHED
			= "android.hardware.usb.action.USB_ACCESSORY_DETACHED";

	private volatile boolean mAccessoryConnected = false;
	private final AtomicBoolean mQuit = new AtomicBoolean(false);

	private UsbAccessory mAccessory;

	private ParcelFileDescriptor mParcelFileDescriptor = null;
	private FileDescriptor mFileDescriptor = null;
	private FileInputStream mInput = null;
	private FileOutputStream mOutput = null;

	public interface Callback {
		void onConnectionEstablished();
		void onDeviceDisconnected();
		void onConnectionClosed();
		void onDataReceived(byte[] data, int num);
	}

	public AOAMgr(Context ctx, Callback callback) {
		mCtx = ctx;
		mCallback = callback;
		mUsbManager = (UsbManager) ctx.getSystemService(Context.USB_SERVICE);
		ctx.registerReceiver(mDetachedReceiver, new IntentFilter(ACTION_ACCESSORY_DETACHED));
	}

	public void onNewIntent(Intent intent) {
		if (mUsbManager.getAccessoryList() != null) {
			mAccessory = mUsbManager.getAccessoryList()[0];
			connect();
		}
	}

	private void connect() {
		if (mAccessory != null) {
			if (mAOAThread == null) {
				mAOAThread = new Thread(mAOARunnable, "USB Data Receiver");
				mAOAThread.start();
			} else {
				Log.d(TAG, "USB Data Receiver already started!!");
			}
		} else {
			Log.d(TAG, "Accessory is null!");
		}
	}

	public void onDestroy() {
		mQuit.set(true);
		mCtx.unregisterReceiver(mDetachedReceiver);
	}

	public void write(byte[] data) {
		if (mAccessoryConnected && mOutput != null) {
			try {
				mOutput.write(data);
			} catch (IOException ex) {
				Log.w(TAG, "", ex);
			}
		} else {
			Log.d(TAG, "Accessory is not connected!!");
		}
	}

	private final BroadcastReceiver mDetachedReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent != null && ACTION_ACCESSORY_DETACHED.equals(intent.getAction())) {
				mCallback.onDeviceDisconnected();
			}
		}
	};

	private static Thread mAOAThread;
	private final Runnable mAOARunnable = new Runnable() {
		@Override
		public void run() {
			Log.d(TAG, "");
			mParcelFileDescriptor = mUsbManager.openAccessory(mAccessory);
			if (mParcelFileDescriptor == null) {
				Log.w(TAG, "Can't open accessory!!");
				mCallback.onConnectionClosed();
				return;
			}

			mFileDescriptor = mParcelFileDescriptor.getFileDescriptor();
			mInput = new FileInputStream(mFileDescriptor);
			mOutput = new FileOutputStream(mFileDescriptor);
			mCallback.onConnectionEstablished();
			mAccessoryConnected = true;

			byte[] buf = new byte[BUFFER_SIZE];
			while (!mQuit.get()) {
				try {
					int read = mInput.read(buf);
					mCallback.onDataReceived(buf, read);
				} catch (Exception ex) {
					Log.w(TAG, "", ex);
					break;
				}
			}

			Log.d(TAG, "Exit read thread!");
			mCallback.onConnectionClosed();

			if (mParcelFileDescriptor != null) {
				try {
					mParcelFileDescriptor.close();
				} catch (IOException ex) {
					Log.w(TAG, "", ex);
				}
			}

			if (mInput != null) {
				try {
					mInput.close();
				} catch (IOException ex) {
					Log.w(TAG, "", ex);
				}
			}

			if (mOutput != null) {
				try {
					mOutput.close();
				} catch (IOException ex) {
					Log.w(TAG, "", ex);
				}
			}

			mAccessoryConnected = false;
			mQuit.set(false);
			mAOAThread = null;
		}
	};
}

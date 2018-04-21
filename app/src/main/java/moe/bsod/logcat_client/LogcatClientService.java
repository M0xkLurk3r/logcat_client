package moe.bsod.logcat_client;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.InflaterInputStream;

public class LogcatClientService extends Service {

	public class LCBinder extends Binder {
		public LogcatClientService getService() {
			return LogcatClientService.this;
		}
		public String getCurrentStatus() {
			return currentstatus;
		}
	}

	public static final String LOGCAT_SERVICE_START = "LOGCAT_SERVICE_START";
	public static final String LOGCAT_SERVICE_STOP = "LOGCAT_SERVICE_STOP";

	public static String currentstatus = LOGCAT_SERVICE_STOP;
	private final IBinder mBinder = new LCBinder();

	private String mRESTfulAPI = null;
	private int mScarchPid = -1;
	private Thread workerThread = null;

	public LogcatClientService() {

	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO: Return the communication channel to the service.
		return mBinder;
	}

	public static String getLogcatCmdLine(int pid) {
		if (Build.VERSION.SDK_INT < 23)
			return "logcat | grep \"\\(\\ *" + String.valueOf(pid) + "\\)\"\n";
		else
			return
			"logcat | while read LINE; do " +
				"strarr=($LINE);" +
				"pid=${strarr[2]};" +
				"if [ \"$pid\" = \"" + String.valueOf(pid) + "\" ]; then " +
					"echo $LINE;" +
				"fi;" +
			"done;\n";

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		if (intent == null)
			return START_NOT_STICKY;

		String action = intent.getAction();

		if (LOGCAT_SERVICE_START.equals(action)) {

			int pid = intent.getIntExtra("target_pid", -1);
			String restful = intent.getStringExtra("apiurl");

			if (pid == -1 || restful == null) {
				throw new IllegalArgumentException("Should pass `apiurl` and `target_pid` before start this service!");
			}

			mRESTfulAPI = restful;
			mScarchPid = pid;

			currentstatus = LOGCAT_SERVICE_START;
			workerThread = new Thread(workerRunnable);
			workerThread.setName("workerThread");
			workerThread.start();

			return START_STICKY;
		} else if (LOGCAT_SERVICE_STOP.equals(action)) {
			currentstatus = LOGCAT_SERVICE_STOP;
			if (workerThread != null) {
				workerThread.interrupt();
			}
			return START_NOT_STICKY;
		} else {
			Log.e("LogcatClientService", "Command not understood.");
			return START_NOT_STICKY;
		}
	}

	private void reportLog(byte[] postctx, int off, int len) {
		try {
			URLConnection url = new URL(mRESTfulAPI).openConnection();
			url.setDoOutput(true);
			url.setRequestProperty("Content-Type", "application/octet-stream");
			url.connect();
			url.getOutputStream().write(postctx, off, len);
			url.getOutputStream().flush();
			url.getOutputStream().close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private Runnable workerRunnable = new Runnable() {
		@Override
		public void run() {

			if (mRESTfulAPI == null) {
				throw new IllegalStateException("Should place a valid mRESTfulAPI!");
			}

			byte[] rawbuf = new byte[8192];

			try {
				Process proc = Runtime.getRuntime().exec("su");
				InflaterInputStream istream = new InflaterInputStream(proc.getInputStream());
				DataOutputStream ostream = new DataOutputStream(proc.getOutputStream());
				if (mScarchPid == -1) {
					// Get ALL logcat !!!
					ostream.writeBytes("logcat\n");
					ostream.flush();
				} else {
					String command = getLogcatCmdLine(mScarchPid);
					ostream.writeBytes(command);
					ostream.flush();
				}

				int readed = 0;

				while (readed != -1 && currentstatus.equals(LOGCAT_SERVICE_START)) {
					readed = istream.read(rawbuf);
					if (readed > 0) {
						reportLog(rawbuf, 0, readed);
					}
				}

				proc.destroy();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	};
}

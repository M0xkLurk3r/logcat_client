package moe.bsod.logcat_client;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

	private static boolean hasRootPriv = false;

	private static final int REQ_PROC_LIST = 0xfade;
/*
	public static final int LCS_START_GRACEFULLY = 0xfafa;
	public static final int LCS_START_ERROR = 0xfadd;
	public static final int LCS_STOP_GRACEFULLY = 0xbade;
	public static final int LCS_STOP_ERROR = 0xbaad;
*/

	public Button startsrv = null;
	public EditText apiurl = null;
	public EditText procid = null;

	private boolean buttonSwitch = false;

	private int target_proc_pid = 0;

	private static Handler sHandler = new Handler() {
		@Override
		public void handleMessage(Message message) {

			switch (message.what) {
				case PrivHelper.PRIVHELPER_FAILED:
					hasRootPriv = true;
					break;
				case PrivHelper.PRIVHELPER_SUCCESS:
					hasRootPriv = true;
					break;
			}
			super.handleMessage(message);
		}
	};

	private PrivHelper helper = new PrivHelper(sHandler);

	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.i("debug", "service started");
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.i("debug", "service stop");
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		startsrv = (Button) findViewById(R.id.startsrv);
		apiurl = (EditText) findViewById(R.id.apiurl);
		procid = (EditText) findViewById(R.id.process);

		procid.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivityForResult(new Intent(MainActivity.this, ProcListActivity.class), REQ_PROC_LIST);
			}
		});

		procid.setFocusable(false);
		procid.setFocusableInTouchMode(false);


		if (! helper.isAlive())
			helper.start();

		startsrv.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (! buttonSwitch) {
					Intent intent = new Intent(MainActivity.this, LogcatClientService.class);
					intent.setAction(LogcatClientService.LOGCAT_SERVICE_START);

					String apiurlstr = apiurl.getText().toString();
					if ("".equals(apiurlstr) || target_proc_pid <= 0) {
						Log.e("debug", "error: should place a valid apiurl");
						return;
					}

					intent.putExtra("apiurl", apiurlstr);
					intent.putExtra("target_pid", target_proc_pid);	startsrv.setText(R.string.srv_running);

					startService(intent);
					bindService(intent, mConnection, BIND_AUTO_CREATE);
				} else {
					Intent intent = new Intent(MainActivity.this, LogcatClientService.class);
					intent.setAction(LogcatClientService.LOGCAT_SERVICE_STOP);
					startsrv.setText(R.string.srv_stopped);
					startService(intent);
					unbindService(mConnection);
				}
			}
		});

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case REQ_PROC_LIST:
				if (procid != null && data != null) {
					String packageName = data.getStringExtra("packageName");
					int pid = data.getIntExtra("pid", 0);
					target_proc_pid = pid;
					procid.setText(String.valueOf(pid) + "/" + packageName);
				}
		}
	}
}

package moe.bsod.logcat_client;

import android.content.Intent;
import android.os.Handler;
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

	private PrivHelper helper = new PrivHelper(privHandler);

	public Button startsrv = null;
	public EditText apiurl = null;
	public EditText procid = null;

	private int target_proc_pid = 0;

	private static Handler privHandler = new Handler() {
		@Override
		public void handleMessage(Message message) {
			Log.i("test", "cmdresult=\"" + Utils.humanReadable(message.getData().getString("cmdresult", "none")) + "\"");
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

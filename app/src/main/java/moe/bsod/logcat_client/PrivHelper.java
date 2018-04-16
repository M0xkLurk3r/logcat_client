package moe.bsod.logcat_client;

import android.os.Bundle;
import android.os.Message;
import android.os.Handler;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UTFDataFormatException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by anthony on 4/14/18.
 */

public class PrivHelper extends Thread {
	private Handler onPrivRequestCallback = null;
	private List<String> mCmdListDefault = getDefaultList();
	private List<String> mCmdlist = null;

	public static final int PRIVHELPER_FAILED = -1;
	public static final int PRIVHELPER_SUCCESS = 0;
	public static final int PRIVHELPER_RETURN = 1;

	private PrivHelperListener mListener = null;

	public PrivHelper(Handler callback, List<String> cmdlist) {
		PrivHelperInit(callback, cmdlist);
	}

	private static List<String> getDefaultList() {
		List<String> cmdlist = new ArrayList<>();
		cmdlist.add("id");
		return cmdlist;
	}

	public PrivHelper(Handler callback) {
		PrivHelperInit(callback, mCmdListDefault);
	}

	public void setPrivHelperListener(PrivHelperListener listener) {
		mListener = listener;
	}

	private void PrivHelperInit(Handler callback, List<String> cmdlist) {

		if (callback == null)
			throw new NullPointerException("Handler should not be null!");

		onPrivRequestCallback = callback;
		mCmdlist = cmdlist;

	}

	public void onContentDelivered(int returnVal, String text) {
		if (mListener != null)
			mListener.onResultDelivered(this, returnVal, text);
	}

	@Override
	public void run() {
		try {
			Process process = Runtime.getRuntime().exec("su");
			OutputStream ostream = process.getOutputStream();
			InputStream istream = process.getInputStream();

			if (mCmdlist != null) {
				for (String s : mCmdlist) {
					if (s == null)
						continue;
					ostream.write(s.getBytes());
					ostream.write('\n');
				}
			}
			ostream.write("exit\n".getBytes());
			ostream.flush();

			try {
				process.waitFor();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			String cmdresult = Utils.readBlockingFd(istream);


			Message message = new Message();
			message.what = PRIVHELPER_RETURN;

			if (mCmdlist == mCmdListDefault) {
				if (!cmdresult.startsWith("uid=0(")) {
					Log.i("PrivHelper", "seems we didn't make it");
					message.what = PRIVHELPER_FAILED;
				} else {
					Log.i("PrivHelper", "We had root privileges now...");
					message.what = PRIVHELPER_SUCCESS;
				}
			}

			Bundle data = new Bundle();
			data.putString("cmdresult", cmdresult);
			data.putInt("errorlevel", process.exitValue());
			message.setData(data);

			onPrivRequestCallback.sendMessage(message);

			onContentDelivered(process.exitValue(), cmdresult);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public interface PrivHelperListener {
		void onResultDelivered(PrivHelper object, int returnVal, String content);
	}
}

package moe.bsod.logcat_client;

import android.content.Context;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by anthony on 4/14/18.
 */

public class Utils {
	public static String readBlockingFd(InputStream inputStream) throws IOException {

		final byte[] buf = new byte[1024];

		if (inputStream == null)
			throw new NullPointerException("InputStream cannot be a null object!");

		int available_read = inputStream.available();

		if (available_read < 1024) {
			int actually_readed = inputStream.read(buf);
			return new String(buf, 0, actually_readed < 0 ? 0 : actually_readed);
		} else {
			StringBuilder builder = new StringBuilder();
			while (inputStream.available() > 0) {
				int actually_read = inputStream.read(buf);
				builder.append(new String(buf, 0, actually_read));
			}
			return builder.toString();
		}
	}

	public static String humanReadable(String input) {
		return input.replace("\r", "\\r").replace("\n", "\\n").replace("\b", "\\b");
	}

	public static class PixelUtils {
		public static int dp2px(Context context, float dp) {
			final float scale = context.getResources().getDisplayMetrics().density;
			return (int) (dp * scale + 0.5f);
		}

		public static int px2dp(Context context, float px) {
			final float scale = context.getResources().getDisplayMetrics().density;
			return (int) (px / scale + 0.5f);
		}
	}
}

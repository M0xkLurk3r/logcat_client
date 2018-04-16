package moe.bsod.logcat_client;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.Shape;
import android.media.Image;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.graphics.drawable.DrawerArrowDrawable;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ProcListActivity extends AppCompatActivity {

	private ListView listView;
	private static final int CONTENT_DELIVER = -2345;
	private List<Map<String, Object>> data;
	private static final String shellScript = "for file in /proc/*; do " +
													"if [ -f ${file}\"/stat\" ]; then " +
														"path=${file}\"/stat\"; " +
														"cmdpath=${file}\"/cmdline\"; " +
														"cmdcontent=$(<$cmdpath); " +
														"content=$(<$path); " +
														"if [ \"\" = \"$cmdcontent\" ]; then " +
															"continue; " +
														"fi; " +
														"echo -n $cmdcontent; " +
														"echo -n \" \"; " +
														"echo $content; " +
													"fi; " +
												"done";

	private static Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			}
		}
	};


	protected void syncData(final List<Map<String, Object>> data) {
		listView.setAdapter(new SimpleAdapter(getApplicationContext(), data, R.layout.proc_item, new String[0], new int[0]) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				LinearLayout row = (LinearLayout) super.getView(position, convertView, parent);
				row.removeAllViews();
				row.setOrientation(LinearLayout.HORIZONTAL);
				row.setGravity(Gravity.LEFT | Gravity.CENTER);

				Map<String, Object> stringObjectMap = data.get(position);

				ImageView iv = new ImageView(row.getContext());
				Drawable thumbnail = (Drawable) stringObjectMap.get("thumbnail");
				if (thumbnail == null)
					iv.setImageDrawable(new ShapeDrawable());
				else
					iv.setImageDrawable(thumbnail);
				LinearLayout subtitle = new LinearLayout(row.getContext());
				subtitle.setOrientation(LinearLayout.VERTICAL);
				TextView appName = new TextView(row.getContext());
				appName.setTextSize(Utils.PixelUtils.dp2px(getApplicationContext(), 7));
				appName.setText((String) stringObjectMap.get("appname"));
				appName.setTextColor(Color.BLACK);
				TextView packageName = new TextView(row.getContext());
				packageName.setTextSize(Utils.PixelUtils.dp2px(getApplicationContext(), 5));
				packageName.setText((String) stringObjectMap.get("procname"));
				packageName.setTextColor(Color.GRAY);

				subtitle.addView(appName);
				subtitle.addView(packageName);

				row.addView(iv);
				row.addView(subtitle);
				return row;
			}
		});
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_proc_list);
		setTitle("Pick up a running process...");

		listView = (ListView) findViewById(R.id.list);
		List<String> l = new ArrayList<>();
		l.add(shellScript);
		PrivHelper pv = new PrivHelper(handler, l);
		pv.setPrivHelperListener(new PrivHelper.PrivHelperListener() {
			@Override
			public void onResultDelivered(PrivHelper object, int returnVal, String content) {

				final PackageManager pm = getPackageManager();
				List<ApplicationInfo> installedApplications = pm.getInstalledApplications(PackageManager.GET_META_DATA);
				Map<String, ApplicationInfo> appdata = new HashMap<>();
				for (ApplicationInfo info : installedApplications) {
					appdata.put(info.packageName, info);
				}
				installedApplications = null;	// free this

				String[] split_lines = content.split("\n");
				final List<Map<String, Object>> listData = new ArrayList<>();
				for (String rline : split_lines) {
					String[] proc_detail = rline.split(" ", 6);
					String proc_name = proc_detail[0];
					try {
						if (appdata.containsKey(proc_name)) {
							int pid = Integer.parseInt(proc_detail[1]);
							int ppid = Integer.parseInt(proc_detail[4]);
							ApplicationInfo info = appdata.get(proc_name);
							String packageLauncherName = info.loadLabel(pm).toString();
							Map<String, Object> listItem = new TreeMap<>();
							listItem.put("thumbnail", info.loadIcon(pm));    // TODO: Get the package thumbnail
							listItem.put("appname", packageLauncherName);        // TODO: Get the packageName corresponds to the process.
							listItem.put("procname", proc_name);
							listItem.put("pid", pid);
							listItem.put("ppid", ppid);
							listData.add(listItem);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				data = listData;

				new Handler(Looper.getMainLooper()).post(new Runnable() {
					@Override
					public void run() {
						syncData(listData);
					}
				});
			}
		});
		pv.start();
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Map<String, Object> map = data.get(position);
				Intent intent = new Intent();
				intent.putExtra("packageName", (String) map.get("procname"));
				intent.putExtra("pid", (Integer) map.get("pid"));
				setResult(0, intent);
				finish();
			}
		});
	}
}

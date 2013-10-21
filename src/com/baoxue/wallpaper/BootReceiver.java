package com.baoxue.wallpaper;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

public class BootReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		final PackageManager pm = context.getPackageManager();
		try {
			final ComponentName cn1 = new ComponentName("com.android.launcher",
					"com.android.launcher2.WallpaperChooser");
			pm.setComponentEnabledSetting(cn1,
					PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
					PackageManager.DONT_KILL_APP);
		} catch (Exception e) {
		}
		try {
			final ComponentName cn2 = new ComponentName(
					"com.cyanogenmod.trebuchet",
					"com.cyanogenmod.trebuchet.WallpaperChooser");
			pm.setComponentEnabledSetting(cn2,
					PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
					PackageManager.DONT_KILL_APP);
		} catch (Exception e) {
		}
	}

}

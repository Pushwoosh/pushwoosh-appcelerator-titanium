/**
 * Pushwoosh SDK
 * (c) Pushwoosh 2017
 *
 */
package com.pushwoosh.module;

import android.app.Notification;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Build;
import android.os.PowerManager;

import org.json.JSONObject;

import org.appcelerator.kroll.common.Log;

import com.pushwoosh.notification.DefaultNotificationFactory;
import com.pushwoosh.notification.PushData;
import com.pushwoosh.internal.PushManagerImpl;

import java.util.List;

class NotificationFactory extends DefaultNotificationFactory 
{
	PushData lastHandledPush;

	@Override
	public void onPushReceived(PushData pushData)
	{
		super.onPushReceived(pushData);

		if (PushnotificationsModule.INSTANCE != null) {
			JSONObject data = PushManagerImpl.bundleToJSON(pushData.getExtras());
			boolean handled = PushnotificationsModule.INSTANCE.onPushReceived(data.toString(), isAppOnForeground(getContext()));
			if (handled) {
				lastHandledPush = pushData;
			}
		}
	}

	@Override
	public Notification onGenerateNotification(PushData pushData)
	{
		if (pushData == lastHandledPush) {
			// Push already handled not need to generate notificatoin
			return null;
		}

		return super.onGenerateNotification(pushData);
	}

	private boolean isAppOnForeground(Context context) {
		KeyguardManager kgMgr = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
		boolean lockScreenIsShowing = kgMgr.inKeyguardRestrictedInputMode();
		if (lockScreenIsShowing) {
			return false;
		}

		PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		boolean isScreenAwake = (Build.VERSION.SDK_INT < 20 ? powerManager.isScreenOn() : powerManager.isInteractive());
		if (!isScreenAwake) {
			return false;
		}

		ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
		if (appProcesses == null) {
			return false;
		}

		final String packageName = context.getPackageName();
		for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
			if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && appProcess.processName.equals(packageName)) {
				return true;
			}
		}

		return false;
	}
}

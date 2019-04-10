package com.pushwoosh.module;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollEventCallback;
import org.appcelerator.kroll.KrollFunction;
import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.annotations.Kroll;

import org.appcelerator.titanium.*;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.IntentProxy;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.common.TiConfig;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.pushwoosh.Pushwoosh;
import com.pushwoosh.notification.NotificationServiceExtension;
import com.pushwoosh.notification.PushMessage;

import java.lang.reflect.Method;

public class PushwooshNotificationServiceExtension extends NotificationServiceExtension {

	@Override
	protected boolean onMessageReceived(final PushMessage pushMessage) {
		return super.onMessageReceived(pushMessage) || PushnotificationsModule.onPushReceived(pushMessage.toJson().toString(), isAppOnForeground());
	}

	@Override
	protected void startActivityForPushMessage(final PushMessage pushMessage) {
		Bundle extras = new Bundle();
		extras.putString(Pushwoosh.PUSH_RECEIVE_EVENT, pushMessage.toJson().toString());
		TiApplication appContext = TiApplication.getInstance();
		Activity activity = appContext.getCurrentActivity();
		if (activity == null) {
			activity = appContext.getRootActivity();
		}

		Intent launchIntent = null;
		if(activity != null) {
			/*
				Bugfix of task PUSH-19046
			 */
			if (!hasCorrectIntent(activity)) {
				try {
					Method getLaunchIntentMethod = activity.getClass().getMethod("getLaunchIntent", (Class<?>[]) null);
					launchIntent = (Intent) getLaunchIntentMethod.invoke(activity);
				} catch (Exception e) {
					launchIntent = activity.getIntent();
				}
			} else {
				launchIntent = activity.getIntent();
			}
			/*
				End of bugfix of task PUSH-19046
				the line should be there:
				launchIntent = activity.getIntent();
			 */
		} else {
			launchIntent = getApplicationContext().getPackageManager().getLaunchIntentForPackage(getApplicationContext().getPackageName());
			if(launchIntent == null){
				super.startActivityForPushMessage(pushMessage);
				return;
			}

			launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		}

		launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		launchIntent.putExtras(extras);

		getApplicationContext().startActivity(launchIntent);
	}

	@Override
	protected void onMessageOpened(PushMessage pushMessage) {
		super.onMessageOpened(pushMessage);
		PushnotificationsModule.onPushOpened(pushMessage.toJson().toString());
	}

	private boolean hasCorrectIntent(Activity activity) {
		if (activity.getIntent() == null || activity.getIntent().getComponent() == null) {
			return false;
		}
		String activityClassName = activity.getClass().getName();
		String destinationActivityClassName = activity.getIntent().getComponent().getClassName();
		return TextUtils.equals(activityClassName, destinationActivityClassName);
	}
}
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

import com.pushwoosh.Pushwoosh;
import com.pushwoosh.notification.NotificationServiceExtension;
import com.pushwoosh.notification.PushMessage;

public class PushwooshNotificationServiceExtension extends NotificationServiceExtension {

	@Override
	protected boolean onMessageReceived(final PushMessage pushMessage) {
		return PushnotificationsModule.onPushReceived(pushMessage.toJson().toString(), isAppOnForeground()) || super.onMessageReceived(pushMessage);
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
			launchIntent = activity.getIntent();
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
}

package com.pushwoosh.module;

import android.app.Application.ActivityLifecycleCallbacks;
import android.app.Activity;
import android.os.Bundle;

import org.json.JSONObject;

import org.appcelerator.kroll.common.Log;

import com.pushwoosh.notification.DefaultNotificationFactory;
import com.pushwoosh.notification.PushData;
import com.pushwoosh.internal.PushManagerImpl;

class NotificationFactory extends DefaultNotificationFactory 
{
	@Override
	public void onPushReceived(PushData pushData)
	{
		super.onPushReceived(pushData);

		if (PushnotificationsModule.INSTANCE != null) {
			JSONObject data = PushManagerImpl.bundleToJSON(pushData.getExtras());
			PushnotificationsModule.INSTANCE.sendPushReceived(data.toString());
		}
	}
}

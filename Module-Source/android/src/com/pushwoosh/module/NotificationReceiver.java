/**
 * Pushwoosh SDK
 * (c) Pushwoosh 2012
 *
 */
package com.pushwoosh.module;

import java.util.HashMap;
import java.util.Map;

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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.pushwoosh.PushManager;
import com.pushwoosh.internal.PushManagerImpl;

import android.app.Application.ActivityLifecycleCallbacks;
import android.os.Bundle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Set;


public class NotificationReceiver extends BroadcastReceiver
{
	public void onReceive(Context context, Intent intent)
	{
		if (intent == null) {
			return;
		}

		Log.d("NotificationReceiver", "RECEIVE: " + intent.getAction());

		Bundle pushBundle = PushManagerImpl.preHandlePush(context, intent);
		if(pushBundle == null) {
			return;
		}
			
		JSONObject dataObject = PushManagerImpl.bundleToJSON(pushBundle);
		
		TiApplication appContext = TiApplication.getInstance();
		Activity activity = appContext.getCurrentActivity();
		if (activity == null) {
			activity = appContext.getRootActivity();
		}

		Intent launchIntent = null;
		if(activity != null) {
			launchIntent = activity.getIntent();
		} else {
			launchIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
			launchIntent.addCategory("android.intent.category.LAUNCHER");
		}
					
		launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
		launchIntent.putExtras(pushBundle);
		launchIntent.putExtra(PushManager.PUSH_RECEIVE_EVENT, dataObject.toString());

		context.startActivity(launchIntent);

		if (PushnotificationsModule.INSTANCE != null) {
			PushnotificationsModule.INSTANCE.onNotificationOpened(dataObject.toString());
		} else {
			PushnotificationsModule.saveStartPush(dataObject.toString());
		}
		
		PushManagerImpl.postHandlePush(context, intent);
	}
}


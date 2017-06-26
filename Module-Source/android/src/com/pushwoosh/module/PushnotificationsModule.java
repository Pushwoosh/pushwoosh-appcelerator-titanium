/**
 * Pushwoosh SDK
 * (c) Pushwoosh 2012
 *
 */
package com.pushwoosh.module;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;

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
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.pushwoosh.BasePushMessageReceiver;
import com.pushwoosh.BaseRegistrationReceiver;
import com.pushwoosh.PushManager;
import com.pushwoosh.inapp.InAppFacade;
import com.pushwoosh.internal.utils.JsonUtils;

import org.json.JSONObject;

import android.os.Bundle;

@Kroll.module(name="Pushwoosh", id="com.pushwoosh.module")
public class PushnotificationsModule extends KrollModule
{
	public static PushnotificationsModule INSTANCE = null;

	// Standard Debugging variables
	private static final String LCAT = "PushnotificationsModule";
	private static final boolean DBG = TiConfig.LOGD;
	
	private static AtomicReference<String> startPushData = new AtomicReference<String>(null);
	private AtomicBoolean initialized = new AtomicBoolean(false);

	private boolean broadcastPush = true;

	// You can define constants with @Kroll.constant, for example:
	// @Kroll.constant public static final String EXTERNAL_NAME = value;
	
	private AtomicReference<KrollFunction> registrationSuccessCallback = new AtomicReference<KrollFunction>(null);
	private AtomicReference<KrollFunction> registrationErrorCallback = new AtomicReference<KrollFunction>(null);
	private KrollFunction messageCallback = null;
	private KrollFunction pushOpenCallback = null;
	private KrollFunction pushReceiveCallback = null;
	
	private PushManager pushManager = null;

	// Mandatory method
	@Kroll.onAppCreate
	public static void onAppCreate(TiApplication app)
	{
		Log.d(LCAT, "inside onAppCreate");
		// put module init code that needs to run when the application is created
	}

	public PushnotificationsModule()
	{
		super();
		INSTANCE = this;
		Log.d(LCAT, "Push: create module");

		try {
			Context context = TiApplication.getInstance();
			ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
			if (ai != null && ai.metaData != null) {
				broadcastPush = ai.metaData.getBoolean("PW_BROADCAST_PUSH", true);
			}
			
		} catch(Exception e) {
			Log.e(LCAT, "Failed to read AndroidManifest metaData", e);
		}
	}
	
	@Kroll.method
	public void initialize(HashMap options)
	{
		Log.d(LCAT, "initialize called");

		initialized.set(true);

		String pushwooshAppId = (String)options.get("application");
		String googleProjectId = (String)options.get("gcm_project");
		
		// dispatch saved start notification
		String startPush = startPushData.getAndSet(null);
		if (startPush != null) {
			onNotificationOpened(startPush);
		}

		PushManager.initializePushManager(TiApplication.getInstance(), pushwooshAppId, googleProjectId);
		pushManager = PushManager.getInstance(TiApplication.getInstance());
		pushManager.setNotificationFactory(new NotificationFactory());
		try {
			pushManager.onStartup(TiApplication.getInstance());
		} catch (Exception e) {
			Log.e(LCAT, "Failed to initialize PushManager", e);
		}
	}

	@Kroll.method
	public void registerForPushNotifications(KrollFunction success, KrollFunction error)
	{
		registrationSuccessCallback.set(success);
		registrationErrorCallback.set(error);

		pushManager.registerForPushNotifications();
	}

	@Kroll.method
	public void onPushOpened(KrollFunction callback)
	{
		pushOpenCallback = callback;
	}

	@Kroll.method
	public void onPushReceived(KrollFunction callback)
	{
		pushReceiveCallback = callback;
	}

	@Kroll.method
	public void pushNotificationsRegister(HashMap options)
	{
		Log.w(LCAT, "<pushNotificationsRegister> method is deprecated! Use <initialize> and <register> instead");

		String googleProjectId = (String)options.get("gcm_projectid");
		String pushwooshAppId = (String)options.get("pw_appid");

		registrationSuccessCallback.set((KrollFunction)options.get("success"));
		registrationErrorCallback.set((KrollFunction)options.get("error"));
		messageCallback = (KrollFunction)options.get("callback");

		initialized.set(true);

		// dispatch saved start notification
		String startPush = startPushData.getAndSet(null);
		if (startPush != null) {
			onNotificationOpened(startPush);
		}

		PushManager.initializePushManager(TiApplication.getInstance(), pushwooshAppId, googleProjectId);
		pushManager = PushManager.getInstance(TiApplication.getInstance());

		try {
			pushManager.onStartup(TiApplication.getInstance());
			pushManager.registerForPushNotifications();
		} catch (Exception e) {
			e.printStackTrace();
			onRegistrationFailed("Failed to register for push notifications");
			return;
		}

		Log.d(LCAT, "Push: finished registering for pushes");

		return;
	}

	@Kroll.method
	public void unregister() {
		Log.d(LCAT, "unregister called");
		
		if (pushManager == null) {
			return;
		}
		pushManager.unregisterForPushNotifications();	
	}
	
	@Kroll.method
	public void startTrackingGeoPushes() {
		Log.d(LCAT, "start tracking geo pushes called");
		
		if (pushManager == null) {
			return;
		}
		pushManager.startTrackingGeoPushes();
	}
 
	@Kroll.method
	public void stopTrackingGeoPushes() {
		Log.d(LCAT, "stop tracking geo pushes called");
		
		if (pushManager == null) {
			return;
		}
		pushManager.stopTrackingGeoPushes();
	}

	@Kroll.method
	public void setTags(HashMap params)
	{
		if (pushManager == null) {
			return;
		}

		PushManager.sendTags(TiApplication.getInstance(), params, null);
	}

	@Kroll.method
	public void getTags(final KrollFunction success, final KrollFunction error)
	{
		if (pushManager == null) {
			return;
		}

		PushManager.getTagsAsync(TiApplication.getInstance(), new PushManager.GetTagsListener() {
			@Override
			public void onTagsReceived(Map<String, Object> tags) {
				HashMap result = new HashMap(tags);
				success.callAsync(getKrollObject(), result);
			}

			@Override
			public void onError(Exception e) {
				HashMap result = new HashMap();
				result.put("error", e.getMessage());
				error.callAsync(getKrollObject(), result);
			}
		});
	}

	@Kroll.method
	public int scheduleLocalNotification(String message, int seconds)
	{
		return PushManager.scheduleLocalNotification(TiApplication.getInstance(), message, seconds);
	}

	@Kroll.method
	public void clearLocalNotification(int id)
	{
		PushManager.clearLocalNotification(TiApplication.getInstance(), id);
	}
	
	@Kroll.method
	public void clearLocalNotifications()
	{
		PushManager.clearLocalNotifications(TiApplication.getInstance());
	}

	@Kroll.method
	public void setMultiNotificationMode()
	{
		PushManager.setMultiNotificationMode(TiApplication.getInstance());
	}

	@Kroll.method
	public void setSimpleNotificationMode()
	{
		PushManager.setSimpleNotificationMode(TiApplication.getInstance());
	}

	@Kroll.method
	public void setBadgeNumber(int badgeNumber)
	{
		pushManager.setBadgeNumber(badgeNumber);
	}

	@Kroll.method
	public int getBadgeNumber()
	{
		return pushManager.getBadgeNumber();
	}

	@Kroll.method
	public void addBadgeNumber(int deltaBadge)
	{
		pushManager.addBadgeNumber(deltaBadge);
	}

	@Kroll.method
	public void setUserId(String userId)
	{
		pushManager.setUserId(TiApplication.getInstance(), userId);
	}

	@Kroll.method
	public void postEvent(String event, HashMap attributes)
	{
		InAppFacade.postEvent(TiApplication.getInstance().getRootActivity(), event, (Map<String, Object>)attributes);
	}

	@Kroll.method
	public String getHwid()
	{
		return PushManager.getPushwooshHWID(TiApplication.getInstance());
	}

	@Kroll.method
	public String getPushToken()
	{
		return PushManager.getPushToken(TiApplication.getInstance());
	}

	@Kroll.method
	public Map getNotificationSettings() 
	{
		Map result = new HashMap<String, Object>();
		boolean enabled = false;
		try {
			enabled = PushManager.isNotificationEnabled(TiApplication.getInstance().getRootActivity());
		} catch (Exception e) {
		}

		result.put("enabled", enabled);
		return result;
	}

	static void saveStartPush(String messageData)
	{
		Log.i(LCAT, "Start push saved: " + messageData);
		startPushData.set(messageData);
	}

	void onNotificationOpened(final String messageData) 
	{
		if (!initialized.get() || !isTitaniumReady()) {
			saveStartPush(messageData);
			return;
		}

		TiApplication.getInstance().getRootActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() 
			{
				HashMap data = new HashMap();
				data.put("data", messageData);

				if (messageCallback != null) {
					messageCallback.call(getKrollObject(), data);
				}

				if (pushOpenCallback != null) {
					pushOpenCallback.call(getKrollObject(), convertMessageData(messageData));
				}
			}
		});
	}

	boolean onPushReceived(final String messageData, boolean foreground) 
	{
		boolean handled = false;
		if (!isTitaniumReady()) {
			Log.w(LCAT, "titanium is not ready yet");
			return handled;
		}

		if (foreground && broadcastPush) {
			onNotificationOpened(messageData);
			handled = true;
		}

		if (pushReceiveCallback == null) {
			return handled;
		}

		TiApplication.getInstance().getRootActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() 
			{
				pushReceiveCallback.call(getKrollObject(), convertMessageData(messageData));
			}
		});

		return handled;
	}

	void onRegistrationSucceed(final String registrationId) 
	{
		TiApplication.getInstance().getRootActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() 
			{
				HashMap data = new HashMap();
				data.put("registrationId", registrationId);

				KrollFunction callback = registrationSuccessCallback.getAndSet(null);
				if (callback != null) {
					callback.callAsync(getKrollObject(), data);
				}
			}
		});
	}

	void onRegistrationFailed(final String error) {
		TiApplication.getInstance().getRootActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() 
			{
				HashMap data = new HashMap();
				data.put("error", error);

				KrollFunction callback = registrationErrorCallback.getAndSet(null);
				if (callback != null) {
					callback.callAsync(getKrollObject(), data);
				}
			}
		});
	}

	private HashMap<String, Object> convertMessageData(String messageData)
	{
		HashMap<String, Object> result = new HashMap<String, Object>();
		try {
			JSONObject json = new JSONObject(messageData);
			Boolean foreground = json.optBoolean("foreground");
			String message = json.optString("title");
			JSONObject userData = json.optJSONObject("userdata");

			result.put("data", JsonUtils.jsonToMap(json));
			result.put("foreground", foreground);
			result.put("message", message);

			if (userData != null) {
				result.put("extras", JsonUtils.jsonToMap(userData));
			}
		} catch(Exception e) {
			Log.e(LCAT, "Failed to convert push message data", e);
		}

		return result;
	}

	private boolean isTitaniumReady()
	{
		if (TiApplication.getInstance() == null) {
			return false;
		}

		if (TiApplication.getInstance().getRootActivity() == null) {
			return false;
		}

		return true;
	}
}


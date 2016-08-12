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

	// Standard Debugging variables
	private static final String LCAT = "PushnotificationsModule";
	private static final boolean DBG = TiConfig.LOGD;
	
	boolean broadcastPush = true;

	// You can define constants with @Kroll.constant, for example:
	// @Kroll.constant public static final String EXTERNAL_NAME = value;

	public static PushnotificationsModule INSTANCE = null;
	
	protected void finalize()
	{
		INSTANCE = null;
		Log.d(LCAT, "Push: finalized");
	}
	
	public PushnotificationsModule()
	{
		super();
		INSTANCE = this;
		Log.d(LCAT, "Push: create module");
		
		// lifecycle callbacks are available since android 14 API
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			TiApplication.getInstance().registerActivityLifecycleCallbacks(new ActivityMonitor());
		}

		try
		{
			Context context = TiApplication.getInstance();
			ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
			broadcastPush = ai.metaData.getBoolean("PW_BROADCAST_PUSH", true);
		}
		catch(Exception e)
		{
			// ignore
		}
	}
	
	@Kroll.onAppCreate
	public static void onAppCreate(TiApplication app)
	{
		Log.d(LCAT, "inside onAppCreate");
		// put module init code that needs to run when the application is created
	}
	
	@Override
	protected void initActivity(Activity activity) {
		Log.d(LCAT, "Push: init activity!");
		super.initActivity(activity);
	}

	@Override
	public void onDestroy(Activity activity) {
		super.onDestroy(activity);

		Log.d(LCAT, "Push: on destroy");
	}
	
	@Override
	public void onPause(Activity activity) {
		super.onPause(activity);
		
		Log.d(LCAT, "Push: on pause");
		return;
	}
 
	@Override
	public void onResume(Activity activity) {
		super.onResume(activity);
		
		Log.d(LCAT, "Push: on resume");
	}
	
	//Registration receiver
	BaseRegistrationReceiver mBroadcastReceiver = new BaseRegistrationReceiver()
	{
		@Override
		public void onRegisterActionReceive(Context context, Intent intent)
		{
			Log.d(LCAT, "Push: register broadcast received");

			checkMessage(intent);
		}
	};
	
	//Push message receiver
	private BasePushMessageReceiver mReceiver = new BasePushMessageReceiver()
	{
		@Override
		protected void onMessageReceive(Intent intent)
		{
			Log.d(LCAT, "Push: message received");

			//JSON_DATA_KEY contains JSON payload of push notification.
			sendMessage(intent.getExtras().getString(JSON_DATA_KEY));
		}
	};
	
	//Registration of the receivers
	public void registerReceivers()
	{
		//sometimes titanium alloy doesn't call onPause or onResume 
		unregisterReceivers();
		
		Log.d(LCAT, "Push: register receivers");

		IntentFilter intentFilter = new IntentFilter(TiApplication.getInstance().getRootActivity().getPackageName() + ".action.PUSH_MESSAGE_RECEIVE");

		if(broadcastPush)
			TiApplication.getInstance().getRootActivity().registerReceiver(mReceiver, intentFilter);
		
		TiApplication.getInstance().getRootActivity().registerReceiver(mBroadcastReceiver, new IntentFilter(TiApplication.getInstance().getRootActivity().getPackageName() + "." + PushManager.REGISTER_BROAD_CAST_ACTION));
		
		Log.d(LCAT, "Push: finished registering receivers");

	}
	
	public void unregisterReceivers()
	{
		Log.d(LCAT, "Push: unregistering receivers");

		//Unregister receivers on pause
		try
		{
			TiApplication.getInstance().getRootActivity().unregisterReceiver(mReceiver);
		}
		catch (Exception e)
		{
			// pass.
		}
		
		try
		{
			TiApplication.getInstance().getRootActivity().unregisterReceiver(mBroadcastReceiver);
		}
		catch (Exception e)
		{
			//pass through
		}
		
		Log.d(LCAT, "Push: finished unregistering receivers");

	}
	
	private KrollFunction successCallback = null;
	private KrollFunction errorCallback = null;
	private KrollFunction messageCallback = null;
	private KrollFunction pushOpenCallback = null;
	private KrollFunction pushReceiveCallback = null;
	
	PushManager mPushManager = null;
	

	@Kroll.method
	public void initialize(HashMap options)
	{
		Log.d(LCAT, "initialize called");

		// On Andoid < 4.0 registration is handled by IntentReceiver class
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			registerReceivers();
		}

		String pushwooshAppId = (String)options.get("application");
		String googleProjectId = (String)options.get("gcm_project");
		
		checkMessage(TiApplication.getInstance().getRootActivity().getIntent());
		resetIntentValues(TiApplication.getInstance().getRootActivity());

		PushManager.initializePushManager(TiApplication.getInstance(), pushwooshAppId, googleProjectId);
		mPushManager = PushManager.getInstance(TiApplication.getInstance());
		mPushManager.setNotificationFactory(new NotificationFactory());
		try
		{
			mPushManager.onStartup(TiApplication.getInstance());
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return;
		}
	}

	@Kroll.method
	public void registerForPushNotifications(KrollFunction success, KrollFunction error)
	{
		successCallback = success;
		errorCallback = error;

		mPushManager.registerForPushNotifications();
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

		// On Andoid < 4.0 registration is handled by IntentReceiver class
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			registerReceivers();
		}

		String googleProjectId = (String)options.get("gcm_projectid");
		String pushwooshAppId = (String)options.get("pw_appid");

		successCallback = (KrollFunction)options.get("success");
		errorCallback = (KrollFunction)options.get("error");
		messageCallback = (KrollFunction)options.get("callback");

		checkMessage(TiApplication.getInstance().getRootActivity().getIntent());
		resetIntentValues(TiApplication.getInstance().getRootActivity());

		PushManager.initializePushManager(TiApplication.getInstance(), pushwooshAppId, googleProjectId);
		mPushManager = PushManager.getInstance(TiApplication.getInstance());

		try
		{
			mPushManager.onStartup(TiApplication.getInstance());
			mPushManager.registerForPushNotifications();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			sendError("Failed to register for push notifications");
			return;
		}
		
		Log.d(LCAT, "Push: finished registering for pushes");

		return;
	}

	@Kroll.method
	public void unregister() {
		Log.d(LCAT, "unregister called");
		if (mPushManager == null)
		{
			return;
		}
		mPushManager.unregisterForPushNotifications();	
	}
	
	@Kroll.method
	public void startTrackingGeoPushes() {
		Log.d(LCAT, "start tracking geo pushes called");
		if (mPushManager == null)
		{
			return;
		}
		mPushManager.startTrackingGeoPushes();
	}
 
	@Kroll.method
	public void stopTrackingGeoPushes() {
		Log.d(LCAT, "stop tracking geo pushes called");
		if (mPushManager == null)
		{
			return;
		}
		mPushManager.stopTrackingGeoPushes();
	}

	@Kroll.method
	public void setTags(HashMap params)
	{
		if (mPushManager == null)
		{
			return;
		}

		try
		{
			PushManager.sendTags(TiApplication.getInstance(), params, null);
			return;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return;
		}
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
		mPushManager.setBadgeNumber(badgeNumber);
	}

	@Kroll.method
	public int getBadgeNumber()
	{
		return mPushManager.getBadgeNumber();
	}

	@Kroll.method
	public void addBadgeNumber(int deltaBadge)
	{
		mPushManager.addBadgeNumber(deltaBadge);
	}

	@Kroll.method
	public void setUserId(String userId)
	{
		mPushManager.setUserId(TiApplication.getInstance(), userId);
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

//Function: getPushToken
// Returns push notification token or null if device is not registered yet
	@Kroll.method
	public String getPushToken()
	{
		return PushManager.getPushToken(TiApplication.getInstance());
	}

	public void checkMessage(Intent intent)
	{
		if(intent == null)
		{
			Log.d(LCAT, "CHECK MESSAGE: intent null");
			return;
		}
		if (null != intent)
		{
			if (intent.hasExtra(PushManager.PUSH_RECEIVE_EVENT))
			{
				Log.d(LCAT, "CHECK MESSAGE: push receive");
				sendMessage(intent.getExtras().getString(PushManager.PUSH_RECEIVE_EVENT));
			}
			else if (intent.hasExtra(PushManager.REGISTER_EVENT))
			{
				Log.d(LCAT, "CHECK MESSAGE: push register");
				sendSuccess(intent.getExtras().getString(PushManager.REGISTER_EVENT));
			}
			else if (intent.hasExtra(PushManager.UNREGISTER_EVENT))
			{
				Log.d(LCAT, "CHECK MESSAGE: push unregister");
			}
			else if (intent.hasExtra(PushManager.REGISTER_ERROR_EVENT))
			{
				Log.d(LCAT, "CHECK MESSAGE: push error");
				sendError(intent.getExtras().getString(PushManager.REGISTER_ERROR_EVENT));
			}
			else if (intent.hasExtra(PushManager.UNREGISTER_ERROR_EVENT))
			{
				Log.d(LCAT, "CHECK MESSAGE: unregister error");
			}
		}
	}
	
	public void sendSuccess(final String registrationId) {
		if(successCallback == null)
			return;
		
		TiApplication.getInstance().getRootActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				HashMap data = new HashMap();
				data.put("registrationId", registrationId);

				successCallback.callAsync(getKrollObject(),data);
			}
		});
	}

	public void sendError(final String error) {
		if(errorCallback == null)
			return;
		
		TiApplication.getInstance().getRootActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				HashMap data = new HashMap();
				data.put("error", error);

				errorCallback.callAsync(getKrollObject(),data);
			}
		});
	}

	public void sendMessage(final String messageData) {

		TiApplication.getInstance().getRootActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				HashMap data = new HashMap();
				data.put("data", messageData);

				if (messageCallback != null)
					messageCallback.call(getKrollObject(), data);

				if (pushOpenCallback != null)
					pushOpenCallback.call(getKrollObject(), convertMessageData(messageData));
			}
		});
	}

	public void sendPushReceived(final String messageData) {
		if (pushReceiveCallback == null)
			return;

		TiApplication.getInstance().getRootActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				pushReceiveCallback.call(getKrollObject(), convertMessageData(messageData));
			}
		});
	}

	private HashMap<String, Object> convertMessageData(String messageData)
	{
		HashMap<String, Object> result = new HashMap<String, Object>();
		try
		{
			JSONObject json = new JSONObject(messageData);
			Boolean foreground = json.optBoolean("foreground");
			String message = json.optString("title");
			JSONObject userData = json.optJSONObject("userdata");

			result.put("data", JsonUtils.jsonToMap(json));
			result.put("foreground", foreground);
			result.put("message", message);
			result.put("extras", JsonUtils.jsonToMap(userData));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		return result;
	}
	
	public void resetIntentValues(Activity activity)
	{
		if(activity == null)
			return;
			
		Intent mainAppIntent = activity.getIntent();

		if (mainAppIntent.hasExtra(PushManager.PUSH_RECEIVE_EVENT))
		{
			mainAppIntent.removeExtra(PushManager.PUSH_RECEIVE_EVENT);
		}
		else if (mainAppIntent.hasExtra(PushManager.REGISTER_EVENT))
		{
			mainAppIntent.removeExtra(PushManager.REGISTER_EVENT);
		}
		else if (mainAppIntent.hasExtra(PushManager.UNREGISTER_EVENT))
		{
			mainAppIntent.removeExtra(PushManager.UNREGISTER_EVENT);
		}
		else if (mainAppIntent.hasExtra(PushManager.REGISTER_ERROR_EVENT))
		{
			mainAppIntent.removeExtra(PushManager.REGISTER_ERROR_EVENT);
		}
		else if (mainAppIntent.hasExtra(PushManager.UNREGISTER_ERROR_EVENT))
		{
			mainAppIntent.removeExtra(PushManager.UNREGISTER_ERROR_EVENT);
		}

		activity.setIntent(mainAppIntent);
	}
}


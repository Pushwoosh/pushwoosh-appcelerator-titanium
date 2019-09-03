/**
 * Pushwoosh SDK
 * (c) Pushwoosh 2012
 */
package com.pushwoosh.module;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.pushwoosh.Pushwoosh;
import com.pushwoosh.badge.PushwooshBadge;
import com.pushwoosh.exception.GetTagsException;
import com.pushwoosh.exception.RegisterForPushNotificationsException;
import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.inapp.PushwooshInApp;
import com.pushwoosh.internal.utils.JsonUtils;
import com.pushwoosh.notification.LocalNotification;
import com.pushwoosh.notification.LocalNotificationReceiver;
import com.pushwoosh.notification.PushwooshNotificationSettings;
import com.pushwoosh.tags.Tags;
import com.pushwoosh.tags.TagsBundle;
import com.pushwoosh.GDPRManager;
import com.pushwoosh.exception.PushwooshException;

import org.appcelerator.kroll.KrollFunction;
import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.common.TiConfig;
import org.appcelerator.titanium.*;
import org.json.JSONException;
import org.json.JSONObject;


@Kroll.module(name = "Pushwoosh", id = "com.pushwoosh.module")
public class PushnotificationsModule extends KrollModule {
	public static PushnotificationsModule INSTANCE = null;

	private static final String LCAT = "PushnotificationsModule";

	private static AtomicReference<String> startPushData = new AtomicReference<String>(null);
	private AtomicBoolean initialized = new AtomicBoolean(false);

	private boolean foregroundPush = true;

	private final AtomicReference<KrollFunction> registrationSuccessCallback = new AtomicReference<KrollFunction>(null);
	private final AtomicReference<KrollFunction> registrationErrorCallback = new AtomicReference<KrollFunction>(null);

	private KrollFunction messageCallback = null;
	private KrollFunction pushOpenCallback = null;
	private KrollFunction pushReceiveCallback = null;

	private Handler mainHandler = new Handler(Looper.getMainLooper());

	// Mandatory method
	@Kroll.onAppCreate
	public static void onAppCreate(TiApplication app) {
		Log.d(LCAT, "inside onAppCreate");
		// put module init code that needs to run when the application is created
	}

	public PushnotificationsModule() {
		super();
		INSTANCE = this;
		Log.d(LCAT, "Push: create module");

		initBroadcastPushFormConfig();
	}

	private void initBroadcastPushFormConfig() {
		try {
			Context context = TiApplication.getInstance();
			ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
			if (ai != null && ai.metaData != null) {
				foregroundPush = ai.metaData.getBoolean("PW_BROADCAST_PUSH", false) || ai.metaData.getBoolean("com.pushwoosh.foreground_push", false);
			}

		} catch (Exception e) {
			Log.e(LCAT, "Failed to read AndroidManifest metaData", e);
		}
	}

	@Kroll.method
	public void initialize(HashMap options) {
		Log.d(LCAT, "initialize called");

		initialized.set(true);

		String pushwooshAppId = (String) options.get("application");
		String googleProjectId = (String) options.get("fcm_sender_id");

		// PUSH-20354 backwards compatibility
		if (TextUtils.isEmpty(googleProjectId)) {
			googleProjectId = (String) options.get("gcm_project");
		}

		// dispatch saved start notification
		String startPush = startPushData.getAndSet(null);
		if (startPush != null) {
			notifyPushReceived(startPush);
			notifyPushOpened(startPush);
		}

		Pushwoosh.getInstance().setAppId(pushwooshAppId);
		Pushwoosh.getInstance().setSenderId(googleProjectId);
	}

	@Kroll.method
	public void onPushOpened(KrollFunction callback) {
		pushOpenCallback = callback;
	}

	@Kroll.method
	public void onPushReceived(KrollFunction callback) {
		pushReceiveCallback = callback;
	}

	@Deprecated
	@Kroll.method
	public void pushNotificationsRegister(HashMap options) {
		Log.w(LCAT, "<pushNotificationsRegister> method is deprecated! Use <initialize> and <register> instead");

		KrollFunction success = (KrollFunction) options.get("success");
		KrollFunction error = (KrollFunction) options.get("error");

		messageCallback = (KrollFunction) options.get("callback");

		initialize(options);

		registerForPushNotifications(success, error);

		Log.d(LCAT, "Push: finished registering for pushes");
	}

	@Kroll.method
	public void registerForPushNotifications(KrollFunction success, KrollFunction error) {
		registrationSuccessCallback.set(success);
		registrationErrorCallback.set(error);

		Pushwoosh.getInstance().registerForPushNotifications(new Callback<String, RegisterForPushNotificationsException>() {
			@Override
			public void process(Result<String, RegisterForPushNotificationsException> result) {
				if (result.isSuccess()) {
					onRegistrationSucceed(result.getData());
				} else if (result.getException() != null) {
					onRegistrationFailed(result.getException().getLocalizedMessage());
				}
			}
		});
	}

	@Kroll.method
	public void unregister() {
		Log.d(LCAT, "unregister called");

		Pushwoosh.getInstance().unregisterForPushNotifications();
	}

	@Kroll.method
	public void setTags(HashMap params) {
		Pushwoosh.getInstance().sendTags(Tags.fromJson(JsonUtils.mapToJson(params)), null);
	}

	@Kroll.method
	public void getTags(final KrollFunction success, final KrollFunction error) {
		Pushwoosh.getInstance().getTags(new Callback<TagsBundle, GetTagsException>() {
			@Override
			public void process(Result<TagsBundle, GetTagsException> result) {
				if (result.isSuccess()) {
					try {
						HashMap<String, Object> resultMap = new HashMap<String, Object>(JsonUtils.jsonToMap(result.getData().toJson()));
						success.callAsync(getKrollObject(), resultMap);
					} catch (JSONException e) {
						e.printStackTrace();
					}
				} else {
					HashMap resultMap = new HashMap();
					error.callAsync(getKrollObject(), resultMap);
				}
			}
		});
	}

	@Kroll.method
	public int scheduleLocalNotification(String message, int seconds) {
		LocalNotification notification = new LocalNotification.Builder()
				.setMessage(message)
				.setDelay(seconds)
				.build();

		return Pushwoosh.getInstance().scheduleLocalNotification(notification).getRequestId();
	}

	@Kroll.method
	public void clearLocalNotification(int id) {
		LocalNotificationReceiver.cancelNotification(id);
	}

	@Kroll.method
	public void clearLocalNotifications() {
		LocalNotificationReceiver.cancelAll();
	}

	@Kroll.method
	public void setMultiNotificationMode() {
		PushwooshNotificationSettings.setMultiNotificationMode(true);
	}

	@Kroll.method
	public void setSimpleNotificationMode() {
		PushwooshNotificationSettings.setMultiNotificationMode(false);
	}

	@Kroll.method
	public void setBadgeNumber(int badgeNumber) {
		PushwooshBadge.setBadgeNumber(badgeNumber);
	}

	@Kroll.method
	public int getBadgeNumber() {
		return PushwooshBadge.getBadgeNumber();
	}

	@Kroll.method
	public void addBadgeNumber(int deltaBadge) {
		PushwooshBadge.addBadgeNumber(deltaBadge);
	}

	@Kroll.method
	public void setUserId(String userId) {
		PushwooshInApp.getInstance().setUserId(userId);
	}

	@Kroll.method
	public void postEvent(String event, HashMap attributes) {
		PushwooshInApp.getInstance().postEvent(event, Tags.fromJson(JsonUtils.mapToJson((Map<String, Object>) attributes)));
	}

	@Kroll.method
	public String getHwid() {
		return Pushwoosh.getInstance().getHwid();
	}

	@Kroll.method
	public String getPushToken() {
		return Pushwoosh.getInstance().getPushToken();
	}

	@Kroll.method
	public void setCommunicationEnabled(boolean enable, final KrollFunction success, final KrollFunction error) {
		GDPRManager.getInstance().setCommunicationEnabled(enable, wrapCallback(success, error));
	}

	@Kroll.method
	public void removeAllDeviceData(final KrollFunction success, final KrollFunction error) {
		GDPRManager.getInstance().removeAllDeviceData(wrapCallback(success, error));
	}

	@Kroll.method
	public void showGDPRConsentUI() {
		GDPRManager.getInstance().showGDPRConsentUI();
	}

	@Kroll.method
	public void showGDPRDeletionUI() {
		GDPRManager.getInstance().showGDPRDeletionUI();
	}

	@Kroll.method
	public boolean isCommunicationEnabled() {
		return GDPRManager.getInstance().isCommunicationEnabled();
	}

	@Kroll.method
	public boolean isDeviceDataRemoved() {
		return GDPRManager.getInstance().isDeviceDataRemoved();
	}

	@Kroll.method
	public boolean isGDPRAvailable() {
		return GDPRManager.getInstance().isAvailable();
	}

	@Kroll.method
	public Map getNotificationSettings() {
		boolean enabled = PushwooshNotificationSettings.areNotificationsEnabled();
        
        Map<String, Boolean> result = new HashMap<>();
        result.put("enabled", enabled);
        
		return result;
	}

	private Callback<Void, PushwooshException> wrapCallback(final KrollFunction success, final KrollFunction error){
		return new Callback<Void, PushwooshException>(){
			@Override
			public void process(@NonNull Result<Void, PushwooshException> result){
				if (result.isSuccess()){
					success.callAsync(getKrollObject(),new HashMap<String, Object>());
				} else if (result.getException() != null) {
					HashMap resultMap = new HashMap();
					resultMap.put("error",result.getException().getMessage());
					error.callAsync(getKrollObject(), resultMap);
				}
			}
		};
	}

	private void notifyPushReceived(String messageData) {
		if (pushReceiveCallback != null) {
			pushReceiveCallback.call(INSTANCE.getKrollObject(), INSTANCE.convertMessageData(messageData));
		}
	}

	private void onNotificationOpened(final String messageData) {
		if (!initialized.get() || !isTitaniumReady()) {
			saveStartPush(messageData);
			return;
		}

		mainHandler.post(new Runnable() {
			@Override
			public void run() {
				notifyPushOpened(messageData);
			}
		});
	}

	private void notifyPushOpened(String messageData) {
		if (messageCallback != null) {
			HashMap<String, Object> data = new HashMap<String, Object>();
			data.put("data", messageData);
			messageCallback.call(getKrollObject(), data);
		}

		if (pushOpenCallback != null) {
			pushOpenCallback.call(getKrollObject(), convertMessageData(messageData));
		}
	}

	private void onRegistrationSucceed(final String registrationId) {
		mainHandler.post(new Runnable() {
			@Override
			public void run() {
				HashMap data = new HashMap();
				data.put("registrationId", registrationId);

				KrollFunction callback = registrationSuccessCallback.getAndSet(null);
				if (callback != null) {
					callback.callAsync(getKrollObject(), data);
				}
			}
		});
	}

	private void onRegistrationFailed(final String error) {
		mainHandler.post(new Runnable() {
			@Override
			public void run() {
				HashMap data = new HashMap();
				data.put("error", error);

				KrollFunction callback = registrationErrorCallback.getAndSet(null);
				if (callback != null) {
					callback.callAsync(getKrollObject(), data);
				}
			}
		});
	}

	private HashMap<String, Object> convertMessageData(String messageData) {
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
		} catch (Exception e) {
			Log.e(LCAT, "Failed to convert push message data", e);
		}

		return result;
	}

	private boolean isTitaniumReady() {
		if (TiApplication.getInstance() == null) {
			return false;
		}

		if (TiApplication.getInstance().getRootActivity() == null) {
			return false;
		}

		return true;
	}


	static boolean onPushOpened(String message) {
		if (INSTANCE != null) {
			INSTANCE.onNotificationOpened(message);
			return true;
		} else {
			Log.d("NotificationReceiver", "PushnotificationsModule.INSTANCE is null");
			saveStartPush(message);
			return false;
		}
	}

	static boolean onPushReceived(final String messageData, final boolean foreground) {
		if (INSTANCE == null) {
			return false;
		}

		if (!INSTANCE.isTitaniumReady() || !INSTANCE.initialized.get()) {
			Log.w(LCAT, "titanium is not ready yet");
			return false;
		}

		INSTANCE.mainHandler.post(new Runnable() {
			@Override
			public void run() {
				INSTANCE.notifyPushReceived(messageData);

				if (foreground && INSTANCE.foregroundPush) {
					INSTANCE.notifyPushOpened(messageData);
				}
			}
		});

		return foreground && !INSTANCE.foregroundPush;
	}

	private static void saveStartPush(String messageData) {
		Log.i(LCAT, "Start push saved: " + messageData);
		startPushData.set(messageData);
	}
}


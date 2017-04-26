/**
 * Pushwoosh SDK
 * (c) Pushwoosh 2017
 *
 */
package com.pushwoosh.module;

import org.appcelerator.kroll.common.Log;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.pushwoosh.PushManager;

public class RegistrationReceiver extends BroadcastReceiver
{
	private static final String LCAT = "RegistrationReceiver";

	public void onReceive(Context context, Intent intent)
	{
		if (intent == null) {
			return;
		}

		Log.d(LCAT, "RECEIVE: " + intent.getAction());

		if (PushnotificationsModule.INSTANCE == null) {
			Log.w(LCAT, "PushnotificationsModule is not instatiated yet");
			return;
		}

		if (intent.hasExtra(PushManager.REGISTER_EVENT)) {
			Log.d(LCAT, "CHECK MESSAGE: push register");

			String pushToken = intent.getExtras().getString(PushManager.REGISTER_EVENT);
			PushnotificationsModule.INSTANCE.onRegistrationSucceed(pushToken);
		} else if (intent.hasExtra(PushManager.UNREGISTER_EVENT)) {
			Log.d(LCAT, "CHECK MESSAGE: push unregister");
		} else if (intent.hasExtra(PushManager.REGISTER_ERROR_EVENT)) {
			Log.d(LCAT, "CHECK MESSAGE: push error");

			String errorMessage = intent.getExtras().getString(PushManager.REGISTER_ERROR_EVENT);
			PushnotificationsModule.INSTANCE.onRegistrationFailed(errorMessage);
		} else if (intent.hasExtra(PushManager.UNREGISTER_ERROR_EVENT)) {
			Log.d(LCAT, "CHECK MESSAGE: unregister error");
		}
	}
}

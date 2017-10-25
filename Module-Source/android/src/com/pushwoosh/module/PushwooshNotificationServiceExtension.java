package com.pushwoosh.module;

import com.pushwoosh.notification.NotificationServiceExtension;
import com.pushwoosh.notification.PushMessage;

public class PushwooshNotificationServiceExtension extends NotificationServiceExtension {

	@Override
	protected boolean onMessageReceived(final PushMessage pushMessage) {
		return PushnotificationsModule.onPushReceived(pushMessage.toJson().toString(), isAppOnForeground()) || super.onMessageReceived(pushMessage);
	}

	@Override
	protected void startActivityForPushMessage(final PushMessage pushMessage) {
		super.startActivityForPushMessage(pushMessage);
		PushnotificationsModule.onPushOpened(pushMessage.toJson().toString());

	}
}

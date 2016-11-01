# Pushwoosh Appcelerator Module #

Class to interact with Pushwoosh Push Notifications module


Example:

```js
var pushwoosh = require('com.pushwoosh.module');

pushwoosh.onPushOpened(function(e) {
  Ti.API.info('Push notification opened: ' + JSON.stringify(e));
});

pushwoosh.initialize({ 
	"application" : "ENTER_PUSHWOOSH_APPID_HERE",
	"gcm_project" : "ENTER_GOOGLE_PROJECT_NUMBER_HERE"
});

pushwoosh.registerForPushNotifications(
  function(e) {
      setTimeout(function() {
		Ti.API.info('Registered with push token: ' + e.registrationId);
		}, 0);
	},
	function(e) {
	setTimeout(function() {
		Ti.API.error("Error during registration: " + e.error);
		}, 0);
	}  
);
```

---
## Method summary
[initialize(config)](#init)  
[registerForPushNotifications(success, fail)](#registerforpushnotifications)  
[onPushReceived(callback)](#onpushreceived)  
[onPushOpened(callback)](#onpushopened)  
[pushNotificationsRegister(config)](#pushnotificationsregister)  
[unregister()](#unregister)  
[getPushToken(success)](#getpushtoken)  
[getHwid(success)](#gethwid)  
[setTags(tags, success, fail)](#settags)  
[startTrackingGeoPushes()](#starttrackinggeopushes)  
[stopTrackingGeoPushes()](#stoptrackinggeopushes)  
[setBadgeNumber(badge)](#setbadgenumber)  
[getBadgeNumber()](#getbadgenumber)  
[addBadgeNumber(badge)](#addbadgenumber)  
[setUserId(userId)](#setuserid)  
[postEvent(event, attributes)](#postevent)  
[scheduleLocalNotification(message, delaySec)](#schedulelocalnotification)  
[clearLocalNotification(notificaion)](#clearlocalnotification)  
[clearLocalNotifications()](#clearlocalnotifications)  
[setMultiNotificationMode()](#setmultinotificationmode)  
[setSimpleNotificationMode()](#setsimplenotificationmode)  
---

### initialize

Initializes Pushwoosh module with application id and google project number.

```js
pushwoosh.initialize({ "application" : "ENTER_PUSHWOOSH_APPID_HERE", "gcm_project" : "ENTER_GOOGLE_PROJECT_NUMBER_HERE" });
```

* **config.application** - Pushwoosh application id
* **config.gcm_project** - GCM project number (for Android push notifications)

---

### registerForPushNotifications

Registers current device for push notifications.

```js
pushwoosh.registerForPushNotifications(
	function(e) {
		var pushToken = e.registrationId;

		// handle successful registration
	},
	function(e) {
		var errorMessage = e.error;
		
		// handle registration error
	}
);
```

NOTE: if user does not allow application to receive push notifications and `UIBackgroundModes remote-notificaion` is not set in **Info.plist** none of these callbacks will be called.

---

### onPushReceived

Invokes callback when push notification is received. If application is closed callback is not called.

```js
pushwoosh.onPushReceived(function(e) {
	var message = e.message;
	var foreground = e.foreground;
	var data = e.data;

	// handle push notification receive event here
});
```

---

### onPushOpened

Invokes callback when push notification is opened.

```js
pushwoosh.onPushOpened(function(e) {
	var message = e.message;
	var foreground = e.foreground;
	var data = e.data;

	// handle push notification open event here
});
```

---

### pushNotificationsRegister

Deprecated. Use [initialize](#initialize) and [registerForPushNotifications](#registerforpushnotifications) instead.

---

### unregister

Unregisters device from push notifications.

```js
pushwoosh.unregister();
```

---

### getPushToken

Returns push notification token or `null` if device is not registered yet.

```js
var pushToken = pushwoosh.getPushToken();
```

---

### getHwid

Returns Pushwoosh HWID used for communications with Pushwoosh API.

```js
var hwid = pushwoosh.getHwid();
```

---

### setTags

Set tags for the device.

```js
pushwoosh.setTags({ "tag" : "value" });
```

---

### startTrackingGeoPushes

Starts geolocation based push notifications.  You need to configure Geozones in Pushwoosh Control panel.

```js
pushwoosh.startTrackingGeoPushes();
```

---

### stopTrackingGeoPushes

Stops geolocation based push notifications.

```js
pushwoosh.stopTrackingGeoPushes();
```

---

### setBadgeNumber

Set application icon badge number.

```js
pushwoosh.setBadgeNumber(1);
```

---

### getBadgeNumber

Returns current application icon badge number.

```js
var badge = pushwoosh.getBadgeNumber();
```

---

### addBadgeNumber

Add to application icon badge number

```js
pushwoosh.addBadgeNumber(1);
```

---

### setUserId

Set User indentifier.  This could be Facebook ID, username or email, or any other user ID.  This allows data and events to be matched across multiple user devices.

```js
pushwoosh.setUserId("...");
```

---

### postEvent

Post events for In-App Messages.  This can trigger In-App message display as specified in Pushwoosh Control Panel.

```js
pushwoosh.postEvent("event", { "attribute" : "value" });
```

---

### scheduleLocalNotification

Android only, Creates local notification.

```js
var notificationId = pushwoosh.scheduleLocalNotification("Your pumpkins are ready!", 30);
```

---

### clearLocalNotification

Android only, Clears pending local notification created by [scheduleLocalNotification](#schedulelocalnotification).

```js
var notificationId = pushwoosh.scheduleLocalNotification("Your pumpkins are ready!", 30);
pushwoosh.clearLocalNotification(notificationId);
```

---

### clearLocalNotifications

Android only, Clears all pending local notifications created by [scheduleLocalNotification](#schedulelocalnotification).

```js
pushwoosh.clearLocalNotifications();
```

---

### setMultiNotificationMode

Android only, Allows multiple notifications in notification bar.

```js
pushwoosh.setMultiNotificationMode();
```

---

### setSimpleNotificationMode

Android only, Allows only the last notification in notification bar.

```js
pushwoosh.setSimpleNotificationMode();
```

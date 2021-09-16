
$.index.open();

var pushwoosh = require('com.pushwoosh.module');
Ti.API.info("module is => " + pushwoosh);

pushwoosh.onPushReceived(function(e) {
	Ti.API.info('Push notification received: ' + JSON.stringify(e));
});

pushwoosh.onPushOpened(function(e) {
	Ti.API.info('Push notification opened: ' + JSON.stringify(e));
	$.pushMessage.text = e.message;
	$.pushData.text = JSON.stringify(e);
});

pushwoosh.initialize({ 
    "application" : "11C10-EF18D",
    "fcm_sender_id" : "562785984945"
});

pushwoosh.registerForPushNotifications(
	function(e) {
        Ti.API.info('JS registration success event: ' + e.registrationId);
        Ti.API.info('Push token ' + pushwoosh.getPushToken());
        $.pushRegistration.text = "Registered with token: " + e.registrationId;
        
    },
    function(e) {
        Ti.API.error("Error during registration: " + e.error);
        $.pushRegistration.text = "Failed to register: " + e.error;
    }  
);

Ti.API.info('Pushwoosh hwid: ' + pushwoosh.getHwid());

Ti.API.info("Notification settings: " + JSON.stringify(pushwoosh.getNotificationSettings()));

//start and stop location tracking
var pushwooshGeozones = require('com.pushwoosh.geozones');
function doClickEnableGeozones(e){
  pushwooshGeozones.startTrackingGeoPushes();
};
function doClickDisableGeozones(e){
  pushwooshGeozones.stopTrackingGeoPushes();
};

// Application icon badges
//pushwoosh.setBadgeNumber(5);
//pushwoosh.addBadgeNumber(3);
//Ti.API.info('Badge number: ' + pushwoosh.getBadgeNumber());

// Segmentation
//pushwoosh.setTags({deviceName:"hello", deviceId:10});
//pushwoosh.setTags({"MyTag":["hello", "world"]});

// // Geopushes
//  var pushwooshGeozones = require('com.pushwoosh.geozones');
//  pushwooshGeozones.startTrackingGeoPushes();
//  pushwooshGeozones.stopTrackingGeoPushes();

// Inapp & Events
//pushwoosh.setUserId("pushwooshid%42");
//pushwoosh.postEvent("buttonPressed", { "buttonNumber" : 4, "buttonLabel" : "banner" });

// Local notifications
//pushwoosh.clearLocalNotifications();
//var notificationId = pushwoosh.scheduleLocalNotification("hello, world", 3);
//pushwoosh.clearLocalNotification(notificationId);
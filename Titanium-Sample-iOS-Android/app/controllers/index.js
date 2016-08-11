
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
    "application" : "4FC89B6D14A655.46488481",
    "gcm_project" : "60756016005"
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

// Application icon badges
//pushwoosh.setBadgeNumber(5);
//pushwoosh.addBadgeNumber(3);
//Ti.API.info('Badge number: ' + pushwoosh.getBadgeNumber());

// Segmentation
//pushwoosh.setTags({deviceName:"hello", deviceId:10});
//pushwoosh.setTags({"MyTag":["hello", "world"]});

// Geopushes
//pushwoosh.startTrackingGeoPushes();
//pushwoosh.stopTrackingGeoPushes();

// Inapp & Events
//pushwoosh.setUserId("pushwooshid%42");
//pushwoosh.postEvent("buttonPressed", { "buttonNumber" : 4, "buttonLabel" : "banner" });

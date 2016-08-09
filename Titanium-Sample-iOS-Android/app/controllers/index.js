function doClick(e) {
    alert($.label.text);
}

$.index.open();

var pushwoosh = require('com.pushwoosh.module');
Ti.API.info("module is => " + pushwoosh);

pushwoosh.onPushReceived(function(e) {
	Ti.API.info('Push notification received: ' + JSON.stringify(e.data));
});

pushwoosh.onPushOpened(function(e) {
	Ti.API.info('Push notification opened: ' + JSON.stringify(e.data));
});

pushwoosh.initialize({ 
    "application" : "4FC89B6D14A655.46488481",
    "gcm_project" : "60756016005",
});

pushwoosh.registerForPushNotifications(
	function(e) {
        Ti.API.info('JS registration success event: ' + e.registrationId);
    },
    function(e) {
        Ti.API.error("Error during registration: " + e.error);
    }  
);

// Segmentation
//pushwoosh.setTags({deviceName:"hello", deviceId:10});
//pushwoosh.setTags({"MyTag":["hello", "world"]});

// Geopushes
//pushwoosh.startTrackingGeoPushes();
//pushwoosh.stopTrackingGeoPushes();

// Inapp & Events
//pushwoosh.setUserId("pushwooshid%42");
//pushwoosh.postEvent("buttonPressed", { "buttonNumber" : 4, "buttonLabel" : "banner" });

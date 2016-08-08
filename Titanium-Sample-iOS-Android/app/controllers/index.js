function doClick(e) {
    alert($.label.text);
}

$.index.open();

var pushnotifications = require('com.pushwoosh.module');
Ti.API.info("module is => " + pushnotifications);

pushnotifications.pushNotificationsRegister({
  "pw_appid": "4FC89B6D14A655.46488481",
  "gcm_projectid": "60756016005", // please note this is the project "number" not the "id" when viewed in the google API console.  You can find this under the project settings.
  success:function(e)
  {
      Ti.API.info('JS registration success event: ' + e.registrationId);
  },
  error:function(e)
  {
      Ti.API.error("Error during registration: "+e.error);
  },
  callback:function(e) // called when a push notification is received
  {
      Ti.API.info('JS message event: ' + JSON.stringify(e.data));
  }
});

// Segmentation
//pushnotifications.setTags({deviceName:"hello", deviceId:10});
//pushnotifications.setTags({"MyTag":["hello", "world"]});

// Geopushes
//pushnotifications.startTrackingGeoPushes();
//pushnotifications.stopTrackingGeoPushes();

// Inapp & Events
//pushnotifications.setUserId("pushwooshid%42");
//pushnotifications.postEvent("buttonPressed", { "buttonNumber" : 4, "buttonLabel" : "banner" });

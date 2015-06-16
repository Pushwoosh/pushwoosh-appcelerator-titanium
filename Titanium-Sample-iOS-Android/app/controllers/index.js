function doClick(e) {
    alert($.label.text);
}

$.index.open();

var pushnotifications = require('com.pushwoosh.module');
Ti.API.info("module is => " + pushnotifications);

pushnotifications.pushNotificationsRegister({
  "pw_appid": "EA75E-CE4BD",
  "gcm_projectid": "ENTER_GOOGLE_PROJECTID_HERE",
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
      alert('JS message event: ' + JSON.stringify(e.data));
  }
});

pushnotifications.setTags({deviceName:"hello", deviceId:10});

//setings list tags "MyTag" with values (array) "hello", "world"
pushnotifications.setTags({"MyTag":["hello", "world"]});

pushnotifications.startTrackingGeoPushes();
pushnotifications.stopTrackingGeoPushes();


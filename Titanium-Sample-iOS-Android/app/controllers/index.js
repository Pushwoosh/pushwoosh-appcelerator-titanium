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
    setTimeout(function() {
      Ti.API.info('JS registration success event: ' + e.registrationId);
    }, 0);
  },
  error:function(e)
  {
    setTimeout(function() {
      Ti.API.error("Error during registration: "+e.error);
    }, 0);
  },
  callback:function(e) // called when a push notification is received
  {
    setTimeout(function() {
      //push notifications title: e.data.aps.alert
      Ti.API.info('JS message event: ' + JSON.stringify(e.data));
    }, 0);
  }
});

pushnotifications.setTags({deviceName:"hello", deviceId:10});

//setings list tags "MyTag" with values (array) "hello", "world"
pushnotifications.setTags({"MyTag":["hello", "world"]});

pushnotifications.startTrackingGeoPushes();
pushnotifications.stopTrackingGeoPushes();

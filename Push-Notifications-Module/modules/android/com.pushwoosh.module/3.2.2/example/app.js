// This is a test harness for your module
// You should do something interesting in this harness 
// to test out the module and to provide instructions 
// to users on how to use it by example.


// open a single window
var win = Ti.UI.createWindow({
	backgroundColor:'white'
});
var label = Ti.UI.createLabel();
win.add(label);
win.open();

var pushnotifications = require('com.pushwoosh.module');
Ti.API.info("module is => " + pushnotifications);
     
pushnotifications.pushNotificationsRegister({
    "pw_appid": "ENTER_PUSHWOOSH_APPID_HERE",
    "gcm_projectid": "ENTER_GOOGLE_PROJECTID_HERE",	//not required for iOS
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
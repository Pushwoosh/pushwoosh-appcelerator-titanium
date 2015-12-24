function __processArg(obj, key) {
    var arg = null;
    if (obj) {
        arg = obj[key] || null;
        delete obj[key];
    }
    return arg;
}

function Controller() {
    function doClick() {
        alert($.label.text);
    }
    require("alloy/controllers/BaseController").apply(this, Array.prototype.slice.call(arguments));
    this.__controllerPath = "index";
    this.args = arguments[0] || {};
    if (arguments[0]) {
        {
            __processArg(arguments[0], "__parentSymbol");
        }
        {
            __processArg(arguments[0], "$model");
        }
        {
            __processArg(arguments[0], "__itemTemplate");
        }
    }
    var $ = this;
    var exports = {};
    var __defers = {};
    $.__views.index = Ti.UI.createWindow({
        backgroundColor: "white",
        id: "index"
    });
    $.__views.index && $.addTopLevelView($.__views.index);
    $.__views.label = Ti.UI.createLabel({
        width: Ti.UI.SIZE,
        height: Ti.UI.SIZE,
        color: "#000",
        font: {
            fontSize: 12
        },
        text: "Hello, World",
        id: "label"
    });
    $.__views.index.add($.__views.label);
    doClick ? $.addListener($.__views.label, "click", doClick) : __defers["$.__views.label!click!doClick"] = true;
    exports.destroy = function() {};
    _.extend($, $.__views);
    $.index.open();
    var pushnotifications = require("com.pushwoosh.module");
    Ti.API.info("module is => " + pushnotifications);
    pushnotifications.pushNotificationsRegister({
        pw_appid: "4FC89B6D14A655.46488481",
        gcm_projectid: "60756016005",
        success: function(e) {
            Ti.API.info("JS registration success event: " + e.registrationId);
        },
        error: function(e) {
            Ti.API.error("Error during registration: " + e.error);
        },
        callback: function(e) {
            Ti.API.info("JS message event: " + JSON.stringify(e.data));
            alert("JS message event: " + JSON.stringify(e.data));
        }
    });
    pushnotifications.setTags({
        deviceName: "hello",
        deviceId: 10
    });
    pushnotifications.setTags({
        MyTag: [ "hello", "world" ]
    });
    pushnotifications.startTrackingGeoPushes();
    pushnotifications.stopTrackingGeoPushes();
    __defers["$.__views.label!click!doClick"] && $.addListener($.__views.label, "click", doClick);
    _.extend($, exports);
}

var Alloy = require("alloy"), Backbone = Alloy.Backbone, _ = Alloy._;

module.exports = Controller;
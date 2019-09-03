'use strict';

exports.id = 'pw.gen_google_services_config';
exports.cliVersion = '>=3.2';
exports.init = init;

function init(logger, config, cli, appc) {
	var dstPath = null;
	var fileContent = null;
	cli.on("build.android.writeAndroidManifest", {
		pre: function(data) {
			if (dstPath != null && fileContent != null) {
				var fs = require('fs');
										
				if (fs.existsSync(dstPath)) fs.unlinkSync(dstPath);
				fs.writeFile(dstPath, fileContent, function(err) {
					if(err) {
						return logger.info(err);
					}
				}); 
			}			
		},
		post: function(data) {
			const fs = require('fs');
			var manifestData = fs.readFileSync(data.args[0], 'utf8');
			manifestData = manifestData.replace(/.*internal\.AnalyticsConnectorRegistrar.*/g, '');
			if (manifestData.indexOf('com.google.firebase.iid.Registrar') == -1) {
				var regex = RegExp('android:name="com.google.firebase.components.ComponentDiscoveryService"[\\s\\S]*?>','g');
				if (regex.exec(manifestData) != null) {
					manifestData = manifestData.substring(0, regex.lastIndex) + 
					"\n<meta-data android:name=\"com.google.firebase.components:com.google.firebase.iid.Registrar\" android:value=\"com.google.firebase.components.ComponentRegistrar\" />" +
					manifestData.substring(regex.lastIndex, manifestData.length);
				}
			}
			fs.writeFileSync(data.args[0], manifestData, 'utf8');
		}
	});
	
	cli.on('build.android.copyResource', {
		pre: function(data) {
			if (data.args.length > 0 && data.args[0].endsWith('platform/android/google-services.json')) {
				logger.info(data.args);
				data.fn = null; //do not copy json to build dir
				
				dstPath = data.args[1];
				var components = dstPath.split("/");
				components.pop();
				dstPath = components.join("/") + "/res";
				
				var fs = require('fs');
				
				if (!fs.existsSync(dstPath)) fs.mkdirSync(dstPath); 
				dstPath += "/values";
				if (!fs.existsSync(dstPath)) fs.mkdirSync(dstPath); //create res/values if not exists
				dstPath += "/googleservices.xml";
				
				var googleServicesJSON = JSON.parse(fs.readFileSync(data.args[0], 'utf8'));
				
				var keep = "";
				var resourcesList = "";
				var append = function (key, value) {
					if (value != null) {
						keep += "@string/" + key + ",";
						resourcesList += "<string name=\"" + key + "\">" + value + "<\/string>\n";
					}			
				};
				
				var query = function (obj, query) {
					var o = obj;
					query.split(".").some(function(element) {
						o = o[element];
						return o == null;
					});
					return o;
				};
				
				var projectInfo = googleServicesJSON["project_info"];
				
				append("firebase_database_url", projectInfo["firebase_url"]);
				append("gcm_defaultSenderId", projectInfo["project_number"]);
				append("google_storage_bucket", projectInfo["storage_bucket"]);
				append("project_id", projectInfo["project_id"]);
				
				var client_list = googleServicesJSON['client'];
				
				//TODO add proper clent selection
				var client = client_list[0];
				
				append("google_api_key", query(client, "api_key.0.current_key"));
				append("google_crash_reporting_api_key", query(client, "api_key.0.current_key"));
				
				append("google_app_id", query(client, "client_info.mobilesdk_app_id"));
				
				var oauth_clients_list = client["oauth_client"];
				
				oauth_clients_list.some(function(element) {
					var client_type = element['client_type'];
					var client_id = element['client_id'];
					if (client_type == 3 && client_id != null) {
						append("default_web_client_id", client_id);
						return true;
					}
					return false;
				});
				
				var services = client['services'];
				append("test_banner_ad_unit_id", query(services, "ads_service.test_banner_ad_unit_id"));
				append("test_interstitial_ad_unit_id", query(services, "ads_service.test_interstitial_ad_unit_id"));
				
				append("ga_trackingId", query(services, "analytics_service.analytics_property.tracking_id"));
				
				if (keep.length > 0) {
					keep = keep.substring(0, keep.length - 1);
				}
				
				fileContent = "<?xml version='1.0' encoding='utf-8'?>\n" + 
							  "<resources tools:keep=\"" + keep + "\" xmlns:tools=\"http://schemas.android.com/tools\">\n" +
							  resourcesList +
							  "</resources>";
		
			}
		}
	});
}

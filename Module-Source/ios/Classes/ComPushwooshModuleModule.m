/**
 * PushwooshModule
 *
 * Created by Your Name
 * Copyright (c) 2015 Your Company. All rights reserved.
 */

#import "ComPushwooshModuleModule.h"
#import "TiBase.h"
#import "TiHost.h"
#import "TiUtils.h"

static __strong NSDictionary * gStartPushData = nil;

@implementation ComPushwooshModuleModule

#pragma mark Internal

// this is generated for your module, please do not change it
- (id)moduleGUID
{
	return @"52006c31-539c-4559-829c-b705e760e977";
}

// this is generated for your module, please do not change it
- (NSString*)moduleId
{
	return @"com.pushwoosh.module";
}

#pragma mark Lifecycle

- (void)startup
{
	// this method is called when the module is first loaded
	// you *must* call the superclass
	[super startup];
	
	self.initialized = NO;

	NSLog(@"[INFO][PW-APPC] %@ loaded", self);
}

#pragma Public APIs

- (void)initialize:(id)args
{
	ENSURE_TYPE(args, NSArray);
	ENSURE_ARG_COUNT(args, 1);

	ENSURE_TYPE(args[0], NSDictionary);
	NSDictionary *options = args[0];

	NSString* appCode = options[@"application"];

	ENSURE_TYPE(appCode, NSString);

	[PushNotificationManager initializeWithAppCode:appCode appName:nil];
	PushNotificationManager * pushManager = [PushNotificationManager pushManager];
	
	if (![[NSBundle mainBundle] objectForInfoDictionaryKey:@"Pushwoosh_ALERT_TYPE"] &&
		![[NSBundle mainBundle] objectForInfoDictionaryKey:@"Pushwoosh_SHOW_ALERT"]) {
		// do not show alert in foreground by default
		[pushManager setShowPushnotificationAlert:NO];
	}
	
	NSString * alertTypeString = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"Pushwoosh_ALERT_TYPE"];
	if([alertTypeString isKindOfClass:[NSString class]] && [alertTypeString isEqualToString:@"NONE"]) {
		[pushManager setShowPushnotificationAlert:NO];
	}
	
	[pushManager setDelegate:self];
	[pushManager sendAppOpen];

	if (gStartPushData && !self.initialized) {
		dispatch_async(dispatch_get_main_queue(), ^{
			[self dispatchPush:gStartPushData onStart:YES];
		});
	}

	self.initialized = YES;
}

- (void)registerForPushNotifications:(id)args
{
	if (args) {
		ENSURE_TYPE(args, NSArray);
		
		if ([args count] > 0) {
			ENSURE_TYPE(args[0], KrollCallback);
			self.successCallback = args[0];
		}

		if ([args count] > 1) {
			ENSURE_TYPE(args[1], KrollCallback);
			self.errorCallback = args[1];
		}
	}

	[[PushNotificationManager pushManager] registerForPushNotifications];
}

- (void)onPushOpened:(id)args
{
	args = [self wrapArguments:args];
	
	ENSURE_TYPE(args, NSArray);
	
	ENSURE_TYPE(args[0], KrollCallback);
	self.pushOpenCallback = args[0];
	
}

- (void)onPushReceived:(id)args
{
	args = [self wrapArguments:args];
	
	ENSURE_TYPE(args, NSArray);
	
	ENSURE_TYPE(args[0], KrollCallback);
	self.pushReceiveCallback = args[0];
}

- (void)pushNotificationsRegister:(id)args
{
	NSLog(@"[WARN][PW-APPC] <pushNotificationsRegister> is deprecated! Use <initialize> and <register> instead");

	ENSURE_TYPE(args, NSArray);
	ENSURE_ARG_COUNT(args, 1);
	
	ENSURE_TYPE(args[0], NSDictionary);
	NSDictionary *options = args[0];
	
	if (options[@"success"]) {
		ENSURE_TYPE(options[@"success"], KrollCallback);
	}
	
	if (options[@"error"]) {
		ENSURE_TYPE(options[@"error"], KrollCallback);
	}
	
	ENSURE_TYPE(options[@"callback"], KrollCallback);
	
	self.successCallback = options[@"success"];
	self.errorCallback = options[@"error"];
	self.messageCallback = options[@"callback"];
		
	NSString* appCode = options[@"pw_appid"];
	ENSURE_TYPE(appCode, NSString);

	[PushNotificationManager initializeWithAppCode:appCode appName:nil];
	PushNotificationManager * pushManager = [PushNotificationManager pushManager];
	[pushManager setShowPushnotificationAlert:NO];
	[pushManager setDelegate:self];
	[pushManager sendAppOpen];

	// register for push notifications!
	NSLog(@"[DEBUG][PW-APPC] registering for push notifications");
	[[PushNotificationManager pushManager] registerForPushNotifications];
 
	if (gStartPushData && !self.initialized) {
		dispatch_async(dispatch_get_main_queue(), ^{
			[self dispatchPush:gStartPushData onStart:YES];
		});
	}
		 
	self.initialized = YES;
}

- (void)unregister:(id)unused
{
	[[PushNotificationManager pushManager] unregisterForPushNotifications];
}

- (void)startTrackingGeoPushes:(id)unused
{
	[[PushNotificationManager pushManager] startLocationTracking];
}

- (void)stopTrackingGeoPushes:(id)unused
{
	[[PushNotificationManager pushManager] stopLocationTracking];
}

- (void)setTags:(id)args
{
	args = [self wrapArguments:args];
	
	NSDictionary *tags = args;
	
	ENSURE_ARG_COUNT(args, 1);
	ENSURE_TYPE(args[0], NSDictionary);
	
	[[PushNotificationManager pushManager] setTags:args[0]];
}

- (void)getLaunchNotification:(id)args
{
	args = [self wrapArguments:args];
	
	ENSURE_TYPE(args, NSArray);
	ENSURE_TYPE(args[0], NSDictionary);
	
	NSDictionary *options = args[0];
	KrollCallback *callback = options[@"callback"];
	
	ENSURE_TYPE(callback, KrollCallback);
	
	TiThreadPerformOnMainThread(^{
		[callback call:@[ gStartPushData ?: [NSNull null] ] thisObject:nil];
	}, NO);
}

- (void)setUserId:(id)args
{
	args = [self wrapArguments:args];
	
	ENSURE_ARG_COUNT(args, 1);
	ENSURE_TYPE(args, NSString);

	[[PushNotificationManager pushManager] setUserId:args];
}

- (void)postEvent:(id)args
{
	ENSURE_TYPE(args, NSArray);
	ENSURE_ARG_COUNT(args, 2);

	ENSURE_TYPE(args[0], NSString);
	NSString *event = args[0];

	ENSURE_TYPE(args[1], NSDictionary);
	NSDictionary *attributes = args[1];

	[[PushNotificationManager pushManager] postEvent:event withAttributes:attributes];
}

- (void)setBadgeNumber:(id)args
{
	args = [self wrapArguments:args];
	
	ENSURE_ARG_COUNT(args, 1);
	ENSURE_TYPE(args[0], NSNumber);
	
	[[UIApplication sharedApplication] setApplicationIconBadgeNumber:[args[0] intValue]];
}

- (id)getBadgeNumber:(id)unused
{
	return @([[UIApplication sharedApplication] applicationIconBadgeNumber]);
}

- (void)addBadgeNumber:(id)args
{
	args = [self wrapArguments:args];
	
	ENSURE_ARG_COUNT(args, 1);
	ENSURE_TYPE(args[0], NSNumber);
	
	[UIApplication sharedApplication].applicationIconBadgeNumber += [args[0] intValue];
}

- (id)getHwid:(id)unused
{
	return [[PushNotificationManager pushManager] getHWID];
}

- (id)getPushToken:(id)unused
{
	return [[PushNotificationManager pushManager] getPushToken];
}

#pragma Internal

// For one argument Appcelerator may wrap it in NSArray or may not in a random way
- (id)wrapArguments:(id)args
{
	if (![args isKindOfClass:[NSArray class]])
		return @[ args ];

	return args;
}

- (void)onDidRegisterForRemoteNotificationsWithDeviceToken:(NSString *)token
{
	NSLog(@"[DEBUG][PW-APPC] registered for pushes: %@", token);
	[self.successCallback call:@[ @{ @"registrationId" : token } ] thisObject:nil];
}

- (void)onDidFailToRegisterForRemoteNotificationsWithError:(NSError*)error
{
	NSLog(@"[DEBUG][PW-APPC] failed to register for pushes: %@", error.localizedDescription);
	[self.errorCallback call:@[ @{ @"error" : error.localizedDescription} ] thisObject:nil];
}

- (void)onPushAccepted:(PushNotificationManager *)manager withNotification:(NSDictionary *)pushNotification onStart:(BOOL)onStart
{
	NSLog(@"[DEBUG][PW-APPC] push accepted: onStart: %d, payload: %@", onStart, pushNotification);

	if (onStart) {
		gStartPushData = pushNotification;
	}
	
	[self dispatchPush:pushNotification onStart:onStart];
}

- (void) dispatchPush:(NSDictionary*)pushData onStart:(BOOL)onStart
{
	NSLog(@"[INFO][PW-APPC] dispatch push: %@", pushData);
	
	NSMutableDictionary *pushInfo = [NSMutableDictionary new];
	
	pushInfo[@"data"] = pushData;
	pushInfo[@"foreground"] = @(!onStart);
	
	id alert = pushData[@"aps"][@"alert"];
	NSString *message = alert;
	if ([alert isKindOfClass:[NSDictionary class]]) {
		message = alert[@"body"];
	}
	
	if (message) {
		pushInfo[@"message"] = message;
	}
	
	NSString *userdata = pushData[@"u"];
	if (userdata) {
		id parsedData = [NSJSONSerialization JSONObjectWithData:[userdata dataUsingEncoding:NSUTF8StringEncoding]
														options:NSJSONReadingMutableContainers
														  error:nil];
		
		if (parsedData) {
			pushInfo[@"extras"] = parsedData;
		}
	}

	[self.messageCallback call:@[ @{ @"data" : pushData } ] thisObject:nil];

	if ([[UIApplication sharedApplication] applicationState] == UIApplicationStateBackground) {
		[self.pushReceiveCallback call:@[ pushInfo ] thisObject:nil];
	}
	else if ([[UIApplication sharedApplication] applicationState] == UIApplicationStateActive) {
		[self.pushReceiveCallback call:@[ pushInfo ] thisObject:nil];
		[self.pushOpenCallback call:@[ pushInfo ] thisObject:nil];
	}
	else {
		[self.pushOpenCallback call:@[ pushInfo ] thisObject:nil];
	}
}

@end

@implementation UIApplication(InternalPushRuntime)

- (BOOL)pushwooshUseRuntimeMagic {
	return YES;
}

// Just keep the launch notification until the module starts and callback functions initalizes
- (void)onPushAccepted:(PushNotificationManager *)manager withNotification:(NSDictionary *)pushNotification onStart:(BOOL)onStart
{
	NSLog(@"[DEBUG][PW-APPC] UIApplication(InternalPushRuntime) push accepted: onStart: %d, payload: %@", onStart, pushNotification);

	if (onStart) {
		gStartPushData = pushNotification;
	}
}

// Set delegate to self on start as the module has not been created yet.
// The delegate will be changed to module instance in startup method
- (NSObject<PushNotificationDelegate> *)getPushwooshDelegate {
	return self;
}

@end

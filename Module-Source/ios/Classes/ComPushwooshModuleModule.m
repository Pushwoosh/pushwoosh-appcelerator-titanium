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

+ (NSString *)readAppName {
	NSString *appName = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"CFBundleDisplayName"];
	
	if(!appName)
		appName = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"CFBundleName"];
	
	if(!appName) {
		appName = @"";
	}
	
	return appName;
}

- (void)initialize:(id)args
{
	ENSURE_TYPE(args, NSArray);
	ENSURE_ARG_COUNT(args, 1);

	ENSURE_TYPE(args[0], NSDictionary);
	NSDictionary *options = args[0];

	NSString* appCode = options[@"application"];

	ENSURE_TYPE(appCode, NSString);

	[PushNotificationManager initializeWithAppCode:appCode appName:[ComPushwooshModuleModule readAppName]];
	PushNotificationManager * pushManager = [PushNotificationManager pushManager];
	[pushManager setShowPushnotificationAlert:NO];
	[pushManager setDelegate:self];
	[pushManager sendAppOpen];

	if (gStartPushData && !self.initialized) {
		[self performSelectorOnMainThread:@selector(dispatchPush:) withObject:gStartPushData waitUntilDone:YES];
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
	ENSURE_TYPE(args, NSArray);
	
	ENSURE_TYPE(args[0], KrollCallback);
	self.pushOpenCallback = args[0];
	
}

- (void)onPushReceived:(id)args
{
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

	[PushNotificationManager initializeWithAppCode:appCode appName:[ComPushwooshModuleModule readAppName]];
	PushNotificationManager * pushManager = [PushNotificationManager pushManager];
	[pushManager setShowPushnotificationAlert:NO];
	[pushManager setDelegate:self];
	[pushManager sendAppOpen];

	// register for push notifications!
	NSLog(@"[DEBUG][PW-APPC] registering for push notifications");
	[[PushNotificationManager pushManager] registerForPushNotifications];
 
	if (gStartPushData && !self.initialized) {
		[self performSelectorOnMainThread:@selector(dispatchPush:) withObject:gStartPushData waitUntilDone:YES];
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
	NSDictionary *tags = args;
	if ([args isKindOfClass: [NSArray class]]) {
		// some versions of Titanium may pass argument in array
		tags = args[0];
	}
	
	ENSURE_TYPE(tags, NSDictionary);
	
	[[PushNotificationManager pushManager] setTags:tags];
}

- (void)getLaunchNotification:(id)args
{
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

	//reset badge counter
	[[UIApplication sharedApplication] setApplicationIconBadgeNumber:0];

	if (onStart) {
		gStartPushData = pushNotification;
	}
	
	[self dispatchPush:pushNotification];
}

- (void) dispatchPush:(NSDictionary*)pushData
{
	NSLog(@"[INFO][PW-APPC] dispatch push: %@", pushData);
	
	[self.messageCallback call:@[ @{ @"data" : pushData } ] thisObject:nil];

	if ([[UIApplication sharedApplication] applicationState] == UIApplicationStateBackground) {
		[self.pushReceiveCallback call:@[ @{ @"data" : pushData } ] thisObject:nil];
	}
	else {
		[self.pushOpenCallback call:@[ @{ @"data" : pushData } ] thisObject:nil];
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

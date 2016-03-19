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
	
	[self setRegistered:NO];
	
	PushNotificationManager * pushManager = [PushNotificationManager pushManager];
	[pushManager setShowPushnotificationAlert:NO];
	[pushManager setDelegate:self];

	NSLog(@"[INFO][PW-APPC] %@ loaded", self);
}

#pragma Public APIs

- (void)pushNotificationsRegister:(id)args
{
	ENSURE_TYPE(args, NSArray);
	ENSURE_ARG_COUNT(args, 1);
    
	ENSURE_TYPE([args objectAtIndex:0], NSDictionary)
	NSDictionary *options = [args objectAtIndex:0];
	
	ENSURE_TYPE([options objectForKey:@"success"], KrollCallback);
	ENSURE_TYPE([options objectForKey:@"error"], KrollCallback);
	ENSURE_TYPE([options objectForKey:@"callback"], KrollCallback);
    
	self.successCallback = [options objectForKey:@"success"];
	self.errorCallback = [options objectForKey:@"error"];
	self.messageCallback = [options objectForKey:@"callback"];
		
	NSString* appCode = [options objectForKey:@"pw_appid"];
	ENSURE_TYPE(appCode, NSString);
    
	PushNotificationManager * pushManager = [PushNotificationManager pushManager];
	[[NSUserDefaults standardUserDefaults] setObject:appCode forKey:@"Pushwoosh_APPID"];
	//we need to re-set APPID if it has been changed (on start we have initialized Push Manager with app id from NSUserDefaults)
	[pushManager setAppCode:appCode];

	// register for push notifications!
	[[PushNotificationManager pushManager] registerForPushNotifications];
 
	if (gStartPushData && !self.registered) {
		[self performSelectorOnMainThread:@selector(dispatchPush:) withObject:gStartPushData waitUntilDone:YES];
	}
		 
	[self setRegistered:YES];
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
		tags = [((NSArray*)args) objectAtIndex: 0];
	}
	
	ENSURE_TYPE(tags, NSDictionary);
	
	[[PushNotificationManager pushManager] setTags:tags];
}

- (void)getLaunchNotification:(id)args
{
	ENSURE_TYPE(args, NSArray);
	ENSURE_TYPE([args objectAtIndex:0], NSDictionary);
	
	NSDictionary *options = [args objectAtIndex:0];
	KrollCallback *callback = [options objectForKey:@"callback"];
    
	ENSURE_TYPE(callback, KrollCallback);
	
	TiThreadPerformOnMainThread(^{
		[callback call:[NSArray arrayWithObject:gStartPushData ?: [NSNull null]] thisObject:nil];
	}, NO);
}

- (void)onDidRegisterForRemoteNotificationsWithDeviceToken:(NSString *)token
{
	NSLog(@"[DEBUG][PW-APPC] registered for pushes: %@", token);
	[self.successCallback call:[NSArray arrayWithObject:@{@"registrationId":token}] thisObject:nil];
}

- (void)onDidFailToRegisterForRemoteNotificationsWithError:(NSError*)error
{
	NSLog(@"[DEBUG][PW-APPC] failed to register for pushes: %@", error.localizedDescription);
	[self.errorCallback call:[NSArray arrayWithObject:@{@"error":error.localizedDescription}] thisObject:nil];
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
	[self.messageCallback call:[NSArray arrayWithObject:@{@"data":pushData}] thisObject:nil];
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

@end

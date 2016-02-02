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

#import <Pushwoosh/PushNotificationManager.h>

static __strong NSDictionary * gStartPushData = nil;

static id objectOrNull(id object)
{
	return object ?: [NSNull null];
}

static BOOL checkArgument(id argument, Class expectedClass)
{
	if (!argument || ! [argument isKindOfClass:expectedClass]) {
		NSLog(@"[ERROR] Pushwoosh: Invalid argument. Expected argument of type %@", expectedClass);
		return NO;
	}
	return YES;
}

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
	
	self.registered = NO;
	
	PushNotificationManager * pushManager = [PushNotificationManager pushManager];
	pushManager.showPushnotificationAlert = NO;
	pushManager.delegate = self;

	NSLog(@"[INFO] %@ loaded", self);
}

- (void)shutdown:(id)sender
{
	// this method is called when the module is being unloaded
	// typically this is during shutdown. make sure you don't do too
	// much processing here or the app will be quit forceably

	// you *must* call the superclass
	[super shutdown:sender];
}

#pragma mark Internal Memory Management

- (void)didReceiveMemoryWarning:(NSNotification*)notification
{
	// optionally release any resources that can be dynamically
	// reloaded once memory is available - such as caches
	[super didReceiveMemoryWarning:notification];
}

#pragma mark Listener Notifications

- (void)_listenerAdded:(NSString *)type count:(int)count
{
	if (count == 1 && [type isEqualToString:@"my_event"])
	{
		// the first (of potentially many) listener is being added
		// for event named 'my_event'
	}
}

- (void)_listenerRemoved:(NSString *)type count:(int)count
{
	if (count == 0 && [type isEqualToString:@"my_event"])
	{
		// the last listener called for event named 'my_event' has
		// been removed, we can optionally clean up any resources
		// since no body is listening at this point for that event
	}
}

#pragma Public APIs

- (void)pushNotificationsRegister:(id)args
{
	NSArray *argumentsArray = args;
	if(!checkArgument(argumentsArray, [NSArray class]))
		return;
	
	if([argumentsArray count] != 1)
	{
		NSLog(@"[ERROR] Pushwoosh: pushNotificationsRegister, expected 1 argument");
		return;
	}
	
	NSDictionary *options = [argumentsArray objectAtIndex:0];
	if(!checkArgument(options, [NSDictionary class]))
	   return;
	
	self.successCallback = [options objectForKey:@"success"];
	self.errorCallback = [options objectForKey:@"error"];
	self.messageCallback = [options objectForKey:@"callback"];
	
	if(!checkArgument(self.messageCallback, [KrollCallback class]))
		return;
	
	NSString* appCode = [options objectForKey:@"pw_appid"];
	if(!checkArgument(appCode, [NSString class]))
		return;
	
	PushNotificationManager * pushManager = [PushNotificationManager pushManager];
	[[NSUserDefaults standardUserDefaults] setObject:appCode forKey:@"Pushwoosh_APPID"];
	//we need to re-set APPID if it has been changed (on start we have initialized Push Manager with app id from NSUserDefaults)
	pushManager.appCode = appCode;

	// register for push notifications!
	[[PushNotificationManager pushManager] registerForPushNotifications];
 
	if (gStartPushData && !self.registered)
		[self performSelectorOnMainThread:@selector(dispatchPush:) withObject:gStartPushData waitUntilDone:YES];
		 
	self.registered = YES;
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
	if ([args isKindOfClass: [NSArray class]])
	{
		// some versions of Titanium may pass argument in array
		tags = [((NSArray*)args) objectAtIndex: 0];
	}
	
	checkArgument(tags, [NSDictionary class]);
	
	[[PushNotificationManager pushManager] setTags:tags];
}

- (void)getLaunchNotification:(id)args
{
	NSArray *argumentsArray = args;
	NSDictionary *options = [argumentsArray objectAtIndex:0];
	checkArgument(options, [NSDictionary class]);
	
	KrollCallback *callback = [options objectForKey:@"callback"];
	checkArgument(callback, [KrollCallback class]);
	
	dispatch_async(dispatch_get_main_queue(), ^{
		[callback call:[NSArray arrayWithObject:objectOrNull(gStartPushData)] thisObject:nil];
	});
	
}

- (void)onDidRegisterForRemoteNotificationsWithDeviceToken:(NSString *)token
{
	NSDictionary *result = [NSDictionary dictionaryWithObject:token forKey:@"registrationId"];

	[self.successCallback call:[NSArray arrayWithObject:result] thisObject:nil];
}

- (void)onDidFailToRegisterForRemoteNotificationsWithError:(NSError*)error
{
	NSDictionary *result = [NSDictionary dictionaryWithObject:error.localizedDescription forKey:@"error"];
	[self.errorCallback call:[NSArray arrayWithObject:result] thisObject:nil];
}

- (void)onPushAccepted:(PushNotificationManager *)manager withNotification:(NSDictionary *)pushNotification onStart:(BOOL)onStart
{
	//reset badge counter
	[[UIApplication sharedApplication] setApplicationIconBadgeNumber:0];

	if (onStart)
		gStartPushData = pushNotification;
	
	[self dispatchPush:pushNotification];
}

- (void) dispatchPush:(NSDictionary*)pushData
{
	NSDictionary *result = [NSDictionary dictionaryWithObject:pushData forKey:@"data"];
	[self.messageCallback call:[NSArray arrayWithObject:result] thisObject:nil];
}

@end

@implementation UIApplication(InternalPushRuntime)

- (BOOL)pushwooshUseRuntimeMagic {
	return YES;
}

// Just keep the launch notification until the module starts and callback functions initalizes
- (void)onPushAccepted:(PushNotificationManager *)manager withNotification:(NSDictionary *)pushNotification onStart:(BOOL)onStart
{
	if (onStart)
		gStartPushData = pushNotification;
}

// Set delegate to self on start as the module has not been created yet.
// The delegate will be changed to module instance in startup method
- (NSObject<PushNotificationDelegate> *)getPushwooshDelegate {
	return self;
}

@end

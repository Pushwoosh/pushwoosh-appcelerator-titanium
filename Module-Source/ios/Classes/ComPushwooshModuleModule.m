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
#import "TiApp.h"
#import "Pushwoosh.h"
#import "PWInAppManager.h"
#import "PWGDPRManager.h"
#import <UserNotifications/UserNotifications.h>
#import "PWMUserNotificationCenterDelegateProxy.h"

NSString * const PWMLocalNotificationUidKey = @"pw_localnotification_uid";

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
    
    [PWMUserNotificationCenterDelegateProxy setupWithPushDelegate:[PushNotificationManager pushManager].notificationCenterDelegate];

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

- (void)setTags:(id)args
{
	args = [self wrapArguments:args];
	
	NSDictionary *tags = args;
	
	ENSURE_ARG_COUNT(args, 1);
	ENSURE_TYPE(args[0], NSDictionary);
	
	[[PushNotificationManager pushManager] setTags:args[0]];
}

- (void)getTags:(id)args
{
	ENSURE_TYPE(args[0], KrollCallback);
	KrollCallback *successCallback = args[0];
	KrollCallback *errorCallback = nil;
	
	if ([args count] > 1) {
		ENSURE_TYPE(args[1], KrollCallback);
		errorCallback = args[1];
	}
	
	[[PushNotificationManager pushManager] loadTags:^(NSDictionary* tags) {
		[successCallback call:@[tags] thisObject:nil];
	} error:^(NSError *error) {
		if (errorCallback) {
			[errorCallback call:@[@{ @"error" : error.localizedDescription }] thisObject:nil];
		}
	}];
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
    ENSURE_TYPE(args[0], NSString);

    [[PWInAppManager sharedManager] setUserId:args[0]];
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

- (id)getNotificationSettings:(id)unused
{
	NSDictionary *settings = [PushNotificationManager getRemoteNotificationStatus];
	NSDictionary *result = @{
		 @"enabled" : @((BOOL)![settings[@"enabled"] isEqualToString:@"0"]),
		 @"pushAlert" : @((BOOL)![settings[@"pushAlert"] isEqualToString:@"0"]),
		 @"pushBadge" : @((BOOL)![settings[@"pushBadge"] isEqualToString:@"0"]),
		 @"pushSound" : @((BOOL)![settings[@"pushSound"] isEqualToString:@"0"])
	};
	return result;
}

- (id)scheduleLocalNotification:(id)args {
    ENSURE_ARG_COUNT(args, 2)
    ENSURE_TYPE(args[0], NSString);
    ENSURE_TYPE(args[1], NSNumber);
    
    NSString *body = args[0];
    NSInteger delay = [args[1] integerValue];

    static int32_t uids = 0;
    NSNumber *uid = @(uids++);
    [self sendLocalNotificationWithBody:body delay:delay identifier:[uid integerValue]];
    return uid;
} //(String message, int seconds);

- (NSString *)stringIdentifierFromInt:(NSInteger)identifier {
    return [PWMLocalNotificationUidKey stringByAppendingFormat:@"%ld", (long)identifier];
}

- (void)sendLocalNotificationWithBody:(NSString *)body delay:(NSUInteger)delay identifier:(NSInteger)identifier {
    if (@available(iOS 10, *)) {
        UNUserNotificationCenter *center = [UNUserNotificationCenter currentNotificationCenter];
        UNMutableNotificationContent *content = [UNMutableNotificationContent new];
        content.body = body;
        content.sound = [UNNotificationSound defaultSound];
        UNTimeIntervalNotificationTrigger *trigger = [UNTimeIntervalNotificationTrigger triggerWithTimeInterval:delay repeats:NO];
        UNNotificationRequest *request = [UNNotificationRequest requestWithIdentifier:[self stringIdentifierFromInt:identifier]
                                                                              content:content
                                                                              trigger:trigger];
        
        [center addNotificationRequest:request withCompletionHandler:^(NSError *_Nullable error) {
            if (error != nil) {
                NSLog(@"Something went wrong: %@", error);
            }
        }];
    } else {
        UILocalNotification *localNotification = [[UILocalNotification alloc] init];
        localNotification.fireDate = [NSDate dateWithTimeIntervalSinceNow:delay];
        localNotification.alertBody = body;
        localNotification.timeZone = [NSTimeZone defaultTimeZone];
        localNotification.userInfo = @{ PWMLocalNotificationUidKey : @(identifier) };
        [[UIApplication sharedApplication] scheduleLocalNotification:localNotification];
    }
}

- (void)clearLocalNotification:(id)args {
    args = [self wrapArguments:args];
    ENSURE_ARG_COUNT(args, 1)
    ENSURE_TYPE(args[0], NSNumber);
    
    NSNumber *uid = args[0];

    if (@available(iOS 10, *)) {
        [[UNUserNotificationCenter currentNotificationCenter] removePendingNotificationRequestsWithIdentifiers:@[ [self stringIdentifierFromInt:[uid integerValue]] ]];
    } else {
        __block UILocalNotification *notificationToDelete = nil;
        [[UIApplication sharedApplication].scheduledLocalNotifications enumerateObjectsUsingBlock:^(UILocalNotification * _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
            if ([[obj.userInfo objectForKey:PWMLocalNotificationUidKey] isEqual:uid]) {
                notificationToDelete = obj;
                *stop = YES;
            }
        }];
        if (notificationToDelete) {
            [UIApplication sharedApplication].scheduledLocalNotifications = [[UIApplication sharedApplication].scheduledLocalNotifications filteredArrayUsingPredicate:[NSPredicate predicateWithBlock:^BOOL(id  _Nullable evaluatedObject, NSDictionary<NSString *,id> * _Nullable bindings) {
                return evaluatedObject != notificationToDelete;
            }]];
        }
    }
}

- (void)clearLocalNotifications:(id)unused {
    [UIApplication sharedApplication].scheduledLocalNotifications = @[];
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

- (void)onPushReceived:(PushNotificationManager *)pushManager withNotification:(NSDictionary *)pushNotification onStart:(BOOL)onStart
{
    if (onStart) {
        gStartPushData = pushNotification;
    }
    
    [self dispatchPushReceived:pushNotification onStart:onStart];
}

- (void)onPushAccepted:(PushNotificationManager *)manager withNotification:(NSDictionary *)pushNotification onStart:(BOOL)onStart
{
	NSLog(@"[DEBUG][PW-APPC] push accepted: onStart: %d, payload: %@", onStart, pushNotification);

	if (onStart) {
		gStartPushData = pushNotification;
	}
	
	[self dispatchPushAccepted:pushNotification onStart:onStart];
}

- (NSDictionary *)pushInfoWithPushData:(NSDictionary *)pushData onStart:(BOOL)onStart {
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
    return pushInfo;
}

- (void)dispatchPushReceived:(NSDictionary *)pushData onStart:(BOOL)onStart
{
    NSLog(@"[INFO][PW-APPC] dispatch push accepted: %@", pushData);
    
    NSDictionary *pushInfo = [self pushInfoWithPushData:pushData onStart:onStart];
    
    [self.messageCallback call:@[ @{ @"data" : pushData } ] thisObject:nil];
    [self.pushReceiveCallback call:@[ pushInfo ] thisObject:nil];
}

- (void)dispatchPushAccepted:(NSDictionary *)pushData onStart:(BOOL)onStart
{
    NSLog(@"[INFO][PW-APPC] dispatch push accepted: %@", pushData);
    
    NSDictionary *pushInfo = [self pushInfoWithPushData:pushData onStart:onStart];
    
    [self.messageCallback call:@[ @{ @"data" : pushData } ] thisObject:nil];
    [self.pushOpenCallback call:@[ pushInfo ] thisObject:nil];
}

- (void)dispatchPush:(NSDictionary *)pushData onStart:(BOOL)onStart
{
    NSLog(@"[INFO][PW-APPC] dispatch push: %@", pushData);
    
    NSDictionary *pushInfo = [self pushInfoWithPushData:pushData onStart:onStart];
    
    [self.messageCallback call:@[ @{ @"data" : pushData } ] thisObject:nil];
    [self.pushReceiveCallback call:@[ pushInfo ] thisObject:nil];
    [self.pushOpenCallback call:@[ pushInfo ] thisObject:nil];
}


/**
 Indicates availability of the GDPR compliance solution.
 */
#pragma Public APIs
- (BOOL)isGDPRAvailable:(id)unused
{
    return [PWGDPRManager sharedManager].isAvailable;
}

- (BOOL)isCommunicationEnabled:(id)unused
{
    return [PWGDPRManager sharedManager].isCommunicationEnabled;
}

- (BOOL)isDeviceDataRemoved:(id)unused
{
    return [PWGDPRManager sharedManager].isDeviceDataRemoved;
}

- (void(^)(NSError *))wrapCallback:(id)args callbackIndex:(NSUInteger)index
{
    KrollCallback *successCallback = nil;
    KrollCallback *errorCallback = nil;
    if ([args count] > index) {
        ENSURE_TYPE(args[index], KrollCallback);
        successCallback = args[index];
    }
    if ([args count] > (index + 1)) {
        ENSURE_TYPE(args[index + 1], KrollCallback);
        errorCallback = args[index + 1];
    }

    return ^(NSError *error) {
        if (error) {
            if (errorCallback) {
                [errorCallback call:@[@{ @"error" : error.localizedDescription }] thisObject:nil];
            }
        } else {
            if (successCallback) {
                [successCallback call:nil thisObject:nil];
            }
        }
    };
}
/**
 Enable/disable all communication with Pushwoosh. Enabled by default.
 */
- (void)setCommunicationEnabled:(id)args
{
    ENSURE_ARG_COUNT(args, 3);
    ENSURE_TYPE(args[0], NSNumber);
    BOOL *enable = [args[0] boolValue];
    [[PWGDPRManager sharedManager] setCommunicationEnabled:enable completion:[self wrapCallback:args callbackIndex:1]];
}

/**
 Removes all device data from Pushwoosh and stops all interactions and communication permanently.
 */
- (void)removeAllDeviceDataWithCompletion:(id)args
{
    ENSURE_TYPE(args[0], KrollCallback);
    ENSURE_ARG_COUNT(args, 1);
    [[PWGDPRManager sharedManager] removeAllDeviceDataWithCompletion:[self wrapCallback:args callbackIndex:0]];
}

- (void)showGDPRConsentUI:(id)unused
{
    dispatch_async(dispatch_get_main_queue(), ^{
        [[PWGDPRManager sharedManager] showGDPRConsentUI];
    });    
}

- (void)showGDPRDeletionUI:(id)unused
{
    dispatch_async(dispatch_get_main_queue(), ^{
       [[PWGDPRManager sharedManager] showGDPRDeletionUI];
    });
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

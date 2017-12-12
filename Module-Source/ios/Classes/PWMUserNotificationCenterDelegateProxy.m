//
//  PWUserNotificationCenterDelegateProxy.m
//  test
//
//  Created by Anton Kaizer on 07.12.2017.
//  Copyright © 2017 Pushwoosh. All rights reserved.
//

#import "PWMUserNotificationCenterDelegateProxy.h"
#import "ComPushwooshModuleModule.h"

@interface PWMUserNotificationCenterDelegateProxy()<UNUserNotificationCenterDelegate>

@property (nonatomic, weak) id<UNUserNotificationCenterDelegate> pushDelegate;
@property (nonatomic, weak) id<UNUserNotificationCenterDelegate> prevDelegate;

@end

@implementation PWMUserNotificationCenterDelegateProxy


+ (BOOL)setupWithPushDelegate:(id<UNUserNotificationCenterDelegate>)pushDelegate {
    static PWMUserNotificationCenterDelegateProxy *instance = nil;
    if (instance)
        return YES;
    NSNumber *use = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"PWUserNotificationCenterDelegateProxy"];
    if (use && [use boolValue] == NO) {
        [UNUserNotificationCenter currentNotificationCenter].delegate = pushDelegate;
        return NO;
    }
    instance = [PWMUserNotificationCenterDelegateProxy new];
    instance.pushDelegate = pushDelegate;
    instance.prevDelegate = [UNUserNotificationCenter currentNotificationCenter].delegate;
    [UNUserNotificationCenter currentNotificationCenter].delegate = instance;
    
    return YES;
}

- (BOOL)isPush:(UNNotification *)notification {
    return [notification.request.trigger isKindOfClass:[UNPushNotificationTrigger class]] || [notification.request.content.userInfo objectForKey:@"aps"];
}

- (BOOL)isPlotNotification:(UNNotification *)notification {
    return [notification.request.content.userInfo objectForKey:@"notificationHandlerType"] != nil;
}

- (BOOL)isPluginLocalNotification:(UNNotification *)notification {
    return [notification.request.identifier hasPrefix:PWMLocalNotificationUidKey];
}

- (BOOL)isPushwooshNotification:(UNNotification *)notification {
    return ![self isPlotNotification:notification] && (!_prevDelegate || [self isPush:notification] || [self isPluginLocalNotification:notification]);
}

- (void)userNotificationCenter:(UNUserNotificationCenter *)center willPresentNotification:(UNNotification *)notification withCompletionHandler:(void (^)(UNNotificationPresentationOptions options))completionHandler {
    if ([self isPushwooshNotification:notification]) {
        if ([_pushDelegate respondsToSelector:@selector(userNotificationCenter:willPresentNotification:withCompletionHandler:)]) {
            [_pushDelegate userNotificationCenter:center willPresentNotification:notification withCompletionHandler:completionHandler];
        }
    } else {
        if ([_prevDelegate respondsToSelector:@selector(userNotificationCenter:willPresentNotification:withCompletionHandler:)]) {
            [_prevDelegate userNotificationCenter:center willPresentNotification:notification withCompletionHandler:completionHandler];
        }
    }
}

- (void)userNotificationCenter:(UNUserNotificationCenter *)center didReceiveNotificationResponse:(UNNotificationResponse *)response withCompletionHandler:(void(^)(void))completionHandler {
    if ([self isPushwooshNotification:response.notification]) {
        if ([_pushDelegate respondsToSelector:@selector(userNotificationCenter:didReceiveNotificationResponse:withCompletionHandler:)]) {
            [_pushDelegate userNotificationCenter:center didReceiveNotificationResponse:response withCompletionHandler:completionHandler];
        }
    } else {
        if ([_prevDelegate respondsToSelector:@selector(userNotificationCenter:didReceiveNotificationResponse:withCompletionHandler:)]) {
            [_prevDelegate userNotificationCenter:center didReceiveNotificationResponse:response withCompletionHandler:completionHandler];
        }
    }
}

@end

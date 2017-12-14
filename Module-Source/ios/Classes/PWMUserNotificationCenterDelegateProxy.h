//
//  PWUserNotificationCenterDelegateProxy.h
//  test
//
//  Created by Anton Kaizer on 07.12.2017.
//  Copyright © 2017 Pushwoosh. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <UserNotifications/UserNotifications.h>

@interface PWMUserNotificationCenterDelegateProxy : NSObject

+ (BOOL)setupWithPushDelegate:(id<UNUserNotificationCenterDelegate>)pushDelegate;

@end

/**
 * PushwooshModule
 *
 * Created by Your Name
 * Copyright (c) 2015 Your Company. All rights reserved.
 */

#import "TiModule.h"
#import <Pushwoosh/PushNotificationManager.h>

@interface ComPushwooshModuleModule : TiModule<PushNotificationDelegate>
{
}

@property (strong) KrollCallback *successCallback;
@property (strong) KrollCallback *errorCallback;
@property (strong) KrollCallback *messageCallback;

@property BOOL registered;

@end

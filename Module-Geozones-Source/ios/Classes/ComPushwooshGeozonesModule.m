/**
 * PushwooshGeozones
 *
 * Created by Your Name
 * Copyright (c) 2019 Your Company. All rights reserved.
 */

#import "ComPushwooshGeozonesModule.h"
#import "TiBase.h"
#import "TiHost.h"
#import "TiUtils.h"

#import <PushwooshGeozones/PWGeozonesManager.h>

@implementation ComPushwooshGeozonesModule

#pragma mark Internal

// This is generated for your module, please do not change it
- (id)moduleGUID
{
  return @"91863e7a-51b9-4e6b-a717-9955831b9c93";
}

// This is generated for your module, please do not change it
- (NSString *)moduleId
{
  return @"com.pushwoosh.geozones";
}

#pragma mark Lifecycle

- (void)startup
{
  // This method is called when the module is first loaded
  // You *must* call the superclass
  [super startup];
  DebugLog(@"[DEBUG] %@ loaded", self);
}

#pragma Public APIs

- (void)startTrackingGeoPushes:(id)unused
{
    [[PWGeozonesManager sharedManager] startLocationTracking];
}

- (void)stopTrackingGeoPushes:(id)unused
{
    [[PWGeozonesManager sharedManager] stopLocationTracking];
}

@end

package com.pushwoosh.module;

import android.app.Application.ActivityLifecycleCallbacks;
import android.app.Activity;
import android.os.Bundle;

import org.appcelerator.kroll.common.Log;

class ActivityMonitor implements ActivityLifecycleCallbacks {
	private static final String LCAT = "PushnotificationsModule";
	
	private int activeCount = 0;
	
	//Kroll activity lifecycle methods are completely broken :(
	//Use native Android lifecycle events
	@Override
	public void onActivityPaused(Activity activity) {
		Log.d(LCAT, String.format("Push: ACTIVITY PAUSED: %d", --activeCount));
		
		//whoa whoa, who started activity before our module?
		if(activeCount < 0)
			activeCount = 0;
		
		if(activeCount == 0)
			PushnotificationsModule.INSTANCE.unregisterReceivers();
	}
	
	@Override
	public void onActivityResumed(Activity activity) {
		Log.d(LCAT, String.format("Push: ACTIVITY RESUMED: %d", ++activeCount));
		
		PushnotificationsModule.INSTANCE.checkMessage(activity.getIntent(), false);
		PushnotificationsModule.INSTANCE.resetIntentValues(activity);
		
		if(activeCount != 0)
			PushnotificationsModule.INSTANCE.registerReceivers();
	}
	
	@Override
	public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
		Log.d(LCAT, "Push: ACTIVITY CREATED");
	}
	
	@Override
	public void onActivityStarted(Activity activity) {
		Log.d(LCAT, "Push: ACTIVITY STARTED");
	}
	
	@Override
	public void onActivityStopped(Activity activity) {
		Log.d(LCAT, "Push: ACTIVITY STOPPED");
	}
	
	@Override
	public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
		Log.d(LCAT, "Push: ACTIVITY SAVED INSTANCE");
	}
	
	@Override
	public void onActivityDestroyed(Activity activity) {
		Log.d(LCAT, "Push: ACTIVITY DESTROYED");
	}
}
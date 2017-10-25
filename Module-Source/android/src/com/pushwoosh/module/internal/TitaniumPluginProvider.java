package com.pushwoosh.module.internal;

import com.pushwoosh.internal.PluginProvider;

public class TitaniumPluginProvider implements PluginProvider {
	@Override
	public String getPluginType() {
		return "Titanium";
	}

	@Override
	public int richMediaStartDelay() {
		return DEFAULT_RICH_MEDIA_START_DELAY;
	}
}

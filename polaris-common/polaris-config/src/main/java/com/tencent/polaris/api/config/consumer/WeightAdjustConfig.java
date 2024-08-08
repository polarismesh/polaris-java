package com.tencent.polaris.api.config.consumer;

import java.util.List;

import com.tencent.polaris.api.config.plugin.PluginConfig;
import com.tencent.polaris.api.config.verify.Verifier;

public interface WeightAdjustConfig extends PluginConfig, Verifier {

	boolean isEnable();

	List<String> getChain();
}

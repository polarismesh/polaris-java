package com.tencent.polaris.plugins.connector.nacos;

import org.junit.Assert;
import org.junit.Test;

public class NacosConnectorTests {

	@Test
	public void testNacosAnalyze() {
		String serviceName;

		serviceName = "GROUP__svc";
		Assert.assertEquals("svc", NacosConnector.analyzeNacosService(serviceName));
		Assert.assertEquals("GROUP", NacosConnector.analyzeNacosGroup(serviceName));

		serviceName = "GROUP__svc_svc";
		Assert.assertEquals("svc_svc", NacosConnector.analyzeNacosService(serviceName));
		Assert.assertEquals("GROUP", NacosConnector.analyzeNacosGroup(serviceName));

		serviceName = "GROUP_123__svc_svc";
		Assert.assertEquals("svc_svc", NacosConnector.analyzeNacosService(serviceName));
		Assert.assertEquals("GROUP_123", NacosConnector.analyzeNacosGroup(serviceName));

		serviceName = "GROUP_123__svc__svc";
		Assert.assertEquals("svc__svc", NacosConnector.analyzeNacosService(serviceName));
		Assert.assertEquals("GROUP_123", NacosConnector.analyzeNacosGroup(serviceName));

		serviceName = "__GROUP_123__svc__svc";
		Assert.assertEquals("GROUP_123__svc__svc", NacosConnector.analyzeNacosService(serviceName));
		Assert.assertEquals("DEFAULT_GROUP", NacosConnector.analyzeNacosGroup(serviceName));
	}

}
package com.tencent.polaris.configuration.client;

import com.google.gson.reflect.TypeToken;

import com.tencent.polaris.api.config.configuration.ConfigFileConfig;
import com.tencent.polaris.configuration.client.internal.ConfigFileRepo;
import com.tencent.polaris.configuration.client.internal.ConfigYamlFile;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.Properties;

import static org.mockito.Mockito.when;

/**
 * @author lepdou 2022-03-08
 */
@RunWith(MockitoJUnitRunner.class)
public class ConfigYamlFileTest {

    @Mock
    private ConfigFileRepo   configFileRepo;
    @Mock
    private ConfigFileConfig configFileConfig;

    @Test
    public void testGetYamlKey() {
        String content = "root:\n"
                         + "  k1: 10\n"
                         + "  k2: false\n"
                         + "  k3: 5.6\n"
                         + "  k4: 1,2,3,4,5\n"
                         + "  k5: T1\n"
                         + "  k6: '{\"name\":\"zhangsan\",\"age\":18,\"labels\": {\"key1\":\"value1\"}}'\n"
                         + "  k7: '[{\"name\":\"zhangsan\",\"age\":18,\"labels\": {\"key1\":\"value1\"}},{\"name\":\"lisi\",\"age\":20,\"labels\": {\"key1\":\"value2\"}}]'";
        when(configFileRepo.getContent()).thenReturn(content);

        ConfigYamlFile configFile =
            new ConfigYamlFile(ConfigFileTestUtils.testNamespace, ConfigFileTestUtils.testGroup,
                               ConfigFileTestUtils.testFileName, configFileRepo,
                               configFileConfig);

        Assert.assertEquals(ConfigFileTestUtils.testNamespace, configFile.getNamespace());
        Assert.assertEquals(ConfigFileTestUtils.testGroup, configFile.getFileGroup());
        Assert.assertEquals(ConfigFileTestUtils.testFileName, configFile.getFileName());

        Assert.assertEquals(content, configFile.getContent());
        Assert.assertTrue(configFile.hasContent());

        Assert.assertEquals(new Integer(10), configFile.getIntProperty("root.k1", 8));
        Assert.assertEquals("false", configFile.getProperty("root.k2", "true"));
        Assert.assertFalse(configFile.getBooleanProperty("root.k2", true));
        Assert.assertEquals(new Float(5.6f), configFile.getFloatProperty("root.k3", 1.0f));
        Assert.assertEquals(new Double(5.6), configFile.getDoubleProperty("root.k3", 1.0));

        String[] arr = configFile.getArrayProperty("root.k4", ",", new String[]{});
        Assert.assertEquals(5, arr.length);
        for (int i = 1; i < 6; i++) {
            Assert.assertEquals(i + "", arr[i - 1]);
        }

        Assert.assertEquals(new Byte((byte) 10), configFile.getByteProperty("root.k1", (byte) 1));

        Assert.assertEquals(ConfigFileTestUtils.MyType.T1, configFile.getEnumProperty("root.k5",
                                                                                      ConfigFileTestUtils.MyType.class,
                                                                                      ConfigFileTestUtils.MyType.T2));

        ConfigFileTestUtils.User user = configFile.getJsonProperty("root.k6", ConfigFileTestUtils.User.class, null);
        Assert.assertNotNull(user);
        Assert.assertEquals("zhangsan", user.getName());
        Assert.assertEquals(18, user.getAge());
        Assert.assertEquals(user.getLabels().size(), 1);
        Assert.assertEquals(user.getLabels().get("key1"), "value1");

        List<ConfigFileTestUtils.User>
            users =
            configFile.getJsonProperty("root.k7", new TypeToken<List<ConfigFileTestUtils.User>>() {
            }.getType(), null);
        Assert.assertNotNull(users);
        Assert.assertEquals(2, users.size());
        for (ConfigFileTestUtils.User user2 : users) {
            Assert.assertNotNull(user2.getName());
            Assert.assertTrue(user2.getAge() > 0);
            Assert.assertTrue(user2.getLabels().size() > 0);
        }
    }
}

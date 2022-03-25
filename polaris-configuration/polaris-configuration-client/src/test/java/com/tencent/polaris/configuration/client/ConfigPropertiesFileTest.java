package com.tencent.polaris.configuration.client;

import com.google.gson.reflect.TypeToken;

import com.tencent.polaris.api.config.configuration.ConfigFileConfig;
import com.tencent.polaris.configuration.api.core.ChangeType;
import com.tencent.polaris.configuration.api.core.ConfigFileChangeEvent;
import com.tencent.polaris.configuration.api.core.ConfigFileChangeListener;
import com.tencent.polaris.configuration.api.core.ConfigKVFileChangeEvent;
import com.tencent.polaris.configuration.api.core.ConfigKVFileChangeListener;
import com.tencent.polaris.configuration.api.core.ConfigPropertyChangeInfo;
import com.tencent.polaris.configuration.client.internal.ConfigFileRepo;
import com.tencent.polaris.configuration.client.internal.ConfigPropertiesFile;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.Mockito.when;

/**
 * @author lepdou 2022-03-08
 */
@RunWith(MockitoJUnitRunner.class)
public class ConfigPropertiesFileTest {

    @Mock
    private ConfigFileRepo   configFileRepo;
    @Mock
    private ConfigFileConfig configFileConfig;

    @Test
    public void testGetPropertiesKey() {
        String content = "#some comment\nk1=10\nk2=false\nk3=5.6\nk4=1,2,3,4,5\nk5=T1\n"
                         + "k6={\"name\":\"zhangsan\",\"age\":18,\"labels\": {\"key1\":\"value1\"}}\n"
                         + "k7=[{\"name\":\"zhangsan\",\"age\":18,\"labels\": {\"key1\":\"value1\"}},{\"name\":\"lisi\",\"age\":20,\"labels\": {\"key1\":\"value2\"}}]";
        when(configFileRepo.getContent()).thenReturn(content);

        ConfigPropertiesFile configFile =
            new ConfigPropertiesFile(ConfigFileTestUtils.testNamespace, ConfigFileTestUtils.testGroup,
                                     ConfigFileTestUtils.testFileName, configFileRepo,
                                     configFileConfig);

        Assert.assertEquals(ConfigFileTestUtils.testNamespace, configFile.getNamespace());
        Assert.assertEquals(ConfigFileTestUtils.testGroup, configFile.getFileGroup());
        Assert.assertEquals(ConfigFileTestUtils.testFileName, configFile.getFileName());

        Assert.assertEquals(content, configFile.getContent());
        Assert.assertTrue(configFile.hasContent());

        Assert.assertEquals(new Integer(10), configFile.getIntProperty("k1", 8));
        Assert.assertEquals("false", configFile.getProperty("k2", "true"));
        Assert.assertFalse(configFile.getBooleanProperty("k2", true));
        Assert.assertEquals(new Float(5.6f), configFile.getFloatProperty("k3", 1.0f));
        Assert.assertEquals(new Double(5.6), configFile.getDoubleProperty("k3", 1.0));

        String[] arr = configFile.getArrayProperty("k4", ",", new String[]{});
        Assert.assertEquals(5, arr.length);
        for (int i = 1; i < 6; i++) {
            Assert.assertEquals(i + "", arr[i - 1]);
        }

        Assert.assertEquals(new Byte((byte) 10), configFile.getByteProperty("k1", (byte) 1));

        Assert.assertEquals(ConfigFileTestUtils.MyType.T1, configFile
            .getEnumProperty("k5", ConfigFileTestUtils.MyType.class, ConfigFileTestUtils.MyType.T2));

        ConfigFileTestUtils.User user = configFile.getJsonProperty("k6", ConfigFileTestUtils.User.class, null);
        Assert.assertNotNull(user);
        Assert.assertEquals("zhangsan", user.getName());
        Assert.assertEquals(18, user.getAge());
        Assert.assertEquals(user.getLabels().size(), 1);
        Assert.assertEquals(user.getLabels().get("key1"), "value1");

        List<ConfigFileTestUtils.User>
            users =
            configFile.getJsonProperty("k7", new TypeToken<List<ConfigFileTestUtils.User>>() {
            }.getType(), null);
        Assert.assertNotNull(users);
        Assert.assertEquals(2, users.size());
        for (ConfigFileTestUtils.User user2 : users) {
            Assert.assertNotNull(user2.getName());
            Assert.assertTrue(user2.getAge() > 0);
            Assert.assertTrue(user2.getLabels().size() > 0);
        }
    }

    @Test
    public void testListener() throws InterruptedException {
        String content = "#some comment\nk1=10\nk2=false\nk3=5.6\nk4=1,2,3,4,5\nk5=T1\n"
                         + "k6={\"name\":\"zhangsan\",\"age\":18,\"labels\": {\"key1\":\"value1\"}}\n"
                         + "k7=[{\"name\":\"zhangsan\",\"age\":18,\"labels\": {\"key1\":\"value1\"}},{\"name\":\"lisi\",\"age\":20,\"labels\": {\"key1\":\"value2\"}}]";
        when(configFileRepo.getContent()).thenReturn(content);

        ConfigPropertiesFile configFile =
            new ConfigPropertiesFile(ConfigFileTestUtils.testNamespace, ConfigFileTestUtils.testGroup,
                                     ConfigFileTestUtils.testFileName, configFileRepo,
                                     configFileConfig);

        AtomicBoolean k1Check = new AtomicBoolean(false);//modified
        AtomicBoolean k2Check = new AtomicBoolean(false);//delete
        AtomicBoolean k4Check = new AtomicBoolean(false);//not changed
        AtomicBoolean k8Check = new AtomicBoolean(false);//add
        AtomicBoolean k7Check = new AtomicBoolean(false);//modified
        configFile.addChangeListener(new ConfigKVFileChangeListener() {
            @Override
            public void onChange(ConfigKVFileChangeEvent event) {
                ConfigPropertyChangeInfo k1ChangeInfo = event.getChangeInfo("k1");
                k1Check.set(k1ChangeInfo.getNewValue().equals("1") &&
                            k1ChangeInfo.getOldValue().equals("10") &&
                            k1ChangeInfo.getChangeType() == ChangeType.MODIFIED);

                ConfigPropertyChangeInfo k2ChangeInfo = event.getChangeInfo("k2");
                k2Check.set(k2ChangeInfo.getNewValue() == null &&
                            k2ChangeInfo.getOldValue().equals("false") &&
                            k2ChangeInfo.getChangeType() == ChangeType.DELETED);

                ConfigPropertyChangeInfo k4ChangeInfo = event.getChangeInfo("k4");
                k4Check.set(k4ChangeInfo == null);

                ConfigPropertyChangeInfo k8ChangeInfo = event.getChangeInfo("k8");
                k8Check.set(k8ChangeInfo.getNewValue().equals("xx") &&
                            k8ChangeInfo.getOldValue() == null &&
                            k8ChangeInfo.getChangeType() == ChangeType.ADDED);

                ConfigPropertyChangeInfo k7ChangeInfo = event.getChangeInfo("k7");
                k7Check.set(k7ChangeInfo.getChangeType() == ChangeType.MODIFIED);
            }
        });


        String newContent = "#some comment\nk1=1\nk3=5.6\nk4=1,2,3,4,5\nk5=T1\nk8=xx\n"
                            + "k6={\"name\":\"zhangsan\",\"age\":18,\"labels\": {\"key1\":\"value1\"}}\n"
                            + "k7=[{\"name\":\"zhangsan\",\"age\":19,\"labels\": {\"key1\":\"value1\"}},{\"name\":\"lisi\",\"age\":20,\"labels\": {\"key1\":\"value2\"}}]";

        AtomicBoolean fileCheck = new AtomicBoolean(false);
        configFile.addChangeListener(new ConfigFileChangeListener() {
            @Override
            public void onChange(ConfigFileChangeEvent event) {
                fileCheck.set(event.getNewValue().equals(newContent) &&
                              event.getOldValue().equals(content) &&
                              event.getChangeType() == ChangeType.MODIFIED);
            }
        });

        configFile.onChange(ConfigFileTestUtils.assembleDefaultConfigFileMeta(), newContent);

        TimeUnit.MILLISECONDS.sleep(500);

        Assert.assertTrue(k1Check.get());
        Assert.assertTrue(k2Check.get());
        Assert.assertTrue(k4Check.get());
        Assert.assertTrue(k8Check.get());
        Assert.assertTrue(k7Check.get());
        Assert.assertTrue(fileCheck.get());

        //检查变更之后的值
        Assert.assertEquals(newContent, configFile.getContent());
        Assert.assertTrue(configFile.hasContent());

        Assert.assertEquals(new Integer(1), configFile.getIntProperty("k1", 8));
        Assert.assertEquals("true", configFile.getProperty("k2", "true"));
        Assert.assertTrue(configFile.getBooleanProperty("k2", true));
        Assert.assertEquals(new Float(5.6f), configFile.getFloatProperty("k3", 1.0f));
        Assert.assertEquals(new Double(5.6), configFile.getDoubleProperty("k3", 1.0));
        Assert.assertEquals("xx", configFile.getProperty("k8", ""));

        String[] arr = configFile.getArrayProperty("k4", ",", new String[]{});
        Assert.assertEquals(5, arr.length);
        for (int i = 1; i < 6; i++) {
            Assert.assertEquals(i + "", arr[i - 1]);
        }

        Assert.assertEquals(new Byte((byte) 1), configFile.getByteProperty("k1", (byte) 1));

        Assert.assertEquals(ConfigFileTestUtils.MyType.T1, configFile
            .getEnumProperty("k5", ConfigFileTestUtils.MyType.class, ConfigFileTestUtils.MyType.T2));

        ConfigFileTestUtils.User user = configFile.getJsonProperty("k6", ConfigFileTestUtils.User.class, null);
        Assert.assertNotNull(user);
        Assert.assertEquals("zhangsan", user.getName());
        Assert.assertEquals(18, user.getAge());
        Assert.assertEquals(user.getLabels().size(), 1);
        Assert.assertEquals(user.getLabels().get("key1"), "value1");

        List<ConfigFileTestUtils.User>
            users =
            configFile.getJsonProperty("k7", new TypeToken<List<ConfigFileTestUtils.User>>() {
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

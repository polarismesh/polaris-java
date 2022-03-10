package com.tencent.polaris.configuration.client.util;

import com.tencent.polaris.api.utils.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author lepdou 2022-03-02
 */
public class YamlParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(YamlParser.class);

    /**
     * Transform yaml content to properties
     */
    public Properties yamlToProperties(String yamlContent) {
        Yaml yaml = createYaml();
        final Properties result = new Properties();
        process((properties, map) -> result.putAll(properties), yaml, yamlContent);
        return result;
    }

    /**
     * Create the {@link Yaml} instance to use.
     */
    private Yaml createYaml() {
        LoaderOptions loadingConfig = new LoaderOptions();
        loadingConfig.setAllowDuplicateKeys(false);
        return new Yaml(new SafeConstructor(), new Representer(), new DumperOptions(), loadingConfig);
    }

    private boolean process(MatchCallback callback, Yaml yaml, String content) {
        int count = 0;
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("[Config] Loading from YAML: " + content);
        }
        for (Object object : yaml.loadAll(content)) {
            if (object != null && process(asMap(object), callback)) {
                count++;
            }
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("[Config] Loaded " + count + " document" + (count > 1 ? "s" : "") + " from YAML resource: " + content);
        }
        return (count > 0);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object object) {
        // YAML can have numbers as keys
        Map<String, Object> result = new LinkedHashMap<>();
        if (!(object instanceof Map)) {
            // A document can be a text literal
            result.put("document", object);
            return result;
        }

        Map<Object, Object> map = (Map<Object, Object>) object;
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map) {
                value = asMap(value);
            }
            Object key = entry.getKey();
            if (key instanceof CharSequence) {
                result.put(key.toString(), value);
            } else {
                // It has to be a map key in this case
                result.put("[" + key.toString() + "]", value);
            }
        }
        return result;
    }

    private boolean process(Map<String, Object> map, MatchCallback callback) {
        Properties properties = new Properties();
        properties.putAll(getFlattenedMap(map));

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("[Config] Merging document (no matchers set): " + map);
        }
        callback.process(properties, map);
        return true;
    }

    private Map<String, Object> getFlattenedMap(Map<String, Object> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        buildFlattenedMap(result, source, null);
        return result;
    }

    private void buildFlattenedMap(Map<String, Object> result, Map<String, Object> source, String path) {
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = entry.getKey();
            if (!StringUtils.isBlank(path)) {
                if (key.startsWith("[")) {
                    key = path + key;
                } else {
                    key = path + '.' + key;
                }
            }
            Object value = entry.getValue();
            if (value instanceof String) {
                result.put(key, value);
            } else if (value instanceof Map) {
                // Need a compound key
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) value;
                buildFlattenedMap(result, map, key);
            } else if (value instanceof Collection) {
                // Need a compound key
                @SuppressWarnings("unchecked")
                Collection<Object> collection = (Collection<Object>) value;
                int count = 0;
                for (Object object : collection) {
                    buildFlattenedMap(result, Collections.singletonMap("[" + (count++) + "]", object), key);
                }
            } else {
                result.put(key, (value != null ? value.toString() : ""));
            }
        }
    }

    private interface MatchCallback {

        void process(Properties properties, Map<String, Object> map);
    }

}

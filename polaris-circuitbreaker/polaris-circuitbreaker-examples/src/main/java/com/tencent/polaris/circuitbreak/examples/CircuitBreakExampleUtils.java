package com.tencent.polaris.circuitbreak.examples;

import com.tencent.polaris.api.utils.StringUtils;
import java.util.concurrent.TimeUnit;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Simple basic operation encapsulation.
 *
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
public class CircuitBreakExampleUtils {

    public static class InitResult {

        private final String namespace;
        private final String service;
        private final int instanceCount;
        private final String token;

        public InitResult(String namespace, String service, int instanceCount, String token) {
            this.namespace = namespace;
            this.service = service;
            this.instanceCount = instanceCount;
            this.token = token;
        }

        public String getNamespace() {
            return namespace;
        }

        public String getService() {
            return service;
        }

        public int getInstanceCount() {
            return instanceCount;
        }

        public String getToken() {
            return token;
        }
    }

    public static void doSleep(long mill) {
        try {
            TimeUnit.MILLISECONDS.sleep(mill);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 初始化配置对象
     *
     * @param args 命名行参数
     * @return 配置对象
     * @throws ParseException 解析异常
     */
    public static InitResult initConfiguration(String[] args) throws ParseException {
        CommandLineParser parser = new DefaultParser();
        Options options = new Options();
        options.addOption("namespace", "service namespace", true, "namespace for service");
        options.addOption("service", "service name", true, "service name");
        options.addOption("instCount", "instance count", true, "instance count");
        options.addOption("token", "token", true, "token");

        CommandLine commandLine = parser.parse(options, args);
        String namespace = commandLine.getOptionValue("namespace");
        String service = commandLine.getOptionValue("service");
        String token = commandLine.getOptionValue("token");
        String instCountStr = commandLine.getOptionValue("instCount");
        int instCount;
        if (StringUtils.isNotBlank(instCountStr)) {
            try {
                instCount = Integer.parseInt(instCountStr);
            } catch (NumberFormatException e) {
                e.printStackTrace();
                instCount = 1;
            }
        } else {
            instCount = 1;
        }
        if (StringUtils.isBlank(namespace) || StringUtils.isBlank(service)) {
            System.out.println("namespace or service is required");
            System.exit(1);
        }
        return new InitResult(namespace, service, instCount, token);
    }
}

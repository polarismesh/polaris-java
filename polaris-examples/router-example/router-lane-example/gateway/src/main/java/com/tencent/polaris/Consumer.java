package com.tencent.polaris;

import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.pojo.RouteArgument;
import com.tencent.polaris.api.pojo.SourceService;
import com.tencent.polaris.api.rpc.GetOneInstanceRequest;
import com.tencent.polaris.api.rpc.InstancesResponse;
import com.tencent.polaris.api.rpc.ServiceCallResult;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class Consumer {

    private static final String NAMESPACE_DEFAULT = "default";

    private static final String ECHO_SERVICE_NAME = "service-a";

    private static final String PATH = "/echo";

    public static String invokeByNameResolution(Set<RouteArgument> arguments, String echoValue,
                                                 ConsumerAPI consumerAPI) {
        String namespace = NAMESPACE_DEFAULT;
        String service = ECHO_SERVICE_NAME;

        System.out.println("namespace " + namespace + ", service " + service);
        // 1. we need to do naming resolution to get a load balanced host and port
        GetOneInstanceRequest getOneInstanceRequest = new GetOneInstanceRequest();
        getOneInstanceRequest.setNamespace(namespace);
        getOneInstanceRequest.setService(service);
        SourceService serviceInfo = new SourceService();
        serviceInfo.setArguments(arguments);
        getOneInstanceRequest.setServiceInfo(serviceInfo);
        InstancesResponse oneInstance = consumerAPI.getOneInstance(getOneInstanceRequest);
        Instance[] instances = oneInstance.getInstances();
        System.out.println("instances count is " + instances.length);
        Instance targetInstance = instances[0];
        System.out.printf("target instance is %s:%d%n", targetInstance.getHost(), targetInstance.getPort());

        // 2. invoke the server by the resolved address
        String urlStr = String
                .format("http://%s:%d%s?value=%s", targetInstance.getHost(), targetInstance.getPort(), PATH, echoValue);
        long startMillis = System.currentTimeMillis();
        HttpResult httpResult = httpGet(urlStr);
        long delay = System.currentTimeMillis() - startMillis;
        System.out.printf("invoke %s, code is %d, delay is %d%n", urlStr, httpResult.code, delay);

        // 3. report the invoke result to polaris-java, to eliminate the fail address
        RetStatus status = RetStatus.RetSuccess;
        if (httpResult.code != 200) {
            status = RetStatus.RetFail;
        }
        ServiceCallResult result = new ServiceCallResult();
        result.setNamespace(namespace);
        result.setService(service);
        result.setHost(targetInstance.getHost());
        result.setPort(targetInstance.getPort());
        result.setRetCode(httpResult.code);
        result.setDelay(delay);
        result.setRetStatus(status);
        consumerAPI.updateServiceCallResult(result);
        System.out.println("success to call updateServiceCallResult");
        return httpResult.message;
    }

    private static Map<String, String> splitQuery(URI uri) throws UnsupportedEncodingException {
        Map<String, String> query_pairs = new LinkedHashMap<>();
        String query = uri.getQuery();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"),
                    URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
        }
        return query_pairs;
    }

    private static class HttpResult {

        private final int code;

        private final String message;

        public HttpResult(int code, String message) {
            this.code = code;
            this.message = message;
        }
    }

    private static HttpResult httpGet(String urlStr) {
        HttpURLConnection connection = null;
        String respMessage;
        int code = -1;
        BufferedReader bufferedReader = null;
        try {
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            code = connection.getResponseCode();
            bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            respMessage = bufferedReader.readLine();

        } catch (IOException e) {
            e.printStackTrace();
            respMessage = e.getMessage();
        } finally {
            if (null != connection) {
                connection.disconnect();
            }
            if (null != bufferedReader) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return new HttpResult(code, respMessage);
    }


}

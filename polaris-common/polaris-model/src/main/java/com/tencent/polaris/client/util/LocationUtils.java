package com.tencent.polaris.client.util;

/**
 * @author lepdou 2022-05-23
 */
public class LocationUtils {

    private static final String ENV_POLARIS_REGION_KEY = "POLARIS_INSTANCE_REGION";
    private static final String ENV_POLARIS_ZONE_KEY = "POLARIS_INSTANCE_ZONE";
    private static final String ENV_POLARIS_CAMPUS_KEY = "POLARIS_INSTANCE_CAMPUS";

    private static final String region;
    private static final String zone;
    private static final String campus;

    static {
        region = System.getenv(ENV_POLARIS_REGION_KEY);
        zone = System.getenv(ENV_POLARIS_ZONE_KEY);
        campus = System.getenv(ENV_POLARIS_CAMPUS_KEY);
    }

    public static String getRegion() {
        return region;
    }

    public static String getZone() {
        return zone;
    }

    public static String getCampus() {
        return campus;
    }
}

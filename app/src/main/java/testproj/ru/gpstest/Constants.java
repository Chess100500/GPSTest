package testproj.ru.gpstest;


public final class Constants {

    private Constants() {
    }

    public static final String PACKAGE_NAME = "testproj.ru.gpstest";

    public static final String SHARED_PREFERENCES_NAME = PACKAGE_NAME + ".SHARED_PREFERENCES_NAME";

    public static final String GEOFENCES_ADDED_KEY = PACKAGE_NAME + ".GEOFENCES_ADDED_KEY";

    public static final long GEOFENCE_RADIUS_IN_METERS = 30;

    public static final String REQUESTING_LOCATION_UPDATES_KEY = "RequestingLocationUpdates";
    public static final String LOCATION_KEY = "CurrentLocation";
    public static final String LAST_UPDATED_TIME_STRING_KEY = "LastUpdateTime";

}
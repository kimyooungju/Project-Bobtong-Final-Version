package com.example.project_bobtong;

import com.naver.maps.geometry.LatLng;
import java.util.ArrayList;
import java.util.List;

public class PolyUtil {
    public static List<LatLng> decode(String encodedPath) {
        List<LatLng> path = new ArrayList<>();
        int index = 0;
        int len = encodedPath.length();
        int lat = 0;
        int lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encodedPath.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encodedPath.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            path.add(new LatLng(
                    lat / 1E5, lng / 1E5));
        }

        return path;
    }
}

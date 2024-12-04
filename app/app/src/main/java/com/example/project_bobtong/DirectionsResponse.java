package com.example.project_bobtong;

import java.util.List;

public class DirectionsResponse {
    public List<Route> routes;

    public static class Route {
        public List<Leg> legs;
        public OverviewPolyline overview_polyline;
    }

    public static class Leg {
        public List<Step> steps;
        public Distance distance;
        public Duration duration;
    }

    public static class Step {
        public String travel_mode;
        public TransitDetails transit_details;
        public Duration duration;
        public Location start_location;  // 추가된 필드

        public static class TransitDetails {
            public Line line;
            public Stop departure_stop;
            public Stop arrival_stop;

            public static class Line {
                public Vehicle vehicle;
                public String short_name;
            }

            public static class Vehicle {
                public String type;  // 버스, 지하철 등
            }

            public static class Stop {
                public String name;  // 정류장 이름
            }
        }

        // 새로운 Location 클래스 추가
        public static class Location {
            public double lat;  // 위도
            public double lng;  // 경도
        }
    }

    public static class OverviewPolyline {
        public String points;
    }

    public static class Distance {
        public String text;
        public int value;  // in meters
    }

    public static class Duration {
        public String text;
        public int value;  // in seconds
    }
}

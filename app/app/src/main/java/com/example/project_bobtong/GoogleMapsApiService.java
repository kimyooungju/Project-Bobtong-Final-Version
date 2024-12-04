package com.example.project_bobtong;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface GoogleMapsApiService {
    @GET("maps/api/directions/json")
    Call<DirectionsResponse> getWalkingDirections(
            @Query("origin") String origin,
            @Query("destination") String destination,
            @Query("mode") String mode,
            @Query("key") String apiKey
    );
}

package com.example.project_bobtong;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.NaverMapSdk;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.CircleOverlay;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.overlay.PathOverlay;
import com.naver.maps.map.util.FusedLocationSource;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String SEARCH_CLIENT_ID = BuildConfig.NAVER_SEARCH_CLIENT_ID;
    private static final String SEARCH_CLIENT_SECRET = BuildConfig.NAVER_SEARCH_CLIENT_SECRET;
    private static final String GOOGLE_MAPS_API_KEY = BuildConfig.GOOGLE_MAPS_API_KEY;

    private FusedLocationProviderClient mFusedLocationClient;
    private NaverMap mNaverMap;
    private FusedLocationSource mLocationSource;
    private Location mCurrentLocation;

    // Location 객체를 LatLng로 변환하는 메소드
    private LatLng convertToLatLng(DirectionsResponse.Step.Location location) {return new LatLng(location.lat, location.lng);}

    private RecyclerView mRecyclerView;
    private RestaurantAdapter mAdapter;
    private DatabaseReference mDatabase;
    private NaverApiService mNaverApiService;

    private boolean isFirstLoad = true;
    private EditText editTextQuery;
    private TextView getUserCurrentAddress;
    private Button buttonSearch;

    private SlidingUpPanelLayout slidingLayout;
    private List<Marker> searchMarkerList = new ArrayList<>(); // 검색된 마커 리스트
    private List<Marker> bookmarkMarkerList = new ArrayList<>(); // 북마크된 마커 리스트
    private List<Marker> transitMarkers = new ArrayList<>(); // 교통수단 변경 마커 리스트
    private PathOverlay pathOverlay;
    private CircleOverlay circleOverlay;
    boolean distancePlaceState = false; //새로고침 클릭시 상태 변화
    private String getUserAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // BuildConfig를 사용하여 네이버 맵 클라이언트 ID 설정
        NaverMapSdk.getInstance(this).setClient(new NaverMapSdk.NaverCloudPlatformClient(BuildConfig.NAVER_MAPS_CLIENT_ID));

        // Firebase 초기화
        FirebaseApp.initializeApp(this);

        // 위치 권한 요청
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);

        // RecyclerView 초기화
        mRecyclerView = findViewById(R.id.recyclerView);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new RestaurantAdapter();
        mRecyclerView.setAdapter(mAdapter);

        // Firebase Database 초기화
        mDatabase = FirebaseDatabase.getInstance().getReference("restaurants");

        // 네이버 검색 API 서비스 초기화
        Gson gson = new GsonBuilder().create();
        Retrofit retrofitSearch = new Retrofit.Builder()
                .baseUrl("https://openapi.naver.com/")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        mNaverApiService = retrofitSearch.create(NaverApiService.class);

        // FusedLocationProviderClient 초기화
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // 지도 초기화
        MapFragment mapFragment = (MapFragment) getSupportFragmentManager().findFragmentById(R.id.mapContainer);
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            getSupportFragmentManager().beginTransaction().add(R.id.mapContainer, mapFragment).commit();
        }
        mapFragment.getMapAsync(this);

        // 검색창 및 버튼 초기화
        editTextQuery = findViewById(R.id.editTextQuery);
        buttonSearch = findViewById(R.id.buttonSearch);

        // 사용자 위치 주소 표시
        getUserCurrentAddress = findViewById(R.id.getUserCurrentAddress);

        // FusedLocationSource 초기화
        mLocationSource = new FusedLocationSource(this, PERMISSION_REQUEST_CODE);

        // SlidingUpPanelLayout 초기화
        slidingLayout = findViewById(R.id.sliding_layout);

        // 검색 버튼 클릭 리스너 설정
        buttonSearch.setOnClickListener(v -> {
            String query = editTextQuery.getText().toString().trim();
            if (!query.isEmpty()) {
                // 검색 실행
                searchRestaurants(query);
            } else {
                Toast.makeText(MainActivity.this, "검색어 입력", Toast.LENGTH_SHORT).show();
            }
        });

        // BottomNavigationView 초기화 및 클릭 리스너 설정
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_category) {
                //showCategoryDialog();
                showCategoryDialogWithSpinner();
                return true;
            } else if (itemId == R.id.action_bookmark) {
                startActivity(new Intent(this, BookmarkActivity.class));
                return true;
            } else if (itemId == R.id.action_mypage) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {
                    startActivity(new Intent(this, MyPageActivity.class));
                } else {
                    startActivity(new Intent(this, LoginActivity.class));
                }
                return true;
            }
            return false;
        });

        Button refreshButton = (Button)findViewById(R.id.buttonRefresh);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int id;
                id = v.getId();
                if( id == R.id.buttonRefresh){ // refresh 버튼 클릭시 수행
                    mNaverMap.setLocationSource(mLocationSource);
                    mNaverMap.setLocationTrackingMode(LocationTrackingMode.Follow);
                    if(bookmarkMarkerList != null) { //검색마커 리스트 초기화
                        clearSearchMarkers();
                        circleOverlay.setRadius(0);
                    }

                    if (pathOverlay != null) { //경로 초기화
                        pathOverlay.setMap(null);
                    }

                    getUserLocationBasedMarker();
                    showCurrentLocationCircle();
                    getUserCurrentPlace(mCurrentLocation);
                }
            }
        });
    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        mNaverMap = naverMap;

        // 위치 소스 및 추적 모드 설정
        naverMap.setLocationSource(mLocationSource);
        naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);

        // 현재 위치 표시
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            mCurrentLocation = location;
                            //현재 위치의 사용자 주소를 이용해서 주변 1km 내에 랜덤 식당정보를 생성
                            if(distancePlaceState == false){
                                getUserLocationBasedMarker();
                                showCurrentLocationCircle();

                                getUserCurrentPlace(mCurrentLocation); //사용자의 위치 주소를 가져와 출력

                                distancePlaceState = true;
                            }
                        }
                    });
        }

        // Firebase에서 데이터를 불러와 북마크된 마커 추가
        addBookmarkMarkers();

    }

    private void getUserCurrentPlace(Location CurrentLocation){ //현재 사용자 위치 주소 출력
        String getUserAddress = getAddressFromLocation(CurrentLocation); //사용자의 위치주소를 시각으로 표현하기 위한 위치 주소 호출
        String[] addressParts = getUserAddress.split(" "); // 국가 주소 제거후 문자열 결합
        String addressWithoutFirst = String.join(" ", Arrays.copyOfRange(addressParts, 1, addressParts.length));

        String[] userAddressSplit = addressWithoutFirst.split(" ");
        String userAddress = userAddressSplit[0] + " " + userAddressSplit[1] + " " + userAddressSplit[2] + " " + userAddressSplit[3];
        getUserCurrentAddress.setText(userAddress);
    }

    private void clearSearchMarkers() {
        for (Marker marker : searchMarkerList) {
            marker.setMap(null);
        }
        searchMarkerList.clear();

        // 이동수단 마커 제거
        for (Marker marker : transitMarkers) {
            marker.setMap(null);
        }
        transitMarkers.clear();
    }

    private void SearchUserPlaceRestaurant(String query){ // 앱 처음 실행시, 새로고침 수행시 자동실행
        Log.d("userPlaceRestaurantSearch 전달받은 쿼리", query);
        Call<SearchResponse> call = mNaverApiService.searchRestaurants(query, 10, 1, "random", SEARCH_CLIENT_ID, SEARCH_CLIENT_SECRET);
        call.enqueue(new Callback<SearchResponse>() {
            @Override
            public void onResponse(@NonNull Call<SearchResponse> call, @NonNull Response<SearchResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d("API Response", response.body().toString());
                    List<Restaurant> restaurants = response.body().getItems();

                    for (Restaurant restaurant : restaurants) {
                        String name = restaurant.getTitle();
                        if (name != null) {
                            name = name.replaceAll("<b>", "").replaceAll("</b>", "");
                            restaurant.setTitle(name);
                        } else {
                            restaurant.setTitle("Unknown Restaurant");
                        }

                        // TM128 좌표를 사용하여 위도와 경도로 변환하지 않고 그대로 저장
                        restaurant.setMapx(restaurant.getMapx());
                        restaurant.setMapy(restaurant.getMapy());

                        LatLng userLatLng = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
                        double latitude = restaurant.getLatitude() / 10.0;
                        double longitude = restaurant.getLongitude() / 10.0;
                        LatLng restaurantLatLng = new LatLng(latitude, longitude);;
                        Log.d("사용자, 식당 위도 경도", "사용자" + mCurrentLocation.getLatitude() +"/"+  mCurrentLocation.getLongitude() + "식당" + restaurant.getMapy()+"/" + restaurant.getMapx());

                        if(calculateDistance(userLatLng, restaurantLatLng) <= 1) { //1km 내에서만 db 저장 후 마커생성
                            // Firebase에 데이터 저장
                            String key = mDatabase.push().getKey();
                            if (key != null) {
                                restaurant.setId(key); // ID 설정
                                mDatabase.child(key).setValue(restaurant).addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        Log.d("Firebase", "Restaurant saved: " + restaurant.getTitle());
                                    } else {
                                        Log.e("Firebase", "Failed to save restaurant", task.getException());
                                    }
                                });
                            }

                            // 지도에 마커 추가
                            addMarkerForRestaurant(restaurant, false); // 검색된 마커는 북마크 아님
                            Log.d("지도에 추가된 타이틀, 주소", restaurant.getTitle() + " " + restaurant.getAddress());
                        }
                    }

                } else {
                    Log.e("API Error", response.errorBody().toString());
                    Toast.makeText(MainActivity.this, "검색에 실패했습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<SearchResponse> call, @NonNull Throwable t) {
                Log.e("API Error", "Error fetching data", t);
                Toast.makeText(MainActivity.this, "검색중 에러가 발생했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void searchRestaurants(String query) {
        Call<SearchResponse> call = mNaverApiService.searchRestaurants(query, 10, 1, "random", SEARCH_CLIENT_ID, SEARCH_CLIENT_SECRET);
        call.enqueue(new Callback<SearchResponse>() {
            @Override
            public void onResponse(@NonNull Call<SearchResponse> call, @NonNull Response<SearchResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d("API Response", response.body().toString());
                    List<Restaurant> restaurants = response.body().getItems();

                    for (Restaurant restaurant : restaurants) {
                        String name = restaurant.getTitle();
                        if (name != null) {
                            name = name.replaceAll("<b>", "").replaceAll("</b>", "");
                            restaurant.setTitle(name);
                        } else {
                            restaurant.setTitle("Unknown Restaurant");
                        }

                        // TM128 좌표를 사용하여 위도와 경도로 변환하지 않고 그대로 저장
                        restaurant.setMapx(restaurant.getMapx());
                        restaurant.setMapy(restaurant.getMapy());

                        // Firebase에 데이터 저장
                        String key = mDatabase.push().getKey();
                        if (key != null) {
                            restaurant.setId(key); // ID 설정
                            mDatabase.child(key).setValue(restaurant).addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Log.d("Firebase", "Restaurant saved: " + restaurant.getTitle());
                                } else {
                                    Log.e("Firebase", "Failed to save restaurant", task.getException());
                                }
                            });
                        }

                        // 지도에 마커 추가
                        addMarkerForRestaurant(restaurant, false); // 검색된 마커는 북마크 아님
                    }

                    // RecyclerView에 검색 결과 표시
                    mAdapter.setRestaurants(restaurants);
                    slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
                } else {
                    Log.e("API Error", response.errorBody().toString());
                    Toast.makeText(MainActivity.this, "검색에 실패했습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<SearchResponse> call, @NonNull Throwable t) {
                Log.e("API Error", "Error fetching data", t);
                Toast.makeText(MainActivity.this, "검색중 에러가 발생했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showCategoryDialogWithSpinner() {  // 카테고리 검색
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.category_dialog_spinner, null);
        // Spinner를 레이아웃에서 참조
        Spinner categoryRestaurantSpinner = dialogView.findViewById(R.id.categoryRestaurantSpinner);
        Spinner categoryDistancesSpinner = dialogView.findViewById(R.id.categoryDistancesSpinner);
        CheckBox markerCleanCheckBox = dialogView.findViewById(R.id.markerCleanCheckBox);
        Button positiveButton = dialogView.findViewById(R.id.category_positive_button);
        Button negativeButton = dialogView.findViewById(R.id.category_negative_button);
        // AlertDialog.Builder 생성
        AlertDialog builder = new AlertDialog.Builder(this).create();
        builder.setView(dialogView); // 커스텀 레이아웃을 다이얼로그에 설정
        positiveButton.setOnClickListener(v -> {
            String getAddress = getAddressFromLocation(mCurrentLocation); // 사용자 위치 주소 반환
            // Spinner에서 선택된 항목을 가져옴
            String selectedRestaurantCategory = categoryRestaurantSpinner.getSelectedItem().toString();
            String selectedDistancesCategory = categoryDistancesSpinner.getSelectedItem().toString();
            String[] addressParts = getAddress.split(" "); // 국가 주소 제거후 문자열 결합
            String addressWithoutFirst = String.join(" ", Arrays.copyOfRange(addressParts, 1, addressParts.length));
            String[] filteredParts = addressWithoutFirst.split(" ");
            String filterAddress = "";
            Log.d("selectedRestaurantCategory", selectedRestaurantCategory + "필터된값:" + Arrays.toString(filteredParts));
            if(selectedDistancesCategory.equals("특별시/도")){
                if (filteredParts.length >= 1 && (filteredParts[0].endsWith("도") ||
                        filteredParts[0].endsWith("특별시"))) {
                    filterAddress = filteredParts[0];
                    Log.d("Filter1", "Filtered Address (filterAddress): " + filterAddress);
                }else{
                    Log.d("Filter1 Error","Invalid address:" + Arrays.toString(filteredParts));
                }
            }else if(selectedDistancesCategory.equals("시/구/군")){
                if (filteredParts.length >= 2 && filteredParts[1].endsWith("시") || filteredParts[2].endsWith("구")) {
                    filterAddress = filteredParts[0] + " " + filteredParts[1] + " " + filteredParts[2];
                    Log.d("Filter2", "Filtered Address (filterAddress): " + filterAddress);
                }else if(filteredParts.length >= 2 && (filteredParts[1].endsWith("시") ||
                        filteredParts[1].endsWith("구") || filteredParts[1].endsWith("군"))){
                    filterAddress = filteredParts[0] + " " + filteredParts[1];
                    Log.d("Filter2", "Filtered Address (filterAddress): " + filterAddress);
                }else{
                    Log.d("Filter2 Error","Invalid address:" + Arrays.toString(filteredParts));
                }
            }else if(selectedDistancesCategory.equals("동/읍/면")){
                if (filteredParts.length >= 3 && (filteredParts[2].endsWith("동") || filteredParts[2].endsWith("읍") || filteredParts[2].endsWith("면") || filteredParts[2].endsWith("로") || filteredParts[2].endsWith("길"))) {
                    filterAddress = filteredParts[0] + " " + filteredParts[1] + " " + filteredParts[2];
                    Log.d("Filter3", "Filtered Address (filterAddress): " + filterAddress);
                   } else if (filteredParts.length >= 3 && (filteredParts[3].endsWith("동") || filteredParts[3].endsWith("읍") || filteredParts[3].endsWith("면") || filteredParts[3].endsWith("로") || filteredParts[3].endsWith("길"))) {
                    filterAddress = filteredParts[0] + " " + filteredParts[1] + " " + filteredParts[2] + " " + filteredParts[3];
                    Log.d("Filter3", "Filtered Address (Filter 3): " + filterAddress);
                    }else{
                    Log.d("Filter3 Error","Invalid address:" + Arrays.toString(filteredParts));
                }
            }
            String categoryQuery = selectedRestaurantCategory + " " + filterAddress;
            Log.d("categore",categoryQuery);
            if (markerCleanCheckBox.isChecked()) {
                clearSearchMarkers();
                Log.d("MarkerClean", "기존 마커가 제거되었습니다.");
            }
            searchRestaurants(categoryQuery);
        });
        negativeButton.setOnClickListener(v -> builder.dismiss());
        builder.show();
    }

    private String getAddressFromLocation(Location location) { //현재 위치 주소 반환
        Geocoder geocoder = new Geocoder(this,  Locale.KOREAN); //지역을 한글로 반환
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String addressText = address.getAddressLine(0);
                Log.d("AddressFromLocation", "Current Address: " + addressText);
                return addressText;
            } else {
                Log.d("AddressFromLocation", "No address found");
                return "주소를 찾을 수 없습니다";
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Geocoder service is not available", Toast.LENGTH_SHORT).show();
            return "Geocoder 서비스 에러";
        }
    }

    public void getUserLocationBasedMarker(){ //사용자 위치 주변 식당 정보 표시
        List<String> get200mLatLngAddressList = generateLatLngWithin1Km(mCurrentLocation); //사용자 위치 주소 기반 1km 이내 200m마다 필터된 주소 가져오기
        List<String> restaurantList = Arrays.asList("한식","중식","일식","양식","분식","치킨","피자","국수","디저트","카페","햄버거","생선회","구이","육류","뷔페");
        List<String> addRestaurant = new ArrayList<>();

        Random choiceRestaurant = new Random();

        for(String LatLngQuery : get200mLatLngAddressList) {
            while(true){
                String recommendationRestaurant = restaurantList.get(choiceRestaurant.nextInt(restaurantList.size()));
                if(!addRestaurant.contains(recommendationRestaurant)) {
                    String userPlaceQuery = recommendationRestaurant + " " + LatLngQuery;
                    SearchUserPlaceRestaurant(userPlaceQuery); // 드롭다운 제거한 검색
                    addRestaurant.add(recommendationRestaurant);
                    Log.d("getUserLocationBasedMarker 추가 성공!", userPlaceQuery);
                    break;
                }else if(addRestaurant.size() == restaurantList.size()){
                    Log.d("getUserLocationBasedMarker 카테고리 가득참", "더 이상 추가 불가");
                }else{
                    Log.d("getUserLocationBasedMarker 이미 사용된 카테고리: ", recommendationRestaurant);
                    continue;
                }
            }
        }
    }
    public static double calculateDistance(LatLng point1, LatLng point2) {// 거리 계산 함수 (하버사인 공식 사용)
        double earthRadius = 6371.0; // 지구 반지름 (단위: km)
        double dLat = Math.toRadians(point2.latitude - point1.latitude);
        double dLng = Math.toRadians(point2.longitude - point1.longitude);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(point1.latitude)) * Math.cos(Math.toRadians(point2.latitude))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c; // 두 점 사이의 거리 반환 (단위: km)
    }


    public List<String> generateLatLngWithin1Km(Location userLocation) { // 위도와 경도를 기준으로 200m 간격으로 1km 반경 내의 LatLng 좌표 생성
        List<String> generatedAddresses = new ArrayList<>();
        LatLng userLatLng = new LatLng(userLocation.getLatitude(), userLocation.getLongitude());

        // 100m당 위도 및 경도 차이 계산 (1도 = 약 111.32km)
        double latDiff = 200 / 111320.0; // 200m당 위도 차이
        double lngDiff = 200 / (111320.0 * Math.cos(Math.toRadians(userLatLng.latitude))); // 200m당 경도 차이

        // 동서남북 방향으로 200m 간격으로 좌표를 생성
        for (double lat = -1; lat <= 1; lat += 0.2) {
            for (double lng = -1; lng <= 1; lng += 0.2) {
                double newLat = userLatLng.latitude + lat * latDiff;
                double newLng = userLatLng.longitude + lng * lngDiff;

                LatLng newPoint = new LatLng(newLat, newLng);
                double distance = calculateDistance(userLatLng, newPoint);
                // 1km 반경 내 좌표만 추가
                if (distance <= 1.0) {
                    // LatLng를 Location으로 변환
                    Location location = new Location(""); // Provider는 ""로 설정
                    location.setLatitude(newPoint.latitude);
                    location.setLongitude(newPoint.longitude);

                    // 주소를 얻고 지역 필터 후 리스트에 추가
                    String address = getAddressFromLocation(location);
                    String[] addressParts = address.split(" "); // 국가 주소 제거후 문자열 결합
                    String addressWithoutFirst = String.join(" ", Arrays.copyOfRange(addressParts, 1, addressParts.length));
                    String[] filteredParts = addressWithoutFirst.split(" ");
                    String filterAddress = "";
                    Log.d("generateLatLngWithin1Km 현제주소필터", "적용된 200m 지점 주소필터 =" + Arrays.toString(filteredParts));
                    if (filteredParts.length >= 3 && (filteredParts[2].endsWith("동") || filteredParts[2].endsWith("읍") || filteredParts[2].endsWith("면"))) {
                        filterAddress = filteredParts[0] + " " + filteredParts[1] + " " + filteredParts[2];
                        if(!generatedAddresses.contains(filterAddress)) { // 중복 제거
                            generatedAddresses.add(filterAddress);
                            Log.d("AddressFilterList", "Filtered Address (AddressFilterList): " + filterAddress);
                        }
                    } else if (filteredParts.length >= 4 && (filteredParts[3].endsWith("동") || filteredParts[3].endsWith("읍") || filteredParts[3].endsWith("면"))) {
                        filterAddress = filteredParts[0] + " " + filteredParts[1] + " " + filteredParts[2] + " " + filteredParts[3];
                        if(!generatedAddresses.contains(filterAddress)) {
                            generatedAddresses.add(filterAddress);
                            Log.d("AddressFilterList", "Filtered Address (AddressFilterList): " + filterAddress);
                        }
                    } else {
                        Log.d("AddressFilter Error", "Invalid address:" + Arrays.toString(filteredParts));
                    }
                }
            }
        }
        return generatedAddresses;
    }

    private void filterRestaurantsByCategory(String restaurantCategory, String distancesCategory) {
        // Firebase에서 해당 카테고리에 속하는 음식점을 불러와 지도에 마커로 표시
        mDatabase.orderByChild("category").equalTo(restaurantCategory).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //clearSearchMarkers(); // 기존 마커 제거
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Restaurant restaurant = dataSnapshot.getValue(Restaurant.class);
                    if (restaurant != null) {
                        if(restaurant.getAddress().equals(distancesCategory)) { // 해당되는 주소만 필터
                            addMarkerForRestaurant(restaurant, false); // 검색된 마커는 북마크 아님
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Failed to load restaurants", error.toException());
            }
        });
    }

    private void addMarkerForRestaurant(Restaurant restaurant, boolean isBookmark) {
        double latitude = restaurant.getLatitude() / 10.0;
        double longitude = restaurant.getLongitude() / 10.0;
        LatLng latLng = new LatLng(latitude, longitude);

        // 이미지 리소스를 Bitmap으로 변환
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), com.naver.maps.map.R.drawable.navermap_default_marker_icon_yellow, options);

        //Canvas를 사용하여 Bitmap에 그리기
        Canvas canvas = new Canvas(bitmap);

        Drawable backgroundMarkerImage = getResources().getDrawable(R.drawable.marker_image_background);
        backgroundMarkerImage.setBounds(10,10,100,100);
        backgroundMarkerImage.draw(canvas);

        Marker marker = new Marker();
        marker.setPosition(latLng);
        // 카테고리에 따라 마커 이미지 설정
        String category = restaurant.getCategory().replace(">"," ").replace(","," "); // 음식점의 카테고리 가져오기
        Log.d("검색된 카테고리: ", category);

        String acceptSetIcon = "default"; // 음식아이콘 한번 저장시(true) 분할된 카테고리 반복문 탈출

        if (category != null) {
            String[] categories = category.split(" ");
            for(String divisionCategory : categories) {
                if (acceptSetIcon == "default" || acceptSetIcon == "기본아이콘") {
                    switch (divisionCategory) {
                        case "한식":
                            Drawable koreanFoodMarker = getResources().getDrawable(R.drawable.ic_korean_food); // 한식 마커 이미지
                            koreanFoodMarker.setBounds(20, 20, 90, 90);
                            koreanFoodMarker.draw(canvas);
                            acceptSetIcon = "음식아이콘";
                            break;
                        case "중식":
                            Drawable chineseFoodMarker = getResources().getDrawable(R.drawable.ic_chinese_food); // 중식 마커 이미지
                            chineseFoodMarker.setBounds(20, 20, 90, 90);
                            chineseFoodMarker.draw(canvas);
                            acceptSetIcon = "음식아이콘";
                            break;
                        case "일식":
                            Drawable japaneseFoodMarker = getResources().getDrawable(R.drawable.ic_japanese_food); //일식 마커 이미지
                            japaneseFoodMarker.setBounds(20, 20, 90, 90);
                            japaneseFoodMarker.draw(canvas);
                            acceptSetIcon = "음식아이콘";
                            break;
                        case "양식":
                            Drawable westernFoodMarker = getResources().getDrawable(R.drawable.ic_western_food); //양식 마커 이미지
                            westernFoodMarker.setBounds(20, 20, 90, 90);
                            westernFoodMarker.draw(canvas);
                            acceptSetIcon = "음식아이콘";
                            break;
                        case "분식":
                            Drawable snackMarker = getResources().getDrawable(R.drawable.ic_snack); //분식 마커 이미지
                            snackMarker.setBounds(20, 20, 90, 90);
                            snackMarker.draw(canvas);
                            acceptSetIcon = "음식아이콘";
                            break;
                        case "치킨":
                            Drawable chickenMarker = getResources().getDrawable(R.drawable.ic_chicken); //치킨 마커 이미지
                            chickenMarker.setBounds(20, 20, 90, 90);
                            chickenMarker.draw(canvas);
                            acceptSetIcon = "음식아이콘";
                            break;
                        case "피자":
                            //marker.setIcon(OverlayImage.fromResource(R.drawable.ic_pizza)); // 피자 마커 이미지
                            Drawable pizzaMarker = getResources().getDrawable(R.drawable.ic_pizza);
                            pizzaMarker.setBounds(20, 20, 90, 90);
                            pizzaMarker.draw(canvas);
                            acceptSetIcon = "음식아이콘";
                            break;
                        case "국수":
                            Drawable noodleMarker = getResources().getDrawable(R.drawable.ic_noodle); //국수 마커 이미지
                            noodleMarker.setBounds(20, 20, 90, 90);
                            noodleMarker.draw(canvas);
                            acceptSetIcon = "음식아이콘";
                            break;
                        case "디저트":
                            Drawable dessertMarker = getResources().getDrawable(R.drawable.ic_dessert); // 디저트 마커 이미지
                            dessertMarker.setBounds(20, 20, 90, 90);
                            dessertMarker.draw(canvas);
                            acceptSetIcon = "음식아이콘";
                            break;
                        case "카페":
                            Drawable cafeMarker = getResources().getDrawable(R.drawable.ic_cafe); // 카페 마커 이미지
                            cafeMarker.setBounds(20, 20, 90, 90);
                            cafeMarker.draw(canvas);
                            acceptSetIcon = "음식아이콘";
                            break;
                        case "햄버거":
                            Drawable burgerMarker = getResources().getDrawable(R.drawable.ic_burger); // 버거 마커 이미지
                            burgerMarker.setBounds(20, 20, 90, 90);
                            burgerMarker.draw(canvas);
                            acceptSetIcon = "음식아이콘";
                            break;
                        case "돼지고기구이":
                            Drawable pigGrillMarker = getResources().getDrawable(R.drawable.ic_grill); // 돼지고기구이 마커 이미지 //'구이' 로만 있는 카테고리 분류가 없음
                            pigGrillMarker.setBounds(20, 20, 90, 90);
                            pigGrillMarker.draw(canvas);
                            acceptSetIcon = "음식아이콘";
                            break;
                        case "소고기구이":
                            Drawable cowGrillMarker = getResources().getDrawable(R.drawable.ic_grill); // 소고기구이 마커 이미지
                            cowGrillMarker.setBounds(20, 20, 90, 90);
                            cowGrillMarker.draw(canvas);
                            acceptSetIcon = "음식아이콘";
                            break;
                        case "육류":
                            Drawable meatMarker = getResources().getDrawable(R.drawable.ic_meat); // 고기 마커 이미지
                            meatMarker.setBounds(20, 20, 90, 90);
                            meatMarker.draw(canvas);
                            acceptSetIcon = "음식아이콘";
                            break;
                        default:
                            acceptSetIcon = "기본아이콘";
                    }
                }
            }
            if(acceptSetIcon == "기본아이콘"){
                Drawable defaultFoodMarker = getResources().getDrawable(R.drawable.ic_default_food); // 그외 음식 마커 이미지
                defaultFoodMarker.setBounds(20, 20, 90, 90);
                defaultFoodMarker.draw(canvas);
            }
        } else {
            Drawable defaultMarker = getResources().getDrawable(R.drawable.ic_default); // 기본 마커 이미지
            defaultMarker.setBounds(20, 20, 90, 90);
            defaultMarker.draw(canvas);
        }
        marker.setIcon(OverlayImage.fromBitmap(bitmap));

        marker.setMap(mNaverMap);

        if (isBookmark) {
            bookmarkMarkerList.add(marker);
        } else {
            searchMarkerList.add(marker);
        }

        marker.setOnClickListener(overlay -> {
            showRestaurantInfo(restaurant);
            moveCameraToMarker(marker);
            showDistanceAndPathToMarker(marker);
            return true;
        });
    }

    private void addBookmarkMarkers() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        DatabaseReference bookmarkRef = FirebaseDatabase.getInstance().getReference("bookmarks").child(user.getUid());
        bookmarkRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Restaurant restaurant = dataSnapshot.getValue(Restaurant.class);
                    if (restaurant != null) {
                        addMarkerForRestaurant(restaurant, true); // 북마크된 마커 추가
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Failed to load bookmarks", error.toException());
            }
        });
    }

    private void showRestaurantInfo(Restaurant restaurant) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.restaurant_info, null);
        builder.setView(dialogView);

        TextView foodTitle = dialogView.findViewById(R.id.getFoodTitle);
        TextView foodAddress = dialogView.findViewById(R.id.getFoodAddress);
        TextView foodCategory = dialogView.findViewById(R.id.getFoodCategory);
        Button buttonBookmark = dialogView.findViewById(R.id.buttonBookmark);
        Button buttonViewReviews = dialogView.findViewById(R.id.buttonViewReviews);

        foodTitle.setText(restaurant.getTitle());
        foodAddress.setText(restaurant.getAddress());
        foodCategory.setText(restaurant.getCategory());

        AlertDialog dialog = builder.create();

        buttonBookmark.setOnClickListener(v -> {
            // 북마크 추가/삭제 기능 구현
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                DatabaseReference bookmarkRef = FirebaseDatabase.getInstance().getReference("bookmarks").child(user.getUid()).child(restaurant.getId());
                bookmarkRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            bookmarkRef.removeValue();
                            Toast.makeText(MainActivity.this, "북마크가 제거되었습니다.", Toast.LENGTH_SHORT).show();
                            removeBookmarkMarker(restaurant.getId()); // 북마크 삭제 시 마커 제거
                        } else {
                            bookmarkRef.setValue(restaurant);
                            Toast.makeText(MainActivity.this, "북마크가 추가되었습니다.", Toast.LENGTH_SHORT).show();
                            addMarkerForRestaurant(restaurant, true); // 북마크 추가 시 마커 추가
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("Firebase", "Failed to update bookmark", error.toException());
                    }
                });
            } else {
                Toast.makeText(MainActivity.this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            }
        });

        buttonViewReviews.setOnClickListener(v -> {
            // 리뷰 보기 기능 구현
            Intent intent = new Intent(MainActivity.this, RestaurantReviewsActivity.class);
            intent.putExtra("restaurantId", restaurant.getId());
            startActivity(intent);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void removeBookmarkMarker(String restaurantId) {
        for (Marker marker : bookmarkMarkerList) {
            if (marker.getTag() != null && marker.getTag().equals(restaurantId)) {
                marker.setMap(null);
                bookmarkMarkerList.remove(marker);
                break;
            }
        }
    }

    private void moveCameraToMarker(Marker marker) {
        CameraUpdate cameraUpdate = CameraUpdate.scrollTo(marker.getPosition());
        mNaverMap.moveCamera(cameraUpdate);
    }

    private void showDistanceAndPathToMarker(Marker marker) {
        if (mCurrentLocation == null) return;

        LatLng markerPosition = marker.getPosition();
        String origin = mCurrentLocation.getLatitude() + "," + mCurrentLocation.getLongitude();
        String destination = markerPosition.latitude + "," + markerPosition.longitude;

        Log.d("API Request", "origin: " + origin + ", destination: " + destination);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://maps.googleapis.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        GoogleMapsApiService service = retrofit.create(GoogleMapsApiService.class);
        Call<DirectionsResponse> call = service.getWalkingDirections(origin, destination, "transit", GOOGLE_MAPS_API_KEY);

        call.enqueue(new Callback<DirectionsResponse>() {
            @Override
            public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    DirectionsResponse directionsResponse = response.body();
                    Log.d("API Response", new Gson().toJson(directionsResponse));

                    if (directionsResponse.routes != null && !directionsResponse.routes.isEmpty()) {
                        DirectionsResponse.Route route = directionsResponse.routes.get(0);
                        DirectionsResponse.Leg leg = route.legs.get(0);
                        List<DirectionsResponse.Step> steps = leg.steps;

                        StringBuilder transitInfo = new StringBuilder();

                        for (DirectionsResponse.Step step : steps) {
                            String travelMode = step.travel_mode;

                            if ("TRANSIT".equals(travelMode)) {
                                DirectionsResponse.Step.TransitDetails transitDetails = step.transit_details;
                                String vehicleType = transitDetails.line.vehicle.type;
                                String lineName = transitDetails.line.short_name;
                                String departureStop = transitDetails.departure_stop.name;
                                String arrivalStop = transitDetails.arrival_stop.name;
                                String duration = step.duration.text;

                                transitInfo.append(String.format("이동수단: %s (%s)\n출발: %s\n도착: %s\n소요시간: %s\n\n",
                                        vehicleType, lineName, departureStop, arrivalStop, duration));
                            } else if ("WALKING".equals(travelMode)) {
                                String walkDuration = step.duration.text;
                                transitInfo.append(String.format("도보이동: %s\n\n", walkDuration));
                            }
                        }

                        // 경로를 지도에 표시하기
                        List<LatLng> path = PolyUtil.decode(route.overview_polyline.points);

                        // 기존 오버레이 제거
                        if (pathOverlay != null) {
                            pathOverlay.setMap(null);
                        }

                        pathOverlay = new PathOverlay();
                        pathOverlay.setCoords(path);
                        pathOverlay.setWidth(15); // 경로 선 두께 조절
                        pathOverlay.setColor(Color.parseColor("#00FFFF"));
                        pathOverlay.setMap(mNaverMap);

                        // 교통수단 변경 지점 마커 추가
                        for (DirectionsResponse.Step step : steps) {
                            String travelMode = step.travel_mode.toLowerCase();
                            LatLng changeLatLng = convertToLatLng(step.start_location);  // Location을 LatLng로 변환

                            Marker transportMarker = new Marker();
                            transportMarker.setPosition(changeLatLng);

                            // 이동 수단에 따라 아이콘 설정
                            switch (travelMode.toLowerCase()) {
                                case "walking":
                                    transportMarker.setIcon(OverlayImage.fromResource(R.drawable.ic_walk));
                                    break;
                                case "transit":
                                    if (step.transit_details != null) {
                                        String vehicleType = step.transit_details.line.vehicle.type.toLowerCase();
                                        if (vehicleType.contains("subway")) {
                                            transportMarker.setIcon(OverlayImage.fromResource(R.drawable.ic_subway));
                                        } else if (vehicleType.contains("bus")) {
                                            transportMarker.setIcon(OverlayImage.fromResource(R.drawable.ic_bus));
                                        }
                                    }
                                    break;
                            }

                            // 이동수단 마커 설정 및 반응형 사이즈 조절
                            transportMarker.setMap(mNaverMap);
                            transitMarkers.add(transportMarker); // 마커 리스트에 추가

                            // 이동 수단 마커를 지도에 추가하는 코드 내 수정 부분
                            mNaverMap.addOnCameraChangeListener((reason, animated) -> {
                                double zoom = mNaverMap.getCameraPosition().zoom;

                                // 새로운 아이콘 크기 조절 공식 - 축소 상태에서 크기를 키우고 확대 시 너무 작아지지 않도록 조정
                                int size = (int) (60 * Math.pow(0.9, (15 - zoom) / 2.0)); // 확대 시 더 크게, 축소 시 적당히 보이도록 설정

                                // 아이콘 크기 설정
                                transportMarker.setWidth(Math.max(size, 150));  // 최소 크기를 40으로 설정하여 너무 작아지지 않게
                                transportMarker.setHeight(Math.max(size, 150));
                            });

                            transportMarker.setMap(mNaverMap);
                            transitMarkers.add(transportMarker); // 마커 리스트에 추가
                        }

                        // 전체 소요 시간과 거리 정보
                        double totalDistance = leg.distance.value / 1000.0; // km로 변환
                        double totalDuration = leg.duration.value / 3600.0; // 시간으로 변환

                        // 거리와 소요 시간 출력 (소요 시간 2시간 이상일 경우, 단순 경고)
                        if (totalDuration > 2) {
                            Toast.makeText(MainActivity.this, "너무멀어요!", Toast.LENGTH_SHORT).show();
                        } else {
                            String distanceText = totalDistance < 1 ? String.format("%dm", (int) (totalDistance * 1000))
                                    : String.format("%.1fkm", totalDistance);
                            showTransitInfoDialog(transitInfo.toString(), distanceText, leg.duration.text);
                        }

                    } else {
                        Log.e("API Error", "No routes found in response");
                        Toast.makeText(MainActivity.this, "경로를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e("API Error", "Response Code: " + response.code() + ", Message: " + response.message());
                    Toast.makeText(MainActivity.this, "경로를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                Log.e("API Failure", t.getMessage(), t);
                Toast.makeText(MainActivity.this, "경로탐색 실패" + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    // 경로 안내 정보를 다이얼로그로 표시하는 메소드
    private void showTransitInfoDialog(String transitInfo, String distanceText, String durationText) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("경로정보");
        builder.setMessage("거리: " + distanceText + "\n소요시간: " + durationText + "\n\n" + transitInfo);
        builder.setPositiveButton("확인", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showCurrentLocationCircle() {
        if (circleOverlay != null) {
            circleOverlay.setMap(null);
        }

        LatLng currentLatLng = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());

        circleOverlay = new CircleOverlay();
        circleOverlay.setCenter(currentLatLng);
        circleOverlay.setRadius(1000);
        circleOverlay.setColor(Color.parseColor("#220000FF")); // 반투명한 파란색
        circleOverlay.setMap(mNaverMap);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (mLocationSource.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
            if (!mLocationSource.isActivated()) { // 권한 거부됨
                mNaverMap.setLocationTrackingMode(LocationTrackingMode.None);
            }
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}

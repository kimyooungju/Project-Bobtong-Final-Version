package com.example.project_bobtong;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RestaurantReviewsActivity extends AppCompatActivity {

    private ListView listView;
    private TextView noReviewsText;
    private Button buttonWriteReview;
    private List<Review> reviewList;
    private ReviewAdapter adapter;
    private DatabaseReference reviewsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant_reviews);

        listView = findViewById(R.id.listView);
        noReviewsText = findViewById(R.id.no_reviews_text);
        buttonWriteReview = findViewById(R.id.buttonSubmitReview);
        reviewList = new ArrayList<>();
        adapter = new ReviewAdapter(this, reviewList);
        listView.setAdapter(adapter);

        Button backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        String restaurantId = getIntent().getStringExtra("restaurantId");
        if (restaurantId != null) {
            Log.d("FirebaseData", "Restaurant ID: " + restaurantId); // 경로 확인을 위해 로그 추가
            reviewsRef = FirebaseDatabase.getInstance().getReference("restaurant_reviews").child(restaurantId);
            loadReviews();

            buttonWriteReview.setOnClickListener(v -> {
                Intent intent = new Intent(RestaurantReviewsActivity.this, WriteReviewActivity.class);
                intent.putExtra("restaurantId", restaurantId);
                startActivity(intent);
            });
        } else {
            Log.e("FirebaseData", "restaurantId is null");
            noReviewsText.setText("음식점 정보가 없습니다.");
            noReviewsText.setVisibility(View.VISIBLE);
        }
    }

    private void loadReviews() {
        reviewsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                reviewList.clear();
                Log.d("FirebaseData", "DataSnapshot exists: " + snapshot.exists()); // 존재 여부 로그 추가
                Log.d("FirebaseData", "DataSnapshot contents: " + snapshot.toString()); // DataSnapshot 내용 로그 추가

                if (snapshot.exists()) {
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        Review review = dataSnapshot.getValue(Review.class);
                        if (review != null) {
                            reviewList.add(review); // 리뷰 리스트에 추가
                        }
                    }

                    // 최신순으로 정렬 (타임스탬프를 기준으로 내림차순 정렬)
                    Collections.sort(reviewList, (r1, r2) -> Long.compare(r2.getTimestamp(), r1.getTimestamp()));

                    adapter.notifyDataSetChanged(); // 어댑터 업데이트
                }

                noReviewsText.setVisibility(reviewList.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseData", "리뷰 불러오기 실패: " + error.getMessage());
                noReviewsText.setText("리뷰 불러오기를 실패했습니다.");
                noReviewsText.setVisibility(View.VISIBLE);
            }
        });
    }
}
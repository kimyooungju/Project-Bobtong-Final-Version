package com.example.project_bobtong;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MyReviewsActivity extends AppCompatActivity {

    private ListView listView;
    private TextView noReviewsText;
    private List<Review> reviewList; // Review 객체 리스트
    private ReviewAdapter adapter;
    private DatabaseReference reviewsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_reviews);

        listView = findViewById(R.id.listView);
        noReviewsText = findViewById(R.id.no_reviews_text);
        reviewList = new ArrayList<>();
        adapter = new ReviewAdapter(this, reviewList); // ReviewAdapter 사용
        listView.setAdapter(adapter);

        Button backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // restaurant_reviews 경로에서 사용자가 작성한 모든 리뷰 불러오기
            reviewsRef = FirebaseDatabase.getInstance().getReference("restaurant_reviews");
            loadAllUserReviews(user.getUid());
        } else {
            noReviewsText.setText("로그인이 필요합니다.");
            noReviewsText.setVisibility(View.VISIBLE);
        }
    }

    private void loadAllUserReviews(String userId) {
        reviewsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                reviewList.clear();
                for (DataSnapshot restaurantSnapshot : snapshot.getChildren()) {
                    for (DataSnapshot reviewSnapshot : restaurantSnapshot.getChildren()) {
                        Review review = reviewSnapshot.getValue(Review.class);
                        if (review != null && userId.equals(review.getUserId())) {
                            reviewList.add(review);
                        }
                    }
                }

                // 작성 시간이 오래된 순서대로 정렬
                Collections.sort(reviewList, new Comparator<Review>() {
                    @Override
                    public int compare(Review r1, Review r2) {
                        return Long.compare(r2.getTimestamp(), r1.getTimestamp());
                    }
                });

                adapter.notifyDataSetChanged();
                noReviewsText.setVisibility(reviewList.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                noReviewsText.setText("리뷰 불러오기를 실패했습니다.");
                noReviewsText.setVisibility(View.VISIBLE);
            }
        });
    }
}
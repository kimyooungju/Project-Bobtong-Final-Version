package com.example.project_bobtong;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MyPageActivity extends AppCompatActivity {

    private Button buttonLogout;
    private Button buttonMainPage;
    private Button buttonMyReviews;
    private TextView userEmailText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mypage);

        buttonLogout = findViewById(R.id.buttonLogout);
        buttonMainPage = findViewById(R.id.buttonMainPage);
        buttonMyReviews = findViewById(R.id.buttonMyReviews);
        userEmailText = findViewById(R.id.userEmailText);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            // 로그인이 되어있지 않으면 LoginActivity로 이동
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        userEmailText.setText(user.getEmail());

        buttonLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        buttonMainPage.setOnClickListener(v -> {
            // 메인 페이지로 이동
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        buttonMyReviews.setOnClickListener(v -> {
            // 내가 작성한 리뷰 페이지로 이동
            startActivity(new Intent(this, MyReviewsActivity.class));
        });
    }
}

package com.example.project_bobtong;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class RestaurantInfoDialog extends Dialog {

    private Restaurant restaurant;
    private Context context;

    public RestaurantInfoDialog(@NonNull Context context, Restaurant restaurant) {
        super(context);
        this.context = context;
        this.restaurant = restaurant;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.restaurant_info);

        TextView foodTitle = findViewById(R.id.getFoodTitle);
        TextView foodAddress = findViewById(R.id.getFoodAddress);
        TextView foodCategory = findViewById(R.id.getFoodCategory);
        Button buttonBookmark = findViewById(R.id.buttonBookmark);
        Button buttonViewReviews = findViewById(R.id.buttonViewReviews);

        foodTitle.setText(restaurant.getTitle());
        foodAddress.setText(restaurant.getAddress());
        foodCategory.setText(restaurant.getCategory());

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
                            Toast.makeText(context, "북마크가 제거되었습니다.", Toast.LENGTH_SHORT).show();
                        } else {
                            bookmarkRef.setValue(restaurant);
                            Toast.makeText(context, "북마크가 추가되었습니다.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(context, "북마크의 제거/추가에 실패했습니다.", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(context, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            }
        });

        buttonViewReviews.setOnClickListener(v -> {
            // 리뷰 보기 기능 구현
            Intent intent = new Intent(context, RestaurantReviewsActivity.class);
            intent.putExtra("restaurantId", restaurant.getId());
            context.startActivity(intent);
            dismiss();
        });
    }
}

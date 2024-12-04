package com.example.project_bobtong;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.List;

public class BookmarkActivity extends AppCompatActivity {

    private ListView listView;
    private TextView noBookmarksText;
    private List<Restaurant> bookmarkList;
    private ArrayAdapter<String> adapter;
    private DatabaseReference bookmarkRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookmark);

        listView = findViewById(R.id.listViewBookmarks);
        noBookmarksText = findViewById(R.id.textViewNoBookmarks);
        bookmarkList = new ArrayList<>();
        List<String> bookmarkTitles = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, bookmarkTitles);
        listView.setAdapter(adapter);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            bookmarkRef = FirebaseDatabase.getInstance().getReference("bookmarks").child(user.getUid());
            loadBookmarks(bookmarkTitles);
        } else {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            finish();
        }

        // 북마크 클릭 시 음식점으로 지도 이동
        listView.setOnItemClickListener((parent, view, position, id) -> {
            Restaurant selectedRestaurant = bookmarkList.get(position);
            if (selectedRestaurant != null) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("restaurantId", selectedRestaurant.getId());
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                Toast.makeText(BookmarkActivity.this, "음식점을 불러 올 수 없습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        // 메인 페이지로 돌아가기 버튼 추가
        Button backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());
    }

    private void loadBookmarks(List<String> bookmarkTitles) {
        bookmarkRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                bookmarkList.clear();
                bookmarkTitles.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        Restaurant restaurant = dataSnapshot.getValue(Restaurant.class);
                        if (restaurant != null) {
                            bookmarkList.add(restaurant);
                            bookmarkTitles.add(restaurant.getTitle());
                        }
                    }
                    adapter.notifyDataSetChanged();
                }

                if (bookmarkList.isEmpty()) {
                    noBookmarksText.setVisibility(View.VISIBLE);
                } else {
                    noBookmarksText.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                noBookmarksText.setText("북마크 불러오기에 실패했습니다.");
                noBookmarksText.setVisibility(View.VISIBLE);
            }
        });
    }
}
package com.example.project_bobtong;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

public class CategoryActivity extends AppCompatActivity {
    private Spinner categorySpinner;
    private Spinner distanceSpinner;
    private Button searchButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        categorySpinner = findViewById(R.id.categorySpinner);
        distanceSpinner = findViewById(R.id.distanceSpinner);
        searchButton = findViewById(R.id.searchButton);

        searchButton.setOnClickListener(v -> {
            String category = categorySpinner.getSelectedItem().toString();
            String distance = distanceSpinner.getSelectedItem().toString();

            Intent resultIntent = new Intent();
            resultIntent.putExtra("category", category);
            resultIntent.putExtra("distance", distance);
            setResult(RESULT_OK, resultIntent);
            finish();
        });

        // 메인 페이지로 돌아가기 버튼 추가
        Button backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());
    }
}

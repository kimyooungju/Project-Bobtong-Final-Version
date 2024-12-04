package com.example.project_bobtong;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.UUID;

public class WriteReviewActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PERMISSION_REQUEST_CODE = 100;

    private EditText editTextReview;
    private Button buttonSubmitReview, buttonChooseImage;
    private ImageView imageViewPreview;
    private Uri imageUri;

    private DatabaseReference mDatabase;
    private StorageReference mStorageRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_review);

        // Firebase 초기화
        mDatabase = FirebaseDatabase.getInstance().getReference("restaurant_reviews");
        mStorageRef = FirebaseStorage.getInstance().getReference("review_images");

        // UI 요소 연결
        editTextReview = findViewById(R.id.editTextReview);
        buttonSubmitReview = findViewById(R.id.buttonSubmitReview);
        buttonChooseImage = findViewById(R.id.buttonChooseImage);
        imageViewPreview = findViewById(R.id.imageViewPreview);

        String restaurantId = getIntent().getStringExtra("restaurantId");

        // 이미지 선택 버튼 클릭 리스너
        buttonChooseImage.setOnClickListener(v -> {
            if (checkPermissions()) {
                openFileChooser();
            }
        });

        // 리뷰 제출 버튼 클릭 리스너
        buttonSubmitReview.setOnClickListener(v -> {
            String reviewText = editTextReview.getText().toString().trim();
            if (!reviewText.isEmpty()) {
                if (imageUri != null) {
                    uploadImageAndSubmitReview(restaurantId, reviewText);
                } else {
                    submitReview(restaurantId, reviewText, null); // 이미지 없이 리뷰 제출
                }
            } else {
                Toast.makeText(WriteReviewActivity.this, "리뷰를 입력해주세요", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 파일 선택을 위한 메소드
    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    // 이미지 선택 결과 처리
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            imageViewPreview.setImageURI(imageUri); // 이미지 미리보기
        }
    }

    // 이미지 업로드 및 리뷰 제출
    private void uploadImageAndSubmitReview(String restaurantId, String reviewText) {
        if (imageUri == null) {
            Toast.makeText(this, "이미지를 선택해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = UUID.randomUUID().toString(); // 파일 이름 UUID로 생성
        StorageReference fileRef = mStorageRef.child(fileName);

        // 이미지 업로드
        fileRef.putFile(imageUri).addOnSuccessListener(taskSnapshot -> {
            fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String imageUrl = uri.toString();
                submitReview(restaurantId, reviewText, imageUrl); // 이미지 URL과 함께 리뷰 제출
                // 이미지 업로드 후에도 선택한 이미지를 계속 프리뷰로 보여줌
                imageViewPreview.setImageURI(imageUri);  // 업로드 후에도 프리뷰 유지
            });
        }).addOnFailureListener(e -> {
            Log.e("FirebaseImageUpload", "Image upload failed: " + e.getMessage());
            Toast.makeText(WriteReviewActivity.this, "이미지 업로드에 실패했습니다.", Toast.LENGTH_SHORT).show();
        });
    }

    // 리뷰 제출
    private void submitReview(String restaurantId, String reviewText, @Nullable String imageUrl) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            String userName = user.getDisplayName();
            long currentTime = System.currentTimeMillis();

            // 리뷰 객체 생성
            Review review = new Review(userId, restaurantId, reviewText, userName, imageUrl, currentTime);
            String key = mDatabase.child(restaurantId).push().getKey(); // Firebase에 저장할 키 생성
            if (key != null) {
                mDatabase.child(restaurantId).child(key).setValue(review).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("WriteReviewActivity", "Review submitted: " + reviewText); // 로그 추가
                        Toast.makeText(WriteReviewActivity.this, "리뷰가 제출되었습니다.", Toast.LENGTH_SHORT).show();
                        finish(); // 작성 완료 후 액티비티 종료
                    } else {
                        Toast.makeText(WriteReviewActivity.this, "리뷰 제출에 실패했습니다.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } else {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // API 33 이상에서는 READ_MEDIA_IMAGES 권한 필요
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, PERMISSION_REQUEST_CODE);
                return false;
            }
        } else {
            // API 33 미만에서는 READ_EXTERNAL_STORAGE 및 WRITE_EXTERNAL_STORAGE 필요
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
                return false;
            }
        }
        // 모든 권한이 허용된 경우 true 반환
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한이 허용된 경우 파일 선택 창을 열기
                openFileChooser();
            } else {
                // 권한이 거부된 경우 사용자에게 알림
                Toast.makeText(this, "사진 첨부를 위해 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
                // 권한 설정 화면으로 유도 (필요시)
                Intent intent = new Intent();
                intent.setAction(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            }
        }
    }
}
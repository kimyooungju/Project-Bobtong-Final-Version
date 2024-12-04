package com.example.project_bobtong;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReviewAdapter extends ArrayAdapter<Review> {

    public ReviewAdapter(@NonNull Context context, @NonNull List<Review> objects) {
        super(context, 0, objects);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_review, parent, false);
        }

        Review review = getItem(position);

        TextView textViewUserId = convertView.findViewById(R.id.textViewUserId);
        TextView textViewReviewText = convertView.findViewById(R.id.textViewReviewText);
        ImageView imageViewReview = convertView.findViewById(R.id.imageViewReview);
        TextView textViewTimestamp = convertView.findViewById(R.id.textViewTimestamp); // 타임스탬프를 위한 텍스트뷰

        if (review != null) {
            textViewUserId.setText(review.getUserName()); // 닉네임만 보여줌
            textViewReviewText.setText(review.getReviewText());

            // 이미지 URL이 있을 경우, 이미지를 로드하고 없으면 ImageView를 숨김
            if (review.getImageUrl() != null) {
                imageViewReview.setVisibility(View.VISIBLE);
                Glide.with(getContext())
                        .load(review.getImageUrl())
                        .into(imageViewReview);
            } else {
                imageViewReview.setVisibility(View.GONE);  // 이미지가 없을 경우 숨김
            }

            // 작성일시 추가
            long timestamp = review.getTimestamp();
            String formattedDate;
            if (System.currentTimeMillis() - timestamp < 86400000) { // 24시간 이내
                long hoursAgo = (System.currentTimeMillis() - timestamp) / 3600000;
                formattedDate = hoursAgo + "시간 전";
            } else {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault());
                formattedDate = sdf.format(new Date(timestamp));
            }
            textViewTimestamp.setText(formattedDate); // 작성일시 설정
        }

        return convertView;
    }
}
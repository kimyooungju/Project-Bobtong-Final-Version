package com.example.project_bobtong;

public class Review {
    private String userId;
    private String restaurantId;
    private String reviewText;
    private String userName;
    private String imageUrl; // 이미지 URL을 위한 필드 추가
    private long timestamp;

    // 기본 생성자 (Firebase에서 필요로 함)
    public Review() {
    }

    // 생성자 수정: 이미지가 없을 경우를 위해 imageUrl을 null 허용
    public Review(String userId, String restaurantId, String reviewText, String userName, String imageUrl, long timestamp) {
        this.userId = userId;
        this.restaurantId = restaurantId;
        this.reviewText = reviewText;
        this.userName = userName;
        this.imageUrl = imageUrl; // 이미지 URL을 설정
        this.timestamp = timestamp;
    }

    // Getter 및 Setter 메소드
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(String restaurantId) {
        this.restaurantId = restaurantId;
    }

    public String getReviewText() {
        return reviewText;
    }

    public void setReviewText(String reviewText) {
        this.reviewText = reviewText;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
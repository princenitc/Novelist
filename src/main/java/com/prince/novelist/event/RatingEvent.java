package com.prince.novelist.event;

public class RatingEvent {
    
    private String eventId;
    private String userId;
    private String bookId;
    private int rating;
    private String review;
    private long timestamp;

    public RatingEvent() {
    }

    public RatingEvent(String eventId, String userId, String bookId, int rating, String review, long timestamp) {
        this.eventId = eventId;
        this.userId = userId;
        this.bookId = bookId;
        this.rating = rating;
        this.review = review;
        this.timestamp = timestamp;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getReview() {
        return review;
    }

    public void setReview(String review) {
        this.review = review;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}

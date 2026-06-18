package com.prince.novelist.event;

import com.prince.novelist.dto.response.BookResponse;

public class BookEvent {
    
    public enum Action {
        CREATED, UPDATED, DELETED
    }

    private String eventId;
    private Action action;
    private BookResponse book;
    private long timestamp;

    public BookEvent() {
    }

    public BookEvent(String eventId, Action action, BookResponse book, long timestamp) {
        this.eventId = eventId;
        this.action = action;
        this.book = book;
        this.timestamp = timestamp;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public BookResponse getBook() {
        return book;
    }

    public void setBook(BookResponse book) {
        this.book = book;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}

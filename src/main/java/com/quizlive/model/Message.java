package com.quizlive.model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Represents a direct communication message between a Participant and a Quiz Creator.
 */
public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private int fromUserId;
    private String fromUserName;
    private int toUserId;
    private String toUserName;
    private Integer quizId;
    private String quizTitle;
    private String content;
    private Timestamp sentAt;

    public Message() {
    }

    public Message(int id, int fromUserId, int toUserId, Integer quizId, String content, Timestamp sentAt) {
        this.id = id;
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.quizId = quizId;
        this.content = content;
        this.sentAt = sentAt;
    }

    // ==========================================
    // Getters and Setters
    // ==========================================

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getFromUserId() {
        return fromUserId;
    }

    public void setFromUserId(int fromUserId) {
        this.fromUserId = fromUserId;
    }

    public String getFromUserName() {
        return fromUserName;
    }

    public void setFromUserName(String fromUserName) {
        this.fromUserName = fromUserName;
    }

    public int getToUserId() {
        return toUserId;
    }

    public void setToUserId(int toUserId) {
        this.toUserId = toUserId;
    }

    public String getToUserName() {
        return toUserName;
    }

    public void setToUserName(String toUserName) {
        this.toUserName = toUserName;
    }

    public Integer getQuizId() {
        return quizId;
    }

    public void setQuizId(Integer quizId) {
        this.quizId = quizId;
    }

    public String getQuizTitle() {
        return quizTitle;
    }

    public void setQuizTitle(String quizTitle) {
        this.quizTitle = quizTitle;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Timestamp getSentAt() {
        return sentAt;
    }

    public void setSentAt(Timestamp sentAt) {
        this.sentAt = sentAt;
    }

    @Override
    public String toString() {
        return "Message{" +
                "id=" + id +
                ", fromUserId=" + fromUserId +
                ", toUserId=" + toUserId +
                ", quizId=" + quizId +
                ", content='" + content + '\'' +
                ", sentAt=" + sentAt +
                '}';
    }
}

package com.invest.domain.entities;

import java.time.LocalDateTime;

public class Alert {

    private final Long id;
    private final Long ruleId;
    private final Long groupId;
    private String status;
    private LocalDateTime sentAt;

    public Alert(Long id, Long ruleId, Long groupId, String status, LocalDateTime sentAt) {
        this.id = id;
        this.ruleId = ruleId;
        this.groupId = groupId;
        this.status = status;
        this.sentAt = sentAt;
    }

    public void markAsSent() {
        this.status = "SENT";
        this.sentAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getRuleId() {
        return ruleId;
    }

    public Long getGroupId() {
        return groupId;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }
}

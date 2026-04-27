package com.invest.domain.entities;

public class Rule {

    private final Long id;
    private boolean active;

    public Rule(Long id, boolean active) {
        this.id = id;
        this.active = active;
    }

    public void deactivate() {
        this.active = false;
    }

    public Long getId() {
        return id;
    }

    public boolean isActive() {
        return active;
    }
}

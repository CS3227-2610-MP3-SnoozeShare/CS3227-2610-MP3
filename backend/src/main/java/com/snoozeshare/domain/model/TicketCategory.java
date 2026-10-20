package com.snoozeshare.domain.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A category that tickets can be filed under. */
@Entity
@Table(name = "ticket_categories")
public class TicketCategory {

    @Id
    @Column(name = "category_id")
    private UUID categoryId;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false)
    private boolean active;

    /** JPA requires a no-arg constructor. */
    protected TicketCategory() {}

    /** Creates a new ticket category. */
    public TicketCategory(UUID categoryId, String label,
                          boolean active) {
        this.categoryId = categoryId;
        this.label = label;
        this.active = active;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}

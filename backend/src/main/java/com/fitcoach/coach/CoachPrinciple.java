package com.fitcoach.coach;

import com.fitcoach.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "coach_principles")
public class CoachPrinciple extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "key", unique = true, nullable = false)
    private String key;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "category", nullable = false)
    private String category;

    protected CoachPrinciple() {}

    public UUID getId() { return id; }
    public String getKey() { return key; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getCategory() { return category; }
}

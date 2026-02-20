package com.hiddenloner.backend.domain.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "recommendation_votes")
public class RecommendationVote {

    @EmbeddedId
    private RecommendationVoteId id;

    @Column(name = "vote_type", nullable = false)
    private short voteType;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public RecommendationVoteId getId() {
        return id;
    }

    public void setId(RecommendationVoteId id) {
        this.id = id;
    }

    public short getVoteType() {
        return voteType;
    }

    public void setVoteType(short voteType) {
        this.voteType = voteType;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

package com.hiddenloner.backend.domain.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class PostLikeId implements Serializable {
    private UUID postId;
    private UUID userId;

    public PostLikeId() {
    }

    public PostLikeId(UUID postId, UUID userId) {
        this.postId = postId;
        this.userId = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PostLikeId that = (PostLikeId) o;
        return Objects.equals(postId, that.postId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(postId, userId);
    }
}

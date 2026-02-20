package com.hiddenloner.backend.recommendation.dto;

import java.util.UUID;

public record VoteRecommendationResponse(
    UUID postId,
    String voteType,
    long upvoteCount,
    long downvoteCount,
    long score
) {
}

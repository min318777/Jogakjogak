package com.zb.jogakjogak.jobDescription.repository;

public record JdStatsDto(int postedJdCount, int applyJdCount, int allCompletedPieces,
                          int allTotalPieces, int perfectJdCount) {
}

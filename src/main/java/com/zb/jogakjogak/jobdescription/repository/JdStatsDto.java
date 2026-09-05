package com.zb.jogakjogak.jobdescription.repository;

public record JdStatsDto(int postedJdCount, int applyJdCount, int allCompletedPieces,
                          int allTotalPieces, int perfectJdCount) {
}

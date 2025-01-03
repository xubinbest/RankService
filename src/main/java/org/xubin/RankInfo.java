package org.xubin;

public class RankInfo {
    public String playerId;
    public int score;
    public int rank;

    public RankInfo(String playerId, int score, int rank) {
        this.playerId = playerId;
        this.score = score;
        this.rank = rank;
    }

    @Override
    public String toString() {
        return "RankInfo{" +
                "playerId='" + playerId + '\'' +
                ", score=" + score +
                ", rank=" + rank +
                '}';
    }
}

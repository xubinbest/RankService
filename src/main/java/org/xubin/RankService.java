package org.xubin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class RankService {
    private static volatile RankService instance;

    private final ConcurrentSkipListMap<Integer, Set<String>> rankMap;
    private final ConcurrentHashMap<String, Integer> playerScoreMap;

    public RankService() {
        rankMap = new ConcurrentSkipListMap<>(Collections.reverseOrder());
        playerScoreMap = new ConcurrentHashMap<>();
    }

    public static RankService instance() {
        if (instance == null) {
            synchronized (RankService.class) {
                if (instance == null) {
                    instance = new RankService();
                }
            }
        }
        return instance;
    }

    /**
     * 更新玩家分数
     * @param playerId
     * @param score
     */
    public void updateScore(String playerId, int score) {
        if (playerId == null || score < 0) {
            throw new IllegalArgumentException("Invalid playerId or score");
        }

        Integer oldScore = playerScoreMap.get(playerId);

        // 如果分数未变化，直接返回
        if (oldScore != null && oldScore == score) {
            return;
        }

        // 如果玩家有旧分数，移除旧分数对应的记录
        if (oldScore != null) {
            removePlayerFromRankMap(playerId, oldScore);
        }

        // 更新玩家分数
        playerScoreMap.put(playerId, score);

        // 添加到新分数对应的集合
        rankMap.computeIfAbsent(score, k -> Collections.synchronizedSet(new LinkedHashSet<>())).add(playerId);
    }

    /**
     * 获取玩家排名
     * @param playerId
     * @return RankInfo
     */
    public RankInfo getPlayerRank(String playerId) {
        String threadName = Thread.currentThread().getName();
        if (!playerScoreMap.containsKey(playerId)) {
            return new RankInfo(playerId, 0, -1);
        }
        int score = playerScoreMap.get(playerId);
        int rank = 1;
        for (Map.Entry<Integer, Set<String>> entry : rankMap.entrySet()) {
            if(entry.getKey() > score) {
                rank += entry.getValue().size();
            } else if (entry.getKey() == score) {
                for(String id : entry.getValue()) {
                    if (id.equals(playerId)) {
                        break;
                    }
                    rank++;
                }
                break;
            } else {
                break;
            }
        }
        return new RankInfo(playerId, score, rank);
    }

    /**
     * 获取玩家排名扩展 相同分数玩家排名并列
     * @param playerId
     * @return RankInfo
     */
    public RankInfo getPlayerRankExt(String playerId) {
        if (!playerScoreMap.containsKey(playerId)) {
            return new RankInfo(playerId, 0, -1);
        }
        int score = playerScoreMap.get(playerId);
        int rank = 1;
        for (Map.Entry<Integer, Set<String>> entry : rankMap.entrySet()) {
            if(entry.getKey() > score) {
                rank++;
            } else {
                break;
            }
        }
        return new RankInfo(playerId, score, rank);
    }

    /**
     * 获取前N名
     * @param n
     * @return List<RankInfo>
     */
    public List<RankInfo> getTopN(int n) {
        List<RankInfo> topN = new ArrayList<>();
        if(n <= 0) {
            return topN;
        }
        int count = 0;
        for (Map.Entry<Integer, Set<String>> entry : rankMap.entrySet()) {
            for (String playerId : entry.getValue()) {
                RankInfo rankInfo = new RankInfo(playerId, entry.getKey(), ++count);
                topN.add(rankInfo);
                if (count >= n) {
                    return topN;
                }
            }
        }

        return topN;
    }

    /**
     * 获取玩家排名范围
     * @param playerId
     * @param range
     * @return List<RankInfo>
     */
    public List<RankInfo> getPlayerRankRange(String playerId, int range) {
        List<RankInfo> rankRange = new ArrayList<>();
        if (!playerScoreMap.containsKey(playerId)) {
            return rankRange;
        }

        // 获取玩家当前分数和排名
        RankInfo selfRankInfo = getPlayerRank(playerId);
        int selfRank = selfRankInfo.rank;

        // 计算目标排名范围
        int startRank = Math.max(1, selfRank - range);
        int endRank = selfRank + range;

        // 快速跳转到目标范围
        int currentRank = 1;
        for (Map.Entry<Integer, Set<String>> entry : rankMap.entrySet()) {
            int score = entry.getKey();
            Set<String> players = entry.getValue();

            // 如果当前排名超出范围，停止遍历
            if (currentRank > endRank) {
                break;
            }

            if(currentRank + players.size() < startRank) {
                currentRank += players.size();
                continue;
            }


            // 遍历当前分数段的玩家
            for (String id : players) {
                if (currentRank >= startRank && currentRank <= endRank) {
                    rankRange.add(new RankInfo(id, score, currentRank));
                }
                currentRank++;

                // 如果达到目标范围的结束排名，停止遍历
                if (currentRank > endRank) {
                    break;
                }
            }
        }

        return rankRange;
    }

    private void removePlayerFromRankMap(String playerId, int score) {
        rankMap.computeIfPresent(score, (key, set) -> {
            set.remove(playerId);
            return set.isEmpty() ? null : set;
        });
    }
}

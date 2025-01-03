package org.xubin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class RankService {
    private static volatile RankService instance;

    private ConcurrentSkipListMap<Integer, LinkedHashSet<String>> rankMap;
    private ConcurrentHashMap<String, Integer> playerScoreMap;

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
        if (playerScoreMap.containsKey(playerId)) {
            int oldScore = playerScoreMap.get(playerId);
            // 分数不变不处理
            if (oldScore == score) {
                return;
            }
            // 删除旧分数集合中的数据
            Set<String> rankPlayerSet = rankMap.get(oldScore);
            rankPlayerSet.remove(playerId);
            if (rankPlayerSet.isEmpty()) {
                rankMap.remove(oldScore);
            }
        }

        // 更新新分数
        playerScoreMap.put(playerId, score);
        // 将数据加入到新分数集合中
        Set<String> playerSet = rankMap.computeIfAbsent(score, k -> new LinkedHashSet<>());
        playerSet.add(playerId);
    }

    /**
     * 获取玩家排名
     * @param playerId
     * @return RankInfo
     */
    public RankInfo getPlayerRank(String playerId) {
        RankInfo rankInfo = new RankInfo(playerId, 0, -1);
        if (!playerScoreMap.containsKey(playerId)) {
            rankInfo.score = 0;
            rankInfo.rank = -1;
            return rankInfo;
        }
        int score = playerScoreMap.get(playerId);
        rankInfo.score = score;
        int rank = 1;
        for (Map.Entry<Integer, LinkedHashSet<String>> entry : rankMap.entrySet()) {
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
        rankInfo.rank = rank;
        return rankInfo;
    }

    /**
     * 获取玩家排名扩展 相同分数玩家排名并列
     * @param playerId
     * @return RankInfo
     */
    public RankInfo getPlayerRankExt(String playerId) {
        RankInfo rankInfo;
        if (!playerScoreMap.containsKey(playerId)) {
            return new RankInfo(playerId, 0, -1);
        }
        int score = playerScoreMap.get(playerId);
        int rank = 1;
        for (Map.Entry<Integer, LinkedHashSet<String>> entry : rankMap.entrySet()) {
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
        int count = 0;
        for (Map.Entry<Integer, LinkedHashSet<String>> entry : rankMap.entrySet()) {
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

        int score = playerScoreMap.get(playerId);
        int rank = 1;

        LinkedList<String> prevList = new LinkedList<>();
        LinkedList<String> nextList = new LinkedList<>();

        boolean find = false;
        int selfRank = 0;

        for (Map.Entry<Integer, LinkedHashSet<String>> entry : rankMap.entrySet()) {
            if (entry.getKey() > score) {
                rank += entry.getValue().size();
            } else if (entry.getKey() == score) {
                for (String k : entry.getValue()) {
                    if (k.equals(playerId)) {
                        find = true;
                        selfRank = ++rank;
                    } else {
                        rank++;
                        if (find) {
                            nextList.add(k);
                            if (nextList.size() >= range) {
                                break;
                            }
                        } else {
                            prevList.add(k);
                            if (prevList.size() > range) {
                                prevList.remove(0);
                            }
                        }
                    }
                }
            } else {
                for (String k : entry.getValue()) {
                    nextList.add(k);
                    if (nextList.size() >= range) {
                        break;
                    }
                }
            }
            if (nextList.size() >= range) {
                break;
            }
        }

        if (prevList.size() < range) {
            Integer prevKey = rankMap.lowerKey(score);
            if (prevKey != null) {
                prevList = findPrevPlayer(prevKey, range - prevList.size(), prevList);
            }
        }

        for (int i = 0; i < prevList.size(); i++) {
            RankInfo rankInfo = new RankInfo(prevList.get(i), playerScoreMap.get(prevList.get(i)), selfRank - prevList.size() + i);
            rankRange.add(rankInfo);
        }

        RankInfo selfRankInfo = new RankInfo(playerId, score, selfRank);
        rankRange.add(selfRankInfo);

        for (int i = 0; i < nextList.size(); i++) {
            RankInfo rankInfo = new RankInfo(nextList.get(i), playerScoreMap.get(nextList.get(i)), selfRank + i + 1);
            rankRange.add(rankInfo);
        }

        return rankRange;
    }

    private LinkedList<String> findPrevPlayer(int score, int count, LinkedList<String> list) {
        LinkedHashSet<String> playerSet = rankMap.get(score);
        if (playerSet.size() < count) {
            LinkedList<String> setList = new LinkedList<>(playerSet);
            setList.addAll(list);
            list = setList;
            Integer prevKey = rankMap.lowerKey(score);
            if (prevKey != null) {
                list = findPrevPlayer(prevKey, count - playerSet.size(), list);
            }
        } else {
            List<String> setList = new LinkedList<>(playerSet);
            for (int i = setList.size() - 1; i >= 0; i--) {
                list.addFirst(setList.get(i));
                if (list.size() >= count) {
                    break;
                }
            }
        }
        return list;
    }
}

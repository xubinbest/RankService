package org.xubin;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class Main {

    private static final int PLAYER_COUNT = 1000000;


    public static void main(String[] args) throws InterruptedException {
        test();
//        test1();
//        test2();
    }

    // 功能测试
    private static void test() {
        RankService.instance().updateScore("1", 100);
        RankService.instance().updateScore("2", 110);
        RankService.instance().updateScore("1", 150);
        RankService.instance().updateScore("3", 400);
        RankService.instance().updateScore("4", 200);
        RankService.instance().updateScore("5", 300);
        RankService.instance().updateScore("6", 80);
        RankService.instance().updateScore("7", 220);
        RankService.instance().updateScore("8", 300);
        RankService.instance().updateScore("9", 150);

        System.out.println(RankService.instance().getPlayerRank("1").toString());
        System.out.println(RankService.instance().getPlayerRank("2").toString());
        System.out.println(RankService.instance().getPlayerRank("3").toString());
        System.out.println(RankService.instance().getPlayerRank("4").toString());
        System.out.println(RankService.instance().getPlayerRank("5").toString());
        System.out.println(RankService.instance().getPlayerRank("6").toString());
        System.out.println(RankService.instance().getPlayerRank("7").toString());
        System.out.println(RankService.instance().getPlayerRank("8").toString());
        System.out.println(RankService.instance().getPlayerRank("9").toString());

        System.out.println("\n");

        System.out.println(RankService.instance().getPlayerRankExt("1").toString());
        System.out.println(RankService.instance().getPlayerRankExt("2").toString());
        System.out.println(RankService.instance().getPlayerRankExt("3").toString());
        System.out.println(RankService.instance().getPlayerRankExt("4").toString());
        System.out.println(RankService.instance().getPlayerRankExt("5").toString());
        System.out.println(RankService.instance().getPlayerRankExt("6").toString());
        System.out.println(RankService.instance().getPlayerRankExt("7").toString());
        System.out.println(RankService.instance().getPlayerRankExt("8").toString());
        System.out.println(RankService.instance().getPlayerRankExt("9").toString());

        System.out.println("\n");

        List<RankInfo> topN = RankService.instance().getTopN(2);
        System.out.println("TopN:");
        for (RankInfo rankInfo : topN) {
            System.out.println(rankInfo.toString());
        }
        System.out.println("\n");

        List<RankInfo> rangeList = RankService.instance().getPlayerRankRange("1", 2);
        System.out.println("RangeList:");
        for(RankInfo rankInfo : rangeList) {
            System.out.println(rankInfo.toString());
        }
    }

    // 性能测试
    private static void test1() throws InterruptedException {
        // 初始化数据
        initTestData();

        Thread.sleep(1000);
        // 随机更新分数
        randomUpdateScore(100);


        Thread.sleep(1000);
        // 随机查看排名
        randomShowRank(10);

        Thread.sleep(1000);
        // 查看TopN
        showTopN(10);

        Thread.sleep(1000);
        // 随机查看玩家排名范围
        randomShowPlayerRange(2);

    }

    // 并发测试
    private static void test2() throws InterruptedException {
        // 初始化数据
        initTestData();

        Thread.sleep(1000);

        CountDownLatch updateLatch = new CountDownLatch(10);
        CountDownLatch showLatch = new CountDownLatch(5);

        asyncRandomShowRank(5, showLatch);
        asyncRandomUpdateScore(10, updateLatch);

        updateLatch.await();
        showLatch.await();
    }

    private static void asyncRandomUpdateScore(int count, CountDownLatch latch) throws InterruptedException {
        System.out.println("asyncRandomUpdateScore start");

        for (int i = 0; i < count; i++) {
            Thread t = new Thread(() -> {
                try {
                    int playerId = (int)(Math.random() * 100000) % 100000 + 1;
                    updateScore(String.valueOf(playerId));
                } finally {
                    latch.countDown();
                }
            });
            t.start();
        }
        System.out.println("asyncRandomUpdateScore end");
    }

    private static void asyncRandomShowRank(int showCount, CountDownLatch latch) throws InterruptedException {
        System.out.println("asyncRandomShowRank start");
        for (int i = 0; i < showCount; i++) {
            Thread t = new Thread(() -> {
                try {
                    int playerId = (int)(Math.random() * 1000000) % PLAYER_COUNT + 1;
                    String playerIdStr = String.valueOf(playerId);
                    showRank(playerIdStr);
                } finally {
                    latch.countDown();
                }
            });
            t.start();
        }
        System.out.println("asyncRandomShowRank end");
    }



    private static void initTestData() {
        long start = System.currentTimeMillis();
        for (int i = 1; i <= PLAYER_COUNT; i++) {
            // 随机分数
            int score = (int)(Math.random() * 100000);
            RankService.instance().updateScore(String.valueOf(i), score);
        }
        long end = System.currentTimeMillis();
        System.out.println("initTestData cost: " + (end - start) + "ms \n");
    }

    private static void randomShowRank(int showCount) {
        System.out.println("Rank:");
        long start = System.currentTimeMillis();
        for (int i = 0; i < showCount; i++) {
            int playerId = (int)(Math.random() * 1000000) % PLAYER_COUNT + 1;
            String playerIdStr = String.valueOf(playerId);
            long perStart = System.currentTimeMillis();
            RankService.instance().getPlayerRank(playerIdStr);
            long perEnd = System.currentTimeMillis();
            System.out.println("playerId: " + playerId + " cost: " + (perEnd - perStart) + "ms");
        }
        long end = System.currentTimeMillis();
        System.out.println("randomShowRank cost: " + (end - start) + "ms \n");
    }

    private static void showRank(String playerId) {
        long start = System.currentTimeMillis();
        RankService.instance().getPlayerRank(playerId);
        long end = System.currentTimeMillis();
        System.out.println(playerId + " showRank cost: " + (end - start) + "ms \n");
    }

    private static void showTopN(int n) {
        long start = System.currentTimeMillis();
        List<RankInfo> topN = RankService.instance().getTopN(n);
        long end = System.currentTimeMillis();
        System.out.println("TopN:");
        for (RankInfo rankInfo : topN) {
            System.out.println(rankInfo.toString());
        }
        System.out.println("showTopN cost: " + (end - start) + "ms \n");
    }

    private static void randomUpdateScore(int count) {
        long start = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            int playerId = (int)(Math.random() * 100000) % 100000 + 1;
            int score = (int)(Math.random() * 100000);
            RankService.instance().updateScore(String.valueOf(playerId), score);
        }

        long end = System.currentTimeMillis();
        System.out.println("randomUpdateScore cost: " + (end - start) + "ms \n");
    }

    private static void updateScore(String playerId) {
        int score = (int)(Math.random() * 100000);
        long start = System.currentTimeMillis();
        RankService.instance().updateScore(playerId, score);
        long end = System.currentTimeMillis();
        System.out.println(playerId + " updateScore cost: " + (end - start) + "ms \n");
    }

    private static void randomShowPlayerRange(int n) {
        int playerId = (int)(Math.random() * 100000) % 100000 + 1;
        String playerIdStr = String.valueOf(playerId);
        long start = System.currentTimeMillis();
        List<RankInfo> rangeList = RankService.instance().getPlayerRankRange(playerIdStr, n);
        long end = System.currentTimeMillis();
        System.out.println("player: " + playerId +  " RangeList:");
        for (RankInfo rankInfo : rangeList) {
            System.out.println(rankInfo.toString());
        }
        System.out.println("randomShowPlayerRange cost: " + (end - start) + "ms \n");
    }
}
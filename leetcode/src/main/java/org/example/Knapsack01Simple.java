package org.example;

import java.util.*;

/**
 * 用动态规划法求解0-1背包问题
 */
import java.util.*;

public class Knapsack01Simple {

    /**
     * 动态规划解决0-1背包问题（简化版）
     * @param weights 物品重量数组
     * @param values 物品价值数组
     * @param capacity 背包容量
     * @return 最大价值和选择的物品总重量
     */
    public static int[] solveKnapsack(int[] weights, int[] values, int capacity) {
        int n = weights.length;
        int[] dp = new int[capacity + 1];

        // 动态规划
        for (int i = 0; i < n; i++) {
            for (int j = capacity; j >= weights[i]; j--) {
                dp[j] = Math.max(dp[j], dp[j - weights[i]] + values[i]);
            }
        }

        // 回溯找出被选物品的总重量
        int remainingCapacity = capacity;
        int totalWeight = 0;

        for (int i = n - 1; i >= 0; i--) {
            if (remainingCapacity >= weights[i] &&
                    dp[remainingCapacity] == dp[remainingCapacity - weights[i]] + values[i]) {
                totalWeight += weights[i];
                remainingCapacity -= weights[i];
            }
        }

        return new int[] {dp[capacity], totalWeight}; // 返回最大价值和总重量
    }

    public static void main(String[] args) {
        // 示例数据
        int[] weights = {2, 3, 4, 5};
        int[] values = {3, 4, 5, 6};
        int capacity = 8;

        int[] result = solveKnapsack(weights, values, capacity);

        System.out.println("最大价值: " + result[0]);
        System.out.println("总重量: " + result[1]);
    }
}
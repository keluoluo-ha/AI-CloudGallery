package com.hhk.picturebacked;


import static java.util.Collections.max;
import static java.util.Collections.min;


//class Solution {
//    public int minMeetingRooms(int[][] intervals) {
//        PriorityQueue<Integer> meetingQueue = new PriorityQueue<>();
//        Arrays.sort(intervals,(a,b)->a[0]-b[0]);
//
//     int end;
//    for(end=0;end<intervals.length;end++){
////     if(meetingQueue.isEmpty()) meetingQueue.add(intervals[end][1]);
//     if(!meetingQueue.isEmpty()&&meetingQueue.peek()<intervals[end][0]) {
//         meetingQueue.poll();
//
//     }
//        meetingQueue.add(intervals[end][1]);
//    }
//    return meetingQueue.size();
//    }
//class Solution {
//    public int leastInterval(char[] tasks, int n) {
//        int res=0;
//        //找出现最多次的字符
//        HashMap<Character, Integer> taskMap = new HashMap<>();
//        for (char task:tasks){
//            taskMap.put(task, taskMap.getOrDefault(task, 0) + 1);
//        }
//        int maxCount = 0;
//        int maxTaskNum = 0; // 记录：出现次数为最大值的任务有多少种
//
//        // 遍历找到最大次数，以及有多少个任务达到了这个最大次数
//        for (Map.Entry<Character, Integer> entry : taskMap.entrySet()) {
//            int count = entry.getValue();
//            if (count > maxCount) {
//                maxCount = count;
//                maxTaskNum = 1; // 新的最大值，重置数量
//            } else if (count == maxCount) {
//                maxTaskNum++; // 相同最大值，数量+1
//            }
//        }
//        //任务塞不满
//        res=(maxCount-1)*(n+1)+maxTaskNum;
//        //任务塞得满
//        res=Math.max(res,tasks.length);
//
//        return res;
//    }
//class Solution {
//    public int coinChange(int[] coins, int amount) {
//        if(amount==0) return 0;
//        //凑出金额 i 所需要的最少硬币数量
//        int[] dp=new int[amount+1];
//
//        //初始化
//
//        Arrays.fill(dp, amount + 1);
//        dp[0]=0;
//        for (int i=1;i<=amount;i++){
//            for (int j=0;j<coins.length;j++){
//              if(coins[j]<=i){
//                  dp[i]=Math.min(dp[i],dp[i-coins[j]]+1);
//              }
//            }
//        }
//        return dp[amount]==amount+1?-1:dp[amount];
//
//
//    }
//}
//
//class Solution {
//    public int findTargetSumWays(int[] nums, int target) {
//        if(nums.length==1){
//            if(target==nums[0]) {
//                return 0;
//            }
//        }
//        int sum = Arrays.stream(nums).sum();
//
//        //初始化状态dp dp[i][j] = nums[]前 i 个数，凑出和为 j 的方案数
//        int[][] dp=new int[nums.length][2*sum+1];
//        //初始化
//        dp[0][sum+nums[0]]+=1;
//        dp[0][sum-nums[0]]+=1;
//
//
//        for (int i=1;i<nums.length;i++){
//            for (int j=0;j<2*sum+1;j++){
//                if(j-nums[i]>=0){
//                    dp[i][j]+=dp[i-1][j-nums[i]];
//
//                }
//                if(j+nums[i]<=2*sum){
//                    dp[i][j]+=dp[i-1][j+nums[i]];
//
//                }
//
//
//            }
//        }
//
//
//
//
//        // 在 return 之前加上判断
//        if (target < -sum || target > sum) {
//            return 0;
//        }
//
//        return dp[nums.length-1][target+sum];
//
//    }
//}
//
//
//

//多状态要流转、互相依赖 → 统统写成 dp [i][状态数]
//        有多种互斥身份、且会影响下一天规则 → 必须二维（股票 309）
//        只是需要多保留一个最值做判断，没有身份、没有后续规则限制 → 只用变量 / 一维就行（最大乘积、最大子数组和）
//
//        要单独保存每一段 i~j 的结果 → 必须二维
//        只看到某个位置整体行不行，不用保存每一段 → 只用一维
//        打家劫舍 / 股票：每种状态只有唯一最优值 → 可以用 dp[n][2]
//        目标和/回文子串数：每种选择会衍生出无数种累加和 → 只用 dp[n][范围] 存不下所有和的情况，没法做状态转移
//class Solution {
//    public String longestPalindrome(String s) {
//
//
//        int n = s.length();
//        if(n<2) return s;
//
//        String res=null;
//        //表示s第i~j位是不是回文子串
//        boolean[][] dp = new boolean[s.length()][s.length()];
//
//        for (int i=n-1;i>=0;i--){
//
//            for (int j=i;j<n;j++){
//                if(i==j){
//                    dp[i][j]=true;
//                    if(res==null||res.length()<j-i+1){
//                        res=s.substring(i,j+1);
//                    }
//                }else if(i==j-1&&s.charAt(i)==s.charAt(j)){
//                    dp[i][j]=true;
//                    if(res==null||res.length()<j-i+1){
//                        res=s.substring(i,j+1);
//                    }
//                }else if(s.charAt(i)==s.charAt(j)&&dp[i+1][j-1]){
//                    dp[i][j]=true;
//                    if(res==null||res.length()<j-i+1){
//                        res=s.substring(i,j+1);
//                    }
//                }
//            }
//        }
//        return res;
//    }
//}
//
//
//
//
//
//

class Solution {
    public boolean canPartition(int[] nums) {
        //背包dp
        int target =0;
        for (int num:nums){
            target +=num;
        }
        if(target%2==1) return false;
        target=target/2;

        // dp [j] 表示能否凑出数值和为 j
        boolean[] dp = new boolean[target+1];
        dp[0]=true;
        for (int num : nums) {
            for (int j=target;j>=num;j--){
                dp[j]=dp[j]||dp[j-num];
            }
        }
        return dp[target];
    }
}



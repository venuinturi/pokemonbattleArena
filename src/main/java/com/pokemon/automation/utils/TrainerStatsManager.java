package com.pokemon.automation.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;
import java.util.Map;
import java.util.HashMap;

public class TrainerStatsManager {
    private static final String STATS_FILE = "trainer_stats.properties";
    private static Properties stats = new Properties();

    static {
        loadStats();
    }

    private static void loadStats() {
        try {
            File f = new File(STATS_FILE);
            if (f.exists()) {
                FileInputStream in = new FileInputStream(f);
                stats.load(in);
                in.close();
            }
        } catch (Exception e) {
            System.out.println("Could not load trainer stats: " + e.getMessage());
        }
    }

    private static void saveStats() {
        try {
            FileOutputStream out = new FileOutputStream(STATS_FILE);
            stats.store(out, "Trainer Rewards");
            out.close();
        } catch (Exception e) {
            System.out.println("Could not save trainer stats: " + e.getMessage());
        }
    }

    public static void saveReward(String trainerName, int amount) {
        if (trainerName == null || trainerName.trim().isEmpty()) return;
        trainerName = trainerName.trim();
        
        // Only update if it's higher than the previously recorded amount (just in case it varies slightly)
        int existing = getReward(trainerName);
        if (amount > existing) {
            stats.setProperty(trainerName, String.valueOf(amount));
            saveStats();
            System.out.println("Recorded new max reward for " + trainerName + ": $" + amount);
        }
    }

    public static int getReward(String trainerName) {
        if (trainerName == null) return 0;
        String val = stats.getProperty(trainerName.trim());
        if (val != null) {
            try {
                return Integer.parseInt(val);
            } catch (Exception e) {}
        }
        return 0;
    }

    public static String getBestTrainer(java.util.List<String> availableTrainers) {
        String bestTrainer = null;
        int maxReward = -1;

        for (String trainer : availableTrainers) {
            int reward = getReward(trainer);
            if (reward > maxReward) {
                maxReward = reward;
                bestTrainer = trainer;
            }
        }
        return bestTrainer;
    }
    
    public static Map<String, Integer> getAllStats() {
        Map<String, Integer> allStats = new HashMap<>();
        for (String key : stats.stringPropertyNames()) {
            allStats.put(key, getReward(key));
        }
        return allStats;
    }
}

package com.pokemon.automation.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigReader {
    private static Properties properties;
    private static final String FILE_PATH = "src/test/resources/config.properties";

    static {
        properties = new Properties();
        try (FileInputStream fis = new FileInputStream(FILE_PATH)) {
            properties.load(fis);
        } catch (IOException e) {
            System.err.println("Warning: config.properties file not found or could not be loaded at " + FILE_PATH);
        }
    }

    public static String getProperty(String key) {
        String value = System.getProperty(key);
        if (value != null && !value.trim().isEmpty()) {
            return value;
        }

        String envKey = key.toUpperCase().replace(".", "_");
        value = System.getenv(envKey);
        if (value != null && !value.trim().isEmpty()) {
            return value;
        }
        
        value = System.getenv(key);
        if (value != null && !value.trim().isEmpty()) {
            return value;
        }

        value = properties.getProperty(key);
        if (value != null && !value.trim().isEmpty()) {
            return value;
        }

        if ("url".equalsIgnoreCase(key)) {
            return "https://pokemonbattlearena.net";
        } else if ("browser".equalsIgnoreCase(key)) {
            return "chrome";
        }
        return null;
    }
}

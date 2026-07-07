package com.pokemon.automation.mapping;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MapGraph {
    private Map<Integer, MapNode> nodes = new HashMap<>();

    public void addNode(MapNode node) {
        if (!nodes.containsKey(node.getMapId())) {
            nodes.put(node.getMapId(), node);
        }
    }

    public MapNode getNode(int mapId) {
        return nodes.get(mapId);
    }

    public boolean hasNode(int mapId) {
        return nodes.containsKey(mapId);
    }

    public void recordConnection(int fromMapId, String direction, int toMapId) {
        if (nodes.containsKey(fromMapId)) {
            nodes.get(fromMapId).addConnection(direction, toMapId);
        }
    }

    public void saveGraphToFile(String filePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (MapNode node : nodes.values()) {
                StringBuilder sb = new StringBuilder();
                sb.append(node.getMapId()).append(",")
                  .append(node.getMapName()).append(",")
                  .append(node.getUrl());
                
                for (Map.Entry<String, Integer> entry : node.getConnections().entrySet()) {
                    sb.append(",").append(entry.getKey()).append(":").append(entry.getValue());
                }
                
                writer.write(sb.toString());
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error saving map graph: " + e.getMessage());
        }
    }
}

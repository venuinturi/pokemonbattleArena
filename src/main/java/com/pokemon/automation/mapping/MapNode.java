package com.pokemon.automation.mapping;

import java.util.HashMap;
import java.util.Map;

public class MapNode {
    private int mapId;
    private String mapName;
    private String url;
    
    // Connections: direction -> target map ID
    private Map<String, Integer> connections;

    public MapNode(int mapId, String mapName, String url) {
        this.mapId = mapId;
        this.mapName = mapName;
        this.url = url;
        this.connections = new HashMap<>();
    }

    public int getMapId() {
        return mapId;
    }

    public String getMapName() {
        return mapName;
    }

    public String getUrl() {
        return url;
    }

    public void addConnection(String direction, int targetMapId) {
        connections.put(direction, targetMapId);
    }

    public Map<String, Integer> getConnections() {
        return connections;
    }

    @Override
    public String toString() {
        return "MapNode{" +
                "mapId=" + mapId +
                ", mapName='" + mapName + '\'' +
                ", connections=" + connections +
                '}';
    }
}

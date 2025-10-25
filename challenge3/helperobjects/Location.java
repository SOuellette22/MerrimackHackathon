package helperobjects;

import merrimackutil.util.Tuple;

import java.util.HashMap;

public class Location {
    private int x;
    private int y;
    private int demand;
    private boolean visited;
    private HashMap<Location, Integer> connectedRoads;

    public Location(int x, int y, int demand) {
        this.x = x;
        this.y = y;
        this.demand = demand;
        this.visited = false;
        this.connectedRoads = new HashMap<>();
    }

    public Tuple<Integer, Integer> getLocation() {
        return new Tuple<>(x, y);
    }

    public int getDemand() {
        return demand;
    }

    public boolean isVisited() {
        return visited;
    }

    public void visited() {
        this.visited = true;
    }

    public void addRoad(Location loc, int distance) {
        connectedRoads.put(loc, distance);
    }
}

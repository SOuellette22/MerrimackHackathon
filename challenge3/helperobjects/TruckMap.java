package helperobjects;

import helperobjects.Location;

import java.util.HashMap;
import java.util.LinkedList;

public class TruckMap {
    private HashMap<Integer, Location> locations;
    private LinkedList<Truck> trucks;

    public TruckMap(LinkedList<Truck> trucks, HashMap<Integer, Location> locations){
        this.trucks = trucks;
        this.locations = locations;
    }

    /**
     * builds the map object from the given locations and roads
     */
    public void buildMap(){

    }
    /**
     * draws the map object visually
     */
    public void drawMap(){

    }
}
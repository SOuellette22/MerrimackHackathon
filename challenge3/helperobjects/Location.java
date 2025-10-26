package helperobjects;

import merrimackutil.util.Tuple;

import java.util.HashMap;

/**
 * class representing a location (or a node in a graph)
 */
public class Location {
    Tuple<Double, Double> location;
    private int demand;
    private boolean visited;
    private boolean isDepot;
    private int id;

    /**
     * basic constructor
     * 
     * @param x      x coordinate (longitude)
     * @param y      y coordinate (latitude)
     * @param demand the amount of demand
     */
    public Location(double x, double y, int demand) {
        this.location = new Tuple<Double, Double>(x, y);
        this.demand = demand;
        this.visited = false;
    }

    /**
     * overloaded constructor to set a Location to the depot
     * 
     * @param x
     * @param y
     */
    public Location(double x, double y) {
        this.location = new Tuple<Double, Double>(x, y);
        this.visited = false;
        this.isDepot = true; // if the location doesn't have a demand, its automatically a depot
    }

    public int getId() {
        return id;
    }

    /**
     * returns a tuple containing the coordinates of
     * 
     * @return
     */
    public Tuple<Double, Double> getLocation() {
        return this.location;
    }

    /**
     * returns the demand number value of this Location
     * 
     * @return
     */
    public int getDemand() {
        return demand;
    }

    /**
     * returns the parity value representing whether or not this location has been
     * visited
     * 
     * @return
     */
    public boolean isVisited() {
        return visited;
    }

    /**
     * sets the isVisited variable to true (wont need to be reversed)
     */
    public void visit() {
        this.visited = true;
    }

    /**
     * returns the parity value representing whether or not this Location object is
     * the Depot
     * 
     * @return
     */
    public boolean isDepot() {
        return this.isDepot;
    }
}

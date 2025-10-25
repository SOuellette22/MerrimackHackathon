package helperobjects;

import java.util.LinkedList;

/**
 * class representing a truck
 */
public class Truck {
    private int id;
    private int load;
    private int capacity;
    private LinkedList<Location> visitedLocations;
    private int minutesTravelled;
    private Location currentLocation;

    /**
     * basic constructor
     * @param id
     * @param capacity
     */
    public Truck(int id, int capacity, Location location) {
        this.id = id;
        this.capacity = capacity;
        this.load = capacity;
        this.visitedLocations = new LinkedList<>();
        this.minutesTravelled = 0;
        this.currentLocation = location;
    }

    /**
     * checks if a location has been visited by this truck
     * @param loc
     * @return
     */
    public boolean checkVisit(Location loc) {
        return this.visitedLocations.contains(loc);
        // if (loc.getDemand() <= load) {
        //     return true;
        // } else {
        //     return false;
        // }
    }

    /**
     * visits a location and subtracts it's demand from this truck's capacity
     * @param loc
     */
    public void visit(Location nextLoc) {
        if (checkVisit(nextLoc)) {
            load -= nextLoc.getDemand();
            this.visitedLocations.add(nextLoc);
            nextLoc.setVisited();
            this.minutesTravelled += this.currentLocation.getMinutes(nextLoc);
        } else {
            System.out.println("Not enough load to visit this location.");
        }
    }

    /**
     * goes back to the depot (<- implementation TBD) and refills capacity
     */
    public void refill() {
        load = capacity;
    }

    /**
     * returns the total amount of time this truck has travelled for
     * @return
     */
    public int getMinutesTravelled() {
        return this.minutesTravelled;
    }

    /**
     * returns the amt of capacity this truck has left
     * @return
     */
    public int getLoad() {
        return this.load;
    }

    /**
     * returns the id number for this truck
     * @return
     */
    public int getId() {
        return id;
    }
}

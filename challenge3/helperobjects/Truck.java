package helperobjects;

public class Truck {
    private int id;
    private int load;
    private int capacity;

    public Truck(int id, int capacity) {
        this.id = id;
        this.capacity = capacity;
        this.load = capacity;
    }

    public boolean checkVisit(Location loc) {
        if (loc.getDemand() <= load) {
            return true;
        } else {
            return false;
        }
    }

    public void visit(Location loc) {
        if (checkVisit(loc)) {
            load -= loc.getDemand();
            loc.visited();
        } else {
            System.out.println("Not enough load to visit this location.");
        }
    }

    public void refill() {
        load = capacity;
    }

    public int getId() {
        return id;
    }
}

package helperobjects;

import java.util.*;

/**
 * Implementation of Clarke-Wright Savings Algorithm for Vehicle Routing Problem
 */
public class ClarkeWrightAlgorithm {
    
    private HashMap<Integer, Location> locations;
    private Map<Integer, Map<Integer, Double>> roads;
    private LinkedList<Truck> trucks;
    private List<Route> routes;
    
    /**
     * Inner class to represent a route
     */
    public static class Route {
        public LinkedList<Integer> path;
        public int totalDemand;
        public double totalDistance;
        boolean canMergeStart; // Can merge at the beginning
        boolean canMergeEnd;   // Can merge at the end
        
        Route(int locationId, int demand, double distanceToDepot) {
            path = new LinkedList<>();
            path.add(0); // depot
            path.add(locationId);
            path.add(0); // depot
            totalDemand = demand;
            totalDistance = distanceToDepot * 2;
            canMergeStart = true;
            canMergeEnd = true;
        }
        
        boolean contains(int locationId) {
            return path.contains(locationId);
        }
        
        int getFirstLocation() {
            return path.get(1);
        }
        
        int getLastLocation() {
            return path.get(path.size() - 2);
        }
    }
    
    /**
     * Inner class to represent savings between two locations
     */
    class Saving implements Comparable<Saving> {
        int i, j;
        double savings;
        
        Saving(int i, int j, double savings) {
            this.i = i;
            this.j = j;
            this.savings = savings;
        }
        
        @Override
        public int compareTo(Saving other) {
            return Double.compare(other.savings, this.savings); // Descending order
        }
    }
    
    public ClarkeWrightAlgorithm(HashMap<Integer, Location> locations, 
                                 Map<Integer, Map<Integer, Double>> roads,
                                 LinkedList<Truck> trucks) {
        this.locations = locations;
        this.roads = roads;
        this.trucks = trucks;
        this.routes = new ArrayList<>();
    }
    
    /**
     * Get distance between two locations
     */
    private double getDistance(int from, int to) {
        if (from == to) return 0;
        
        if (roads.containsKey(from) && roads.get(from).containsKey(to)) {
            return roads.get(from).get(to);
        } else if (roads.containsKey(to) && roads.get(to).containsKey(from)) {
            return roads.get(to).get(from);
        }
        
        // If no road exists in the dataset, calculate Euclidean distance as fallback
        Location loc1 = locations.get(from);
        Location loc2 = locations.get(to);
        double dx = loc1.getLocation().getFirst() - loc2.getLocation().getFirst();
        double dy = loc1.getLocation().getSecond() - loc2.getLocation().getSecond();
        return Math.sqrt(dx * dx + dy * dy);
    }
    
    /**
     * Execute the Clarke-Wright Savings Algorithm
     */
    public void execute() {
        System.out.println("\n=== Starting Clarke-Wright Savings Algorithm ===\n");
        
        // Step 1: Create initial routes (depot -> location -> depot)
        System.out.println("Step 1: Creating initial routes...");
        for (Integer locId : locations.keySet()) {
            if (locId != 0) { // Skip depot
                Location loc = locations.get(locId);
                routes.add(new Route(locId, loc.getDemand(), getDistance(0, locId)));
            }
        }
        System.out.println("Created " + routes.size() + " initial routes\n");
        
        // Step 2: Calculate savings
        System.out.println("Step 2: Calculating savings...");
        List<Saving> savings = new ArrayList<>();
        for (Integer i : locations.keySet()) {
            if (i == 0) continue; // Skip depot
            for (Integer j : locations.keySet()) {
                if (j == 0 || i >= j) continue; // Skip depot and avoid duplicates
                
                double saving = getDistance(0, i) + getDistance(0, j) - getDistance(i, j);
                savings.add(new Saving(i, j, saving));
            }
        }
        
        // Step 3: Sort savings in descending order
        Collections.sort(savings);
        System.out.println("Calculated " + savings.size() + " savings values");
        System.out.println("Top 10 savings:");
        for (int i = 0; i < Math.min(10, savings.size()); i++) {
            Saving s = savings.get(i);
            System.out.printf("  Locations %d-%d: %.2f\n", s.i, s.j, s.savings);
        }
        System.out.println();
        
        // Step 4 & 5: Merge routes based on savings
        System.out.println("Step 3-5: Merging routes based on savings...\n");
        int mergeCount = 0;
        int truckCapacity = trucks.get(0).getLoad(); // All trucks have same capacity
        
        for (Saving saving : savings) {
            Route route1 = findRoute(saving.i);
            Route route2 = findRoute(saving.j);
            
            // Check if locations are on different routes
            if (route1 == route2) continue;
            
            // Check if merge is possible (both locations must be at route ends)
            boolean canMerge = false;
            boolean reverseRoute2 = false;
            
            if (route1.getLastLocation() == saving.i && route2.getFirstLocation() == saving.j) {
                // Connect end of route1 to start of route2
                canMerge = route1.canMergeEnd && route2.canMergeStart;
            } else if (route1.getLastLocation() == saving.i && route2.getLastLocation() == saving.j) {
                // Connect end of route1 to end of route2 (need to reverse route2)
                canMerge = route1.canMergeEnd && route2.canMergeEnd;
                reverseRoute2 = true;
            } else if (route1.getFirstLocation() == saving.i && route2.getFirstLocation() == saving.j) {
                // Connect start of route1 to start of route2 (need to reverse route1)
                canMerge = route1.canMergeStart && route2.canMergeStart;
                // Swap routes to keep logic simpler
                Route temp = route1;
                route1 = route2;
                route2 = temp;
                reverseRoute2 = true;
            } else if (route1.getFirstLocation() == saving.i && route2.getLastLocation() == saving.j) {
                // Connect start of route1 to end of route2 (swap routes)
                canMerge = route1.canMergeStart && route2.canMergeEnd;
                Route temp = route1;
                route1 = route2;
                route2 = temp;
            }
            
            if (!canMerge) continue;
            
            // Check capacity constraint
            if (route1.totalDemand + route2.totalDemand > truckCapacity) {
                continue;
            }
            
            // Perform merge
            mergeRoutes(route1, route2, reverseRoute2);
            mergeCount++;
            System.out.printf("Merged routes containing locations %d and %d (Savings: %.2f)\n", 
                            saving.i, saving.j, saving.savings);
        }
        
        System.out.println("\nTotal merges performed: " + mergeCount);
        System.out.println("\n=== Final Routes ===\n");
        printFinalRoutes();
    }
    
    /**
     * Find which route contains a location
     */
    private Route findRoute(int locationId) {
        for (Route route : routes) {
            if (route.contains(locationId)) {
                return route;
            }
        }
        return null;
    }
    
    /**
     * Merge two routes
     */
    private void mergeRoutes(Route route1, Route route2, boolean reverseRoute2) {
        // Remove depot from end of route1 and start of route2
        route1.path.removeLast();
        route2.path.removeFirst();
        
        if (reverseRoute2) {
            // Reverse route2 (excluding depot at the end)
            LinkedList<Integer> temp = new LinkedList<>();
            temp.add(route2.path.removeLast()); // Keep depot at end
            while (!route2.path.isEmpty()) {
                temp.addFirst(route2.path.removeLast());
            }
            route2.path = temp;
        }
        
        // Merge paths
        route1.path.addAll(route2.path);
        
        // Update demand and distance
        route1.totalDemand += route2.totalDemand;
        route1.totalDistance = calculateRouteDistance(route1.path);
        
        // Update merge capabilities
        route1.canMergeEnd = route2.canMergeEnd;
        
        // Remove route2 from routes list
        routes.remove(route2);
    }
    
    /**
     * Calculate total distance of a route
     */
    private double calculateRouteDistance(LinkedList<Integer> path) {
        double distance = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            distance += getDistance(path.get(i), path.get(i + 1));
        }
        return distance;
    }
    
    /**
     * Print final routes with statistics
     */
    private void printFinalRoutes() {
        double totalDistance = 0;
        int routeNum = 1;
        
        for (Route route : routes) {
            System.out.printf("Route %d: ", routeNum++);
            
            // Print path
            for (int i = 0; i < route.path.size(); i++) {
                System.out.print(route.path.get(i));
                if (i < route.path.size() - 1) {
                    System.out.print(" -> ");
                }
            }
            
            System.out.printf("\n  Demand: %d/%d units", route.totalDemand, trucks.get(0).getLoad());
            System.out.printf("\n  Distance: %.2f\n\n", route.totalDistance);
            
            totalDistance += route.totalDistance;
        }
        
        System.out.println("=== Summary ===");
        System.out.println("Total number of routes: " + routes.size());
        System.out.println("Total number of available trucks: " + trucks.size());
        System.out.printf("Total distance: %.2f\n", totalDistance);
        
        if (routes.size() > trucks.size()) {
            System.out.println("\nWARNING: More routes needed than trucks available!");
        }
    }
    
    /**
     * Get the final routes for visualization or further processing
     */
    public List<Route> getRoutes() {
        return routes;
    }
}
package helperobjects;

import java.awt.Color;
import java.util.*;

/**
 * Visualizes the truck routes using the turtle graphics library
 */
public class TruckRouteVisualizer {
    
    public Turtle turtle = new Turtle();
    private HashMap<Integer, Location> locations;
    private List<ClarkeWrightAlgorithm.Route> routes;
    private double minX, maxX, minY, maxY;
    private double scale;
    private Color[] ROUTE_COLORS = {
        Color.RED, Color.BLUE, Color.GREEN, Color.MAGENTA, 
        Color.ORANGE, Color.CYAN, Color.PINK, Color.YELLOW
    };
    
    public TruckRouteVisualizer(HashMap<Integer, Location> locations, 
                                List<ClarkeWrightAlgorithm.Route> routes) {
        this.locations = locations;
        this.routes = routes;
        calculateBounds();
    }
    
    /**
     * Calculate the bounds of all locations to properly scale the visualization
     */
    private void calculateBounds() {
        minX = Double.MAX_VALUE;
        maxX = Double.MIN_VALUE;
        minY = Double.MAX_VALUE;
        maxY = Double.MIN_VALUE;
        
        for (Location loc : locations.values()) {
            double x = loc.getLocation().getFirst();
            double y = loc.getLocation().getSecond();
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }
        
        // Add padding
        double padX = (maxX - minX) * 0.1;
        double padY = (maxY - minY) * 0.1;
        minX -= padX;
        maxX += padX;
        minY -= padY;
        maxY += padY;
        
        // Calculate scale to fit in window (assuming 400x400 drawing area)
        double scaleX = 400.0 / (maxX - minX);
        double scaleY = 400.0 / (maxY - minY);
        scale = Math.min(scaleX, scaleY) * 0.8; // Use 80% of available space
    }
    
    /**
     * Convert location coordinates to screen coordinates
     */
    private double screenX(double x) {
        return (x - minX) * scale - 200;
    }
    
    private double screenY(double y) {
        return (y - minY) * scale - 200;
    }
    
    /**
     * Draw the visualization
     */
    public void visualize() {
        // Clear the screen and set up
        turtle.speed(0); // Fastest speed
        Turtle.bgcolor("white");
        //turtle.title("Vehicle Routing - Clarke-Wright Algorithm");
        
        // Draw grid lines for reference
        drawGrid();
        
        // Draw all locations first
        drawLocations();
        
        // Draw routes with different colors
        drawRoutes();
        
        // Draw legend
        drawLegend();
        
        // Hide turtle and update
        turtle.hide();
        Turtle.update();
    }
    
    /**
     * Draw a grid for reference
     */
    private void drawGrid() {
        turtle.penColor("lightgray");
        turtle.width(1);
        
        // Vertical lines
        for (double x = -200; x <= 200; x += 50) {
            turtle.up();
            turtle.setPosition(x, -200);
            turtle.down();
            turtle.setPosition(x, 200);
        }
        
        // Horizontal lines
        for (double y = -200; y <= 200; y += 50) {
            turtle.up();
            turtle.setPosition(-200, y);
            turtle.down();
            turtle.setPosition(200, y);
        }
    }
    
    /**
     * Draw all locations
     */
    private void drawLocations() {
        for (Map.Entry<Integer, Location> entry : locations.entrySet()) {
            int id = entry.getKey();
            Location loc = entry.getValue();
            double x = screenX(loc.getLocation().getFirst());
            double y = screenY(loc.getLocation().getSecond());
            
            turtle.up();
            turtle.setPosition(x, y);
            
            if (id == 0) {
                // Draw depot as a larger square
                drawDepot(x, y);
            } else {
                // Draw regular location as a circle
                drawLocation(x, y, id, loc.getDemand());
            }
        }
    }
    
    /**
     * Draw the depot
     */
    private void drawDepot(double x, double y) {
        turtle.penColor("black");
        turtle.fillColor("gold");
        turtle.width(2);
        
        // Draw a square for depot
        turtle.up();
        turtle.setPosition(x - 10, y - 10);
        turtle.down();
        //turtle.startFill();
        for (int i = 0; i < 4; i++) {
            turtle.forward(20);
            turtle.left(90);
        }
        //turtle.endFill();
        
        // Label
        turtle.up();
        turtle.setPosition(x, y - 25);
        turtle.penColor("black");
        //turtle.write("DEPOT", "center");
    }
    
    /**
     * Draw a regular location
     */
    private void drawLocation(double x, double y, int id, int demand) {
        turtle.penColor("black");
        turtle.fillColor("lightblue");
        turtle.width(2);
        
        // Draw circle
        turtle.up();
        turtle.setPosition(x, y - 8);
        turtle.down();
        // turtle.startFill();
        // turtle.circle(8);
        // turtle.endFill();
        
        // Draw ID inside circle
        turtle.up();
        turtle.setPosition(x, y - 5);
        turtle.penColor("black");
        //turtle.write(String.valueOf(id), "center");
        
        // Draw demand below
        turtle.setPosition(x, y - 20);
        //turtle.write("D:" + demand, "center", "Arial", 8);
    }
    
    /**
     * Draw all routes with different colors
     */
    private void drawRoutes() {
        int routeIndex = 0;
        
        for (ClarkeWrightAlgorithm.Route route : routes) {
            Color routeColor = ROUTE_COLORS[routeIndex % ROUTE_COLORS.length];
            drawRoute(route, routeColor, routeIndex + 1);
            routeIndex++;
        }
    }
    
    /**
     * Draw a single route
     */
    private void drawRoute(ClarkeWrightAlgorithm.Route route, Color color, int routeNum) {
        turtle.penColor(color);
        turtle.width(3);
        
        LinkedList<Integer> path = route.path;
        
        // Start at first location
        if (path.size() > 0) {
            Location firstLoc = locations.get(path.get(0));
            double x = screenX(firstLoc.getLocation().getFirst());
            double y = screenY(firstLoc.getLocation().getSecond());
            turtle.up();
            turtle.setPosition(x, y);
            turtle.down();
            
            // Draw path
            for (int i = 1; i < path.size(); i++) {
                Location loc = locations.get(path.get(i));
                x = screenX(loc.getLocation().getFirst());
                y = screenY(loc.getLocation().getSecond());
                
                // Draw arrow pointing to next location
                drawArrowTo(x, y, color);
            }
        }
    }
    
    /**
     * Draw an arrow to the specified position
     */
    private void drawArrowTo(double x, double y, Color color) {
        double startX = turtle.getX();
        double startY = turtle.getY();
        
        // Draw line
        turtle.setPosition(x, y);
        
        // Calculate arrow head
        double angle = Math.atan2(y - startY, x - startX);
        double arrowLength = 10;
        double arrowAngle = Math.PI / 6; // 30 degrees
        
        // Draw arrow head
        turtle.up();
        turtle.setPosition(x, y);
        turtle.down();
        
        double x1 = x - arrowLength * Math.cos(angle - arrowAngle);
        double y1 = y - arrowLength * Math.sin(angle - arrowAngle);
        turtle.setPosition(x1, y1);
        
        turtle.up();
        turtle.setPosition(x, y);
        turtle.down();
        
        double x2 = x - arrowLength * Math.cos(angle + arrowAngle);
        double y2 = y - arrowLength * Math.sin(angle + arrowAngle);
        turtle.setPosition(x2, y2);
        
        // Return to end point
        turtle.up();
        turtle.setPosition(x, y);
        turtle.down();
    }
    
    /**
     * Draw a legend showing route colors and statistics
     */
    private void drawLegend() {
        double legendX = -190;
        double legendY = 180;
        
        turtle.up();
        turtle.setPosition(legendX, legendY);
        turtle.penColor("black");
        //turtle.write("ROUTES:", "left", "Arial", 12);
        
        int routeIndex = 0;
        for (ClarkeWrightAlgorithm.Route route : routes) {
            legendY -= 20;
            Color routeColor = ROUTE_COLORS[routeIndex % ROUTE_COLORS.length];
            
            // Draw colored line
            turtle.penColor(routeColor);
            turtle.width(3);
            turtle.up();
            turtle.setPosition(legendX, legendY + 3);
            turtle.down();
            turtle.setPosition(legendX + 20, legendY + 3);
            
            // Write route info
            turtle.up();
            turtle.setPosition(legendX + 25, legendY);
            turtle.penColor("black");
            String routeInfo = String.format("Route %d: Demand=%d, Dist=%.1f", 
                                            routeIndex + 1, 
                                            route.totalDemand, 
                                            route.totalDistance);
            //turtle.write(routeInfo, "left", "Arial", 10);
            
            routeIndex++;
        }
    }
}
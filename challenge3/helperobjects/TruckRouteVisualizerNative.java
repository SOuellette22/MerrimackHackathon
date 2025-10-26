package helperobjects;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Native Java Swing/AWT visualization of truck routes
 */
public class TruckRouteVisualizerNative extends JPanel {
    
    private HashMap<Integer, Location> locations;
    private List<ClarkeWrightAlgorithm.Route> routes;
    private double minX, maxX, minY, maxY;
    private double scale;
    private static final Color[] ROUTE_COLORS = {
        Color.RED, Color.BLUE, Color.GREEN, Color.MAGENTA, 
        Color.ORANGE, Color.CYAN, Color.PINK, Color.YELLOW
    };
    
    public TruckRouteVisualizerNative(HashMap<Integer, Location> locations, 
                                      List<ClarkeWrightAlgorithm.Route> routes) {
        this.locations = locations;
        this.routes = routes;
        calculateBounds();
        setPreferredSize(new Dimension(900, 900));
        setBackground(Color.WHITE);
    }
    
    /**
     * Calculate bounds and scale
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
        double padX = (maxX - minX) * 0.2;
        double padY = (maxY - minY) * 0.2;
        minX -= padX;
        maxX += padX;
        minY -= padY;
        maxY += padY;
        
        // Calculate scale
        double scaleX = 700.0 / (maxX - minX);
        double scaleY = 700.0 / (maxY - minY);
        scale = Math.min(scaleX, scaleY);
    }
    
    /**
     * Convert location coordinates to screen coordinates
     */
    private int screenX(double x) {
        return (int)((x - minX) * scale + 100);
    }
    
    private int screenY(double y) {
        // Flip Y axis for normal coordinate system
        return (int)(800 - ((y - minY) * scale + 100));
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        
        // Enable antialiasing for smoother graphics
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                            RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, 
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        drawGrid(g2d);
        drawRoutes(g2d);
        drawLocations(g2d);
        drawLegend(g2d);
        drawTitle(g2d);
    }
    
    /**
     * Draw grid
     */
    private void drawGrid(Graphics2D g2d) {
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.setStroke(new BasicStroke(1));
        
        // Vertical lines
        for (int x = 0; x <= 900; x += 100) {
            g2d.drawLine(x, 0, x, 900);
        }
        
        // Horizontal lines
        for (int y = 0; y <= 900; y += 100) {
            g2d.drawLine(0, y, 900, y);
        }
    }
    
    /**
     * Draw all routes with load information
     */
    private void drawRoutes(Graphics2D g2d) {
        for (int routeIndex = 0; routeIndex < routes.size(); routeIndex++) {
            ClarkeWrightAlgorithm.Route route = routes.get(routeIndex);
            Color routeColor = ROUTE_COLORS[routeIndex % ROUTE_COLORS.length];
            drawSingleRoute(g2d, route, routeColor, routeIndex + 1);
        }
    }
    
    /**
     * Draw a single route
     */
    private void drawSingleRoute(Graphics2D g2d, ClarkeWrightAlgorithm.Route route, 
                                 Color color, int routeNum) {
        LinkedList<Integer> path = route.path;
        int truckCapacity = 100;
        int currentLoad = truckCapacity;
        
        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(3));
        
        for (int i = 1; i < path.size(); i++) {
            Location fromLoc = locations.get(path.get(i-1));
            Location toLoc = locations.get(path.get(i));
            
            int x1 = screenX(fromLoc.getLocation().getFirst());
            int y1 = screenY(fromLoc.getLocation().getSecond());
            int x2 = screenX(toLoc.getLocation().getFirst());
            int y2 = screenY(toLoc.getLocation().getSecond());
            
            // Update load
            if (path.get(i-1) != 0) {
                currentLoad -= fromLoc.getDemand();
            }
            
            // Draw line
            g2d.setColor(color);
            g2d.drawLine(x1, y1, x2, y2);
            
            // Draw arrow
            drawArrow(g2d, x1, y1, x2, y2, color);
            
            // Draw load information
            drawLoadInfo(g2d, x1, y1, x2, y2, currentLoad, truckCapacity, color);
            
            // Reset load at depot
            if (path.get(i) == 0) {
                currentLoad = truckCapacity;
            }
        }
    }
    
    /**
     * Draw arrow head
     */
    private void drawArrow(Graphics2D g2d, int x1, int y1, int x2, int y2, Color color) {
        double angle = Math.atan2(y2 - y1, x2 - x1);
        int arrowLength = 12;
        double arrowAngle = Math.PI / 6;
        
        int arrowX = (int)(x2 - 20 * Math.cos(angle));
        int arrowY = (int)(y2 - 20 * Math.sin(angle));
        
        g2d.setColor(color);
        
        int[] xPoints = new int[3];
        int[] yPoints = new int[3];
        
        xPoints[0] = arrowX;
        yPoints[0] = arrowY;
        
        xPoints[1] = (int)(arrowX - arrowLength * Math.cos(angle - arrowAngle));
        yPoints[1] = (int)(arrowY - arrowLength * Math.sin(angle - arrowAngle));
        
        xPoints[2] = (int)(arrowX - arrowLength * Math.cos(angle + arrowAngle));
        yPoints[2] = (int)(arrowY - arrowLength * Math.sin(angle + arrowAngle));
        
        g2d.fillPolygon(xPoints, yPoints, 3);
    }
    
    /**
     * Draw load information on edge
     */
    private void drawLoadInfo(Graphics2D g2d, int x1, int y1, int x2, int y2, 
                             int load, int capacity, Color routeColor) {
        // Calculate midpoint
        int midX = (x1 + x2) / 2;
        int midY = (y1 + y2) / 2;
        
        // Offset perpendicular to line for visibility
        double angle = Math.atan2(y2 - y1, x2 - x1);
        int offsetX = (int)(15 * Math.sin(angle));
        int offsetY = (int)(-15 * Math.cos(angle));
        
        midX += offsetX;
        midY += offsetY;
        
        // Draw white background circle
        g2d.setColor(Color.WHITE);
        g2d.fillOval(midX - 20, midY - 12, 40, 24);
        
        // Draw colored border
        g2d.setColor(routeColor);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawOval(midX - 20, midY - 12, 40, 24);
        
        // Draw load text
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        String loadText = load + "/" + capacity;
        
        // Center the text
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(loadText);
        int textHeight = fm.getHeight();
        
        g2d.drawString(loadText, midX - textWidth/2, midY + textHeight/4);
    }
    
    /**
     * Draw all locations
     */
    private void drawLocations(Graphics2D g2d) {
        for (Map.Entry<Integer, Location> entry : locations.entrySet()) {
            int id = entry.getKey();
            Location loc = entry.getValue();
            int x = screenX(loc.getLocation().getFirst());
            int y = screenY(loc.getLocation().getSecond());
            
            if (id == 0) {
                drawDepot(g2d, x, y);
            } else {
                drawLocation(g2d, x, y, id, loc.getDemand());
            }
        }
    }
    
    /**
     * Draw depot
     */
    private void drawDepot(Graphics2D g2d, int x, int y) {
        // Draw gold square
        g2d.setColor(new Color(255, 215, 0)); // Gold
        g2d.fillRect(x - 15, y - 15, 30, 30);
        
        // Draw black border
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(3));
        g2d.drawRect(x - 15, y - 15, 30, 30);
        
        // Draw label
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString("DEPOT", x - 25, y + 35);
    }
    
    /**
     * Draw regular location
     */
    private void drawLocation(Graphics2D g2d, int x, int y, int id, int demand) {
        // Draw light blue circle
        g2d.setColor(new Color(173, 216, 230)); // Light blue
        g2d.fillOval(x - 12, y - 12, 24, 24);
        
        // Draw black border
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawOval(x - 12, y - 12, 24, 24);
        
        // Draw ID
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        String idStr = String.valueOf(id);
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(idStr);
        g2d.drawString(idStr, x - textWidth/2, y + 5);
        
        // Draw demand below
        g2d.setFont(new Font("Arial", Font.PLAIN, 11));
        String demandStr = "D:" + demand;
        textWidth = fm.stringWidth(demandStr);
        g2d.drawString(demandStr, x - textWidth/2, y + 30);
    }
    
    /**
     * Draw legend
     */
    private void drawLegend(Graphics2D g2d) {
        int legendX = 30;
        int legendY = 50;
        
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString("ROUTES:", legendX, legendY);
        
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        
        for (int i = 0; i < routes.size(); i++) {
            ClarkeWrightAlgorithm.Route route = routes.get(i);
            legendY += 25;
            Color routeColor = ROUTE_COLORS[i % ROUTE_COLORS.length];
            
            // Draw colored line
            g2d.setColor(routeColor);
            g2d.setStroke(new BasicStroke(3));
            g2d.drawLine(legendX, legendY - 5, legendX + 30, legendY - 5);
            
            // Draw route info
            g2d.setColor(Color.BLACK);
            String info = String.format("Route %d: Demand=%d, Distance=%.1f", 
                                       i + 1, route.totalDemand, route.totalDistance);
            g2d.drawString(info, legendX + 40, legendY);
        }
    }
    
    /**
     * Draw title
     */
    private void drawTitle(Graphics2D g2d) {
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        String title = "Clarke-Wright Savings Algorithm - Vehicle Routing";
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(title);
        g2d.drawString(title, 450 - textWidth/2, 30);
        
        g2d.setFont(new Font("Arial", Font.PLAIN, 14));
        String subtitle = "Routes: " + routes.size() + " | Locations: " + (locations.size() - 1);
        fm = g2d.getFontMetrics();
        textWidth = fm.stringWidth(subtitle);
        g2d.drawString(subtitle, 450 - textWidth/2, 50);
    }
    
    /**
     * Launch the visualization in a JFrame
     */
    public static void visualize(HashMap<Integer, Location> locations, 
                                List<ClarkeWrightAlgorithm.Route> routes) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Vehicle Routing Visualization");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            
            TruckRouteVisualizerNative panel = new TruckRouteVisualizerNative(locations, routes);
            frame.add(panel);
            
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
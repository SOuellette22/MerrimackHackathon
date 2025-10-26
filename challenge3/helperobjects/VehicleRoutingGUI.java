package helperobjects;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import merrimackutil.json.JsonIO;
import merrimackutil.json.types.JSONArray;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

/**
 * GUI Application for Clarke-Wright Vehicle Routing Algorithm
 */
public class VehicleRoutingGUI extends JFrame {
    
    private JTextArea outputArea;
    private JButton selectFileButton;
    private JButton runAlgorithmButton;
    private JButton visualizeButton;
    private JLabel fileLabel;
    private JLabel statusLabel;
    
    private File selectedFile;
    private HashMap<Integer, Location> locations;
    private Map<Integer, Map<Integer, Double>> roads;
    private LinkedList<Truck> trucks;
    private ClarkeWrightAlgorithm.Route[] finalRoutes;
    
    public VehicleRoutingGUI() {
        initializeGUI();
    }
    
    private void initializeGUI() {
        setTitle("Vehicle Routing - Clarke-Wright Algorithm");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        // Create main panels
        JPanel topPanel = createTopPanel();
        JPanel centerPanel = createCenterPanel();
        JPanel bottomPanel = createBottomPanel();
        
        // Add panels to frame
        add(topPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
        
        // Set frame properties
        setSize(800, 600);
        setLocationRelativeTo(null);
        
        // Initially disable algorithm and visualize buttons
        runAlgorithmButton.setEnabled(false);
        visualizeButton.setEnabled(false);
    }
    
    private JPanel createTopPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Title
        JLabel titleLabel = new JLabel("Clarke-Wright Vehicle Routing Algorithm", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        panel.add(titleLabel, BorderLayout.NORTH);
        
        // File selection panel
        JPanel filePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        selectFileButton = new JButton("Select JSON File");
        selectFileButton.setFont(new Font("Arial", Font.PLAIN, 14));
        selectFileButton.addActionListener(e -> selectFile());
        
        fileLabel = new JLabel("No file selected");
        fileLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        
        filePanel.add(selectFileButton);
        filePanel.add(Box.createHorizontalStrut(10));
        filePanel.add(fileLabel);
        
        panel.add(filePanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        
        // Output text area with scroll
        outputArea = new JTextArea();
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        outputArea.setEditable(false);
        outputArea.setText("Welcome to Vehicle Routing Algorithm!\n\n" +
                          "Instructions:\n" +
                          "1. Click 'Select JSON File' to choose your dataset\n" +
                          "2. Click 'Run Algorithm' to execute Clarke-Wright algorithm\n" +
                          "3. Click 'Visualize Routes' to see the graphical representation\n\n" +
                          "Ready to begin...\n");
        
        JScrollPane scrollPane = new JScrollPane(outputArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Algorithm Output"));
        
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        
        runAlgorithmButton = new JButton("Run Algorithm");
        runAlgorithmButton.setFont(new Font("Arial", Font.BOLD, 14));
        runAlgorithmButton.setPreferredSize(new Dimension(150, 35));
        runAlgorithmButton.addActionListener(e -> runAlgorithm());
        
        visualizeButton = new JButton("Visualize Routes");
        visualizeButton.setFont(new Font("Arial", Font.BOLD, 14));
        visualizeButton.setPreferredSize(new Dimension(150, 35));
        visualizeButton.addActionListener(e -> visualizeRoutes());
        
        JButton clearButton = new JButton("Clear Output");
        clearButton.setFont(new Font("Arial", Font.PLAIN, 14));
        clearButton.setPreferredSize(new Dimension(150, 35));
        clearButton.addActionListener(e -> clearOutput());
        
        buttonPanel.add(runAlgorithmButton);
        buttonPanel.add(visualizeButton);
        buttonPanel.add(clearButton);
        
        panel.add(buttonPanel, BorderLayout.CENTER);
        
        // Status bar
        statusLabel = new JLabel("Ready");
        statusLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.GRAY),
            BorderFactory.createEmptyBorder(2, 5, 2, 5)
        ));
        panel.add(statusLabel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private void selectFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Vehicle Routing Dataset (JSON)");
        fileChooser.setFileFilter(new FileNameExtensionFilter("JSON Files", "json"));
        
        // Set initial directory to current directory or project folder
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
        
        int result = fileChooser.showOpenDialog(this);
        
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedFile = fileChooser.getSelectedFile();
            fileLabel.setText(selectedFile.getName());
            runAlgorithmButton.setEnabled(true);
            visualizeButton.setEnabled(false);
            statusLabel.setText("File selected: " + selectedFile.getName());
            
            outputArea.append("\n\nFile selected: " + selectedFile.getAbsolutePath() + "\n");
            outputArea.append("Ready to run algorithm...\n");
        }
    }
    
    private void runAlgorithm() {
        if (selectedFile == null) {
            JOptionPane.showMessageDialog(this, "Please select a JSON file first!", 
                                        "No File Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        outputArea.append("\n" + "=".repeat(60) + "\n");
        outputArea.append("Running Clarke-Wright Algorithm...\n");
        outputArea.append("=".repeat(60) + "\n\n");
        
        try {
            // Clear previous data
            locations = new HashMap<>();
            roads = null;
            trucks = null;
            
            // Load and process the JSON file
            JSONType jsonData = JsonIO.readObject(selectedFile);
            deserialize(jsonData);
            
            outputArea.append("Data loaded successfully!\n");
            outputArea.append("Locations processed: " + locations.size() + "\n");
            outputArea.append("Roads processed: " + (roads != null ? roads.size() : 0) + "\n");
            outputArea.append("Number of trucks: " + (trucks != null ? trucks.size() : 0) + "\n\n");
            
            // Run the algorithm
            ClarkeWrightAlgorithm algorithm = new ClarkeWrightAlgorithm(locations, roads, trucks);
            
            // Redirect algorithm output to our text area
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            java.io.PrintStream ps = new java.io.PrintStream(baos);
            java.io.PrintStream old = System.out;
            System.setOut(ps);
            
            algorithm.execute();
            
            System.out.flush();
            System.setOut(old);
            
            // Add algorithm output to text area
            outputArea.append(baos.toString());
            
            // Store routes for visualization
            finalRoutes = algorithm.getRoutes().toArray(new ClarkeWrightAlgorithm.Route[0]);
            
            visualizeButton.setEnabled(true);
            statusLabel.setText("Algorithm completed successfully!");
            
            // Auto-scroll to bottom
            outputArea.setCaretPosition(outputArea.getDocument().getLength());
            
        } catch (Exception e) {
            outputArea.append("\nERROR: " + e.getMessage() + "\n");
            e.printStackTrace();
            statusLabel.setText("Error occurred during algorithm execution");
            JOptionPane.showMessageDialog(this, 
                "Error running algorithm:\n" + e.getMessage(), 
                "Algorithm Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void visualizeRoutes() {
        if (finalRoutes == null || locations == null) {
            JOptionPane.showMessageDialog(this, 
                "Please run the algorithm first!", 
                "No Routes Available", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        outputArea.append("\nLaunching visualization window...\n");
        statusLabel.setText("Visualization launched");
        
        // Convert array back to list for the visualizer
        java.util.List<ClarkeWrightAlgorithm.Route> routesList = 
            java.util.Arrays.asList(finalRoutes);
        
        // Launch the visualization
        TruckRouteVisualizerNative.visualize(locations, routesList);
    }
    
    private void clearOutput() {
        outputArea.setText("Output cleared.\n\n");
        statusLabel.setText("Ready");
    }
    
    private void deserialize(JSONType type) throws Exception {
        if (!(type instanceof JSONObject)) {
            throw new IllegalArgumentException("Expected a JSONObject");
        }
        
        JSONObject object = (JSONObject) type;
        
        // Process the JSON data
        processLocations(object.getArray("locations"));
        processRoads(object.getArray("roads"));
        processMeta(object.getObject("meta"));
    }
    
    private void processMeta(JSONObject object) throws Exception {
        int numTrucks = object.getInt("trucks");
        int truckCapacity = object.getInt("truck_capacity");
        
        trucks = new LinkedList<>();
        Location depot = locations.get(0);
        
        for (int i = 0; i < numTrucks; i++) {
            trucks.add(new Truck(i, truckCapacity, depot));
        }
    }
    
    private void processLocations(JSONArray l) throws Exception {
        HashMap<Integer, Location> locationsMap = new HashMap<>();
        
        for (int i = 0; i < l.size(); i++) {
            JSONObject location = (JSONObject) l.get(i);
            
            int id = location.getInt("id");
            String name = location.getString("name");
            double longitude = location.getDouble("longitude") * 10;
            double latitude = location.getDouble("latitude") * 10;
            int demand = location.getInt("demand");
            
            if (name.equals("Depot")) {
                locationsMap.put(id, new Location(longitude, latitude));
            } else {
                locationsMap.put(id, new Location(longitude, latitude, demand));
            }
        }
        locations = locationsMap;
    }
    
    private void processRoads(JSONArray r) throws Exception {
        roads = new HashMap<>();
        
        for (int i = 0; i < r.size(); i++) {
            JSONObject road = r.getObject(i);
            
            int fromId = road.getInt("from_id");
            int toId = road.getInt("to_id");
            int timeTraveled = road.getInt("travel_time_minutes");
            
            if (!roads.containsKey(fromId)) {
                roads.put(fromId, new HashMap<>());
            }
            roads.get(fromId).put(toId, (double) timeTraveled);
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            VehicleRoutingGUI gui = new VehicleRoutingGUI();
            gui.setVisible(true);
        });
    }
}
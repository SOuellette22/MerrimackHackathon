import helperobjects.*;
import merrimackutil.json.InvalidJSONException;
import merrimackutil.json.JsonIO;
import merrimackutil.json.types.JSONArray;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InvalidObjectException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class Main {

    private static HashMap<Integer, Location> locations;
    private static Map<Integer, Map<Integer, Double>> roads;
    private static LinkedList<Truck> trucks;

    public static void main(String[] args) throws InvalidJSONException, FileNotFoundException, InvalidObjectException {
        locations = new HashMap<>();

        deserialize(JsonIO.readObject(new File("challenge3/testing/test5.json")));

        // Execute Clarke-Wright Algorithm
        ClarkeWrightAlgorithm algorithm = new ClarkeWrightAlgorithm(locations, roads, trucks);
        algorithm.execute();

        // Visualize the results
        TruckRouteVisualizer visualizer = new TruckRouteVisualizer(locations, algorithm.getRoutes());
        visualizer.visualize();

    }

    public static void deserialize(JSONType type) throws InvalidObjectException {
        if (!(type instanceof JSONObject)) {
            throw new IllegalArgumentException("Expected a JSONObject");
        }

        JSONObject object = (JSONObject) type;

        String[] keys = {"meta", "locations", "roads", "blocked"};
        object.checkValidity(keys);


        processLocations(object.getArray("locations"));
        processRoads(object.getArray("roads"));
        processMeta(object.getObject("meta"));
    }

    public static void processMeta(JSONObject object) throws InvalidObjectException {
        String[] metaKeys = {"trucks", "truck_capacity"};
        object.checkValidity(metaKeys);

        int numTrucks = object.getInt("trucks");
        int truckCapacity = object.getInt("truck_capacity");

        trucks = new LinkedList<>();
        Location depot = locations.get(0);

        for (int i = 0; i < numTrucks; i++) {
            trucks.add(new Truck(i, truckCapacity, depot));
        }

    }

    public static void processLocations(JSONArray l) throws InvalidObjectException {
        HashMap<Integer, Location> locationsMap = new HashMap<>();
        for (int i = 0; i < l.size(); i++) {
            JSONObject location = (JSONObject) l.get(i);

            String[] locationKeys = {"id", "name",  "longitude", "latitude", "demand"};
            location.checkValidity(locationKeys);

            int id = location.getInt("id");
            String name = location.getString("name");
            double longitude = location.getDouble("longitude") * 100;
            double latitude = location.getDouble("latitude") * 100;
            int demand = location.getInt("demand");

            if (name.equals("Depot")) {
                locationsMap.put(id, new Location(longitude, latitude));
            } else {
                locationsMap.put(id, new Location(longitude, latitude, demand));
            }
        }
        locations = locationsMap;
    }

    public static void processRoads(JSONArray r) throws InvalidObjectException {
        roads = new HashMap<>();
        for (int i = 0; i < r.size(); i++) {
            JSONObject road = r.getObject(i);

            String[] roadKeys = {"from_id", "to_id", "travel_time_minutes"};
            road.checkValidity(roadKeys);

            int fromId = road.getInt("from_id");
            int toId = road.getInt("to_id");
            int timeTraveled = road.getInt("travel_time_minutes");

            if (!roads.containsKey(fromId)) {
                roads.put(fromId, new HashMap<>());
                roads.get(fromId).put(toId, (double) timeTraveled);
            } else {
                roads.get(fromId).put(toId, (double) timeTraveled);
            }
        }
    }
}

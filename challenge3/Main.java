import helperobjects.Location;
import helperobjects.Turtle;
import merrimackutil.json.InvalidJSONException;
import merrimackutil.json.JsonIO;
import merrimackutil.json.types.JSONArray;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;
import merrimackutil.util.Tuple;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InvalidObjectException;
import java.util.HashMap;

public class Main {

    private static HashMap<Integer, Location> locations;

    public static void main(String[] args) throws InvalidJSONException, FileNotFoundException, InvalidObjectException {
        locations = new HashMap<>();
        Tuple<JSONArray, JSONArray> arrays = deserialize(JsonIO.readObject(new File("challenge3/truck_routedataset.json")));

        locations = processLocations(arrays.getFirst());
        processRoads(arrays.getSecond());

        System.out.println("Done!");
        System.out.println("Locations processed: " + locations.size());
    }

    public static Tuple<JSONArray, JSONArray> deserialize(JSONType type) throws InvalidObjectException {
        if (!(type instanceof JSONObject)) {
            throw new IllegalArgumentException("Expected a JSONObject");
        }

        JSONObject object = (JSONObject) type;

        String[] keys = {"meta", "locations", "roads", "blocked"};
        object.checkValidity(keys);

        JSONArray locations = object.getArray("locations");
        JSONArray roads = object.getArray("roads");

        return  new Tuple<>(locations, roads);
    }

    public static HashMap<Integer, Location> processLocations(JSONArray l) throws InvalidObjectException {
        HashMap<Integer, Location> locations = new HashMap<>();
        for (int i = 0; i < l.size(); i++) {
            JSONObject location = (JSONObject) l.get(i);

            String[] locationKeys = {"id", "name",  "longitude", "latitude", "demand"};
            location.checkValidity(locationKeys);

            int id = location.getInt("id");
            String name = location.getString("name");
            double longitude = location.getDouble("longitude");
            double latitude = location.getDouble("latitude");
            int demand = location.getInt("demand");

            if (name.equals("Depot")) {
                locations.put(id, new Location(longitude, latitude));
            } else {
                locations.put(id, new Location(longitude, latitude, demand));
            }
        }
        return locations;
    }

    public static void processRoads(JSONArray r) throws InvalidObjectException {
        for (int i = 0; i < r.size(); i++) {
            JSONObject road = r.getObject(i);

            String[] roadKeys = {"from_id", "to_id", "travel_time_minutes"};
            road.checkValidity(roadKeys);

            int fromId = road.getInt("from_id");
            int toId = road.getInt("to_id");
            int timeTraveled = road.getInt("travel_time_minutes");

            Location fromLocation = locations.get(fromId);
            Location toLocation = locations.get(toId);

            if (fromLocation == null || toLocation == null) {
                throw new InvalidObjectException("Invalid location ID in roads");
            }

            // Assuming undirected roads
            fromLocation.addRoad(toLocation, timeTraveled);
        }
    }
}

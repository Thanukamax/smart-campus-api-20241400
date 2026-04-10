package com.smartcampus.storage;

import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataStore {

    private static DataStore instance = new DataStore();

    private Map<String, Room> rooms = new HashMap<>();
    private Map<String, Sensor> sensors = new HashMap<>();
    private Map<String, List<SensorReading>> readings = new HashMap<>();

    private DataStore() {}

    public static DataStore getInstance() {
        return instance;
    }

    // Room methods
    public Map<String, Room> getRooms() { return rooms; }
    public Room getRoom(String id) { return rooms.get(id); }
    public void addRoom(Room room) { rooms.put(room.getId(), room); }
    public Room removeRoom(String id) { return rooms.remove(id); }

    // Sensor methods
    public Map<String, Sensor> getSensors() { return sensors; }
    public Sensor getSensor(String id) { return sensors.get(id); }
    public void addSensor(Sensor sensor) { sensors.put(sensor.getId(), sensor); }
    public Sensor removeSensor(String id) { return sensors.remove(id); }

    // Reading methods
    public List<SensorReading> getReadings(String sensorId) {
        return readings.getOrDefault(sensorId, new ArrayList<>());
    }

    public void addReading(String sensorId, SensorReading reading) {
        if (!readings.containsKey(sensorId)) {
            readings.put(sensorId, new ArrayList<>());
        }
        readings.get(sensorId).add(reading);
    }
}

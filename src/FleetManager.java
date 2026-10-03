import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.TreeMap;

public class FleetManager implements Dispatchable {
    private ArrayList<Driver> drivers = new ArrayList<Driver>();
    private ArrayList<Trip> trips = new ArrayList<Trip>();
    private LinkedList<Trip> history = new LinkedList<Trip>();
    private HashMap<Integer, Trip> tripMap = new HashMap<Integer, Trip>();
    private int nextDriverId = 1;
    private int nextTripId = 1;

    public synchronized Driver addDriver(String name, String phone, String vehicleNumber) {
        validateDriver(name, phone, vehicleNumber, 0);
        Vehicle vehicle = new Vehicle(vehicleNumber.trim().toUpperCase(Locale.ROOT));
        Driver driver = new Driver(nextDriverId, name.trim(), phone.trim(), vehicle);
        drivers.add(driver);
        nextDriverId++;
        return driver;
    }

    public synchronized void updateDriver(int id, String name, String phone, String vehicleNumber) {
        Driver driver = requireDriver(id);
        checkDriverNotBusy(driver);
        validateDriver(name, phone, vehicleNumber, id);
        driver.name = name.trim();
        driver.phone = phone.trim();
        driver.vehicle.number = vehicleNumber.trim().toUpperCase(Locale.ROOT);
    }

    public synchronized void deleteDriver(int id) {
        Driver driver = requireDriver(id);
        checkDriverNotBusy(driver);
        drivers.remove(driver);
    }

    public synchronized void setDriverAvailable(int id, boolean available) {
        Driver driver = requireDriver(id);
        checkDriverNotBusy(driver);
        driver.available = available;
        driver.vehicle.available = available;
    }

    public synchronized Trip addTrip(String passenger, String pickup, String destination, double distance, int duration) {
        validateTrip(passenger, pickup, destination, distance, duration);
        Trip trip = new Trip(nextTripId, passenger.trim(), pickup.trim(), destination.trim(), distance, duration);
        trips.add(trip);
        tripMap.put(trip.id, trip);
        nextTripId++;
        return trip;
    }

    public synchronized void updateTrip(int id, String passenger, String pickup, String destination, double distance, int duration) {
        Trip trip = requireTrip(id);
        if (!trip.status.equals("Requested")) {
            throw new IllegalArgumentException("Only requested trips can be edited.");
        }
        validateTrip(passenger, pickup, destination, distance, duration);
        trip.passenger = passenger.trim();
        trip.pickup = pickup.trim();
        trip.destination = destination.trim();
        trip.distance = distance;
        trip.duration = duration;
        trip.fare = Fare.calculate(distance);
    }

    public synchronized void deleteTrip(int id) {
        Trip trip = requireTrip(id);
        if (trip.status.equals("Ongoing")) {
            throw new IllegalArgumentException("Complete or cancel the ongoing trip before deleting it.");
        }
        trips.remove(trip);
        tripMap.remove(id);
        history.remove(trip);
    }

    public synchronized void dispatchTrip(int tripId) {
        Trip trip = requireTrip(tripId);
        if (!trip.status.equals("Requested")) {
            throw new IllegalArgumentException("Only requested trips can be dispatched.");
        }
        for (Driver driver : drivers) {
            if (driver.available && driver.vehicle.available) {
                trip.driver = driver;
                trip.status = "Ongoing";
                driver.available = false;
                driver.vehicle.available = false;
                return;
            }
        }
        throw new IllegalArgumentException("No driver and vehicle are available. Request kept for later.");
    }

    public synchronized void completeTrip(int id) {
        Trip trip = requireTrip(id);
        if (!trip.status.equals("Ongoing")) {
            throw new IllegalArgumentException("Only ongoing trips can be completed.");
        }
        trip.status = "Completed";
        releaseDriver(trip);
        history.add(trip);
    }

    public synchronized void cancelTrip(int id) {
        Trip trip = requireTrip(id);
        if (!trip.status.equals("Requested") && !trip.status.equals("Ongoing")) {
            throw new IllegalArgumentException("Only requested or ongoing trips can be cancelled.");
        }
        if (trip.status.equals("Ongoing")) {
            releaseDriver(trip);
        }
        trip.status = "Cancelled";
    }

    private void releaseDriver(Trip trip) {
        trip.driver.available = true;
        trip.driver.vehicle.available = true;
    }

    public synchronized Trip findTrip(int id) {
        return tripMap.get(id);
    }

    public synchronized ArrayList<Driver> getDrivers() {
        return new ArrayList<Driver>(drivers);
    }

    public synchronized ArrayList<Trip> getTrips() {
        return new ArrayList<Trip>(trips);
    }

    public synchronized LinkedList<Trip> getHistory() {
        return new LinkedList<Trip>(history);
    }

    public synchronized ArrayList<Driver> searchDrivers(String query) {
        ArrayList<Driver> result = new ArrayList<Driver>();
        String search = query.trim().toLowerCase(Locale.ROOT);
        for (Driver driver : drivers) {
            String details = driver.id + " " + driver.name + " " + driver.phone + " " + driver.vehicle.number;
            if (details.toLowerCase(Locale.ROOT).contains(search)) {
                result.add(driver);
            }
        }
        return result;
    }

    public synchronized ArrayList<Trip> searchTrips(String query) {
        ArrayList<Trip> result = new ArrayList<Trip>();
        String search = query.trim().toLowerCase(Locale.ROOT);
        for (Trip trip : trips) {
            String details = trip.id + " " + trip.passenger + " " + trip.pickup + " " + trip.destination + " " + trip.status;
            if (trip.driver != null) {
                details += " " + trip.driver.id + " " + trip.driver.name;
            }
            if (details.toLowerCase(Locale.ROOT).contains(search)) {
                result.add(trip);
            }
        }
        return result;
    }

    public synchronized ArrayList<Trip> sortedTrips(String sortBy) {
        ArrayList<Trip> result = new ArrayList<Trip>(trips);
        if (sortBy.equals("Fare") || sortBy.equals("Distance")) {
            TreeMap<Double, ArrayList<Trip>> sorted = new TreeMap<Double, ArrayList<Trip>>();
            for (Trip trip : trips) {
                double key = trip.fare;
                if (sortBy.equals("Distance")) {
                    key = trip.distance;
                }
                if (!sorted.containsKey(key)) {
                    sorted.put(key, new ArrayList<Trip>());
                }
                sorted.get(key).add(trip);
            }
            result.clear();
            for (ArrayList<Trip> group : sorted.values()) {
                result.addAll(group);
            }
        } else if (sortBy.equals("Duration")) {
            for (int i = 0; i < result.size() - 1; i++) {
                for (int j = 0; j < result.size() - i - 1; j++) {
                    if (result.get(j).duration > result.get(j + 1).duration) {
                        Trip temp = result.get(j);
                        result.set(j, result.get(j + 1));
                        result.set(j + 1, temp);
                    }
                }
            }
        }
        return result;
    }

    public synchronized String getReport() {
        int available = 0;
        int requested = 0;
        int ongoing = 0;
        int cancelled = 0;
        double revenue = 0;
        double ongoingFare = 0;
        for (Driver driver : drivers) {
            if (driver.available && driver.vehicle.available) {
                available++;
            }
        }
        for (Trip trip : trips) {
            if (trip.status.equals("Requested")) requested++;
            if (trip.status.equals("Cancelled")) cancelled++;
            if (trip.status.equals("Ongoing")) {
                ongoing++;
                ongoingFare += trip.fare;
            }
        }
        for (Trip trip : history) {
            revenue += trip.fare;
        }
        String report = "FLEET REPORT\n\n";
        report += "Drivers / vehicles: " + drivers.size() + "\n";
        report += "Available driver-vehicle pairs: " + available + "\n";
        report += "Unavailable pairs (busy or off duty): " + (drivers.size() - available) + "\n\n";
        report += "Total trip records: " + trips.size() + "\n";
        report += "Requested: " + requested + "\nOngoing: " + ongoing;
        report += "\nCompleted: " + history.size() + "\nCancelled: " + cancelled + "\n\n";
        report += String.format(Locale.US, "Completed revenue: Rs. %.2f\nOngoing fares: Rs. %.2f\n", revenue, ongoingFare);
        report += "\nCOMPLETED TRIP HISTORY (completion order)\n";
        if (history.isEmpty()) {
            report += "No completed trips yet.\n";
        }
        for (Trip trip : history) {
            report += String.format(Locale.US, "Trip %d | %s | Driver %d | Rs. %.2f\n", trip.id, trip.passenger, trip.driver.id, trip.fare);
        }
        return report;
    }

    private Driver requireDriver(int id) {
        for (Driver driver : drivers) {
            if (driver.id == id) return driver;
        }
        throw new IllegalArgumentException("Driver ID not found.");
    }

    private Trip requireTrip(int id) {
        Trip trip = tripMap.get(id);
        if (trip == null) {
            throw new IllegalArgumentException("Trip ID not found.");
        }
        return trip;
    }

    private void checkDriverNotBusy(Driver driver) {
        for (Trip trip : trips) {
            if (trip.driver == driver && trip.status.equals("Ongoing")) {
                throw new IllegalArgumentException("This driver has an ongoing trip.");
            }
        }
    }

    private void validateDriver(String name, String phone, String vehicleNumber, int editingId) {
        requireText(name, "Driver name");
        requireText(phone, "Phone");
        requireText(vehicleNumber, "Vehicle number");
        if (!phone.trim().matches("[0-9]{10}")) {
            throw new IllegalArgumentException("Phone must contain exactly 10 digits.");
        }
        for (Driver driver : drivers) {
            if (driver.id != editingId && driver.vehicle.number.equalsIgnoreCase(vehicleNumber.trim())) {
                throw new IllegalArgumentException("This vehicle is already registered.");
            }
        }
    }

    private void validateTrip(String passenger, String pickup, String destination, double distance, int duration) {
        requireText(passenger, "Passenger name");
        requireText(pickup, "Pickup");
        requireText(destination, "Destination");
        Fare.calculate(distance);
        if (duration <= 0 || duration > 10080) {
            throw new IllegalArgumentException("Estimated duration must be 1 to 10080 minutes.");
        }
    }

    private void requireText(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " cannot be blank.");
        }
    }
}

public class Trip {
    final int id;
    String passenger;
    String pickup;
    String destination;
    double distance;
    int duration;
    double fare;
    String status;
    Driver driver;

    public Trip(int id, String passenger, String pickup, String destination, double distance, int duration) {
        this.id = id;
        this.passenger = passenger;
        this.pickup = pickup;
        this.destination = destination;
        this.distance = distance;
        this.duration = duration;
        fare = Fare.calculate(distance);
        status = "Requested";
    }
}

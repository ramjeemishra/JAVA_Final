public class Driver {
    final int id;
    String name;
    String phone;
    Vehicle vehicle;
    boolean available;

    public Driver(int id, String name, String phone, Vehicle vehicle) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.vehicle = vehicle;
        available = true;
    }
}

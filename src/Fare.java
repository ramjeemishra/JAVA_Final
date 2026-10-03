public class Fare {
    public static final double BASE_FARE = 50;
    public static final double RATE_PER_KM = 15;

    public static double calculate(double distance) {
        if (Double.isNaN(distance) || Double.isInfinite(distance) || distance <= 0 || distance > 10000) {
            throw new IllegalArgumentException("Distance must be greater than 0 and at most 10000 km.");
        }
        return Math.round((BASE_FARE + distance * RATE_PER_KM) * 100.0) / 100.0;
    }
}

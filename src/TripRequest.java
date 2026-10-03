public class TripRequest implements Runnable {
    private Dispatchable dispatcher;
    private int tripId;
    private String message;

    public TripRequest(Dispatchable dispatcher, int tripId) {
        this.dispatcher = dispatcher;
        this.tripId = tripId;
        message = "Trip " + tripId + ": waiting";
    }

    public void run() {
        try {
            dispatcher.dispatchTrip(tripId);
            message = "Trip " + tripId + ": dispatched";
        } catch (IllegalArgumentException e) {
            message = "Trip " + tripId + ": " + e.getMessage();
        }
    }

    public String getMessage() {
        return message;
    }
}

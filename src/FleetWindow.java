import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Locale;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

public class FleetWindow extends JFrame implements ActionListener {
    private static final long serialVersionUID = 1L;
    private FleetManager manager = new FleetManager();
    private DefaultTableModel driverModel;
    private DefaultTableModel tripModel;
    private JTable driverTable;
    private JTable tripTable;
    private JTextField driverSearch = new JTextField(18);
    private JTextField tripSearch = new JTextField(18);
    private JComboBox<String> sortBox = new JComboBox<String>(new String[] {"ID", "Fare", "Distance", "Duration"});
    private JTextArea reportArea = new JTextArea();
    private JTextArea simulationArea = new JTextArea(5, 50);
    private JButton simulateButton;
    private JLabel message = new JLabel("Ready. Register drivers, then create and dispatch a trip.");

    public FleetWindow() {
        setTitle("Taxi Fleet Dispatch and Trip Management");
        setSize(1100, 700);
        setMinimumSize(new java.awt.Dimension(950, 600));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        JPanel heading = new JPanel(new GridLayout(2, 1));
        JLabel title = new JLabel("  TAXI FLEET DISPATCH");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        heading.add(title);
        heading.add(new JLabel("  Fare: Rs. 50 + Rs. 15/km | Duration is estimated | Data resets when closed"));
        add(heading, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Drivers and Vehicles", createDriverPanel());
        tabs.addTab("Trips and Dispatch", createTripPanel());
        reportArea.setEditable(false);
        reportArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        tabs.addTab("Fleet Reports", new JScrollPane(reportArea));
        add(tabs, BorderLayout.CENTER);
        add(message, BorderLayout.SOUTH);
        refreshTables();
    }

    private DefaultTableModel createModel(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private JButton createButton(String text) {
        JButton button = new JButton(text);
        button.addActionListener(this);
        return button;
    }

    private JPanel createDriverPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel top = new JPanel(new GridLayout(2, 1));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.add(createButton("Add Driver"));
        actions.add(createButton("Edit Driver"));
        actions.add(createButton("Delete Driver"));
        actions.add(createButton("Toggle Availability"));
        actions.add(createButton("Add Sample Fleet"));
        top.add(actions);
        JPanel search = new JPanel(new FlowLayout(FlowLayout.LEFT));
        search.add(new JLabel("ID, name, phone or vehicle:"));
        search.add(driverSearch);
        search.add(createButton("Search Drivers"));
        search.add(createButton("Show All Drivers"));
        top.add(search);
        panel.add(top, BorderLayout.NORTH);
        driverModel = createModel(new String[] {"ID", "Name", "Phone", "Vehicle", "Driver available", "Vehicle available"});
        driverTable = new JTable(driverModel);
        driverTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        driverTable.setRowHeight(26);
        panel.add(new JScrollPane(driverTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createTripPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel top = new JPanel(new GridLayout(3, 1));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.add(createButton("Add Trip"));
        actions.add(createButton("Edit Trip"));
        actions.add(createButton("Delete Trip"));
        actions.add(createButton("Dispatch"));
        actions.add(createButton("Complete"));
        actions.add(createButton("Cancel Trip"));
        top.add(actions);
        JPanel search = new JPanel(new FlowLayout(FlowLayout.LEFT));
        search.add(new JLabel("Trip, passenger, route, status or driver:"));
        search.add(tripSearch);
        search.add(createButton("Search Trips"));
        search.add(createButton("Find Trip ID"));
        search.add(createButton("Show All Trips"));
        top.add(search);
        JPanel sorting = new JPanel(new FlowLayout(FlowLayout.LEFT));
        sorting.add(new JLabel("Sort ascending:"));
        sorting.add(sortBox);
        sortBox.addActionListener(this);
        simulateButton = createButton("Simulate 3 Requests");
        sorting.add(simulateButton);
        sorting.add(new JLabel("Creates three trips and dispatches them using threads."));
        top.add(sorting);
        panel.add(top, BorderLayout.NORTH);
        tripModel = createModel(new String[] {"ID", "Passenger", "Pickup", "Destination", "Km", "Minutes", "Fare (Rs.)", "Status", "Driver"});
        tripTable = new JTable(tripModel);
        tripTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tripTable.setRowHeight(26);
        panel.add(new JScrollPane(tripTable), BorderLayout.CENTER);
        simulationArea.setEditable(false);
        simulationArea.setLineWrap(true);
        simulationArea.setWrapStyleWord(true);
        simulationArea.setText("Simulation results will appear here. Trip duration is entered in minutes; completion is manual.");
        panel.add(new JScrollPane(simulationArea), BorderLayout.SOUTH);
        return panel;
    }

    public void actionPerformed(ActionEvent event) {
        String action = event.getActionCommand();
        try {
            if (action.equals("Add Driver")) editDriver(false);
            else if (action.equals("Edit Driver")) editDriver(true);
            else if (action.equals("Delete Driver")) {
                int id = selectedId(driverTable);
                if (confirm("Delete driver " + id + " and the linked vehicle?")) manager.deleteDriver(id);
            } else if (action.equals("Toggle Availability")) {
                int id = selectedId(driverTable);
                synchronized (manager) {
                    for (Driver driver : manager.getDrivers()) {
                        if (driver.id == id) manager.setDriverAvailable(id, !driver.available);
                    }
                }
            } else if (action.equals("Add Sample Fleet")) addSampleFleet();
            else if (action.equals("Add Trip")) editTrip(false);
            else if (action.equals("Edit Trip")) editTrip(true);
            else if (action.equals("Delete Trip")) {
                int id = selectedId(tripTable);
                if (confirm("Delete trip " + id + "? It will also be removed from reports and history.")) manager.deleteTrip(id);
            } else if (action.equals("Dispatch")) manager.dispatchTrip(selectedId(tripTable));
            else if (action.equals("Complete")) manager.completeTrip(selectedId(tripTable));
            else if (action.equals("Cancel Trip")) manager.cancelTrip(selectedId(tripTable));
            else if (action.equals("Show All Drivers")) driverSearch.setText("");
            else if (action.equals("Show All Trips")) tripSearch.setText("");
            else if (action.equals("Find Trip ID")) {
                findTripById();
                return;
            } else if (action.equals("Simulate 3 Requests")) simulateRequests();
            refreshTables();
            message.setText("Ready. Drivers shown: " + driverModel.getRowCount() + " | Trips shown: " + tripModel.getRowCount());
        } catch (NumberFormatException e) {
            showError("Enter valid numbers for distance, duration and IDs. Duration and IDs must be whole numbers.");
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        }
    }

    private int selectedId(JTable table) {
        int row = table.getSelectedRow();
        if (row == -1) throw new IllegalArgumentException("Select a row in the table first.");
        return (Integer) table.getValueAt(row, 0);
    }

    private boolean confirm(String text) {
        return JOptionPane.showConfirmDialog(this, text, "Confirm deletion", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    private void showError(String text) {
        message.setText(text);
        JOptionPane.showMessageDialog(this, text, "Please check your input", JOptionPane.ERROR_MESSAGE);
    }

    private String[] showForm(String title, String[] labels, String[] values) {
        JPanel form = new JPanel(new GridLayout(labels.length, 2, 8, 8));
        JTextField[] fields = new JTextField[labels.length];
        for (int i = 0; i < labels.length; i++) {
            form.add(new JLabel(labels[i]));
            fields[i] = new JTextField(values[i], 20);
            form.add(fields[i]);
        }
        int choice = JOptionPane.showConfirmDialog(this, form, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) return null;
        String[] result = new String[labels.length];
        for (int i = 0; i < labels.length; i++) result[i] = fields[i].getText().trim();
        return result;
    }

    private void editDriver(boolean editing) {
        int id = 0;
        String[] values = {"", "", ""};
        if (editing) {
            id = selectedId(driverTable);
            for (Driver driver : manager.getDrivers()) {
                if (driver.id == id) values = new String[] {driver.name, driver.phone, driver.vehicle.number};
            }
        }
        String title = "Register driver and vehicle";
        if (editing) title = "Edit driver and vehicle";
        String[] input = showForm(title, new String[] {"Driver name", "Phone (10 digits)", "Vehicle number"}, values);
        if (input == null) return;
        if (editing) manager.updateDriver(id, input[0], input[1], input[2]);
        else manager.addDriver(input[0], input[1], input[2]);
    }

    private void editTrip(boolean editing) {
        int id = 0;
        String[] values = {"", "", "", "", ""};
        if (editing) {
            id = selectedId(tripTable);
            Trip trip = manager.findTrip(id);
            values = new String[] {trip.passenger, trip.pickup, trip.destination, String.valueOf(trip.distance), String.valueOf(trip.duration)};
        }
        String title = "New trip request";
        if (editing) title = "Edit requested trip";
        String[] input = showForm(title, new String[] {"Passenger name", "Pickup", "Destination", "Distance (km)", "Estimated duration (minutes)"}, values);
        if (input == null) return;
        double distance = Double.parseDouble(input[3]);
        int duration = Integer.parseInt(input[4]);
        if (editing) manager.updateTrip(id, input[0], input[1], input[2], distance, duration);
        else manager.addTrip(input[0], input[1], input[2], distance, duration);
    }

    private void addSampleFleet() {
        if (!manager.getDrivers().isEmpty()) {
            throw new IllegalArgumentException("Sample fleet can be added only when the driver list is empty.");
        }
        manager.addDriver("Amit", "9876543210", "MH01AB1234");
        manager.addDriver("Neha", "9876543211", "MH02CD5678");
        manager.addDriver("Ravi", "9876543212", "MH03EF9012");
    }

    private void findTripById() {
        String input = JOptionPane.showInputDialog(this, "Enter exact trip ID:");
        if (input == null) return;
        synchronized (manager) {
            Trip trip = manager.findTrip(Integer.parseInt(input.trim()));
            if (trip == null) throw new IllegalArgumentException("Trip ID not found.");
            String text = "Trip " + trip.id + " | " + trip.passenger + "\n" + trip.pickup + " to " + trip.destination;
            text += "\nStatus: " + trip.status + "\nFare: Rs. " + money(trip.fare);
            JOptionPane.showMessageDialog(this, text, "Trip details", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void simulateRequests() {
        simulateButton.setEnabled(false);
        tripSearch.setText("");
        simulationArea.setText("Dispatching three requests...\n");
        final TripRequest[] requests = new TripRequest[3];
        final Thread[] workers = new Thread[3];
        for (int i = 0; i < 3; i++) {
            Trip trip = manager.addTrip("Demo Passenger " + (i + 1), "Station", "College", 5 + i, 15 + i * 5);
            requests[i] = new TripRequest(manager, trip.id);
            workers[i] = new Thread(requests[i], "Request-" + trip.id);
        }
        Thread coordinator = new Thread(new Runnable() {
            public void run() {
                for (Thread worker : workers) worker.start();
                try {
                    for (Thread worker : workers) worker.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            simulationArea.append("Simulation wait interrupted. Refresh to check trip status.\n");
                            simulateButton.setEnabled(true);
                            refreshTables();
                        }
                    });
                    return;
                }
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        for (TripRequest request : requests) simulationArea.append(request.getMessage() + "\n");
                        simulateButton.setEnabled(true);
                        refreshTables();
                        message.setText("Simulation finished. Complete ongoing trips to free drivers.");
                    }
                });
            }
        }, "Simulation-coordinator");
        coordinator.start();
    }

    private String money(double amount) {
        return String.format(Locale.US, "%.2f", amount);
    }

    private void refreshTables() {
        synchronized (manager) {
            driverModel.setRowCount(0);
            for (Driver driver : manager.searchDrivers(driverSearch.getText())) {
                driverModel.addRow(new Object[] {driver.id, driver.name, driver.phone, driver.vehicle.number, driver.available, driver.vehicle.available});
            }
            tripModel.setRowCount(0);
            ArrayList<Trip> matching = manager.searchTrips(tripSearch.getText());
            for (Trip trip : manager.sortedTrips((String) sortBox.getSelectedItem())) {
                if (matching.contains(trip)) {
                    String driverName = "Unassigned";
                    if (trip.driver != null) driverName = trip.driver.id + " - " + trip.driver.name;
                    tripModel.addRow(new Object[] {trip.id, trip.passenger, trip.pickup, trip.destination, trip.distance, trip.duration, money(trip.fare), trip.status, driverName});
                }
            }
            reportArea.setText(manager.getReport());
            reportArea.setCaretPosition(0);
        }
    }
}

// File: src/main/java/csc478project/restaurantapp/RestaurantTable.java
package csc478project.restaurantapp;

public class RestaurantTable {
    private int tableNumber;
    private int seatingCapacity;
    private boolean isOccupied;
    private String assignedWaiterId;

    // Constructors
    public RestaurantTable() {}

    public RestaurantTable(int tableNumber, int seatingCapacity, boolean isOccupied, String assignedWaiterId) {
        this.tableNumber = tableNumber;
        this.seatingCapacity = seatingCapacity;
        this.isOccupied = isOccupied;
        this.assignedWaiterId = assignedWaiterId;
    }

    // Getters and Setters
    public int getTableNumber() { return tableNumber; }
    public void setTableNumber(int tableNumber) { this.tableNumber = tableNumber; }

    public int getSeatingCapacity() { return seatingCapacity; }
    public void setSeatingCapacity(int seatingCapacity) { this.seatingCapacity = seatingCapacity; }

    public boolean isOccupied() { return isOccupied; }
    public void setOccupied(boolean occupied) { isOccupied = occupied; }

    public String getAssignedWaiterId() { return assignedWaiterId; }
    public void setAssignedWaiterId(String assignedWaiterId) { this.assignedWaiterId = assignedWaiterId; }
}

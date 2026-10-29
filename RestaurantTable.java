package csc478project.restaurantapp;

public class RestaurantTable {
    private int tableId;
    private int capacity;
    private boolean isOccupied;
    private String assignedWaiterId;

    public RestaurantTable() {}

    public RestaurantTable(int tableId, int capacity) {
        this.tableId = tableId;
        this.capacity = capacity;
        this.isOccupied = false;
    }

    public int getTableId() { return tableId; }
    public void setTableId(int tableId) { this.tableId = tableId; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public boolean isOccupied() { return isOccupied; }
    public void setOccupied(boolean occupied) { isOccupied = occupied; }
    public String getAssignedWaiterId() { return assignedWaiterId; }
    public void setAssignedWaiterId(String assignedWaiterId) { this.assignedWaiterId = assignedWaiterId; }
}

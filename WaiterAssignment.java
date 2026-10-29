package csc478project.restaurantapp.model;

import java.util.HashSet;
import java.util.Set;

public class WaiterAssignment {
    private String waiterEmployeeId;
    private String waiterName;
    private Set<Integer> assignedTableIds; // Maps directly to instances of RestaurantTable
    private double shiftTipsEarned;

    public WaiterAssignment() {
        this.assignedTableIds = new HashSet<>();
    }

    public WaiterAssignment(String waiterEmployeeId, String waiterName) {
        this.waiterEmployeeId = waiterEmployeeId;
        this.waiterName = waiterName;
        this.assignedTableIds = new HashSet<>();
        this.shiftTipsEarned = 0.0;
    }

    public String getWaiterEmployeeId() { return waiterEmployeeId; }
    public void setWaiterEmployeeId(String waiterEmployeeId) { this.waiterEmployeeId = waiterEmployeeId; }

    public String getWaiterName() { return waiterName; }
    public void setWaiterName(String waiterName) { this.waiterName = waiterName; }

    public Set<Integer> getAssignedTableIds() { return assignedTableIds; }
    public void addTableToSection(int tableId) { this.assignedTableIds.add(tableId); }
    public void removeTableFromSection(int tableId) { this.assignedTableIds.remove(tableId); }

    public double getShiftTipsEarned() { return shiftTipsEarned; }
    public void logTipPayment(double amount) { this.shiftTipsEarned += amount; }
}

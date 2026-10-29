package csc478project.restaurantapp;

import java.util.HashSet;
import java.util.Set;

public class WaiterAssignment {
    private String waiterEmployeeId;
    private String waiterName;
    private Set<Integer> activeTables = new HashSet<>();
    private double accumulatedTips;

    public WaiterAssignment() {}
    public WaiterAssignment(String waiterEmployeeId, String waiterName) {
        this.waiterEmployeeId = waiterEmployeeId;
        this.waiterName = waiterName;
    }

    public String getWaiterEmployeeId() { return waiterEmployeeId; }
    public void setWaiterEmployeeId(String waiterEmployeeId) { this.waiterEmployeeId = waiterEmployeeId; }
    public String getWaiterName() { return waiterName; }
    public void setWaiterName(String waiterName) { this.waiterName = waiterName; }
    public Set<Integer> getActiveTables() { return activeTables; }
    public double getAccumulatedTips() { return accumulatedTips; }
    public void addTip(double tip) { this.accumulatedTips += tip; }
}

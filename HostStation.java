package csc478project.restaurantapp;

import java.util.ArrayList;
import java.util.List;

public class HostStation {
    private String hostEmployeeId;
    private String hostName;
    private List<Integer> assignedFloorTables = new ArrayList<>();

    public HostStation() {}
    public HostStation(String hostEmployeeId, String hostName) {
        this.hostEmployeeId = hostEmployeeId;
        this.hostName = hostName;
    }

    public String getHostEmployeeId() { return hostEmployeeId; }
    public void setHostEmployeeId(String hostEmployeeId) { this.hostEmployeeId = hostEmployeeId; }
    public String getHostName() { return hostName; }
    public void setHostName(String hostName) { this.hostName = hostName; }
    public List<Integer> getAssignedFloorTables() { return assignedFloorTables; }
    public void setAssignedFloorTables(List<Integer> assignedFloorTables) { this.assignedFloorTables = assignedFloorTables; }
}

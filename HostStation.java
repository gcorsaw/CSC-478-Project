package csc478project.restaurantapp.model;

import java.util.ArrayList;
import java.util.List;

public class HostStation {
    private String hostEmployeeId;
    private String hostName;
    private boolean isFrontDeskActive;
    private List<Integer> recentlyAssignedTableIds;

    public HostStation() {
        this.recentlyAssignedTableIds = new ArrayList<>();
    }

    public HostStation(String hostEmployeeId, String hostName) {
        this.hostEmployeeId = hostEmployeeId;
        this.hostName = hostName;
        this.isFrontDeskActive = true;
        this.recentlyAssignedTableIds = new ArrayList<>();
    }

    public String getHostEmployeeId() { return hostEmployeeId; }
    public void setHostEmployeeId(String hostEmployeeId) { this.hostEmployeeId = hostEmployeeId; }

    public String getHostName() { return hostName; }
    public void setHostName(String hostName) { this.hostName = hostName; }

    public boolean isFrontDeskActive() { return isFrontDeskActive; }
    public void setFrontDeskActive(boolean frontDeskActive) { isFrontDeskActive = frontDeskActive; }

    public List<Integer> getRecentlyAssignedTableIds() { return recentlyAssignedTableIds; }
    public void assignTableToHostQueue(int tableId) { this.recentlyAssignedTableIds.add(tableId); }
}

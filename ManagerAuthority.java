package csc478project.restaurantapp.model;

import java.time.LocalDateTime;

public class ManagerAuthority {
    private String managerEmployeeId;
    private String managerName;
    private String overridePasscode; 
    private LocalDateTime shiftStartTime;

    public ManagerAuthority() {}

    public ManagerAuthority(String managerEmployeeId, String managerName, String overridePasscode) {
        this.managerEmployeeId = managerEmployeeId;
        this.managerName = managerName;
        this.overridePasscode = overridePasscode;
        this.shiftStartTime = LocalDateTime.now();
    }

    public String getManagerEmployeeId() { return managerEmployeeId; }
    public void setManagerEmployeeId(String managerEmployeeId) { this.managerEmployeeId = managerEmployeeId; }

    public String getManagerName() { return managerName; }
    public void setManagerName(String managerName) { this.managerName = managerName; }

    public String getOverridePasscode() { return overridePasscode; }
    public void setOverridePasscode(String overridePasscode) { this.overridePasscode = overridePasscode; }

    public LocalDateTime getShiftStartTime() { return shiftStartTime; }
    public void setShiftStartTime(LocalDateTime shiftStartTime) { this.shiftStartTime = shiftStartTime; }
}

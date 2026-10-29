package csc478project.restaurantapp;

public class ManagerAuthority {
    private String managerEmployeeId;
    private String managerName;
    private String structuralPasscode;

    public ManagerAuthority() {}
    public ManagerAuthority(String managerEmployeeId, String managerName, String structuralPasscode) {
        this.managerEmployeeId = managerEmployeeId;
        this.managerName = managerName;
        this.structuralPasscode = structuralPasscode;
    }

    public String getManagerEmployeeId() { return managerEmployeeId; }
    public void setManagerEmployeeId(String managerEmployeeId) { this.managerEmployeeId = managerEmployeeId; }
    public String getManagerName() { return managerName; }
    public void setManagerName(String managerName) { this.managerName = managerName; }
    public String getStructuralPasscode() { return structuralPasscode; }
    public void setStructuralPasscode(String structuralPasscode) { this.structuralPasscode = structuralPasscode; }
}

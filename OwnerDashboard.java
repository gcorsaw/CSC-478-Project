package csc478project.restaurantapp.model;

public class OwnerDashboard {
    private String ownerId;
    private String ownerName;
    private double dailyGrossRevenue;
    private boolean administrativeReportAccessGranted;

    public OwnerDashboard() {}

    public OwnerDashboard(String ownerId, String ownerName) {
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.dailyGrossRevenue = 0.0;
        this.administrativeReportAccessGranted = true;
    }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public double getDailyGrossRevenue() { return dailyGrossRevenue; }
    public void compileRevenueReport(double terminalEarnings) { this.dailyGrossRevenue += terminalEarnings; }

    public boolean isAdministrativeReportAccessGranted() { return administrativeReportAccessGranted; }
    public void setAdministrativeReportAccessGranted(boolean accessGranted) { this.administrativeReportAccessGranted = accessGranted; }
}

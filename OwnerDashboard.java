package csc478project.restaurantapp;

public class OwnerDashboard {
    private String ownerId;
    private String ownerName;
    private double compiledRevenue;

    public OwnerDashboard() {}
    public OwnerDashboard(String ownerId, String ownerName) {
        this.ownerId = ownerId;
        this.ownerName = ownerName;
    }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public double getCompiledRevenue() { return compiledRevenue; }
    public void addRevenue(double earnings) { this.compiledRevenue += earnings; }
}

package csc478project.restaurantapp.model;

public class ChefStation {
    private String chefEmployeeId;
    private String chefName;
    private String assignedKitchenLine; // e.g., "Grill", "Appetizers", "Desserts"
    private int activeTicketCount;

    public ChefStation() {}

    public ChefStation(String chefEmployeeId, String chefName, String assignedKitchenLine) {
        this.chefEmployeeId = chefEmployeeId;
        this.chefName = chefName;
        this.assignedKitchenLine = assignedKitchenLine;
        this.activeTicketCount = 0;
    }

    public String getChefEmployeeId() { return chefEmployeeId; }
    public void setChefEmployeeId(String chefEmployeeId) { this.chefEmployeeId = chefEmployeeId; }

    public String getChefName() { return chefName; }
    public void setChefName(String chefName) { this.chefName = chefName; }

    public String getAssignedKitchenLine() { return assignedKitchenLine; }
    public void setAssignedKitchenLine(String assignedKitchenLine) { this.assignedKitchenLine = assignedKitchenLine; }

    public int getActiveTicketCount() { return activeTicketCount; }
    public void incrementTickets() { this.activeTicketCount++; }
    public void decrementTickets() { if (this.activeTicketCount > 0) this.activeTicketCount--; }
}

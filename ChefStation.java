package csc478project.restaurantapp;

public class ChefStation {
    private String chefEmployeeId;
    private String chefName;
    private String lineAssignment; // "Grill", "Prep", "Dessert"
    private int ticketCount;

    public ChefStation() {}
    public ChefStation(String chefEmployeeId, String chefName, String lineAssignment) {
        this.chefEmployeeId = chefEmployeeId;
        this.chefName = chefName;
        this.lineAssignment = lineAssignment;
    }

    public String getChefEmployeeId() { return chefEmployeeId; }
    public void setChefEmployeeId(String chefEmployeeId) { this.chefEmployeeId = chefEmployeeId; }
    public String getChefName() { return chefName; }
    public void setChefName(String chefName) { this.chefName = chefName; }
    public String getLineAssignment() { return lineAssignment; }
    public void setLineAssignment(String lineAssignment) { this.lineAssignment = lineAssignment; }
    public int getTicketCount() { return ticketCount; }
    public void setTicketCount(int ticketCount) { this.ticketCount = ticketCount; }
}

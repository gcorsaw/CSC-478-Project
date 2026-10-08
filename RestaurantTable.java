package csc478project.restaurantapp;

public class RestaurantTable {
    /* Instance variable to store the occupants and it's going to be seen by the 
    functions that are in the RestaurantTable class */
    private String tableOccupants;

    // Constructor to initialize the table
    public RestaurantTable(String tableOccupants) {
        /* Initialize the tableOccupants variable with the provided value and the 
        this keyword is used to refer to the current instance of the class */
        this.tableOccupants = tableOccupants;
    }

    /*
    This method is going to check to see if the table is occupied or not. The initial value of the occupied variable is initialized to false.
    If the tableOccupants variable is not null, then the occupied variable is set to true if the table occupants variable is not empty or null.
    The method prints out whether the table is occupied or not and returns the value of the occupied variable. The trim() method
    is used to trim any leading or trailing whitespace from the tableOccupants variable before checking if it's empty.
    This is important because a string with only whitespace would be considered empty. The isEmpty() method checks
    to see if the tableOccupants variable is empty or not. If the tableOccupants variable is null,
    then the occupied variable remains false. In the second if block, the method prints out 'The table is occupied'
    if the occupied variable is true, and in the else block, it prints out 'The table is not occupied'
    if the occupied variable is false. Finally, the method returns the value of the occupied variable.
    */
    public boolean isOccupied() {
        boolean occupied = false;
        if (this.tableOccupants != null) {
            occupied = !this.tableOccupants.trim().isEmpty();
        }

        if (occupied) {
            System.out.println("The table is occupied");
        } else {
            System.out.println("The table is not occupied");
        }

        return occupied;
    }

    public static void main(String[] args) {
        // Example usage:
        RestaurantTable emptyTable = new RestaurantTable(null);
        emptyTable.isOccupied(); // Prints: The table is not occupied

        RestaurantTable fullTable = new RestaurantTable("Grace");
        fullTable.isOccupied(); // Prints: The table is occupied
    }
}

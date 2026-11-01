document.addEventListener("DOMContentLoaded", () => {
    const form = document.getElementById("table-form");
    form?.addEventListener("submit", addTable);
    loadTables();
});

async function loadTables() {
    const tableList = document.getElementById("table-list");
    const feedback = document.getElementById("table-feedback");
    if (!tableList || !feedback) return;

    try {
        const tables = await window.restaurantApi("/api/tables");
        tableList.replaceChildren();

        if (tables.length === 0) {
            const emptyMessage = document.createElement("li");
            emptyMessage.className = "table-empty";
            emptyMessage.textContent = "No tables have been added yet.";
            tableList.append(emptyMessage);
            return;
        }

        feedback.textContent = "";
        tables.forEach((table) => {
            const entry = document.createElement("li");
            const occupied = table.status.toLowerCase() === "occupied";
            entry.className = `table-entry${occupied ? " is-occupied" : ""}`;

            const details = document.createElement("div");
            const name = document.createElement("strong");
            name.textContent = `Table ${table.tableNumber}`;
            const capacity = document.createElement("span");
            capacity.textContent = `Seats ${table.capacity}`;
            details.append(name, capacity);

            const status = document.createElement("span");
            status.className = "status-badge";
            status.textContent = table.status;
            entry.append(details, status);
            tableList.append(entry);
        });
    } catch (error) {
        window.showMessage(feedback, error.message, true);
    }
}

async function addTable(event) {
    event.preventDefault();
    const form = event.currentTarget;
    const feedback = document.getElementById("table-feedback");
    const tableNumber = Number(form.elements.tableNumber.value);
    const capacity = Number(form.elements.capacity.value);

    try {
        await window.restaurantApi("/api/tables", {
            method: "POST",
            body: JSON.stringify({ tableNumber, capacity })
        });
        form.reset();
        window.showMessage(feedback, `Table ${tableNumber} was added.`);
        await loadTables();
    } catch (error) {
        window.showMessage(feedback, error.message, true);
    }
}

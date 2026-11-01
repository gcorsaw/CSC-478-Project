document.addEventListener("DOMContentLoaded", () => {
    const feedback = document.getElementById("waiter-feedback");
    const refresh = () => loadWaiterTables(feedback);
    refresh();
    window.setInterval(refresh, 10000);
});

async function loadWaiterTables(feedback) {
    const grid = document.getElementById("waiter-tables");
    const summary = document.getElementById("waiter-summary");
    if (!grid || !summary || !feedback) return;

    try {
        const tables = await window.restaurantApi("/api/tables");
        const occupiedCount = tables.filter((table) =>
            table.status.toLowerCase() === "occupied"
        ).length;
        summary.replaceChildren();
        renderWaiterStat(summary, "Open tables", tables.length - occupiedCount);
        renderWaiterStat(summary, "Occupied tables", occupiedCount);
        grid.replaceChildren();

        if (tables.length === 0) {
            const empty = document.createElement("p");
            empty.className = "kitchen-empty";
            empty.textContent = "No tables have been added yet.";
            grid.append(empty);
        } else {
            tables.forEach((table) => {
                const card = document.createElement("article");
                const occupied = table.status.toLowerCase() === "occupied";
                card.className = `waiter-table-card${occupied ? " is-occupied" : ""}`;
                const name = document.createElement("h3");
                name.textContent = `Table ${table.tableNumber}`;
                const detail = document.createElement("p");
                detail.textContent = `Seats ${table.capacity}`;
                const status = document.createElement("span");
                status.className = "status-badge";
                status.textContent = table.status;
                card.append(name, detail, status);
                grid.append(card);
            });
        }
        feedback.textContent = "";
    } catch (error) {
        window.showMessage(feedback, error.message, true);
    }
}

function renderWaiterStat(container, label, value) {
    const stat = document.createElement("div");
    stat.className = "role-stat";
    const title = document.createElement("span");
    title.textContent = label;
    const count = document.createElement("strong");
    count.textContent = value;
    stat.append(title, count);
    container.append(stat);
}

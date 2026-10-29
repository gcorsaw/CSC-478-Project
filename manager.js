(() => {
    const summaryElement = document.getElementById("manager-summary");
    const feedback = document.getElementById("manager-feedback");
    const assignmentFeedback = document.getElementById("assignment-feedback");
    const tableSelect = document.getElementById("assignment-table");
    const waiterSelect = document.getElementById("assignment-waiter");
    const assignmentList = document.getElementById("assignment-list");
    const orderList = document.getElementById("manager-orders");

    function addStat(label, value) {
        const card = document.createElement("article");
        card.className = "role-stat";
        const name = document.createElement("span");
        name.textContent = label;
        const content = document.createElement("strong");
        content.textContent = value;
        card.append(name, content);
        summaryElement.append(card);
    }

    async function loadManagerView() {
        const [summary, tables, waiters, assignments, orders] = await Promise.all([
            window.restaurantApi("/api/summary"),
            window.restaurantApi("/api/tables"),
            window.restaurantApi("/api/waiters"),
            window.restaurantApi("/api/table-assignments"),
            window.restaurantApi("/api/orders")
        ]);

        summaryElement.replaceChildren();
        addStat("Staff", summary.activeWorkers);
        addStat("Open tables", summary.totalTables - summary.occupiedTables);
        addStat("Occupied tables", summary.occupiedTables);
        addStat("Open orders", summary.openOrders);

        tableSelect.replaceChildren();
        for (const table of tables) {
            const option = document.createElement("option");
            option.value = table.tableNumber;
            option.textContent = `Table ${table.tableNumber}`;
            tableSelect.append(option);
        }

        waiterSelect.replaceChildren();
        for (const waiter of waiters.filter((entry) => entry.active)) {
            const option = document.createElement("option");
            option.value = waiter.waiterId;
            option.textContent = `${waiter.firstName} ${waiter.lastName}`;
            waiterSelect.append(option);
        }

        assignmentList.replaceChildren();
        if (assignments.length === 0) {
            const empty = document.createElement("li");
            empty.textContent = "No table assignments.";
            assignmentList.append(empty);
        } else {
            for (const assignment of assignments) {
                const row = document.createElement("li");
                row.textContent = `Table ${assignment.tableNumber} — ${assignment.firstName} ${assignment.lastName}`;
                assignmentList.append(row);
            }
        }

        orderList.replaceChildren();
        for (const order of orders.slice(0, 10)) {
            const row = document.createElement("li");
            row.textContent = `#${order.orderId} · Table ${order.tableNumber} · ${order.status} · ${order.items || "No items"}`;
            orderList.append(row);
        }
    }

    document.getElementById("assignment-form").addEventListener("submit", async (event) => {
        event.preventDefault();
        if (!tableSelect.value || !waiterSelect.value) {
            window.showMessage(assignmentFeedback, "Add a table and an active waiter before assigning.", true);
            return;
        }
        try {
            const assignment = await window.restaurantApi("/api/table-assignments", {
                method: "POST",
                body: JSON.stringify({
                    tableNumber: Number(tableSelect.value),
                    waiterId: Number(waiterSelect.value)
                })
            });
            window.showMessage(assignmentFeedback,
                `Table ${assignment.tableNumber} assigned to ${assignment.firstName} ${assignment.lastName}.`);
            await loadManagerView();
        } catch (error) {
            window.showMessage(assignmentFeedback, error.message, true);
        }
    });

    loadManagerView().catch((error) => window.showMessage(feedback, error.message, true));
    window.setInterval(() => loadManagerView().catch((error) =>
        window.showMessage(feedback, error.message, true)), 15000);
})();
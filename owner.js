(() => {
    const summaryElement = document.getElementById("owner-summary");
    const ordersElement = document.getElementById("owner-orders");
    const feedback = document.getElementById("owner-feedback");

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

    async function loadOwnerView() {
        const [summary, orders] = await Promise.all([
            window.restaurantApi("/api/summary"),
            window.restaurantApi("/api/orders")
        ]);

        summaryElement.replaceChildren();
        addStat("Active staff", summary.activeWorkers);
        addStat("Tables", `${summary.occupiedTables} occupied / ${summary.totalTables}`);
        addStat("Orders", `${summary.openOrders} open / ${summary.totalOrders} total`);
        addStat("Payments collected", `$${Number(summary.collected).toFixed(2)}`);

        ordersElement.replaceChildren();
        if (orders.length === 0) {
            const empty = document.createElement("li");
            empty.textContent = "No orders have been recorded.";
            ordersElement.append(empty);
            return;
        }
        for (const order of orders.slice(0, 10)) {
            const row = document.createElement("li");
            row.textContent = `#${order.orderId} · Table ${order.tableNumber} · ${order.status} · ${order.items || "No items"}`;
            ordersElement.append(row);
        }
    }

    loadOwnerView().catch((error) => window.showMessage(feedback, error.message, true));
    window.setInterval(() => loadOwnerView().catch((error) =>
        window.showMessage(feedback, error.message, true)), 15000);
})();
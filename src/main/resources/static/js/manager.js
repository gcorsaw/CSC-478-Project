document.addEventListener("DOMContentLoaded", loadManagerWorkspace);

async function loadManagerWorkspace() {
    const summaryElement = document.getElementById("manager-summary");
    const ordersElement = document.getElementById("manager-orders");
    const feedback = document.getElementById("manager-feedback");

    try {
        const [summary, orders] = await Promise.all([
            window.restaurantApi("/api/summary"),
            window.restaurantApi("/api/orders")
        ]);
        renderManagerStats(summaryElement, summary);
        renderManagerOrders(ordersElement, orders.slice(0, 5));
        feedback.textContent = "";
    } catch (error) {
        window.showMessage(feedback, error.message, true);
    }
}

function renderManagerStats(container, summary) {
    if (!container) return;
    container.replaceChildren();

    [
        ["Active staff", summary.activeWorkers],
        ["Occupied tables", `${summary.occupiedTables} / ${summary.totalTables}`],
        ["Open orders", summary.openOrders]
    ].forEach(([label, value]) => {
        const card = document.createElement("div");
        card.className = "role-stat";
        const title = document.createElement("span");
        title.textContent = label;
        const metric = document.createElement("strong");
        metric.textContent = value;
        card.append(title, metric);
        container.append(card);
    });
}

function renderManagerOrders(container, orders) {
    if (!container) return;
    container.replaceChildren();

    if (orders.length === 0) {
        const empty = document.createElement("li");
        empty.textContent = "No recent orders.";
        container.append(empty);
        return;
    }

    orders.forEach((order) => {
        const item = document.createElement("li");
        item.textContent = `Order #${order.orderId} · Table ${order.tableNumber} · ${order.status}`;
        container.append(item);
    });
}

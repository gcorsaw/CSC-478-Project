document.addEventListener("DOMContentLoaded", loadOwnerWorkspace);

async function loadOwnerWorkspace() {
    const summaryElement = document.getElementById("owner-summary");
    const ordersElement = document.getElementById("owner-orders");
    const feedback = document.getElementById("owner-feedback");

    try {
        const [summary, orders] = await Promise.all([
            window.restaurantApi("/api/summary"),
            window.restaurantApi("/api/orders")
        ]);
        renderOwnerStats(summaryElement, summary);
        renderOwnerOrders(ordersElement, orders.slice(0, 5));
        feedback.textContent = "";
    } catch (error) {
        window.showMessage(feedback, error.message, true);
    }
}

function renderOwnerStats(container, summary) {
    if (!container) return;
    container.replaceChildren();

    [
        ["Active staff", summary.activeWorkers],
        ["Total tables", summary.totalTables],
        ["Occupied tables", summary.occupiedTables],
        ["Total orders", summary.totalOrders],
        ["Open orders", summary.openOrders],
        ["Collected payments", Number(summary.collected).toLocaleString("en-US", {
            style: "currency",
            currency: "USD"
        })]
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

function renderOwnerOrders(container, orders) {
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

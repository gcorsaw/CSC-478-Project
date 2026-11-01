document.addEventListener("DOMContentLoaded", () => {
    const feedback = document.getElementById("kitchen-feedback");
    const refreshButton = document.getElementById("refresh-kitchen");
    const refresh = () => loadKitchenOrders(feedback);

    refreshButton?.addEventListener("click", refresh);
    refresh();
    window.setInterval(refresh, 10000);
});

async function loadKitchenOrders(feedback) {
    const container = document.getElementById("kitchen-orders");
    if (!container || !feedback) return;

    try {
        const orders = await window.restaurantApi("/api/orders");
        container.replaceChildren();

        if (orders.length === 0) {
            const empty = document.createElement("p");
            empty.className = "kitchen-empty";
            empty.textContent = "There are no recent orders.";
            container.append(empty);
        } else {
            orders.forEach((order) => {
                const card = document.createElement("article");
                card.className = "kitchen-order-card";
                const heading = document.createElement("h3");
                heading.textContent = `Order #${order.orderId}`;
                const table = document.createElement("p");
                table.textContent = `Table ${order.tableNumber}`;
                const status = document.createElement("p");
                status.className = "status-badge";
                status.textContent = order.status;
                card.append(heading, table, status);
                container.append(card);
            });
        }
        feedback.textContent = "";
    } catch (error) {
        window.showMessage(feedback, error.message, true);
    }
}

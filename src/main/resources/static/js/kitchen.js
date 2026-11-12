document.addEventListener("DOMContentLoaded", () => {
    const feedback = document.getElementById("kitchen-feedback");
    const refreshButton = document.getElementById("refresh-kitchen");
    const refresh = (showProgress = false) => loadKitchenOrders(feedback, refreshButton, showProgress);

    refreshButton?.addEventListener("click", () => refresh(true));
    refresh();
    window.setInterval(() => refresh(), 10000);
});

let kitchenRefreshInProgress = false;

async function loadKitchenOrders(feedback, refreshButton, showProgress) {
    const container = document.getElementById("kitchen-orders");
    if (!container || !feedback || kitchenRefreshInProgress) return;

    kitchenRefreshInProgress = true;
    const buttonText = refreshButton?.textContent;
    if (showProgress && refreshButton) {
        refreshButton.disabled = true;
        refreshButton.textContent = "Refreshing...";
        window.showMessage(feedback, "Refreshing orders...");
    }

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
        if (showProgress) {
            window.showMessage(feedback, `Orders refreshed at ${new Date().toLocaleTimeString()}.`);
        }
    } catch (error) {
        window.showMessage(feedback, error.message, true);
    } finally {
        kitchenRefreshInProgress = false;
        if (showProgress && refreshButton) {
            refreshButton.disabled = false;
            refreshButton.textContent = buttonText;
        }
    }
}

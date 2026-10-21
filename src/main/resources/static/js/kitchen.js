(() => {
    const ordersElement = document.getElementById("kitchen-orders");
    const feedback = document.getElementById("kitchen-feedback");

    async function loadKitchenOrders() {
        const items = await window.restaurantApi("/api/kitchen/orders");
        ordersElement.replaceChildren();
        if (items.length === 0) {
            const empty = document.createElement("p");
            empty.className = "kitchen-empty";
            empty.textContent = "No open kitchen items.";
            ordersElement.append(empty);
            return;
        }

        for (const item of items) {
            const card = document.createElement("article");
            card.className = "kitchen-order-card";
            const heading = document.createElement("h3");
            heading.textContent = `${item.itemName} × ${item.quantity}`;
            const detail = document.createElement("p");
            detail.textContent = `Order #${item.orderId} · Table ${item.tableNumber} · ${item.status.replaceAll("_", " ")}`;
            card.append(heading, detail);
            if (item.notes) {
                const notes = document.createElement("p");
                notes.textContent = `Notes: ${item.notes}`;
                card.append(notes);
            }

            const action = document.createElement("button");
            action.type = "button";
            action.className = "navi-button";
            action.textContent = item.status === "standing_by" ? "Start preparing" : "Mark done";
            action.addEventListener("click", async () => {
                const nextStatus = item.status === "standing_by" ? "in_progress" : "done";
                try {
                    await window.restaurantApi(`/api/kitchen/items/${item.orderItemId}/status`, {
                        method: "POST",
                        body: JSON.stringify({ status: nextStatus })
                    });
                    window.showMessage(feedback, `${item.itemName} marked ${nextStatus.replaceAll("_", " ")}.`);
                    await loadKitchenOrders();
                } catch (error) {
                    window.showMessage(feedback, error.message, true);
                }
            });
            card.append(action);
            ordersElement.append(card);
        }
    }

    document.getElementById("refresh-kitchen").addEventListener("click", () =>
        loadKitchenOrders().catch((error) => window.showMessage(feedback, error.message, true)));
    loadKitchenOrders().catch((error) => window.showMessage(feedback, error.message, true));
    window.setInterval(() => loadKitchenOrders().catch((error) =>
        window.showMessage(feedback, error.message, true)), 10000);
})();

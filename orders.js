(() => {
    const form = document.getElementById("order-form");
    const waiterSelect = document.getElementById("waiterId");
    const tableSelect = document.getElementById("tableNumber");
    const itemSelect = document.getElementById("menuItem");
    const quantityInput = document.getElementById("quantity");
    const itemsList = document.getElementById("selected-items");
    const feedback = document.getElementById("order-feedback");
    const ordersList = document.getElementById("order-list");
    const selectedItems = new Map();
    let menu = [];

    const money = (value) => `$${Number(value).toFixed(2)}`;

    function renderSelectedItems() {
        itemsList.replaceChildren();
        for (const [itemId, quantity] of selectedItems) {
            const item = menu.find((entry) => entry.itemId === itemId);
            if (!item) continue;
            const row = document.createElement("li");
            row.textContent = `${item.itemName} × ${quantity}`;
            itemsList.append(row);
        }
    }

    // Rebuild a <select> but keep the person's current choice across the 10-second refresh.
    function fillSelect(select, entries, emptyText) {
        const previous = select.value;
        select.replaceChildren();
        for (const [value, label] of entries) {
            const option = document.createElement("option");
            option.value = value;
            option.textContent = label;
            select.append(option);
        }
        if (select.options.length === 0) {
            const option = document.createElement("option");
            option.value = "";
            option.textContent = emptyText;
            select.append(option);
        } else if ([...select.options].some((option) => option.value === previous)) {
            select.value = previous;
        }
    }

    async function payOrder(order, tipInput) {
        const tip = tipInput.value.trim() === "" ? 0 : Number(tipInput.value);
        if (!Number.isFinite(tip) || tip < 0) {
            window.showMessage(feedback, "Enter a tip of 0 or more.", true);
            return;
        }
        try {
            const payment = await window.restaurantApi(`/api/orders/${order.orderId}/payment`, {
                method: "POST",
                body: JSON.stringify({ tip })
            });
            window.showMessage(feedback,
                `Order #${payment.orderId} paid: ${money(payment.amount)} plus ${money(payment.tip)} tip.`);
            await loadData(true);
        } catch (error) {
            window.showMessage(feedback, error.message, true);
        }
    }

    function renderOrders(orders) {
        ordersList.replaceChildren();
        for (const order of orders) {
            const row = document.createElement("li");
            row.className = "order-row";
            const text = document.createElement("span");
            text.className = "order-text";
            text.textContent = `Order #${order.orderId} · Table ${order.tableNumber} · ${order.status}`
                + ` · ${order.items} · ${money(order.total)}`;
            row.append(text);

            if (order.status === "open") {
                const tipInput = document.createElement("input");
                tipInput.type = "number";
                tipInput.min = "0";
                tipInput.step = "0.01";
                tipInput.placeholder = "Tip $";
                tipInput.setAttribute("aria-label", `Tip for order ${order.orderId}`);
                const payButton = document.createElement("button");
                payButton.type = "button";
                payButton.textContent = "Close and pay";
                payButton.addEventListener("click", () => payOrder(order, tipInput));
                row.append(tipInput, payButton);
            }
            ordersList.append(row);
        }
    }

    async function loadData(force = false) {
        const [tables, availableMenu, orders, waiters] = await Promise.all([
            window.restaurantApi("/api/tables"),
            window.restaurantApi("/api/menu"),
            window.restaurantApi("/api/orders"),
            window.restaurantApi("/api/waiters")
        ]);
        menu = availableMenu;

        fillSelect(waiterSelect,
            waiters.filter((waiter) => waiter.active)
                .map((waiter) => [waiter.waiterId, `${waiter.firstName} ${waiter.lastName}`]),
            "No active waiters");
        fillSelect(tableSelect,
            tables.filter((entry) => entry.status === "occupied")
                .map((table) => [table.tableNumber, `Table ${table.tableNumber} — ${table.guestName}`]),
            "Seat a party before creating an order");
        fillSelect(itemSelect,
            menu.map((item) => [item.itemId, `${item.itemName} — ${money(item.price)}`]),
            "No menu items available");

        // Don't wipe a tip that is being typed when the automatic refresh runs.
        if (force || !ordersList.contains(document.activeElement)) {
            renderOrders(orders);
        }
    }

    document.getElementById("add-item").addEventListener("click", () => {
        const itemId = Number(itemSelect.value);
        const quantity = Number(quantityInput.value);
        if (!Number.isInteger(itemId) || !Number.isInteger(quantity) || quantity < 1 || quantity > 50) {
            window.showMessage(feedback, "Select an item and quantity from 1 to 50.", true);
            return;
        }
        const newQuantity = (selectedItems.get(itemId) || 0) + quantity;
        if (newQuantity > 50) {
            window.showMessage(feedback, "An item quantity cannot exceed 50.", true);
            return;
        }
        selectedItems.set(itemId, newQuantity);
        renderSelectedItems();
        window.showMessage(feedback, "");
    });

    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        const tableNumber = Number(tableSelect.value);
        if (!Number.isInteger(tableNumber) || selectedItems.size === 0) {
            window.showMessage(feedback, "Choose an occupied table and add at least one item.", true);
            return;
        }
        const body = {
            tableNumber,
            items: Array.from(selectedItems, ([itemId, quantity]) => ({ itemId, quantity }))
        };
        const waiterId = Number(waiterSelect.value);
        if (Number.isInteger(waiterId) && waiterId > 0) {
            body.waiterId = waiterId;
        }
        try {
            const order = await window.restaurantApi("/api/orders", {
                method: "POST",
                body: JSON.stringify(body)
            });
            selectedItems.clear();
            renderSelectedItems();
            window.showMessage(feedback, `Order #${order.orderId} saved.`);
            await loadData(true);
        } catch (error) {
            window.showMessage(feedback, error.message, true);
        }
    });

    loadData().catch((error) => window.showMessage(feedback, error.message, true));
    window.setInterval(() => loadData().catch((error) =>
        window.showMessage(feedback, error.message, true)), 10000);
})();
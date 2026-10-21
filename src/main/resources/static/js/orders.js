(() => {
    const form = document.getElementById("order-form");
    const tableSelect = document.getElementById("tableNumber");
    const itemSelect = document.getElementById("menuItem");
    const quantityInput = document.getElementById("quantity");
    const itemsList = document.getElementById("selected-items");
    const feedback = document.getElementById("order-feedback");
    const ordersList = document.getElementById("order-list");
    const selectedItems = new Map();
    let menu = [];

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

    async function loadData() {
        const [tables, availableMenu, orders] = await Promise.all([
            window.restaurantApi("/api/tables"),
            window.restaurantApi("/api/menu"),
            window.restaurantApi("/api/orders")
        ]);
        menu = availableMenu;

        tableSelect.replaceChildren();
        for (const table of tables.filter((entry) => entry.status === "occupied")) {
            const option = document.createElement("option");
            option.value = table.tableNumber;
            option.textContent = `Table ${table.tableNumber} — ${table.guestName}`;
            tableSelect.append(option);
        }
        if (tableSelect.options.length === 0) {
            const option = document.createElement("option");
            option.value = "";
            option.textContent = "Seat a party before creating an order";
            tableSelect.append(option);
        }

        itemSelect.replaceChildren();
        for (const item of menu) {
            const option = document.createElement("option");
            option.value = item.itemId;
            option.textContent = `${item.itemName} — $${Number(item.price).toFixed(2)}`;
            itemSelect.append(option);
        }

        ordersList.replaceChildren();
        for (const order of orders) {
            const row = document.createElement("li");
            row.textContent = `Order #${order.orderId} · Table ${order.tableNumber} · ${order.status} · ${order.items}`;
            ordersList.append(row);
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
        try {
            const order = await window.restaurantApi("/api/orders", {
                method: "POST",
                body: JSON.stringify({
                    tableNumber,
                    items: Array.from(selectedItems, ([itemId, quantity]) => ({ itemId, quantity }))
                })
            });
            selectedItems.clear();
            renderSelectedItems();
            window.showMessage(feedback, `Order #${order.orderId} saved.`);
            await loadData();
        } catch (error) {
            window.showMessage(feedback, error.message, true);
        }
    });

    loadData().catch((error) => window.showMessage(feedback, error.message, true));
    window.setInterval(() => loadData().catch((error) =>
        window.showMessage(feedback, error.message, true)), 10000);
})();

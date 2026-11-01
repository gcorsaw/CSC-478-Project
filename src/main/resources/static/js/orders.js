let currentOrderItems = [];

document.addEventListener("DOMContentLoaded", () => {
    document.getElementById("addItemBtn")?.addEventListener("click", addOrderItem);
    document.getElementById("saveOrderBtn")?.addEventListener("click", saveOrder);
    loadOrderOptions();
    loadRecentOrders();
});

async function loadOrderOptions() {
    const feedback = document.getElementById("errorMessage");
    try {
        const [waiters, tables, menuItems] = await Promise.all([
            window.restaurantApi("/api/waiters"),
            window.restaurantApi("/api/tables"),
            window.restaurantApi("/api/menu")
        ]);
        populateOptions(
            document.getElementById("waiterSelect"),
            waiters.filter((waiter) => waiter.active),
            "Select an active waiter...",
            (waiter) => waiter.waiterId,
            (waiter) => `${waiter.firstName} ${waiter.lastName}`
        );
        populateOptions(
            document.getElementById("tableSelect"),
            tables,
            "Select a dining table...",
            (table) => table.tableNumber,
            (table) => `Table ${table.tableNumber} — ${table.status}`
        );
        populateOptions(
            document.getElementById("itemSelect"),
            menuItems,
            "Choose a menu item...",
            (item) => item.itemId,
            (item) => `${item.itemName} (${Number(item.price).toLocaleString("en-US", {
                style: "currency",
                currency: "USD"
            })})`
        );
    } catch (error) {
        window.showMessage(feedback, error.message, true);
    }
}

function populateOptions(select, items, placeholder, getValue, getLabel) {
    if (!select) return;
    select.replaceChildren();
    const prompt = document.createElement("option");
    prompt.value = "";
    prompt.textContent = placeholder;
    prompt.disabled = true;
    prompt.selected = true;
    select.append(prompt);

    items.forEach((item) => {
        const option = document.createElement("option");
        option.value = getValue(item);
        option.textContent = getLabel(item);
        select.append(option);
    });
}

function addOrderItem() {
    const itemSelect = document.getElementById("itemSelect");
    const quantityInput = document.getElementById("quantityInput");
    const list = document.getElementById("currentOrderItems");
    const feedback = document.getElementById("errorMessage");
    const quantity = Number(quantityInput?.value);

    if (!itemSelect?.value || !Number.isInteger(quantity) || quantity < 1 || !list) {
        window.showMessage(feedback, "Choose a menu item and enter a quantity of at least one.", true);
        return;
    }

    currentOrderItems.push({
        itemId: Number(itemSelect.value),
        quantity,
        itemName: itemSelect.options[itemSelect.selectedIndex].textContent
    });
    renderStagedItems(list);
    itemSelect.value = "";
    quantityInput.value = "1";
    feedback.textContent = "";
}

function renderStagedItems(list) {
    list.replaceChildren();
    currentOrderItems.forEach((item) => {
        const row = document.createElement("li");
        const name = document.createElement("span");
        name.textContent = item.itemName;
        const quantity = document.createElement("span");
        quantity.textContent = `Qty: ${item.quantity}`;
        row.append(name, quantity);
        list.append(row);
    });
}

async function saveOrder() {
    const waiterSelect = document.getElementById("waiterSelect");
    const tableSelect = document.getElementById("tableSelect");
    const feedback = document.getElementById("errorMessage");
    if (!waiterSelect?.value || !tableSelect?.value || currentOrderItems.length === 0) {
        window.showMessage(feedback, "Select a waiter and table, then add at least one menu item.", true);
        return;
    }

    try {
        const result = await window.restaurantApi("/api/orders", {
            method: "POST",
            body: JSON.stringify({
                tableNumber: Number(tableSelect.value),
                waiterId: Number(waiterSelect.value),
                items: currentOrderItems.map(({ itemId, quantity }) => ({ itemId, quantity }))
            })
        });
        currentOrderItems = [];
        renderStagedItems(document.getElementById("currentOrderItems"));
        tableSelect.value = "";
        document.getElementById("itemSelect").value = "";
        window.showMessage(feedback, `Order #${result.orderId} was saved.`);
        await loadRecentOrders();
    } catch (error) {
        window.showMessage(feedback, error.message, true);
    }
}

async function loadRecentOrders() {
    const tableBody = document.getElementById("ordersTableBody");
    const feedback = document.getElementById("errorMessage");
    if (!tableBody) return;

    try {
        const orders = await window.restaurantApi("/api/orders");
        tableBody.replaceChildren();

        if (orders.length === 0) {
            const row = document.createElement("tr");
            const cell = document.createElement("td");
            cell.colSpan = 4;
            cell.textContent = "No recent orders.";
            row.append(cell);
            tableBody.append(row);
            return;
        }

        orders.forEach((order) => {
            const row = document.createElement("tr");
            const orderCell = document.createElement("td");
            orderCell.textContent = `#${order.orderId}`;
            const tableCell = document.createElement("td");
            tableCell.textContent = `Table ${order.tableNumber}`;
            const statusCell = document.createElement("td");
            const status = document.createElement("span");
            status.className = "status-badge";
            status.textContent = order.status;
            statusCell.append(status);
            const createdCell = document.createElement("td");
            const date = new Date(order.createdAt);
            createdCell.textContent = Number.isNaN(date.getTime())
                ? order.createdAt
                : date.toLocaleString();
            row.append(orderCell, tableCell, statusCell, createdCell);
            tableBody.append(row);
        });
    } catch (error) {
        window.showMessage(feedback, error.message, true);
    }
}

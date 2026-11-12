document.addEventListener("DOMContentLoaded", loadManagerWorkspace);

restrictAccountRoleOptions();

async function restrictAccountRoleOptions() {
    const roleSelect = document.getElementById("staff-role");
    if (!roleSelect) return;

    try {
        const session = await window.restaurantApi("/api/auth/session");
        if (session.role === "OWNER") return;
    } catch {}

    [...roleSelect.options]
        .filter((option) => ["MANAGER", "ADMIN", "OWNER"].includes(option.value))
        .forEach((option) => option.remove());
}

document.getElementById("manager-override-form")?.addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    const feedback = document.getElementById("manager-override-feedback");
    const credentials = Object.fromEntries(new FormData(form));

    try {
        await window.restaurantApi("/api/manager/override", {
            method: "POST",
            body: JSON.stringify(credentials)
        });
        form.reset();
        window.showMessage(feedback, "Manager credentials verified.");
    } catch (error) {
        window.showMessage(feedback, error.message, true);
    }
});

document.getElementById("staff-account-form")?.addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    const feedback = document.getElementById("staff-account-feedback");

    try {
        const account = await window.restaurantApi("/api/staff/accounts", {
            method: "POST",
            body: JSON.stringify(Object.fromEntries(new FormData(form)))
        });
        form.reset();
        window.showMessage(feedback, `Account created for ${account.name} (${account.username}).`);
    } catch (error) {
        window.showMessage(feedback, error.message, true);
    }
});

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

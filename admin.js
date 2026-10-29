(() => {
    const healthElement = document.getElementById("admin-health");
    const feedback = document.getElementById("admin-feedback");
    const staffList = document.getElementById("staff-list");

    function addHealthValue(label, value) {
        const card = document.createElement("article");
        card.className = "role-stat";
        const name = document.createElement("span");
        name.textContent = label;
        const content = document.createElement("strong");
        content.textContent = value;
        card.append(name, content);
        healthElement.append(card);
    }

    async function loadHealth() {
        window.showMessage(feedback, "Checking database connection…");
        try {
            const health = await window.restaurantApi("/api/admin/health");
            healthElement.replaceChildren();
            addHealthValue("Status", "Connected");
            addHealthValue("Database", health.database);
            addHealthValue("Workers", health.workers);
            addHealthValue("Tables", health.tables);
            addHealthValue("Orders", health.orders);
            addHealthValue("Last checked", new Date(health.checkedAt).toLocaleString());
            window.showMessage(feedback, "The app successfully queried PostgreSQL.");
        } catch (error) {
            healthElement.replaceChildren();
            window.showMessage(feedback, error.message, true);
        }
    }

    async function loadStaff() {
        const workers = await window.restaurantApi("/api/workers");
        staffList.replaceChildren();
        if (workers.length === 0) {
            const empty = document.createElement("li");
            empty.textContent = "No staff records found.";
            staffList.append(empty);
            return;
        }
        for (const worker of workers) {
            const row = document.createElement("li");
            const roles = worker.roles.length ? worker.roles.join(", ") : "No station assigned";
            row.textContent = `${worker.firstName} ${worker.lastName} (${worker.username}) — ${worker.active ? "Active" : "Inactive"} — ${roles}`;
            staffList.append(row);
        }
    }

    document.getElementById("refresh-health").addEventListener("click", loadHealth);
    loadHealth();
    loadStaff().catch((error) => window.showMessage(feedback, error.message, true));
})();
document.addEventListener("DOMContentLoaded", () => {
    const refreshButton = document.getElementById("refresh-health");
    refreshButton?.addEventListener("click", loadSystemHealth);
    loadSystemHealth();
});

async function loadSystemHealth() {
    const healthContainer = document.getElementById("admin-health");
    const feedback = document.getElementById("admin-feedback");
    if (!healthContainer || !feedback) return;

    try {
        const health = await window.restaurantApi("/api/admin/health");
        healthContainer.replaceChildren();
        [
            ["Database", health.database],
            ["Staff", health.workers],
            ["Tables", health.tables],
            ["Orders", health.orders]
        ].forEach(([label, value]) => {
            const card = document.createElement("div");
            card.className = "role-stat";
            const title = document.createElement("span");
            title.textContent = label;
            const metric = document.createElement("strong");
            metric.textContent = value;
            card.append(title, metric);
            healthContainer.append(card);
        });

        const checkedAt = new Date(health.checkedAt);
        feedback.textContent = Number.isNaN(checkedAt.getTime())
            ? "System status refreshed."
            : `Checked ${checkedAt.toLocaleString()}.`;
    } catch (error) {
        window.showMessage(feedback, error.message, true);
    }
}

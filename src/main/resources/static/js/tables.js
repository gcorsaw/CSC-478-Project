(() => {
    const form = document.getElementById("table-form");
    const feedback = document.getElementById("table-feedback");
    const list = document.getElementById("table-list");

    async function loadTables() {
        const tables = await window.restaurantApi("/api/tables");
        list.replaceChildren();
        for (const table of tables) {
            const item = document.createElement("li");
            item.textContent = `Table ${table.tableNumber} — ${table.capacity} seats — ${table.status}`;
            list.append(item);
        }
    }

    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        const tableNumber = Number(document.getElementById("tableNumber").value);
        const capacity = Number(document.getElementById("capacity").value);
        try {
            const table = await window.restaurantApi("/api/tables", {
                method: "POST",
                body: JSON.stringify({ tableNumber, capacity })
            });
            window.showMessage(feedback, `Table ${table.tableNumber} added.`);
            form.reset();
            await loadTables();
        } catch (error) {
            window.showMessage(feedback, error.message, true);
        }
    });

    loadTables().catch((error) => window.showMessage(feedback, error.message, true));
})();

(() => {
    const grid = document.getElementById("table-grid");
    const form = document.getElementById("guest-form");
    const feedback = document.getElementById("waiter-feedback");
    let tables = [];

    function render() {
        grid.replaceChildren();
        const open = tables.filter((table) => table.status === "open").length;
        document.getElementById("open-count").textContent = tables.length - open;
        document.getElementById("closed-count").textContent = open;

        if (tables.length === 0) {
            const empty = document.createElement("p");
            empty.className = "waiter-empty";
            empty.textContent = "No tables are configured.";
            grid.append(empty);
            return;
        }

        for (const table of tables) {
            const occupied = table.status === "occupied";
            const card = document.createElement("article");
            card.className = `waiter-table-card${occupied ? " is-occupied" : ""}`;
            const title = document.createElement("h3");
            title.textContent = `Table ${table.tableNumber} · ${occupied ? "Occupied" : "Open"}`;
            const detail = document.createElement("p");
            detail.textContent = occupied
                ? `${table.guestName} · Party of ${table.partySize} · Seats ${table.capacity}`
                : `${table.capacity} seats`;
            card.append(title, detail);

            if (occupied) {
                const release = document.createElement("button");
                release.type = "button";
                release.className = "waiter-button waiter-button-secondary";
                release.textContent = "Mark table open";
                release.addEventListener("click", () => releaseTable(table.tableNumber));
                card.append(release);
            }
            grid.append(card);
        }
    }

    async function loadTables() {
        tables = await window.restaurantApi("/api/tables");
        render();
    }

    async function releaseTable(tableNumber) {
        try {
            await window.restaurantApi(`/api/tables/${tableNumber}/seating`, { method: "DELETE" });
            window.showMessage(feedback, `Table ${tableNumber} is open.`);
            await loadTables();
        } catch (error) {
            window.showMessage(feedback, error.message, true);
        }
    }

    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        const name = document.getElementById("guest-name").value.trim();
        const partySize = Number(document.getElementById("party-size").value);
        if (!name || !Number.isInteger(partySize) || partySize < 1 || partySize > 20) {
            window.showMessage(feedback, "Enter a guest name and a party size from 1 to 20.", true);
            return;
        }

        try {
            const table = await window.restaurantApi("/api/seatings", {
                method: "POST",
                body: JSON.stringify({ guestName: name, partySize })
            });
            window.showMessage(feedback, `${name} has been seated at table ${table.tableNumber}.`);
            form.reset();
            await loadTables();
        } catch (error) {
            window.showMessage(feedback, error.message, true);
        }
    });

    document.getElementById("show-all-tables").addEventListener("click", loadTables);
    loadTables().catch((error) => window.showMessage(feedback, error.message, true));
    window.setInterval(() => loadTables().catch((error) =>
        window.showMessage(feedback, error.message, true)), 10000);
})();

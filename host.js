(() => {
    const tableGrid = document.getElementById("table-grid");
    const guestForm = document.getElementById("guest-form");
    const guestNameInput = document.getElementById("guest-name");
    const partySizeInput = document.getElementById("party-size");
    const feedback = document.getElementById("guest-feedback");
    const instructions = document.getElementById("table-instructions");
    let tables = [];
    let currentParty = null;

    function updateCounts() {
        const open = tables.filter((table) => table.status === "open").length;
        document.getElementById("open-count").textContent = open;
        document.getElementById("occupied-count").textContent = tables.length - open;
    }

    function renderTables() {
        tableGrid.replaceChildren();
        const visibleTables = tables.filter((table) =>
            !currentParty
            || table.status !== "open"
            || table.capacity >= currentParty.partySize
        );

        if (visibleTables.length === 0) {
            const empty = document.createElement("p");
            empty.className = "host-empty";
            empty.textContent = currentParty
                ? "No open tables fit this party right now."
                : "No tables to show.";
            tableGrid.append(empty);
            return;
        }

        for (const table of visibleTables) {
            const occupied = table.status === "occupied";
            const card = document.createElement("article");
            card.className = `host-table-card${occupied ? " is-occupied" : ""}`;

            const topline = document.createElement("div");
            topline.className = "host-table-topline";
            const heading = document.createElement("h3");
            heading.className = "host-table-name";
            heading.textContent = `Table ${table.tableNumber}`;
            const status = document.createElement("span");
            status.className = "host-status";
            status.textContent = occupied ? "Occupied" : "Open";
            topline.append(heading, status);

            const detail = document.createElement("p");
            detail.className = "host-table-detail";
            detail.textContent = occupied
                ? `${table.guestName} · Party of ${table.partySize} · Seats ${table.capacity}`
                : `${table.capacity} seats`;
            card.append(topline, detail);

            const action = document.createElement("button");
            action.type = "button";
            action.className = occupied ? "host-button host-button-release" : "host-button";
            if (occupied) {
                action.textContent = "Guest has left · Mark open";
                action.setAttribute("aria-label", `Mark table ${table.tableNumber} open`);
                action.addEventListener("click", () => releaseTable(table.tableNumber));
            } else {
                action.textContent = currentParty ? "Seat this party here" : "Select a party first";
                action.disabled = !currentParty || table.capacity < currentParty.partySize;
                action.setAttribute("aria-label", `Seat party at table ${table.tableNumber}`);
                action.addEventListener("click", () => seatParty(table.tableNumber));
            }
            card.append(action);
            tableGrid.append(card);
        }
    }

    async function loadTables() {
        tables = await window.restaurantApi("/api/tables");
        updateCounts();
        renderTables();
    }

    async function seatParty(tableNumber) {
        if (!currentParty) return;
        try {
            const seated = await window.restaurantApi("/api/seatings", {
                method: "POST",
                body: JSON.stringify({
                    guestName: currentParty.name,
                    partySize: currentParty.partySize,
                    tableNumber
                })
            });
            window.showMessage(feedback,
                `${seated.guestName} has been seated at table ${seated.tableNumber}.`);
            currentParty = null;
            instructions.textContent = "When a guest leaves, mark their table open.";
            await loadTables();
        } catch (error) {
            window.showMessage(feedback, error.message, true);
            await loadTables();
        }
    }

    async function releaseTable(tableNumber) {
        try {
            await window.restaurantApi(`/api/tables/${tableNumber}/seating`, { method: "DELETE" });
            window.showMessage(feedback, `Table ${tableNumber} is open.`);
            await loadTables();
        } catch (error) {
            window.showMessage(feedback, error.message, true);
            await loadTables();
        }
    }

    guestForm.addEventListener("submit", (event) => {
        event.preventDefault();
        const name = guestNameInput.value.trim();
        const partySize = Number(partySizeInput.value);
        if (!name || !Number.isInteger(partySize) || partySize < 1 || partySize > 12) {
            window.showMessage(feedback, "Enter a guest name and a party size from 1 to 12.", true);
            return;
        }

        currentParty = { name, partySize };
        instructions.textContent = `Open tables that fit a party of ${partySize} are highlighted below.`;
        window.showMessage(feedback, `Welcome, ${name}! Choose an open table.`);
        renderTables();
    });

    document.getElementById("show-all-tables").addEventListener("click", () => {
        currentParty = null;
        instructions.textContent = "Choose a party above to find a suitable open table.";
        renderTables();
    });

    loadTables().catch((error) => window.showMessage(feedback, error.message, true));
    window.setInterval(() => loadTables().catch((error) =>
        window.showMessage(feedback, error.message, true)), 10000);
})();
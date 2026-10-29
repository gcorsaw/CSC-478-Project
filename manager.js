// Supervisor Queue Dispatcher Dashboard
async function refreshManagerApprovalQueue() {
    const layoutContainer = document.getElementById('supervisor-approval-deck');
    if (!layoutContainer) return;

    try {
        const response = await fetch('/api/v1/override/incoming');
        const tasks = await response.json();
        
        layoutContainer.innerHTML = ''; // Wipe stale dashboard card data
        
        tasks.forEach(task => {
            const viewCard = document.createElement('div');
            viewCard.className = 'supervisor-alert-card';
            viewCard.innerHTML = `
                <div class="card-details">
                    <h5>Ticket #${task.ticketId} (Table ${task.tableId})</h5>
                    <p>Staff: ${task.staffId} -> Request: <em>${task.details}</em></p>
                </div>
                <div class="card-controls">
                    <button class="btn-approve" onclick="resolveTicket(${task.ticketId}, 'APPROVE')">Approve</button>
                    <button class="btn-deny" onclick="resolveTicket(${task.ticketId}, 'DENY')">Deny</button>
                </div>
            `;
            layoutContainer.appendChild(viewCard);
        });
    } catch (e) {
        console.error("Could not fetch real-time supervisor operational arrays:", e);
    }
}

async function resolveTicket(id, authorizationDecision) {
    try {
        await fetch(`/api/v1/override/signoff?ticketId=${id}&decision=${authorizationDecision}`, { method: 'POST' });
        refreshManagerApprovalQueue(); // Redraw view layout models instantly
    } catch (err) {
        console.error("Failed to commit signature confirmation matrix down path mapping:", err);
    }
}

// Instantiate background refresh loops every 4 seconds to listen for floor waiter prompts
if (document.getElementById('supervisor-approval-deck')) {
    setInterval(refreshManagerApprovalQueue, 4000);
}

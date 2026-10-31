document.addEventListener('DOMContentLoaded', function() {
    loadKitchenOrders();
    setInterval(loadKitchenOrders, 5000); // Polling every 5s
});

function loadKitchenOrders() {
    fetch('/api/kitchen/orders')
        .then(response => response.json())
        .then(orders => {
            const container = document.getElementById('kitchen-orders-container');
            if (!container) return;
            container.innerHTML = '';

            if (orders.length === 0) {
                container.innerHTML = '<div class="alert alert-info">No active food orders to prepare.</div>';
                return;
            }

            orders.forEach(order => {
                const card = document.createElement('div');
                card.className = 'card mb-3 order-card';
                card.innerHTML = `
                    <div class="card-header d-flex justify-content-between align-items-center">
                        <h5>Order #${order.orderId} — Table ${order.tableNumber}</h5>
                        <span class="badge bg-warning text-dark">${order.status}</span>
                    </div>
                    <div class="card-body">
                        <p class="card-text"><strong>Items:</strong></p>
                        <ul>${order.items ? order.items.split(',').map(item => `<li>${item.trim()}</li>`).join('') : '<li>No details available</li>'}</ul>
                        <div class="d-flex gap-2 mt-3">
                            <button class="btn btn-success btn-sm" onclick="updateKitchenItemStatus(${order.orderId}, 'served')">Mark Ready</button>
                            <button class="btn btn-secondary btn-sm" onclick="updateKitchenItemStatus(${order.orderId}, 'preparing')">Set Preparing</button>
                        </div>
                    </div>
                `;
                container.appendChild(card);
            });
        })
        .catch(err => console.error('Chef workflow pipeline sync fault:', err));
}

function updateKitchenItemStatus(orderId, nextStatus) {
    fetch(`/api/kitchen/items/${orderId}/status`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ status: nextStatus })
    })
    .then(response => {
        if (response.ok) loadKitchenOrders();
        else alert('Failed to change order line production state.');
    });
}

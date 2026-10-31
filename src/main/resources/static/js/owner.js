// ============================================================================
// FULLY CORRECTED OWNER WORKSPACE JAVASCRIPT LAYER (owner.js)
// Location: src/main/resources/static/js/owner.js
// ============================================================================

document.addEventListener('DOMContentLoaded', function () {
    loadOwnerWorkspaceOverview();
    loadRecentOrdersTable();
});

/**
 * Fetches aggregate analytical summaries from /api/summary
 */
function loadOwnerWorkspaceOverview() {
    fetch('/api/summary')
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response failure fetching summary: ' + response.status);
            }
            return response.json();
        })
        .then(data => {
            // Bind variables directly to UI element IDs
            const workersElem = document.getElementById('metric-active-workers') || document.querySelector('[id*="workers"]');
            const totalTablesElem = document.getElementById('metric-total-tables') || document.querySelector('[id*="total-tables"]');
            const occupiedTablesElem = document.getElementById('metric-occupied-tables') || document.querySelector('[id*="occupied-tables"]');
            const totalOrdersElem = document.getElementById('metric-total-orders') || document.querySelector('[id*="total-orders"]');
            const openOrdersElem = document.getElementById('metric-open-orders') || document.querySelector('[id*="open-orders"]');
            const revenueElem = document.getElementById('metric-collected-revenue') || document.querySelector('[id*="collected"]') || document.querySelector('[id*="revenue"]');

            if (workersElem) workersElem.textContent = data.activeWorkers ?? 0;
            if (totalTablesElem) totalTablesElem.textContent = data.totalTables ?? 0;
            if (occupiedTablesElem) occupiedTablesElem.textContent = data.occupiedTables ?? 0;
            if (totalOrdersElem) totalOrdersElem.textContent = data.totalOrders ?? 0;
            if (openOrdersElem) openOrdersElem.textContent = data.openOrders ?? 0;
            
            if (revenueElem) {
                const rawCollected = parseFloat(data.collected || 0);
                revenueElem.textContent = '$' + rawCollected.toFixed(2);
            }
        })
        .catch(error => {
            console.error('Owner dashboard metrics runtime link fault:', error);
        });
}

/**
 * Table loader to fetch and display the raw continuous feed of order records
 */
function loadRecentOrdersTable() {
    fetch('/api/orders')
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response failure fetching orders: ' + response.status);
            }
            return response.json();
        })
        .then(orders => {
            const tableBody = document.getElementById('recent-orders-list') || document.querySelector('table tbody');
            if (!tableBody) return;

            tableBody.innerHTML = ''; // Clear fallback stub rows safely
            
            // Limit output slice to a clean reference window of the latest 5 entries
            const visibleOrders = orders.slice(0, 5);
            
            if (visibleOrders.length === 0) {
                tableBody.innerHTML = '<tr><td colspan="4" class="text-center">No recent orders found.</td></tr>';
                return;
            }

            visibleOrders.forEach(order => {
                const row = document.createElement('tr');
                row.innerHTML = `
                    <td><strong>#${order.orderId ?? 'N/A'}</strong></td>
                    <td>Table ${order.tableNumber ?? 'N/A'}</td>
                    <td><span class="badge status-${order.status ? order.status.toLowerCase() : 'open'}">${order.status ?? 'open'}</span></td>
                    <td>${order.createdAt ? order.createdAt : 'Just now'}</td>
                `;
                tableBody.appendChild(row);
            });
        })
        .catch(err => console.error('Failed to populate recent orders table feed:', err));
}

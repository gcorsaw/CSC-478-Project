// ============================================================================
// FULLY CORRECTED ADMIN INTERFACE JAVASCRIPT LAYER (admin.js)
// Location: src/main/resources/static/js/admin.js
// ============================================================================

document.addEventListener('DOMContentLoaded', function () {
    loadSystemHealthStatus();
    
    const refreshBtn = document.getElementById('refresh-health');
    if (refreshBtn) {
        refreshBtn.addEventListener('click', loadSystemHealthStatus);
    }
});

/**
 * Synchronizes real-time core platform health tracking metrics from the backend API
 */
function loadSystemHealthStatus() {
    fetch('/api/admin/health')
        .then(response => {
            if (!response.ok) {
                throw new Error('Server returned internal code: ' + response.status);
            }
            return response.json();
        })
        .then(health => {
            // Securely bind variables directly onto UI data container elements
            const dbElement = document.getElementById('health-db-status') || document.querySelector('[id*="db"]');
            const checkedElement = document.getElementById('health-checked-at') || document.querySelector('[id*="checked"]');
            const workersElement = document.getElementById('health-total-workers') || document.querySelector('[id*="workers"]');
            const tablesElement = document.getElementById('health-total-tables') || document.querySelector('[id*="tables"]');
            const ordersElement = document.getElementById('health-total-orders') || document.querySelector('[id*="orders"]');

            if (dbElement) dbElement.textContent = health.database ?? 'Connected';
            if (checkedElement) checkedElement.textContent = health.checkedAt ? new Date(health.checkedAt).toLocaleString() : 'Just now';
            if (workersElement) workersElement.textContent = health.workers ?? 0;
            if (tablesElement) tablesElement.textContent = health.tables ?? 0;
            if (ordersElement) ordersElement.textContent = health.orders ?? 0;
        })
        .catch(error => {
            console.error('Admin tracking synchronization fault:', error);
            const statusBox = document.getElementById('health-status-error');
            if (statusBox) {
                statusBox.textContent = 'Could not synchronize active maintenance data vectors.';
                statusBox.style.display = 'block';
            }
        });
}

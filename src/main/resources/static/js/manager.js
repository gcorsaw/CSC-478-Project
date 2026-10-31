document.addEventListener('DOMContentLoaded', function() {
    loadSystemHealthStatus();
    loadActiveStaffDirectory();
});

function loadSystemHealthStatus() {
    fetch('/api/admin/health')
        .then(res => res.json())
        .then(health => {
            const dbStatus = document.getElementById('health-db-status');
            const totalStaff = document.getElementById('health-total-staff');
            if (dbStatus) dbStatus.textContent = health.database;
            if (totalStaff) totalStaff.textContent = health.workers;
        });
}

function loadActiveStaffDirectory() {
    fetch('/api/workers')
        .then(res => res.json())
        .then(workers => {
            const tableBody = document.getElementById('staff-directory-body');
            if (!tableBody) return;
            tableBody.innerHTML = '';

            workers.forEach(worker => {
                const row = document.createElement('tr');
                row.innerHTML = `
                    <td><strong>#${worker.workerId}</strong></td>
                    <td>${worker.firstName} ${worker.lastName}</td>
                    <td><code>${worker.username}</code></td>
                    <td><span class="badge bg-secondary">${worker.roles ? worker.roles.join(', ') : 'STAFF'}</span></td>
                    <td><span class="badge ${worker.active ? 'bg-success' : 'bg-danger'}">${worker.active ? 'Active' : 'Inactive'}</span></td>
                `;
                tableBody.appendChild(row);
            });
        });
}

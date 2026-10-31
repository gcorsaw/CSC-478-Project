// ============================================================================
// FULLY REWRITTEN TABLE MANAGEMENT CONTROLLER (tables.js)
// Location: src/main/resources/static/js/tables.js
// ============================================================================

document.addEventListener('DOMContentLoaded', function () {
    loadActiveTablesGrid();
    setupAddTableFormEngine();
});

function loadActiveTablesGrid() {
    fetch('/api/tables')
        .then(response => {
            if (!response.ok) throw new Error('Failed to load table dataset.');
            return response.json();
        })
        .then(tables => {
            const tableListContainer = document.getElementById('current-tables-list') || document.querySelector('.Current');
            if (!tableListContainer) return;
            tableListContainer.innerHTML = '';

            if (tables.length === 0) {
                tableListContainer.innerHTML = '<p class="text-muted p-3">No tables registered.</p>';
                return;
            }

            tables.forEach(table => {
                const recordRow = document.createElement('div');
                recordRow.className = 'd-flex justify-content-between align-items-center border-bottom p-2';
                recordRow.style.backgroundColor = '#fff';
                recordRow.innerHTML = `
                    <div>
                        <strong>Table ${table.tableNumber}</strong> 
                        <span class="text-muted small">(Capacity: ${table.capacity})</span>
                    </div>
                    <div>
                        <span class="badge bg-secondary">${table.status.toUpperCase()}</span>
                    </div>
                `;
                tableListContainer.appendChild(recordRow);
            });
        })
        .catch(error => {
            console.error('Table grid refresh process error:', error);
        });
}

function setupAddTableFormEngine() {
    const tableForm = document.querySelector('form');
    if (!tableForm) return;

    tableForm.addEventListener('submit', function (e) {
        e.preventDefault();
        
        const numberInput = tableForm.querySelector('input[type="number"], #table-number');
        const capacityInput = tableForm.querySelectorAll('input[type="number"]')[1] || document.getElementById('table-capacity');

        if (!numberInput || !capacityInput) return;

        const payload = {
            tableNumber: parseInt(numberInput.value),
            capacity: parseInt(capacityInput.value)
        };

        fetch('/api/tables', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        })
        .then(response => {
            if (!response.ok) throw new Error('Violation of table identification key rules.');
            tableForm.reset();
            loadActiveTablesGrid();
        })
        .catch(err => {
            alert('Could not save table configuration. Check for duplicate numbers.');
        });
    });
}

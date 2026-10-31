// ============================================================================
// FINAL COMPLIANT TABLE MANAGEMENT LAYER (tables.js)
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
            // Safe fallback selector matrix to bind data to the UI container
            const tableListContainer = document.getElementById('tables-list') || 
                                       document.getElementById('current-tables-list') || 
                                       document.querySelector('.Current') || 
                                       document.body.querySelector('div:last-child');
            if (!tableListContainer) return;
            tableListContainer.innerHTML = '';

            if (tables.length === 0) {
                tableListContainer.innerHTML = '<p class="text-muted p-3">No active tables registered.</p>';
                return;
            }

            tables.forEach(table => {
                const recordRow = document.createElement('div');
                recordRow.className = 'd-flex justify-content-between align-items-center border-bottom p-2 mb-2';
                recordRow.style.backgroundColor = '#fff';
                recordRow.style.border = '1px solid #ddd';
                recordRow.style.borderRadius = '4px';
                recordRow.style.padding = '12px';
                
                recordRow.innerHTML = `
                    <div>
                        <strong>Table ${table.tableNumber}</strong> 
                        <span class="text-muted small">(Max Capacity: ${table.capacity})</span>
                    </div>
                    <div>
                        <span class="badge ${table.status.toLowerCase() === 'available' ? 'bg-success' : 'bg-warning text-dark'}" style="padding: 6px 12px; border-radius: 4px; text-transform: uppercase; font-size: 0.8rem; font-weight: bold;">
                            ${table.status}
                        </span>
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
    const tableForm = document.querySelector('form') || document.getElementById('add-table-form');
    if (!tableForm) return;

    tableForm.addEventListener('submit', function (e) {
        e.preventDefault();
        
        const numberInput = document.getElementById('tableNumber') || tableForm.querySelector('input[type="number"]');
        const capacityInput = document.getElementById('capacity') || tableForm.querySelectorAll('input[type="number"]')[1];

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

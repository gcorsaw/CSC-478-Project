// ============================================================================
// FINAL COMPLIANT TABLE MANAGEMENT LAYER (tables.js)
// Location: src/main/resources/static/js/tables.js
// ============================================================================

document.addEventListener('DOMContentLoaded', function () {
    loadActiveTablesGrid();
    setupAddTableFormEngine();
});

/**
 * Synchronizes and displays the complete restaurant floor map table entries
 */
function loadActiveTablesGrid() {
    fetch('/api/tables')
        .then(response => {
            if (!response.ok) throw new Error('Failed to load table dataset.');
            return response.json();
        })
        .then(tables => {
            const tableListContainer = document.getElementById('tables-list') || 
                                       document.getElementById('current-tables-list') || 
                                       document.querySelector('.Current');
            if (!tableListContainer) return;
            tableListContainer.innerHTML = '';

            if (tables.length === 0) {
                tableListContainer.innerHTML = '<p class="text-muted p-3">No active tables registered.</p>';
                return;
            }

            tables.forEach(table => {
                const recordRow = document.createElement('div');
                recordRow.className = 'd-flex justify-content-between align-items-center border-bottom p-3 mb-2';
                recordRow.style.backgroundColor = '#fff';
                recordRow.style.border = '1px solid #e0e0e0';
                recordRow.style.borderRadius = '6px';
                
                recordRow.innerHTML = `
                    <div>
                        <strong style="font-size: 1.1rem; color: #1b5e20;">Table ${table.tableNumber}</strong> 
                        <span class="text-muted small" style="margin-left: 10px;">(Max Capacity: ${table.capacity} guests)</span>
                    </div>
                    <div>
                        <span class="badge" style="padding: 6px 12px; border-radius: 4px; text-transform: uppercase; font-size: 0.75rem; font-weight: bold; background-color: ${table.status.toLowerCase() === 'available' ? '#e8f5e9; color: #2e7d32;' : '#fff3e0; color: #ef6c00;'}">
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

/**
 * Extracts form field items cleanly and handles secure POST binding payload generation
 */
function setupAddTableFormEngine() {
    const tableForm = document.querySelector('form') || document.getElementById('add-table-form');
    if (!tableForm) return;

    tableForm.addEventListener('submit', function (e) {
        e.preventDefault();
        
        const numberInput = tableForm.querySelector('input:nth-of-type(1)');
        const capacityInput = tableForm.querySelector('input:nth-of-type(2)');

        if (!numberInput || !capacityInput || !numberInput.value || !capacityInput.value) {
            alert('Please specify values for both input fields.');
            return;
        }

        const payload = {
            tableNumber: parseInt(numberInput.value, 10),
            capacity: parseInt(capacityInput.value, 10)
        };

        fetch('/api/tables', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        })
        .then(response => {
            if (!response.ok) throw new Error('Server validation failure.');
            tableForm.reset();
            loadActiveTablesGrid(); // Instantly update view grid without manual reload loops
        })
        .catch(err => {
            console.error('Add Table validation tracking fault:', err);
            alert('Could not save table configuration. Ensure the table number is unique.');
        });
    });
}

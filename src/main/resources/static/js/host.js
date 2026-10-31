document.addEventListener('DOMContentLoaded', function() {
    loadFloorMapLayout();
    setupHostFormListeners();
});

function loadFloorMapLayout() {
    fetch('/api/tables')
        .then(response => response.json())
        .then(tables => {
            const grid = document.getElementById('host-floor-grid');
            if (!grid) return;
            grid.innerHTML = '';

            tables.forEach(table => {
                const element = document.createElement('div');
                element.className = `table-box status-${table.status.toLowerCase()} p-3 border text-center`;
                element.innerHTML = `
                    <h4>Table ${table.tableNumber}</h4>
                    <p class="small text-muted">Capacity: ${table.capacity}</p>
                    <p><strong>${table.status.toUpperCase()}</strong></p>
                    ${table.guestName ? `<p class="small text-truncate">Guest: ${table.guestName} (${table.partySize})</p>` : ''}
                    ${table.status.toLowerCase() === 'occupied' ? 
                        `<button class="btn btn-danger btn-xs mt-2" onclick="clearTableSeating(${table.tableNumber})">Vacate</button>` : ''}
                `;
                grid.appendChild(element);
            });
        })
        .catch(err => console.error('Floor structure layout fetch exception:', err));
}

function setupHostFormListeners() {
    const form = document.getElementById('seating-form');
    if (!form) return;
    form.addEventListener('submit', function(e) {
        e.preventDefault();
        const payload = {
            guestName: document.getElementById('guest-name').value,
            partySize: parseInt(document.getElementById('party-size').value),
            tableNumber: parseInt(document.getElementById('target-table').value)
        };

        fetch('/api/seatings', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        }).then(res => {
            if (res.ok) {
                form.reset();
                loadFloorMapLayout();
            } else {
                alert('Table seating constraint breach or table already occupied.');
            }
        });
    });
}

function clearTableSeating(tableNumber) {
    fetch(`/api/tables/${tableNumber}/seating`, { method: 'DELETE' })
        .then(res => { if (res.ok) loadFloorMapLayout(); });
}

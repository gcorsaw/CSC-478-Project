document.addEventListener('DOMContentLoaded', function() {
    loadWaiterSelectors();
    setupWaiterAssignmentForm();
});

function loadWaiterSelectors() {
    fetch('/api/waiters')
        .then(response => response.json())
        .then(waiters => {
            const selector = document.getElementById('waiter-selection-dropdown');
            if (!selector) return;
            selector.innerHTML = '<option value="">-- Choose Waiter --</option>';
            waiters.forEach(waiter => {
                const opt = document.createElement('option');
                opt.value = waiter.waiterId;
                opt.textContent = `${waiter.firstName} ${waiter.lastName} ${waiter.active ? '(Active)' : '(Offline)'}`;
                selector.appendChild(opt);
            });
        });
}

function setupWaiterAssignmentForm() {
    const form = document.getElementById('waiter-assignment-form');
    if (!form) return;
    form.addEventListener('submit', function(e) {
        e.preventDefault();
        const payload = {
            tableNumber: parseInt(document.getElementById('assign-table-number').value),
            waiterId: parseInt(document.getElementById('waiter-selection-dropdown').value)
        };

        fetch('/api/table-assignments', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        }).then(res => {
            if (res.ok) {
                alert('Waiter section assignment verified successfully.');
                form.reset();
            } else {
                alert('Error making section assignment. Check ID properties.');
            }
        });
    });
}

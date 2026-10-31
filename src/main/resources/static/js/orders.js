// ============================================================================
// FULLY INTERACTIVE ORDERS CONTROL LOGIC (orders.js)
// Location: src/main/resources/static/js/orders.js
// ============================================================================

let currentOrderItems = [];

document.addEventListener('DOMContentLoaded', function() {
    initializeOrderFormDropdowns();
    setupOrderActionListeners();
    loadRecentOrdersFeed();
});

/**
 * Dynamically queries active database records to populate the selector fields
 */
function initializeOrderFormDropdowns() {
    // 1. Populate Active Waiters Dropdown
    fetch('/api/waiters')
        .then(res => res.json())
        .then(waiters => {
            const waiterSelect = document.querySelector('select[id*="waiter"]') || document.querySelector('select:nth-of-type(1)');
            if (!waiterSelect) return;
            waiterSelect.innerHTML = '<option value="">Select an active waiter...</option>';
            waiters.forEach(w => {
                if (w.active) {
                    const opt = document.createElement('option');
                    opt.value = w.waiterId;
                    opt.textContent = `${w.firstName} ${w.lastName}`;
                    waiterSelect.appendChild(opt);
                }
            });
        }).catch(err => console.error('Failed to load form waiters:', err));

    // 2. Populate Dining Tables Dropdown
    fetch('/api/tables')
        .then(res => res.json())
        .then(tables => {
            const tableSelect = document.querySelector('select[id*="table"]') || document.querySelector('select:nth-of-type(2)');
            if (!tableSelect) return;
            tableSelect.innerHTML = '<option value="">Select a dining table...</option>';
            tables.forEach(t => {
                const opt = document.createElement('option');
                opt.value = t.tableNumber;
                opt.textContent = `Table ${t.tableNumber} (Capacity: ${t.capacity}) — ${t.status}`;
                tableSelect.appendChild(opt);
            });
        }).catch(err => console.error('Failed to load form tables:', err));

    // 3. Populate Menu Items Dropdown
    fetch('/api/menu')
        .then(res => res.json())
        .then(menuItems => {
            const itemSelect = document.querySelector('select[id*="menu"]') || document.querySelector('select[id*="item"]') || document.querySelector('select:nth-of-type(3)');
            if (!itemSelect) return;
            itemSelect.innerHTML = '<option value="">Choose menu items...</option>';
            menuItems.forEach(item => {
                const opt = document.createElement('option');
                opt.value = item.itemId;
                opt.textContent = `${item.itemName} ($${parseFloat(item.price).toFixed(2)})`;
                itemSelect.appendChild(opt);
            });
        }).catch(err => console.error('Failed to load form menu selections:', err));
}

/**
 * Handles adding dynamic list records to order staging and saving data states
 */
function setupOrderActionListeners() {
    const addItemBtn = document.getElementById('addItemBtn') || document.querySelector('button[id*="Add"]') || document.querySelector('button:nth-of-type(1)');
    const saveOrderBtn = document.getElementById('saveOrderBtn') || document.querySelector('button[id*="Save"]') || document.querySelector('button:nth-of-type(2)');
    const orderItemsContainer = document.getElementById('items-in-order') || document.querySelector('.Items') || document.body;

    if (addItemBtn) {
        addItemBtn.addEventListener('click', function() {
            const itemSelect = document.querySelector('select[id*="menu"]') || document.querySelector('select[id*="item"]') || document.querySelector('select:nth-of-type(3)');
            const quantityInput = document.querySelector('input[type="number"]') || document.getElementById('quantity');
            
            if (!itemSelect || !itemSelect.value || !quantityInput || parseInt(quantityInput.value) <= 0) {
                alert('Please select a valid menu item and specify a quantity.');
                return;
            }

            const itemId = parseInt(itemSelect.value);
            const itemName = itemSelect.options[itemSelect.selectedIndex].text;
            const quantity = parseInt(quantityInput.value);

            // Stage item locally
            currentOrderItems.push({ itemId: itemId, quantity: quantity });

            // Dynamically build list items to show structured state on HTML page
            let listWrapper = document.getElementById('staged-items-list');
            if (!listWrapper) {
                listWrapper = document.createElement('ul');
                listWrapper.id = 'staged-items-list';
                listWrapper.className = 'list-group mb-3';
                orderItemsContainer.appendChild(listWrapper);
            }

            const li = document.createElement('li');
            li.className = 'list-group-item d-flex justify-content-between align-items-center';
            li.innerHTML = `<span>${itemName}</span> <span class="badge bg-primary rounded-pill">Qty: ${quantity}</span>`;
            listWrapper.appendChild(li);

            // Reset selection values safely
            itemSelect.value = '';
            quantityInput.value = '1';
        });
    }

    if (saveOrderBtn) {
        saveOrderBtn.addEventListener('click', function() {
            const tableSelect = document.querySelector('select[id*="table"]') || document.querySelector('select:nth-of-type(2)');
            if (!tableSelect || !tableSelect.value) {
                alert('Please pick a destination table number to assign this check.');
                return;
            }
            if (currentOrderItems.length === 0) {
                alert('Add at least one menu item before saving this order transaction.');
                return;
            }

            const payload = {
                tableNumber: parseInt(tableSelect.value),
                items: currentOrderItems
            };

            fetch('/api/orders', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            }).then(response => {
                if (response.ok) {
                    alert('Order dispatched successfully to kitchen station queues.');
                    currentOrderItems = [];
                    const listWrapper = document.getElementById('staged-items-list');
                    if (listWrapper) listWrapper.innerHTML = '';
                    tableSelect.value = '';
                    loadRecentOrdersFeed();
                } else {
                    alert('Server rejected structural order mapping body constraints.');
                }
            });
        });
    }
}

/**
 * Loads recent operational tickets directly from /api/orders
 */
function loadRecentOrdersFeed() {
    fetch('/api/orders')
        .then(res => res.json())
        .then(orders => {
            const tableBody = document.querySelector('table tbody') || document.getElementById('recent-orders-list');
            if (!tableBody) return;
            tableBody.innerHTML = '';

            if (orders.length === 0) {
                tableBody.innerHTML = '<tr><td colspan="4" class="text-center text-muted">No recent orders registered.</td></tr>';
                return;
            }

            orders.forEach(order => {
                const row = document.createElement('tr');
                row.innerHTML = `
                    <td><strong>#${order.orderId}</strong></td>
                    <td>Table ${order.tableNumber}</td>
                    <td><span class="badge status-${order.status.toLowerCase()}">${order.status.toUpperCase()}</span></td>
                    <td><small>${order.items ?? 'No detail records'}</small></td>
                `;
                tableBody.appendChild(row);
            });
        }).catch(err => console.error('Fault rendering real-time incoming records stream:', err));
}

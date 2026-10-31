document.addEventListener('DOMContentLoaded', function() {
    loadPublicRestaurantMenu();
});

function loadPublicRestaurantMenu() {
    fetch('/api/menu')
        .then(res => {
            if (!res.ok) throw new Error('Menu dataset unavailable.');
            return res.json();
        })
        .then(items => {
            const menuGrid = document.getElementById('public-menu-grid');
            if (!menuGrid) return;
            menuGrid.innerHTML = '';

            items.forEach(item => {
                const block = document.createElement('div');
                block.className = 'col-md-4 mb-3';
                block.innerHTML = `
                    <div class="card h-100 shadow-sm">
                        <div class="card-body">
                            <span class="badge bg-primary float-end">${item.category}</span>
                            <h5 class="card-title">${item.itemName}</h5>
                            <p class="card-text text-success font-monospace fw-bold">$${parseFloat(item.price).toFixed(2)}</p>
                        </div>
                    </div>
                `;
                menuGrid.appendChild(block);
            });
        })
        .catch(err => console.error('Menu card generation engine failure:', err));
}

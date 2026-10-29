// Example implementation for kitchen.js dashboard updating the backend
const API_URL = '/api/orders';

async function loadKitchenOrders() {
    try {
        const response = await fetch(API_URL);
        const orders = await response.json();
        renderKitchenDisplay(orders);
    } catch (error) {
        console.error("Error loading kitchen orders:", error);
    }
}

async function completeOrder(orderId) {
    try {
        await fetch(`${API_URL}/${orderId}/status`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ status: 'COMPLETED' })
        });
        loadKitchenOrders(); // Refresh dashboard view
    } catch (error) {
        console.error("Error completing order:", error);
    }
}

// Poll for live kitchen orders updates every 5 seconds
setInterval(loadKitchenOrders, 5000);
window.onload = loadKitchenOrders;

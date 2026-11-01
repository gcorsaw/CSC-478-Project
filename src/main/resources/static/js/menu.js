document.addEventListener("DOMContentLoaded", loadMenu);

async function loadMenu() {
    const menuContent = document.getElementById("menu-content");
    const feedback = document.getElementById("menu-feedback");
    if (!menuContent || !feedback) return;

    try {
        const items = await window.restaurantApi("/api/menu");
        menuContent.replaceChildren();

        if (items.length === 0) {
            feedback.textContent = "No menu items are currently available.";
            return;
        }

        feedback.textContent = "";
        items.forEach((item) => {
            const card = document.createElement("article");
            card.className = "menu-card";

            const name = document.createElement("h2");
            name.textContent = item.itemName;
            const category = document.createElement("p");
            category.textContent = item.category;
            const price = document.createElement("p");
            price.className = "menu-price";
            price.textContent = Number(item.price).toLocaleString("en-US", {
                style: "currency",
                currency: "USD"
            });

            card.append(name, category, price);
            menuContent.append(card);
        });
    } catch (error) {
        window.showMessage(feedback, error.message, true);
    }
}

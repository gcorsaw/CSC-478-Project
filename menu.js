(() => {
    const menuElement = document.getElementById("menu-content");
    const feedback = document.getElementById("menu-feedback");
    const labels = {
        starter: "Starters",
        main: "Main Courses",
        dessert: "Desserts",
        drink: "Beverages"
    };

    async function loadMenu() {
        const items = await window.restaurantApi("/api/menu");
        menuElement.replaceChildren();
        for (const [category, title] of Object.entries(labels)) {
            const categoryItems = items.filter((item) => item.category === category);
            const section = document.createElement("section");
            const heading = document.createElement("h2");
            heading.textContent = title;
            const list = document.createElement("ul");
            for (const item of categoryItems) {
                const row = document.createElement("li");
                row.textContent = `${item.itemName} — $${Number(item.price).toFixed(2)}`;
                list.append(row);
            }
            section.append(heading, list);
            menuElement.append(section);
        }
    }

    loadMenu().catch((error) => window.showMessage(feedback, error.message, true));
})();
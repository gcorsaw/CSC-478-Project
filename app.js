window.restaurantApi = async function restaurantApi(path, options = {}) {
    const response = await fetch(path, {
        ...options,
        headers: {
            "Content-Type": "application/json",
            ...options.headers
        }
    });

    const contentType = response.headers.get("content-type") || "";
    const body = response.status === 204
        ? null
        : contentType.includes("application/json")
            ? await response.json()
            : { error: await response.text() };
    if (!response.ok) {
        throw new Error(body?.error || `Request failed (${response.status}).`);
    }
    return body;
};

window.showMessage = function showMessage(element, message, isError = false) {
    element.textContent = message;
    element.classList.toggle("is-error", isError);
};
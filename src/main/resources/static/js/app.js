window.restaurantApi = async function restaurantApi(path, options = {}) {
    const method = (options.method || "GET").toUpperCase();
    const headers = { "Content-Type": "application/json", ...options.headers };
    if (!["GET", "HEAD", "OPTIONS"].includes(method)) {
        const csrfResponse = await fetch("/api/auth/csrf");
        const csrfBody = await csrfResponse.json();
        headers["X-XSRF-TOKEN"] = csrfBody.token;
    }
    const response = await fetch(path, {
        ...options,
        method,
        headers
    });

    const contentType = response.headers.get("content-type") || "";
    const body = response.status === 204
        ? null
        : contentType.includes("application/json")
            ? await response.json()
            : { error: contentType.includes("text/html") ? null : await response.text() };
    if (!response.ok) {
        if (response.status === 401 && !window.location.pathname.endsWith("/login.html")) {
            const returnTo = `${window.location.pathname}${window.location.search}${window.location.hash}`;
            window.location.assign(`/login.html?returnTo=${encodeURIComponent(returnTo)}`);
        }
        if (response.status === 403) {
            throw new Error("Your account does not have permission to access this information.");
        }
        throw new Error(body?.error || `Request failed (${response.status}).`);
    }
    return body;
};

document.addEventListener("DOMContentLoaded", () => {
    const header = document.querySelector(".staff-header");
    if (!header) return;

    const signOut = document.createElement("button");
    signOut.className = "auth-sign-out";
    signOut.type = "button";
    signOut.textContent = "Sign out";
    signOut.addEventListener("click", async () => {
        const csrfResponse = await fetch("/api/auth/csrf");
        const csrfBody = await csrfResponse.json();
        await fetch("/logout", {
            method: "POST",
            headers: { "X-XSRF-TOKEN": csrfBody.token }
        });
        window.location.assign("/login.html?logout");
    });
    header.append(signOut);
});

window.showMessage = function showMessage(element, message, isError = false) {
    element.textContent = message;
    element.classList.toggle("is-error", isError);
};

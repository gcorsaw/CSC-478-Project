const ownerSetupForm = document.getElementById("owner-setup-form");
const ownerSetupFeedback = document.getElementById("owner-setup-feedback");
const ownerSetupUnavailable = document.getElementById("owner-setup-unavailable");

fetch("/api/auth/owner-setup-available")
    .then((response) => response.json())
    .then(({ available }) => {
        ownerSetupForm.hidden = !available;
        if (!available) {
            ownerSetupUnavailable.textContent = "An Owner login is already configured.";
        }
    })
    .catch(() => {
        ownerSetupForm.hidden = true;
        ownerSetupUnavailable.textContent = "Unable to check Owner setup availability.";
    });

ownerSetupForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    ownerSetupFeedback.textContent = "";
    ownerSetupFeedback.classList.remove("is-error");

    try {
        const csrfResponse = await fetch("/api/auth/csrf");
        const csrfBody = await csrfResponse.json();
        const response = await fetch("/api/auth/owner-setup", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "X-XSRF-TOKEN": csrfBody.token
            },
            body: JSON.stringify(Object.fromEntries(new FormData(ownerSetupForm)))
        });
        const responseBody = await response.json();
        if (!response.ok) {
            throw new Error(responseBody.error || `Owner setup failed (${response.status}).`);
        }
        window.location.assign(
            `/login.html?role=owner&returnTo=%2Fowner.html&signup=owner&username=${encodeURIComponent(responseBody.username)}`
        );
    } catch (error) {
        ownerSetupFeedback.textContent = error.message || "Unable to create the Owner account.";
        ownerSetupFeedback.classList.add("is-error");
    }
});
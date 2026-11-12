const loginForm = document.getElementById("login-form");
const feedback = document.getElementById("login-feedback");
const loginParams = new URLSearchParams(window.location.search);

if (loginParams.get("role") === "owner") {
    document.title = "Owner sign in | Green Olives";
    document.querySelector(".staff-header span").textContent = "Owner sign in";
    document.querySelector(".login-main h1").textContent = "Owner sign in";
    loginForm.elements.username.value = "owner";
}

const ownerSetupLink = document.getElementById("owner-setup-link");
fetch("/api/auth/owner-setup-available")
    .then((response) => response.json())
    .then(({ available }) => {
        ownerSetupLink.hidden = !available;
    })
    .catch(() => {
        ownerSetupLink.hidden = true;
    });

loginForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    feedback.textContent = "";
    feedback.classList.remove("is-error");

    try {
        const csrfResponse = await fetch("/api/auth/csrf");
        const csrfBody = await csrfResponse.json();
        const formData = new FormData(loginForm);
        const loginResponse = await fetch("/login", {
            method: "POST",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded",
                "X-XSRF-TOKEN": csrfBody.token
            },
            body: new URLSearchParams(formData)
        });

        const responseDestination = new URL(loginResponse.url);
        if (responseDestination.pathname === "/login.html") {
            throw new Error("The username or password was not accepted.");
        }
        const requestedDestination = loginParams.get("returnTo");
        const destination = requestedDestination
            ? new URL(requestedDestination, window.location.origin)
            : responseDestination;
        if (destination.origin !== window.location.origin) {
            throw new Error("The sign-in destination is invalid.");
        }
        window.location.assign(destination.href);
    } catch (error) {
        feedback.textContent = error.message || "Unable to sign in. Please try again.";
        feedback.classList.add("is-error");
    }
});

if (loginParams.has("logout")) {
    feedback.textContent = "You have been signed out.";
}

if (loginParams.has("signup")) {
    feedback.textContent = "Account created. Sign in with your new credentials.";
    const newUsername = loginParams.get("username");
    if (newUsername) loginForm.elements.username.value = newUsername;
}

if (loginParams.has("error")) {
    feedback.textContent = "The username or password was not accepted.";
    feedback.classList.add("is-error");
}
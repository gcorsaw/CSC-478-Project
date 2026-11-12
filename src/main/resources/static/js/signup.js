const signupForm = document.getElementById("signup-form");
const signupFeedback = document.getElementById("signup-feedback");

signupForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    signupFeedback.textContent = "";
    signupFeedback.classList.remove("is-error");

    try {
        const csrfResponse = await fetch("/api/auth/csrf");
        const csrfBody = await csrfResponse.json();
        const signupResponse = await fetch("/api/auth/signup", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "X-XSRF-TOKEN": csrfBody.token
            },
            body: JSON.stringify(Object.fromEntries(new FormData(signupForm)))
        });
        const responseBody = await signupResponse.json();
        if (!signupResponse.ok) {
            throw new Error(responseBody.error || `Account creation failed (${signupResponse.status}).`);
        }
        window.location.assign("/login.html?signup=success");
    } catch (error) {
        signupFeedback.textContent = error.message || "Unable to create the account. Please try again.";
        signupFeedback.classList.add("is-error");
    }
});
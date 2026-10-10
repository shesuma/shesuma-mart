document.addEventListener("DOMContentLoaded", () => {
    const form = document.getElementById("loginForm");
    const message = document.getElementById("loginMessage");
    const button = document.getElementById("loginButton");

    if (!form || !message || !button) {
        console.error("Login form elements were not found.");
        return;
    }

    form.addEventListener("submit", async (event) => {
        event.preventDefault();

        const email = document.getElementById("email").value.trim();
        const password = document.getElementById("password").value;

        button.disabled = true;
        button.textContent = "Logging in...";
        message.textContent = "";

        try {
            const response = await fetch("api/v1/auth/login", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                credentials: "same-origin",
                body: JSON.stringify({ email, password })
            });

            const text = await response.text();
            let data;

            try {
                data = JSON.parse(text);
            } catch {
                throw new Error(
                    `Server returned an invalid response (HTTP ${response.status}).`
                );
            }

            if (!response.ok || data.success !== true) {
                throw new Error(
                    data.message || `Login failed (HTTP ${response.status}).`
                );
            }

            message.textContent = "Login successful! Opening ShesumaMart...";

            window.location.href = "index.html";

        } catch (error) {
            message.textContent =
                error.message || "Unable to connect to the server.";
        } finally {
            button.disabled = false;
            button.textContent = "Login";
        }
    });
});
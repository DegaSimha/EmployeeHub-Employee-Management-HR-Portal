(() => {
  const form = document.getElementById("login-form");
  form.addEventListener("submit", async event => {
    event.preventDefault();
    const button = form.querySelector("button[type=submit]");
    const error = document.getElementById("login-error");
    error.textContent = "";
    button.disabled = true;
    button.textContent = "Signing in…";
    const data = new FormData(form);
    try {
      const result = await Api.post("auth/login", {
        email: data.get("email"), password: data.get("password")
      });
      Api.saveSession(result);
      window.location.hash = "dashboard";
      window.dispatchEvent(new Event("employeehub:authenticated"));
    } catch (reason) {
      error.textContent = reason.message;
    } finally {
      button.disabled = false;
      button.innerHTML = 'Sign in <span aria-hidden="true">→</span>';
    }
  });
})();

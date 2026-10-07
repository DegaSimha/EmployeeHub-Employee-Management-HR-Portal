(() => {
  const TOKEN_KEY = "employeehub.token";
  const USER_KEY = "employeehub.user";
  const token = () => sessionStorage.getItem(TOKEN_KEY);
  async function request(path, options = {}) {
    const headers = new Headers(options.headers || {});
    headers.set("Accept", "application/json");
    if (options.body !== undefined) headers.set("Content-Type", "application/json");
    if (token()) headers.set("Authorization", `Bearer ${token()}`);
    let response;
    try {
      response = await fetch(path.startsWith("/api/") ? path : `/api/${path}`, {
        ...options, headers, body: options.body === undefined ? undefined : JSON.stringify(options.body)
      });
    } catch {
      throw new Error("Unable to connect to the server. Please make sure the Spring Boot application is running.");
    }
    if (response.status === 401 && token()) {
      sessionStorage.removeItem(TOKEN_KEY);
      sessionStorage.removeItem(USER_KEY);
      window.dispatchEvent(new CustomEvent("employeehub:session-expired"));
      throw new Error("Your session has expired. Please sign in again.");
    }
    if (!response.ok) {
      let message = `Request failed (${response.status}).`;
      try {
        const body = await response.json();
        if (body.message) message = body.message;
      } catch { /* The server may return a non-JSON error page. */ }
      throw new Error(message);
    }
    if (response.status === 204) return null;
    return response.json();
  }
  window.Api = {
    get: path => request(path),
    post: (path, body) => request(path, { method: "POST", body }),
    put: (path, body) => request(path, { method: "PUT", body }),
    delete: path => request(path, { method: "DELETE" }),
    saveSession(result) {
      sessionStorage.setItem(TOKEN_KEY, result.token);
      sessionStorage.setItem(USER_KEY, JSON.stringify(result));
    },
    clearSession() {
      sessionStorage.removeItem(TOKEN_KEY);
      sessionStorage.removeItem(USER_KEY);
    },
    getUser() {
      try { return JSON.parse(sessionStorage.getItem(USER_KEY) || "null"); }
      catch { return null; }
    },
    isAuthenticated: () => Boolean(token())
  };
})();

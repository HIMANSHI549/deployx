const baseUrl = import.meta.env.VITE_API_URL ?? "http://localhost:8080/api/v1";

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const response = await fetch(`${baseUrl}${path}`, {
    ...options,
    headers: { "Content-Type": "application/json", ...(options.headers ?? {}) },
  });
  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new Error(body.message ?? `Request failed (${response.status})`);
  }
  return response.status === 204 ? (undefined as T) : response.json();
}

export const api = {
  auth: (mode: "login" | "register", email: string, password: string) =>
    request<{ token: string; email: string }>(`/auth/${mode}`, { method: "POST", body: JSON.stringify({ email, password }) }),
  projects: (token: string) => request<{ content: import("../types/api").Project[] }>("/projects", { headers: { Authorization: `Bearer ${token}` } }),
  createProject: (token: string, input: { name: string; repoUrl: string; branch: string }) =>
    request<import("../types/api").Project>("/projects", { method: "POST", headers: { Authorization: `Bearer ${token}` }, body: JSON.stringify(input) }),
  deployments: (token: string, projectId: string) => request<{ content: import("../types/api").Deployment[] }>(`/projects/${projectId}/deployments`, { headers: { Authorization: `Bearer ${token}` } }),
  deploy: (token: string, projectId: string) => request<import("../types/api").Deployment>(`/projects/${projectId}/deployments`, { method: "POST", headers: { Authorization: `Bearer ${token}` } }),
  logs: (token: string, deploymentId: string) => request<{ content: import("../types/api").DeploymentLog[] }>(`/deployments/${deploymentId}/logs`, { headers: { Authorization: `Bearer ${token}` } }),
  stop: (token: string, deploymentId: string) => request<void>(`/deployments/${deploymentId}/stop`, { method: "POST", headers: { Authorization: `Bearer ${token}` } }),
};

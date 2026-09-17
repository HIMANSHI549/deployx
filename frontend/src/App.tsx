import { FormEvent, useEffect, useState } from "react";
import { api } from "./services/api";
import type { Deployment, DeploymentLog, Project } from "./types/api";

export default function App() {
  const [token, setToken] = useState(localStorage.getItem("deployx-token"));
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [authMode, setAuthMode] = useState<"login" | "register">("login");
  const [projects, setProjects] = useState<Project[]>([]);
  const [selected, setSelected] = useState<Project | null>(null);
  const [deployments, setDeployments] = useState<Deployment[]>([]);
  const [logs, setLogs] = useState<DeploymentLog[]>([]);
  const [error, setError] = useState("");
  const [newProject, setNewProject] = useState({ name: "", repoUrl: "", branch: "main" });

  useEffect(() => { if (token) api.projects(token).then((page) => setProjects(page.content)).catch((e) => setError(e.message)); }, [token]);
  useEffect(() => {
    if (!token || !selected) return;
    const load = () => api.deployments(token, selected.id).then((page) => setDeployments(page.content)).catch((e) => setError(e.message));
    load();
    const timer = window.setInterval(load, 3000);
    return () => window.clearInterval(timer);
  }, [token, selected]);

  async function authenticate(event: FormEvent) {
    event.preventDefault(); setError("");
    try { const result = await api.auth(authMode, email, password); localStorage.setItem("deployx-token", result.token); setToken(result.token); }
    catch (e) { setError((e as Error).message); }
  }

  async function createProject(event: FormEvent) {
    event.preventDefault(); if (!token) return;
    try { const project = await api.createProject(token, newProject); setProjects([project, ...projects]); setSelected(project); setNewProject({ name: "", repoUrl: "", branch: "main" }); }
    catch (e) { setError((e as Error).message); }
  }

  async function deploy() { if (!token || !selected) return; try { const deployment = await api.deploy(token, selected.id); setDeployments([deployment, ...deployments]); } catch (e) { setError((e as Error).message); } }
  async function inspect(deployment: Deployment) { if (!token) return; try { setLogs((await api.logs(token, deployment.id)).content); } catch (e) { setError((e as Error).message); } }
  async function stop(deployment: Deployment) { if (!token) return; try { await api.stop(token, deployment.id); setDeployments(deployments.map((item) => item.id === deployment.id ? { ...item, status: "STOPPED" } : item)); } catch (e) { setError((e as Error).message); } }

  if (!token) return <main className="auth-shell"><section className="brand"><span>DX</span><p>DeployX</p><h1>Ship code with a clear view of what happens next.</h1></section><form className="auth-form" onSubmit={authenticate}><p className="eyebrow">CONTROL PLANE</p><h2>{authMode === "login" ? "Welcome back" : "Create your workspace"}</h2><input type="email" placeholder="Email" value={email} onChange={(e) => setEmail(e.target.value)} required /><input type="password" placeholder="Password" value={password} onChange={(e) => setPassword(e.target.value)} minLength={8} required /><button>{authMode === "login" ? "Sign in" : "Create account"}</button><button type="button" className="link-button" onClick={() => setAuthMode(authMode === "login" ? "register" : "login")}>{authMode === "login" ? "Need an account? Register" : "Already registered? Sign in"}</button>{error && <p className="error">{error}</p>}</form></main>;

  return <main className="app-shell"><header><div className="brand compact"><span>DX</span><strong>DeployX</strong></div><button className="quiet" onClick={() => { localStorage.removeItem("deployx-token"); setToken(null); }}>Sign out</button></header><div className="workspace"><aside><div className="section-heading"><div><p className="eyebrow">PROJECTS</p><h2>Your services</h2></div></div><form className="project-form" onSubmit={createProject}><input placeholder="Project name" value={newProject.name} onChange={(e) => setNewProject({ ...newProject, name: e.target.value })} required /><input placeholder="https://github.com/org/repo" value={newProject.repoUrl} onChange={(e) => setNewProject({ ...newProject, repoUrl: e.target.value })} required /><input placeholder="Branch" value={newProject.branch} onChange={(e) => setNewProject({ ...newProject, branch: e.target.value })} /><button>Add project</button></form>{projects.map((project) => <button className={`project-row ${selected?.id === project.id ? "active" : ""}`} key={project.id} onClick={() => { setSelected(project); setLogs([]); }}><strong>{project.name}</strong><small>{project.repoUrl.replace("https://", "")}</small></button>)}</aside><section className="content">{selected ? <><div className="content-heading"><div><p className="eyebrow">PROJECT / {selected.branch}</p><h1>{selected.name}</h1></div><button onClick={deploy}>Deploy latest</button></div><div className="deployment-list">{deployments.length === 0 && <p className="empty">No deployments yet. Your next one will appear here.</p>}{deployments.map((deployment) => <article className="deployment" key={deployment.id} onClick={() => inspect(deployment)}><div className="status-dot" data-status={deployment.status} /><div className="deployment-main"><strong>{deployment.status}</strong><small>{new Date(deployment.createdAt).toLocaleString()}</small></div><span className="deployment-id">{deployment.id.slice(0, 8)}</span>{deployment.publicUrl && <a href={deployment.publicUrl} target="_blank" rel="noreferrer" onClick={(e) => e.stopPropagation()}>Open app</a>}{deployment.status === "READY" && <button className="stop" onClick={(e) => { e.stopPropagation(); stop(deployment); }}>Stop</button>}</article>)}</div><section className="logs"><div className="section-heading"><div><p className="eyebrow">OBSERVABILITY</p><h2>Deployment logs</h2></div></div>{logs.length === 0 ? <p className="empty">Select a deployment to inspect its log stream.</p> : logs.map((log) => <p className="log-line" key={log.id}><time>{new Date(log.createdAt).toLocaleTimeString()}</time><span className={log.level.toLowerCase()}>{log.level}</span>{log.message}</p>)}</section></> : <div className="empty welcome"><p className="eyebrow">READY WHEN YOU ARE</p><h1>Choose a project to see its release history.</h1></div>}{error && <p className="error">{error}</p>}</section></div></main>;
}

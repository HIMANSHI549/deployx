export type Status = "QUEUED" | "BUILDING" | "READY" | "FAILED" | "STOPPED" | "SUPERSEDED";

export type Project = {
  id: string;
  name: string;
  repoUrl: string;
  branch: string;
  createdAt: string;
};

export type Deployment = {
  id: string;
  projectId: string;
  status: Status;
  imageTag: string | null;
  publicUrl: string | null;
  hostPort: number | null;
  errorMessage: string | null;
  createdAt: string;
  updatedAt: string;
};

export type DeploymentLog = {
  id: number;
  level: string;
  message: string;
  createdAt: string;
};

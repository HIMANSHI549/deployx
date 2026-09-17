package com.deployx.worker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.ServerSocket;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;


@Service
public class InProcessDeploymentWorker {

    private static final Logger log = LoggerFactory.getLogger(InProcessDeploymentWorker.class);

    private final WorkerPersistence persistence;
    private final String mode;
    private final Path workspace;
    private final String publicBaseUrl;
        private final String healthPath;
        private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(2))
            .build();

    public InProcessDeploymentWorker(
            WorkerPersistence persistence,
            @Value("${deployx.worker.mode}") String mode,
            @Value("${deployx.worker.workspace}") String workspace,
            @Value("${deployx.worker.base-url}") String publicBaseUrl,
            @Value("${deployx.worker.health-path:/}") String healthPath) {
        this.persistence = persistence;
        this.mode = mode;
        this.workspace = Path.of(workspace);
        this.publicBaseUrl = publicBaseUrl;
        this.healthPath = healthPath.startsWith("/") ? healthPath : "/" + healthPath;
    }

    @Async("deploymentExecutor")
    public void process(UUID deploymentId) {
        try {
            persistence.markBuilding(deploymentId, mode);
            if ("docker".equalsIgnoreCase(mode)) {
                runDockerPipeline(deploymentId);
            } else {
                runSimulatedPipeline(deploymentId);
            }
        } catch (Exception ex) {
            log.warn("Deployment {} failed", deploymentId, ex);
            persistence.markFailed(deploymentId, ex.getMessage());
        }
    }

    private void runSimulatedPipeline(UUID deploymentId) throws InterruptedException {
        DeploymentJobView job = persistence.loadJob(deploymentId);
        persistence.append(deploymentId, "INFO", "Validating repository URL " + job.repoUrl());
        Thread.sleep(400);
        persistence.append(deploymentId, "INFO", "Simulated clone of branch " + job.branch());
        Thread.sleep(400);
        persistence.append(deploymentId, "INFO", "Simulated docker build (set DEPLOYX_WORKER_MODE=docker for a real build)");
        Thread.sleep(600);
        int port = findAvailablePort();
        String slug = job.projectName().toLowerCase().replaceAll("[^a-z0-9-]", "-");
        String imageTag = "deployx/" + slug + ":" + job.deploymentId().toString().substring(0, 8);
        String publicUrl = publicBaseUrl + ":" + port;
        persistence.append(deploymentId, "INFO", "Health check passed (simulated)");
        persistence.append(deploymentId, "INFO", "Published at " + publicUrl);
        persistence.markReady(deploymentId, "simulated", imageTag, port, publicUrl);
    }

    private void runDockerPipeline(UUID deploymentId) throws IOException, InterruptedException {
        DeploymentJobView job = persistence.loadJob(deploymentId);
        Path workDir = workspace.resolve(job.deploymentId().toString());
        Files.createDirectories(workDir);
        persistence.append(deploymentId, "INFO", "Cloning " + job.repoUrl());
        runCommand(deploymentId, workDir, List.of(
                "git", "clone", "--depth", "1", "--branch", job.branch(),
                job.repoUrl(), "."));
        if (!Files.exists(workDir.resolve("Dockerfile"))) {
            throw new IllegalStateException("Repository has no Dockerfile in the root");
        }
        String imageTag = "deployx/" + job.projectName().toLowerCase().replaceAll("[^a-z0-9]", "")
                + ":" + job.deploymentId().toString().substring(0, 8);
        persistence.append(deploymentId, "INFO", "Building image " + imageTag);
        runCommand(deploymentId, workDir, List.of("docker", "build", "-t", imageTag, "."));
        String containerName = "deployx-" + job.deploymentId().toString().substring(0, 8);
        int port = findAvailablePort();
        persistence.append(deploymentId, "INFO", "Starting container " + containerName);
        runCommand(deploymentId, workDir, List.of(
                "docker", "run", "-d",
                "--name", containerName,
                "--memory", "256m",
                "--cpus", "0.5",
                "-p", port + ":8080",
                imageTag));
        String publicUrl = publicBaseUrl + ":" + port;
        persistence.append(deploymentId, "INFO", "Container started. Probing " + publicUrl + healthPath);
        waitForHealth(deploymentId, publicUrl + healthPath);
        persistence.append(deploymentId, "INFO", "Health check passed");
        persistence.markReady(deploymentId, null, imageTag, port, publicUrl);
    }

    public void stop(UUID deploymentId) {
        String containerName = "deployx-" + deploymentId.toString().substring(0, 8);
        try {
            runCommand(deploymentId, workspace, List.of("docker", "rm", "-f", containerName));
        } catch (Exception ex) {
            log.debug("Container {} was not running", containerName, ex);
        }
        persistence.markStopped(deploymentId);
    }

    private int findAvailablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private void waitForHealth(UUID deploymentId, String url) throws InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(java.time.Duration.ofSeconds(2))
                .GET()
                .build();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        while (System.nanoTime() < deadline) {
            try {
                HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
                if (response.statusCode() >= 200 && response.statusCode() < 500) {
                    return;
                }
            } catch (IOException ex) {
                // The container may need a few seconds to bind its application port.
            }
            Thread.sleep(500);
        }
        throw new IllegalStateException("Health check timed out for " + url);
    }

    private void runCommand(UUID deploymentId, Path workDir, List<String> command)
            throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(workDir.toFile());
        builder.redirectErrorStream(true);
        Process process = builder.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                persistence.append(deploymentId, "INFO", line);
            }
        }
        boolean finished = process.waitFor(5, TimeUnit.MINUTES);
        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("Timed out running: " + String.join(" ", command));
        }
        if (process.exitValue() != 0) {
            throw new IllegalStateException("Command failed (" + process.exitValue() + "): " + String.join(" ", command));
        }
    }
}

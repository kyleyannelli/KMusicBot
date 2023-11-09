package dev.kmfg.musicbot.api.routes;

import dev.kmfg.musicbot.api.controllers.HealthCheckController;
import dev.kmfg.musicbot.api.sockets.CoreSocket;
import spark.Spark;

public class ApiV1 {

    public ApiV1(HealthCheckController healthCheckController) {
        this.setupRoutes(healthCheckController);
    }

    private void setupRoutes(HealthCheckController healthCheckController) {
        Spark.webSocket("/api/socket", CoreSocket.class);
        Spark.path("/api", () -> {
            // route to update the last communicated time
            Spark.post("/health", healthCheckController::updateLastCommTime);
            // route to get the last communicated time
            Spark.get("/health", healthCheckController::isLastCommTimeReasonable);
        });
    }
}

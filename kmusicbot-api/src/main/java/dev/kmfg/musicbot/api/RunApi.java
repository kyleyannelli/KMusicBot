package dev.kmfg.musicbot.api;

import dev.kmfg.musicbot.api.controllers.HealthCheckController;
import dev.kmfg.musicbot.api.routes.ApiV1;
import spark.Spark;

public class RunApi {
    private static final int MAX_API_THREADS = 15;
    private static final int API_PORT = 8712;

    public static void main(String[] args) throws Exception {
        Spark.port(API_PORT);
        Spark.threadPool(MAX_API_THREADS);

        // no params required for now
        // this should be structured better if they are required in the future.
        new ApiV1(new HealthCheckController());
    }
}

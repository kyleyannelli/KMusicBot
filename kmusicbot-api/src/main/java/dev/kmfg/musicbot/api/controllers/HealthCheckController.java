package dev.kmfg.musicbot.api.controllers;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.time.Instant;

import org.eclipse.jetty.http.HttpStatus;

import spark.Request;
import spark.Response;
import spark.Spark;

public class HealthCheckController {
    private Timestamp lastCommTime;
    // 60 SECONDS * MINUTE MULTIPLIER
    private final int MAX_TIME_DISCREPANCY_SECONDS = 60 * 2;

    public HealthCheckController() {

    }

    public String isLastCommTimeReasonable(Request request, Response response) {
        String reason = generateOk();
        if(!isLastCommTimeReasonable()) {
            reason = this.generateTooLong();
            Spark.halt(HttpStatus.SERVICE_UNAVAILABLE_503, reason);
            return reason;
        }

        response.status(HttpStatus.OK_200);
        return reason;
    }

    public String updateLastCommTime(Request request, Response response) throws UnknownHostException  {
        String requesterIp = request.ip();
        String reason = "Time updated.";

        if(!isIpPrivate(requesterIp)) {
            reason = "Access not allowed from non-local addresses.";
            Spark.halt(HttpStatus.UNAUTHORIZED_401, reason);
            return reason;
        }

        this.updateLastCommTime();
        response.status(HttpStatus.OK_200);
        return reason;
    }

    protected String generateOk() {
        return new StringBuilder()
            .append("OK. Last Communicated at ")
            .append(this.lastCommTime)
            .toString();
    }

    protected String generateTooLong() {
        return new StringBuilder()
                .append("Last time communicated was too long ago! Max discrepancy is set at ")
                .append(this.MAX_TIME_DISCREPANCY_SECONDS)
                .append(" Last communicated at ")
                .append(this.lastCommTime)
                .toString();
    }

    protected boolean isLastCommTimeReasonable() {
        if(this.lastCommTime == null) return false;
        Instant currentTime = Instant.now();
        Instant lastCommTime = this.lastCommTime.toInstant();
        int discrepancy = Math.abs(currentTime.compareTo(lastCommTime));
        return discrepancy <= MAX_TIME_DISCREPANCY_SECONDS;
    }

    protected boolean isIpPrivate(String ip) throws UnknownHostException {
        InetAddress inetAddress = InetAddress.getByName(ip);
        return inetAddress.isLinkLocalAddress() || inetAddress.isLoopbackAddress();
    }

    protected void updateLastCommTime() {
        this.lastCommTime = Timestamp.from(Instant.now());
    }
}

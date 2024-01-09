package dev.kmfg.musicbot.database.models;

import java.util.List;

import com.google.gson.annotations.Expose;

public class PaginatedResponse<T> {
    @Expose
    private final List<T> data;
    @Expose
    private final int currentPage;
    @Expose
    private final int totalPages;
    @Expose
    private final long totalItems;
    @Expose
    private final int pageSize;

    public PaginatedResponse(List<T> data, int currentPage, int totalPages, long totalItems, int pageSize) {
        this.data = data;
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        this.totalItems = totalItems;
        this.pageSize = pageSize;
    }
}

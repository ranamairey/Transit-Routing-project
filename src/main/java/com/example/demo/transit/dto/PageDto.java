package com.example.demo.transit.dto;

import java.util.List;

public record PageDto<T>(List<T> items, int page, int size, long total) {
    public int totalPages() {
        return size == 0 ? 0 : (int) Math.ceil(total / (double) size);
    }
}

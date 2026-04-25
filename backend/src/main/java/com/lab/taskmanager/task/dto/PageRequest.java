package com.lab.taskmanager.task.dto;

import com.lab.taskmanager.common.exception.BusinessException;
import com.lab.taskmanager.task.entity.SortBy;
import jakarta.validation.constraints.Min;

public record PageRequest(
        @Min(value = 1, message = "Size of each page cannot be lower than 1") Integer size,
        @Min(value = 1, message = "Page number starts from 1") Integer page,
        SortBy sortBy) {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int DEFAULT_PAGE_NUMBER = 1;
    private static final int MAX_PAGE_SIZE = 100;

    public PageRequest {
        if (size == null) {
            size = DEFAULT_PAGE_SIZE;
        }
        if (page == null) {
            page = DEFAULT_PAGE_NUMBER;
        }
        if (sortBy == null) {
            sortBy = SortBy.UPDATED_AT;
        }

        if (size < 1) {
            throw new BusinessException("每页大小必须大于等于 1");
        }
        if (page < 1) {
            throw new BusinessException("页码必须大于等于 1");
        }
        if (size > MAX_PAGE_SIZE) {
            throw new BusinessException("每页大小不能超过 " + MAX_PAGE_SIZE);
        }
    }

    public static PageRequest of(Integer size, Integer page, String sortByStr) {
        return new PageRequest(size, page, SortBy.fromString(sortByStr));
    }
}

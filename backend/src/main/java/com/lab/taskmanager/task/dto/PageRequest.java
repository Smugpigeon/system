package com.lab.taskmanager.task.dto;

import jakarta.validation.constraints.Min;
import com.lab.taskmanager.task.entity.SortBy;

public record PageRequest(
                @Min(value = 1, message = "Size of each page cannot be lower than 1") Integer size,
                @Min(value = 1, message = "Page number starts from 1") Integer page,
                SortBy sortBy) {
        public PageRequest {
                if (size == null)
                        size = 10;
                if (page == null)
                        page = 1;
                if (sortBy == null)
                        sortBy = SortBy.UPDATED_AT;
        }

        public static PageRequest of(Integer size, Integer page, String sortByStr) {
                return new PageRequest(size, page, SortBy.fromString(sortByStr));
        }
}

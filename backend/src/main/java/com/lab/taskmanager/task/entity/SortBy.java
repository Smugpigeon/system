package com.lab.taskmanager.task.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Sorting criteria for task list queries.
 */
@Getter
@AllArgsConstructor
@Schema(description = "Sorting criteria for the task list")
public enum SortBy {
    
    @Schema(description = "Intelligent sorting based on priority, status, and deadline")
    RANK("rank", "Intelligent sorting based on priority, status, and deadline"),
    
    @Schema(description = "Ascending by due date (most urgent first)")
    DUE_AT("dueAt", "Ascending by due date (most urgent first)"),
    
    @Schema(description = "Descending by creation date (newest first)")
    CREATED_AT("createdAt", "Descending by creation date (newest first)"),
    
    @Schema(description = "Descending by priority (HIGH > MEDIUM > LOW)")
    PRIORITY("priority", "Descending by priority"),
    
    @Schema(description = "Ascending by status (TODO → IN_PROGRESS → DONE)")
    STATUS("status", "Ascending by status"),
    
    @Schema(description = "Descending by last update time (default)")
    UPDATED_AT("updatedAt", "Descending by last update time (default)");
    
    private final String value;
    private final String description;
    
    /**
     * Convert string to SortBy enum, case-insensitive.
     * 
     * @param value the string value to convert
     * @return the corresponding SortBy enum, or UPDATED_AT as default if not found
     */
    public static SortBy fromString(String value) {
        if (value == null || value.isBlank()) {
            return UPDATED_AT;
        }
        
        for (SortBy sortBy : SortBy.values()) {
            if (sortBy.getValue().equalsIgnoreCase(value)) {
                return sortBy;
            }
        }
        return UPDATED_AT;
    }
}
package com.lab.taskmanager.task.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 封装分页查询结果
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageResult<T> {
    private Integer totalRecords;
    private Integer totalPages;
    private Integer currPage;
    private Integer size;
    private List<T> records;
}

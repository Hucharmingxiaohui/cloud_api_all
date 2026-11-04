package com.dji.sample.df.cqDockDf.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

@Getter
@AllArgsConstructor
public class CqDockReadPage<T> {
    private final List<T> records;
    private final long total;
    private final long current;
    private final long size;
}

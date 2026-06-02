package com.LAB.study.request;

import lombok.Data;
import java.util.List;

@Data
public class UpdateCategoryRequest {
    private List<Long> ids;
    private String category;
}

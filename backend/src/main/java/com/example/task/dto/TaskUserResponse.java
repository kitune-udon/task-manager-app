package com.example.task.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TaskUserResponse {
    private Long id;
    private String name;
}

package io.wz.userservice.service;

import io.wz.userservice.dto.DepartmentDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(value = "DEPARTMENT-SERVICE")
public interface APIClient {
    @GetMapping(value = "/api/departments/{id}")
    DepartmentDto getDepartmentById(@PathVariable("id") String departmentId);
}
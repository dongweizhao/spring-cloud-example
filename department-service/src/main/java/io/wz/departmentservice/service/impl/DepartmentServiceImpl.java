package io.wz.departmentservice.service.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.wz.departmentservice.entity.Department;
import io.wz.departmentservice.repository.DepartmentRepository;
import io.wz.departmentservice.service.DepartmentService;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class DepartmentServiceImpl implements DepartmentService {

    private DepartmentRepository departmentRepository;

    @Override
    public Department saveDepartment(Department department) {
        return departmentRepository.save(department);
    }

    @Override
    public Department getDepartmentById(Long departmentId) {
        log.info("getDepartment By Id:{} ",departmentId);
        return departmentRepository.findById(departmentId).get();
    }
}
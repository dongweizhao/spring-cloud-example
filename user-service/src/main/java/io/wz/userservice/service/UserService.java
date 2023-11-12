package io.wz.userservice.service;

import io.wz.userservice.dto.ResponseDto;
import io.wz.userservice.entity.User;

public interface UserService {
    User saveUser(User user);

    ResponseDto getUser(Long userId);
}
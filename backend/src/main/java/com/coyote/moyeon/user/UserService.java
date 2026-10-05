package com.coyote.moyeon.user;

import com.coyote.moyeon.common.exception.BusinessException;
import com.coyote.moyeon.user.dto.UpdateUserRequest;
import com.coyote.moyeon.user.dto.UserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

	private final UserRepository userRepository;

	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public UserResponse getMe(Long userId) {
		return UserResponse.from(find(userId));
	}

	@Transactional
	public UserResponse updateMe(Long userId, UpdateUserRequest request) {
		User user = find(userId);
		String address = request.address() == null || request.address().isBlank() ? null : request.address().trim();
		user.updateProfile(request.name().trim(), address);
		return UserResponse.from(user);
	}

	private User find(Long userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));
	}
}

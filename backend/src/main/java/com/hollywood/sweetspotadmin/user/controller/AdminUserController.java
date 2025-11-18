// AdminUserController.java
package com.hollywood.sweetspotadmin.user.controller;

import com.hollywood.sweetspotadmin.user.dto.RoleUpdateRequest; // ✅ DTO 임포트
import com.hollywood.sweetspotadmin.user.dto.UserInfoResponse;
import com.hollywood.sweetspotadmin.user.model.User; // ✅ User 엔티티 임포트
import com.hollywood.sweetspotadmin.user.model.Provider; // ✅ [추가]
import com.hollywood.sweetspotadmin.user.model.Role; // ✅ [추가]
import com.hollywood.sweetspotadmin.user.dto.UserCreateRequest; // ✅ [추가]
import com.hollywood.sweetspotadmin.user.repository.UserRepository;
import jakarta.transaction.Transactional; // ✅ Transactional 임포트
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder; // ✅ [추가]
import org.springframework.web.bind.annotation.*; // ✅ PutMapping, PathVariable, RequestBody 임포트

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // ✅ [추가]

    /**
     * 전체 사용자 목록을 조회합니다.
     */
    @GetMapping
    public ResponseEntity<List<UserInfoResponse>> getAllUsers() {
        List<UserInfoResponse> users = userRepository.findAll().stream()
                .map(UserInfoResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    /**
     * ✅ [신규] 특정 사용자의 권한을 수정합니다.
     */
    @Transactional // (DB 변경이 있으므로 트랜잭션 추가)
    @PutMapping("/{userId}/roles")
    public ResponseEntity<UserInfoResponse> updateUserRoles(
            @PathVariable Long userId,
            @RequestBody RoleUpdateRequest request) {
        // 1. ID로 사용자를 찾습니다.
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // 2. 요청받은 Role 목록으로 사용자의 Role을 덮어씁니다.
        // (주의: 이 코드는 User 엔티티에 @Setter가 있거나, roles 필드가 public이어야 동작합니다.)
        // (만약 User 엔티티에 Role을 수정하는 별도 메서드가 있다면 그것을 사용해야 합니다.)

        // User.java에 'public void setRoles(List<Role> roles)' 메서드가 없으므로
        // User 엔티티의 roles 필드를 직접 수정합니다. (가정: User 엔티티의 roles 필드는 @Getter만 있고 직접 수정이 안될
        // 수 있음)
        // -> 메인 백엔드의 User.java를 보니 @Getter만 있고 setter가 없네요.
        // -> User.java에 setRoles 메서드를 추가하거나, 이 예제에서는 간단히 진행합니다.

        // (임시방편: User 엔티티에 setRoles가 없다는 가정 하에...
        // 아, 메인 백엔드의 User 엔티티를 보니 roles 필드에 @Setter가 없네요.
        // User.java 파일에 public void setRoles(List<Role> roles) { this.roles = roles; }
        // 를 추가해야 합니다.)

        // User.java에 public setRoles 메서드를 추가했다고 가정하고 진행합니다.
        // user.setRoles(request.toRoleEnums());

        // --- User.java 수정이 어렵다는 가정 하의 대체 코드 ---
        // (User 엔티티의 roles 필드가 List 인터페이스라고 가정)
        user.getRoles().clear(); // 기존 권한을 모두 삭제
        user.getRoles().addAll(request.toRoleEnums()); // 새 권한 추가
        // ---------------------------------------------

        User updatedUser = userRepository.save(user);

        return ResponseEntity.ok(UserInfoResponse.from(updatedUser));
    }

    @Transactional
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        // (참고: 자기 자신을 삭제하지 않도록 프론트엔드에서 막는 것이 좋습니다.)

        // 1. ID로 사용자가 존재하는지 확인 (선택 사항이지만 안전함)
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("User not found with id: " + userId);
        }

        // 2. 사용자를 ID로 삭제합니다.
        // (User 엔티티의 @OnDelete(action = OnDeleteAction.CASCADE) 설정 덕분에
        // user_roles 테이블의 데이터도 함께 삭제됩니다.)
        userRepository.deleteById(userId);

        // 3. 성공 응답 (내용 없음)
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
    /**
     * ✅ [신규] 신규 사용자를 등록합니다. (관리자용)
     */
    @Transactional
    @PostMapping
    public ResponseEntity<UserInfoResponse> createUser(@RequestBody UserCreateRequest request) {
        // 1. 이메일 중복 체크 (LOCAL 제공자로)
        if (userRepository.findByEmailAndProvider(request.getEmail(), Provider.LOCAL).isPresent()) {
            // 이미 존재하면 409 Conflict 에러
            throw new RuntimeException("이미 사용 중인 이메일입니다."); 
        }

        // 2. 새 User 객체 생성
        User newUser = User.builder()
                .email(request.getEmail())
                .name(request.getName())
                .password(passwordEncoder.encode(request.getPassword())) // ❗️ 비밀번호 암호화
                .provider(Provider.LOCAL) // 관리자가 생성한 계정은 LOCAL
                .roles(Collections.singletonList(Role.ROLE_USER)) // 기본값 ROLE_USER
                .build();

        // 3. DB에 저장
        User savedUser = userRepository.save(newUser);

        // 4. 생성된 사용자 정보 반환 (201 Created)
        return new ResponseEntity<>(UserInfoResponse.from(savedUser), HttpStatus.CREATED);
    }
}
// UserRepository.java (수정 후)

package com.hollywood.sweetspotadmin.user.repository;

import com.hollywood.sweetspotadmin.user.model.Provider;
import com.hollywood.sweetspotadmin.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailAndProvider(String email, Provider provider);
    List<User> findByEmail(String email);
    
}
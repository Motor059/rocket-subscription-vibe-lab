package com.rocket.subscription.infrastructure;

import com.rocket.subscription.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    // 기본 상속 메서드(findById 등)만으로 유저 조회가 가능하므로 비워둡니다.
}
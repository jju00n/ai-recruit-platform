package com.example.airecruit.user.repository;

import com.example.airecruit.user.domain.SocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {
    Optional<SocialAccount> findByProviderAndSocialId(String provider, String socialId);
}

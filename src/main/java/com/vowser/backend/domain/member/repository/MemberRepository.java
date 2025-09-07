package com.vowser.backend.domain.member.repository;

import com.vowser.backend.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 회원 리포지토리
 */
@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    
    /**
     * 이메일로 회원 조회
     */
    Optional<Member> findByEmail(String email);
    
    /**
     * 네이버 ID로 회원 조회
     */
    Optional<Member> findByNaverId(String naverId);
    
    /**
     * 이메일 중복 확인
     */
    boolean existsByEmail(String email);
    
    /**
     * 네이버 ID 중복 확인
     */
    boolean existsByNaverId(String naverId);
}
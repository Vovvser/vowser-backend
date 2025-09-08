package com.vowser.backend.application.service;

import com.vowser.backend.api.dto.member.MemberResponse;
import com.vowser.backend.common.exception.AuthException;
import com.vowser.backend.common.exception.ErrorCode;
import com.vowser.backend.domain.member.entity.Member;
import com.vowser.backend.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {
    
    private final MemberRepository memberRepository;
    
    /**
     * 회원 정보 조회
     */
    public MemberResponse getMemberInfo(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> {
                    log.error("회원을 찾을 수 없습니다: memberId={}", memberId);
                    return new AuthException(ErrorCode.MEMBER_NOT_FOUND);
                });
        
        log.debug("회원 정보 조회 성공: memberId={}", memberId);
        return MemberResponse.from(member);
    }
    
    /**
     * 회원 엔티티 조회
     */
    public Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> {
                    log.error("회원을 찾을 수 없습니다: memberId={}", memberId);
                    return new AuthException(ErrorCode.MEMBER_NOT_FOUND);
                });
    }
    
    /**
     * 이메일로 회원 조회
     */
    public Member getMemberByEmail(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("회원을 찾을 수 없습니다: email={}", email);
                    return new AuthException(ErrorCode.MEMBER_NOT_FOUND);
                });
    }
    
    /**
     * 네이버 ID로 회원 조회
     */
    public Member getMemberByNaverId(String naverId) {
        return memberRepository.findByNaverId(naverId)
                .orElseThrow(() -> {
                    log.error("회원을 찾을 수 없습니다: naverId={}", naverId);
                    return new AuthException(ErrorCode.MEMBER_NOT_FOUND);
                });
    }
}
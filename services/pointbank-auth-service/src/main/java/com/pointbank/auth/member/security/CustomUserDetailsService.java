package com.pointbank.auth.member.security;

import com.pointbank.auth.global.exception.BusinessException;
import com.pointbank.auth.global.exception.ErrorCode;
import com.pointbank.auth.member.domain.Member;
import com.pointbank.auth.member.domain.MemberStatus;
import com.pointbank.auth.member.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberMapper memberMapper;

    @Override
    public CustomUserDetails loadUserByUsername(String username) {
        Member member = memberMapper.findByPhoneNumber(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        return toActiveUserDetails(member);
    }

    public CustomUserDetails loadByMemberId(Long memberId) {
        Member member = memberMapper.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        return toActiveUserDetails(member);
    }

    private CustomUserDetails toActiveUserDetails(Member member) {
        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_ACTIVE);
        }
        return CustomUserDetails.from(member);
    }
}

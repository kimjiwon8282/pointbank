package com.pointbank.auth.member.mapper;

import com.pointbank.auth.member.domain.Member;
import com.pointbank.auth.member.domain.MemberStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface MemberMapper {
    Optional<Member> findById(Long id);
    Optional<Member> findByPhoneNumber(String phoneNumber);
    boolean existsByPhoneNumber(String phoneNumber);
    int insert(Member member);
    int updateSimplePasswordSet(@Param("id") Long id, @Param("simplePasswordSet") boolean simplePasswordSet);
    int updateStatus(@Param("id") Long id, @Param("status") MemberStatus status);
}

package com.pointbank.auth.member.security;

import com.pointbank.auth.member.domain.Member;
import com.pointbank.auth.member.domain.MemberRole;
import com.pointbank.auth.member.domain.MemberStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {

    private final Long memberId;
    private final String phoneNumber;
    private final String passwordHash;
    private final String name;
    private final MemberRole role;
    private final MemberStatus status;
    private final boolean simplePasswordSet;

    private CustomUserDetails(Member member) {
        this.memberId = member.getId();
        this.phoneNumber = member.getPhoneNumber();
        this.passwordHash = member.getPasswordHash();
        this.name = member.getName();
        this.role = member.getRole();
        this.status = member.getStatus();
        this.simplePasswordSet = member.isSimplePasswordSet();
    }

    public static CustomUserDetails from(Member member) {
        return new CustomUserDetails(member);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() { return passwordHash; }

    @Override
    public String getUsername() { return phoneNumber; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return status != MemberStatus.LOCKED; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return status == MemberStatus.ACTIVE; }

    public Long getMemberId() { return memberId; }
    public String getName() { return name; }
    public MemberRole getRole() { return role; }
    public MemberStatus getStatus() { return status; }
    public boolean isSimplePasswordSet() { return simplePasswordSet; }
}

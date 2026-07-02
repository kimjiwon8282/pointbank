package com.pointbank.auth.member.domain;

import java.time.LocalDateTime;

public class Member {

    private Long id;
    private String name;
    private String phoneNumber;
    private String passwordHash;
    private MemberRole role;
    private MemberStatus status;
    private boolean simplePasswordSet;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Member() {
    }

    public Member(String name, String phoneNumber, String passwordHash, MemberRole role, MemberStatus status) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getPasswordHash() { return passwordHash; }
    public MemberRole getRole() { return role; }
    public MemberStatus getStatus() { return status; }
    public boolean isSimplePasswordSet() { return simplePasswordSet; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}

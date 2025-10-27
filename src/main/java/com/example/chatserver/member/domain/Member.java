package com.example.chatserver.member.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    private String password;

    // Enumerated 설정 안해주면 숫자 값으로 들어가서 아래 설정을 해야지 String으로 들어감
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Role role = Role.USER;


}


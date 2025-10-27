package com.example.chatserver.member.repository;


import com.example.chatserver.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    //Optional 값이 있을수도 있고 없을수도 있고
    Optional<Member> findByEmail(String email);

}

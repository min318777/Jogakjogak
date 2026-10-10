package com.zb.jogakjogak.event.repository;

import com.zb.jogakjogak.event.entity.Event;
import com.zb.jogakjogak.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventRepository extends JpaRepository<Event, Long>, EventRepositoryCustom {
    boolean existsByCode(String code);

    void deleteAllByMember(Member member);
}

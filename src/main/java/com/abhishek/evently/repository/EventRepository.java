package com.abhishek.evently.repository;

import com.abhishek.evently.entity.EventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<EventEntity, Long>
{
    Page<EventEntity> findByOrganizerUserId(Long id, Pageable pageable);
}

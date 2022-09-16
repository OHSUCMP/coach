package edu.ohsu.cmp.coach.repository;

import edu.ohsu.cmp.coach.entity.ContactMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {
    ContactMessage findOneByMessageKey(@Param("key") String key);
}

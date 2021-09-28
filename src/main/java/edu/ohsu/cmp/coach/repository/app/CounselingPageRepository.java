package edu.ohsu.cmp.coach.repository.app;

import edu.ohsu.cmp.coach.entity.app.CounselingPage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface CounselingPageRepository extends JpaRepository<CounselingPage, Long> {
    CounselingPage findOneByPageKey(@Param("key") String key);
}

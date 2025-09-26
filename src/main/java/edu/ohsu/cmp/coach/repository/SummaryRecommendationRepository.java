package edu.ohsu.cmp.coach.repository;

import edu.ohsu.cmp.coach.entity.SummaryRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SummaryRecommendationRepository extends JpaRepository<SummaryRecommendation, Long> {
}

package com.leela.urlshortener.persistence;

import com.leela.urlshortener.domain.ClickEvent;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {

    long countByLinkId(Long linkId);

    Optional<ClickEvent> findTopByLinkIdOrderByClickedAtDesc(Long linkId);

    @Query(
            value =
                    """
                    SELECT (clicked_at AT TIME ZONE 'UTC')::date AS day, COUNT(*) AS cnt
                    FROM click_events
                    WHERE link_id = :linkId AND clicked_at >= :since
                    GROUP BY 1
                    ORDER BY 1
                    """,
            nativeQuery = true)
    List<Object[]> dailyCounts(@Param("linkId") Long linkId, @Param("since") Instant since);
}

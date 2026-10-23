package com.shwab.aiassistedurlshortner.persistence;

import com.shwab.aiassistedurlshortner.domain.ShortLink;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShortLinkRepository extends JpaRepository<ShortLink, Long> {

    Optional<ShortLink> findByCode(String code);
}

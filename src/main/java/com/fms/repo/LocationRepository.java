package com.fms.repo;

import com.fms.domain.Location;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationRepository extends JpaRepository<Location, Long> {
    List<Location> findAllByOrderByMainDescNameAsc();
    List<Location> findByActiveTrueOrderByMainDescNameAsc();
    Optional<Location> findFirstByMainTrue();
    boolean existsByCode(String code);
    boolean existsByCodeAndIdNot(String code, Long id);
}

package com.fms.repo;

import com.fms.domain.Party;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface PartyRepository<T extends Party> extends JpaRepository<T, Long> {
    Optional<T> findByName(String name);
    List<T> findAllByOrderByNameAsc();
    boolean existsByNameAndIdNot(String name, Long id);
    boolean existsByName(String name);
}

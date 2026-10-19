package com.fms.repo;

import com.fms.domain.Prospect;
import java.util.Optional;

public interface ProspectRepository extends PartyRepository<Prospect> {
    /** Names are not unique here (imported NotClients data has duplicates); take the latest. */
    Optional<Prospect> findFirstByNameOrderByRegisteredAtDesc(String name);
}

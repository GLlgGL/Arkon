package com.fms.repo;

import com.fms.domain.UserAccess;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAccessRepository extends JpaRepository<UserAccess, Long> {
    List<UserAccess> findByUsername(String username);
    @Modifying
    @Query("delete from UserAccess a where a.username = :u")
    int deleteUser(@Param("u") String username);
}

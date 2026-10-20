package com.fms.repo;

import com.fms.domain.UserDashboard;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDashboardRepository extends JpaRepository<UserDashboard, String> {
}

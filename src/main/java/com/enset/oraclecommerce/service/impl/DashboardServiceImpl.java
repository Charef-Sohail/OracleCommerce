package com.enset.oraclecommerce.service.impl;

import com.enset.oraclecommerce.dto.response.DashboardStats;
import com.enset.oraclecommerce.jdbc.JdbcDashboardRepository;
import com.enset.oraclecommerce.service.DashboardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author pc
 **/
@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final JdbcDashboardRepository dashboardRepository;

    public DashboardServiceImpl(
            JdbcDashboardRepository dashboardRepository) {
        this.dashboardRepository = dashboardRepository;
    }

    @Override
    public DashboardStats getDashboardStats() {
        return dashboardRepository.getDashboardStats();
    }
}
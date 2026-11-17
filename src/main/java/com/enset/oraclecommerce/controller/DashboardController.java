package com.enset.oraclecommerce.controller;

import com.enset.oraclecommerce.service.DashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @author pc
 **/
@Controller
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {
        model.addAttribute(
                "stats",
                dashboardService.getDashboardStats()
        );
        return "dashboard/index";
    }
}

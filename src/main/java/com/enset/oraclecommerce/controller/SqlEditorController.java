package com.enset.oraclecommerce.controller;

import com.enset.oraclecommerce.service.SqlEditorService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class SqlEditorController {

    private final SqlEditorService service;

    public SqlEditorController(SqlEditorService service) {
        this.service = service;
    }

    @GetMapping("/sql")
    public String page() {
        return "sql/index";
    }

    @PostMapping(value = "/api/sql/execute", produces = "application/json")
    @ResponseBody
    public SqlEditorService.Response execute(@RequestBody SqlEditorService.Request request, HttpSession http) {
        return service.execute(http, request);
    }

    @PostMapping(value = "/api/sql/commit", produces = "application/json")
    @ResponseBody
    public SqlEditorService.Response commit(HttpSession http) {
        return service.commit(http);
    }

    @PostMapping(value = "/api/sql/rollback", produces = "application/json")
    @ResponseBody
    public SqlEditorService.Response rollback(HttpSession http) {
        return service.rollback(http);
    }

    @GetMapping(value = "/api/sql/status", produces = "application/json")
    @ResponseBody
    public SqlEditorService.Status status(HttpSession http) {
        return service.status(http);
    }
}

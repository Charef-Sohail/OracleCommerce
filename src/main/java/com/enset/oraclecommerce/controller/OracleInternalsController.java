package com.enset.oraclecommerce.controller;

import com.enset.oraclecommerce.dto.response.OracleSnapshot;
import com.enset.oraclecommerce.oracle.OracleEvent;
import com.enset.oraclecommerce.oracle.OracleEventBuffer;
import com.enset.oraclecommerce.service.OracleInternalsService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class OracleInternalsController {

    private final OracleInternalsService service;
    private final OracleEventBuffer eventBuffer;

    public OracleInternalsController(OracleInternalsService service, OracleEventBuffer eventBuffer) {
        this.service = service;
        this.eventBuffer = eventBuffer;
    }

    @GetMapping("/oracle")
    public String page() {
        return "oracle/index";
    }

    /** Instance state (SGA, PGA, processes...) from the V$ views. */
    @GetMapping(value = "/api/oracle/internals", produces = "application/json")
    @ResponseBody
    public OracleSnapshot internals() {
        return service.snapshot();
    }

    /** User requests that reached Oracle. since=-1 -> history only; since=N -> events after N. */
    @GetMapping(value = "/api/oracle/events", produces = "application/json")
    @ResponseBody
    public OracleEvent.Page events(@RequestParam(defaultValue = "-1") long since) {
        return eventBuffer.since(since);
    }
}

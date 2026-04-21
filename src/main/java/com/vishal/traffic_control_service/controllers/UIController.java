package com.vishal.traffic_control_service.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves Thymeleaf UI pages.
 * Kept separate from API controllers — UI routes return views, not JSON.
 */
@Controller
public class UIController {

    @GetMapping("/")
    public String intro() {
        return "index";
    }

    @GetMapping("/submit")
    public String submit() {
        return "submit";
    }

    @GetMapping("/poll")
    public String poll() {
        return "poll";
    }

    @GetMapping("/demo")
    public String demo() {
        return "demo";
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }
}
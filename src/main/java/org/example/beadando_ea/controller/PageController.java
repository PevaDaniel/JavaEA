package org.example.beadando_ea.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/soap")
    public String soapPage() {
        return "SOAP";
    }
}

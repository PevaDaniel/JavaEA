package org.example.beadando_ea.controller;

import org.example.beadando_ea.service.MnbService;
import org.example.beadando_ea.service.MnbService.RatePoint;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mnb")
public class MnbController {

    private final MnbService service;

    public MnbController(MnbService service) {
        this.service = service;
    }

    @GetMapping("/rates")
    public List<RatePoint> rates(@RequestParam String currency,
                                 @RequestParam String start,
                                 @RequestParam String end) throws Exception {
        return service.getExchangeRates(start, end, currency);
    }
}

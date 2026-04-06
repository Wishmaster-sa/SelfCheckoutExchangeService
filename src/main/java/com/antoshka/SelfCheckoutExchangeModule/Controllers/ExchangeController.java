
package com.antoshka.SelfCheckoutExchangeModule.Controllers;

import com.antoshka.SelfCheckoutExchangeModule.Models.*;
import com.antoshka.SelfCheckoutExchangeModule.Services.ExchangeService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ExchangeController {

    private final ExchangeService service;

    @PostMapping("loopback")
    public ExchangeResponse loopback(@RequestBody ExchangeRequest request) {
        return service.loopback(request);
    }

    @GetMapping("test")
    public String test() {
        return LocalDateTime.now().toString();
    }

    @GetMapping("checkdb")
    public String checkDb() {
        return service.checkDB();
    }

    @PostMapping("exchange")
    public ExchangeResponse exchange(@RequestBody ExchangeRequest request) {
        return service.process(request);
    }

    

}

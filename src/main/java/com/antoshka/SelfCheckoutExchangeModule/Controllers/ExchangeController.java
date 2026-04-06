
package com.antoshka.SelfCheckoutExchangeModule.Controllers;

import com.antoshka.SelfCheckoutExchangeModule.Models.*;
import com.antoshka.SelfCheckoutExchangeModule.Services.ExchangeService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ExchangeController {

    private final ExchangeService service;

    @Value("${logging.file.name}")
    String logFilePath;
    
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

    @GetMapping("/log")
    public List<String> getLogs(
            @RequestParam(defaultValue = "100") int lines
    ) {

        if (lines <= 0) {
            lines = 100;
        }
        

        return service.readLogFile(logFilePath, lines);
    }
    
    @PostMapping("exchange")
    public ExchangeResponse exchange(@RequestBody ExchangeRequest request) {
        return service.process(request);
    }

    

}

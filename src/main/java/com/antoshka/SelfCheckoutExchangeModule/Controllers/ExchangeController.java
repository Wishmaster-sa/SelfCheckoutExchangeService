
package com.antoshka.SelfCheckoutExchangeModule.Controllers;

import com.antoshka.SelfCheckoutExchangeModule.Models.*;
import com.antoshka.SelfCheckoutExchangeModule.Services.ExchangeBatchService;
import com.antoshka.SelfCheckoutExchangeModule.Services.ExchangeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ExchangeController {

    private final ExchangeService service;
    private final ExchangeBatchService batchService;
    private final ObjectMapper objectMapper;

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
            @RequestParam(defaultValue = "100") int lines,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String since
    ) {

        if (lines <= 0) {
            lines = 100;
        }

        return service.readLogFile(logFilePath, lines, search, since);
    }


    
    @PostMapping(value = "/exchange", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ExchangeResponse exchange(@RequestBody ExchangeRequest request) {
        return service.process(request);
    }


    @PostMapping(value = "/exchange", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    //public ResponseEntity<?> exchange(
    public ExchangeResponse exchange(
            @RequestParam("product") String productJson,
            @RequestParam(value = "image", required = false) MultipartFile image
    ) throws Exception {

        ExchangeRequest product = objectMapper.readValue(productJson, ExchangeRequest.class);

        return service.process(product, image);

        //return ResponseEntity.ok().build();
    }


    @PostMapping(value = "/goods", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ExchangeResponse processBatch(
            @RequestPart("product") String productJson,
            HttpServletRequest request
    ) throws Exception {

        ExchangeRequest req = objectMapper.readValue(productJson, ExchangeRequest.class);

        Map<String, MultipartFile> files = new HashMap<>();

        if (request instanceof MultipartHttpServletRequest multipartRequest) {
            files = multipartRequest.getFileMap();
        }

        return batchService.processBatch(req, files);
    }

    

}

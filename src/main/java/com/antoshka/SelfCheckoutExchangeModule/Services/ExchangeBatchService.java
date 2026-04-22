/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.antoshka.SelfCheckoutExchangeModule.Services;

import com.antoshka.SelfCheckoutExchangeModule.Models.ExchangeRequest;
import com.antoshka.SelfCheckoutExchangeModule.Models.ExchangeResponse;
import com.antoshka.SelfCheckoutExchangeModule.Models.ProductRequest;
import com.antoshka.SelfCheckoutExchangeModule.Models.ProductResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author sa
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ExchangeBatchService {
    private final ExchangeService service;
    
    public ExchangeResponse processBatch(
            ExchangeRequest request,
            Map<String, MultipartFile> files
    ) {

        List<ProductResponse> responses = new ArrayList<>();

        for (ProductRequest p : request.getProducts()) {

            try {
                MultipartFile image = null;

                if (files != null) {
                    String key = "images[" + p.getId() + "]";
                    image = files.get(key);
                    if(image==null){
                        log.info("no file image found for {}",p.getName());                    
                       
                    }else{
                        log.info("image = {}", key);

                        p.setImage_bytes(image.getBytes());
                        
                    }
                }else{
                    log.info("no file image found for {}",p.getName());                    
                }

                service.processSingleProduct(p);

                responses.add(new ProductResponse(p.getId(), true, ""));

            } catch (Exception e) {

                responses.add(new ProductResponse(
                        p.getId(),
                        false,
                        e.getMessage()
                ));
            }
        }

        return new ExchangeResponse(responses);
    }
    
}

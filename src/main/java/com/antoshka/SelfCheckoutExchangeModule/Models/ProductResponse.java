
package com.antoshka.SelfCheckoutExchangeModule.Models;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProductResponse {
    private String id;
    private boolean success;
    private String errorMessage;
}


package com.antoshka.SelfCheckoutExchangeModule.Models;

import lombok.Data;
import java.util.List;

@Data
public class ExchangeRequest {
    private List<ProductRequest> products;
}

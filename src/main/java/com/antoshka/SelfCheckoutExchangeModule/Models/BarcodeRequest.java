
package com.antoshka.SelfCheckoutExchangeModule.Models;

import lombok.Data;

@Data
public class BarcodeRequest {
    private String barcode;
    private String unit;
}


package com.antoshka.SelfCheckoutExchangeModule.Models;

import lombok.Data;
import java.util.List;

@Data
public class ProductRequest {
    private String id;
    private String taxRate;
    private String name;
    private String unit;
    private Integer price;
    private String priceLevel;
    private String image;
    private byte[] image_bytes;
    private List<BarcodeRequest> barcodes;
}

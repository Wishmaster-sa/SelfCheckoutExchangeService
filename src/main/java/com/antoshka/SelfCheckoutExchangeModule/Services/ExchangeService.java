
package com.antoshka.SelfCheckoutExchangeModule.Services;

import com.antoshka.SelfCheckoutExchangeModule.Models.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.*;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeService {

    private final JdbcTemplate jdbc;

    public ExchangeResponse loopback(ExchangeRequest request) {
        List<ProductResponse> responses = new ArrayList<>();
        for (ProductRequest p : request.getProducts()) {
            responses.add(new ProductResponse(p.getId(), true, ""));
        }
        return new ExchangeResponse(responses);
    }

    public String checkDB(){
        Integer result = jdbc.queryForObject("SELECT 1", Integer.class);
        log.info("DB connection OK: {}", result);
        return "DB connection OK: {}";
    }
    
    /**
     * Основной метод обработки входящего JSON
     * Разбивает обработку по товарам
     */
    @Transactional
    public ExchangeResponse process(ExchangeRequest request) {

        List<ProductResponse> responses = new ArrayList<>();

        for (ProductRequest product : request.getProducts()) {
            try {
                log.info("Start processing product {}", product.getId());

                processSingleProduct(product);

                responses.add(new ProductResponse(product.getId(), true, ""));
            } catch (Exception e) {

                log.error("Error processing product {}", product.getId(), e);

                responses.add(new ProductResponse(
                        product.getId(),
                        false,
                        "Произошла ошибка при работе с базой данных"
                ));
            }
        }

        return new ExchangeResponse(responses);
    }

    /**
     * Полный pipeline обработки одного товара
     * ВАЖНО: порядок операций критичен!
     */
    private void processSingleProduct(ProductRequest p) {

        // 1. Получаем или создаем налог
        Integer taxId = getOrCreateTax(p.getTaxRate());

        // 4. Получаем уровень цен (должен существовать!)
        Integer priceLevelId = getPriceLevel(p.getPriceLevel());

        
        // 2. Получаем или создаем товар
        Integer goodsId = getOrCreateGoods(p, taxId);

        // 3. Получаем или создаем единицу измерения
        Integer unitId = getOrCreateUnit(goodsId, p.getUnit());


        // 5. Работа с ценой (insert/update)
        upsertPrice(goodsId, unitId, priceLevelId, p.getPrice());


        // 7. Штрихкоды (если есть)
        if (p.getBarcodes() != null) {
            replaceBarcodes(goodsId, p.getBarcodes());
        }
    }

    /**
     * Получить или создать налоговую группу
     */
    private Integer getOrCreateTax(String taxName) {

        List<Integer> ids = jdbc.query(
                "SELECT id_group_tax FROM front.group_tax WHERE name_group_tax = ?",
                (rs, i) -> rs.getInt(1),
                taxName
        );

        if (!ids.isEmpty()) {
            return ids.get(0);
        }

        log.info("Tax not found. Creating new tax: {}", taxName);

        Integer maxId = jdbc.queryForObject(
            "SELECT COALESCE(MAX(id_group_tax), 0) FROM front.group_tax",
            Integer.class
            ) + 1; 
    
        jdbc.update(
            "INSERT INTO front.group_tax(id_group_tax, name_group_tax, active) VALUES (?, ?, true)",
            maxId,
            taxName
        );


        return jdbc.queryForObject(
                "SELECT id_group_tax FROM front.group_tax WHERE name_group_tax = ?",
                Integer.class,
                taxName
        );
    }

    /**
     * Получить или создать товар + goods_attrs
     */
    private Integer getOrCreateGoods(ProductRequest p, Integer taxId) {

        // 🔍 Ищем товар по GUID
        List<Integer> ids = jdbc.query(
                "SELECT id_goods FROM front.goods WHERE guid = ?::uuid",
                (rs, i) -> rs.getInt(1),
                p.getId()
        );

        // 📦 Создание нового товара
        ensureGroupGoodsExists();

        Integer newId = getMaxId("id_goods", "goods");

        log.info("Creating new goods for GUID {}, id {}", p.getId(), newId);

        UUID guid = UUID.fromString(p.getId());

        Integer id_image = null;
        
        if (!ids.isEmpty()) {
            Integer id = ids.get(0);

            log.info("Updating existing goods {}", id);

            jdbc.update("""
                UPDATE front.goods
                SET name_goods=?, id_group_tax=?
                WHERE id_goods=?
            """, p.getName(), taxId, id);

            // 6. Обработка изображения (если есть)
            if (p.getImage() != null && !p.getImage().isEmpty()) {
               id_image = id;
                updateImage(p.getImage(),id);
            }
            
            // 👉 attrs без extern_service_id
            int res = jdbc.update("""
                UPDATE front.goods_attrs
                SET
                    control_rest = false,
                    permission_type = 0,
                    print_name_goods = ?,
                    free_price = true,
                    use_series = false,
                    min_order = 1,
                    id_image = ?,
                    guid = ?::uuid
                WHERE id_goods = ?
            """, p.getName().substring(0, Math.min(p.getName().length(), 50)),id_image, guid, id);
            
            if(res==0){
                jdbc.update("""
                    INSERT INTO front.goods_attrs(
                        id_goods, control_rest, permission_type,
                        print_name_goods, free_price, use_series, min_order, guid,id_image
                    )
                    VALUES (?, false, 0, ?, true, false, 1,?,?)
                """, id, p.getName().substring(0, Math.min(p.getName().length(), 50)),guid, id_image);
                
            }

            // 👉 series 
            //Integer seriesMaxId = getMaxId("id_series", "series");
            //Мы серии не используем, а они тут обязательны. Пусть ИД серии будет равен ИД товара.
            log.info("Updating series for GUID {}, id {}", p.getId(), newId);
            res = jdbc.update("""
                UPDATE front.series
                SET
                    name_series = '-',
                    id_series = '-',
                    active = true,
                    guid = ?::uuid
                WHERE id_goods = ? 
            """, guid, id );

            if(res==0){
                log.info("Creating series for GUID {}, id {}", p.getId(), newId);
                jdbc.update("""
                    INSERT INTO front.series(
                        id_goods, id_series, name_series,
                        active, guid
                    )
                    VALUES (?, '-', '-', true,?)
                """, id, guid);                
            }
            
            
            return id;
        }

        // 👉 Вставка в goods (с guid!)
        jdbc.update("""
            INSERT INTO front.goods(
                id_goods, id_group, id_group_tax, name_goods,
                id_print_group, type_goods, active, guid
            )
            VALUES (?, 1, ?, ?, 1, 0, true, ?::uuid)
        """, newId, taxId, p.getName(), guid);

            // 6. Обработка изображения (если есть)
            if (p.getImage() != null && !p.getImage().isEmpty()) {
               id_image = newId;
                updateImage(p.getImage(),newId);
            }
        // 👉 attrs без extern_service_id
        jdbc.update("""
            INSERT INTO front.goods_attrs(
                id_goods, control_rest, permission_type,
                print_name_goods, free_price, use_series, min_order, guid,id_image
            )
            VALUES (?, false, 0, ?, true, false, 1,?,?)
        """, newId, p.getName().substring(0, Math.min(p.getName().length(), 50)),guid, id_image);

        // 👉 series 
        //Integer seriesMaxId = getMaxId("id_series", "series");
        //Мы серии не используем, а они тут обязательны. Пусть ИД серии будет равен ИД товара.
        log.info("Creating series for GUID {}, id {}", p.getId(), newId);
        jdbc.update("""
            INSERT INTO front.series(
                id_goods, id_series, name_series,
                active, guid
            )
            VALUES (?, '-', '-', true,?)
        """, newId, guid);
        
        return newId;
    }

    /**
     * Получить или создать unit
     */
    private Integer getOrCreateUnit(Integer goodsId, String unitName) {

        List<Integer> ids = jdbc.query(
                "SELECT id_unit FROM front.unit WHERE id_goods=? AND name_unit=?",
                (rs, i) -> rs.getInt(1),
                goodsId, unitName
        );

        if (!ids.isEmpty()) {
            return ids.get(0);
        }

        log.info("Creating unit '{}' for goods {}", unitName, goodsId);

        Integer newId = getMaxId("id_unit", "unit");
        
        jdbc.update("""
            INSERT INTO front.unit(
                id_unit,id_goods, name_unit,
                is_default, rate, type_unit, active
            )
            VALUES (?, ?, ?, false, 1, 1, true)
        """, newId, goodsId, unitName);

        return jdbc.queryForObject(
                "SELECT id_unit FROM front.unit WHERE id_goods=? AND name_unit=?",
                Integer.class,
                goodsId, unitName
        );
    }

    /**
     * Получить id уровня цен
     * ВАЖНО: если не найден — кидаем ошибку
     */
    private Integer getPriceLevel(String name) {

        // 🔍 Ищем существующий уровень цен
        List<Integer> ids = jdbc.query(
                "SELECT id_price_level FROM front.price_level WHERE name_price_level = ?",
                (rs, i) -> rs.getInt(1),
                name
        );

        if (!ids.isEmpty()) {
            return ids.get(0);
        }

        log.info("Price level '{}' not found. Creating new.", name);

        // 📌 Генерим новый ID
        Integer maxId = jdbc.queryForObject(
                "SELECT COALESCE(MAX(id_price_level), 0) FROM front.price_level",
                Integer.class
        ) + 1;

        // 📌 Генерим GUID
        UUID guid = UUID.randomUUID();

        // ✅ Вставка
        jdbc.update("""
            INSERT INTO front.price_level(
                id_price_level,
                name_price_level,
                active,
                guid
            )
            VALUES (?, ?, true, ?::uuid)
        """, maxId, name, guid.toString());

        return maxId;
    }

    
    /**
     * Вставка или обновление цены
     */
    private void upsertPrice(Integer goodsId, Integer unitId, Integer priceLevelId, Integer priceInput) {

        int price = priceInput * 100; // перевод в копейки

        List<Integer> existing = jdbc.query(
                "SELECT price FROM front.price WHERE id_goods=? AND id_unit=? AND id_price_level=?",
                (rs, i) -> rs.getInt(1),
                goodsId, unitId, priceLevelId
        );

        if (existing.isEmpty()) {

            log.info("Creating price for goods {}, unitId={}", goodsId,unitId);

            jdbc.update("""
                INSERT INTO front.price(
                    id_goods,  id_series, id_unit,
                    id_price_level, price, min_price, max_price, active
                )
                VALUES (?, '-', ?, ?, ?, ?, ?, true)
            """, goodsId,  unitId,priceLevelId, price, price, price);

        } else if (!existing.get(0).equals(price)) {

            log.info("Updating price for goods {}", goodsId);

            jdbc.update("""
                UPDATE front.price
                SET price=?, min_price=?, max_price=?
                WHERE id_goods=? AND id_unit=? AND id_price_level=?
            """, price, price, price, goodsId, unitId, priceLevelId);
        }
    }

    /**
     * Сохраняем изображение
     * (пока без связи с товаром)
     */
    private void updateImage(String base64, Integer id_goods) {

        if (base64 == null || base64.isBlank()) {
            log.info("No image provided for goods {}", id_goods);
            return;
        }

        // убираем переносы и пробелы
        base64 = base64.replaceAll("\\s", "");

        byte[] bytes = Base64.getDecoder().decode(base64);

        log.info("Updating image for goods {}", id_goods);

        int updated = jdbc.update("""
            UPDATE front.images
            SET image = ?, image_format = 'image/jpeg', active = true
            WHERE id_image = ?
        """, bytes, id_goods);

        if (updated == 0) {
            log.info("Image not found, inserting new for goods {}", id_goods);

            jdbc.update("""
                INSERT INTO front.images(id_image, image, image_format, active)
                VALUES (?, ?, 'image/jpeg', true)
            """, id_goods, bytes);
        }
    }

    /**
     * Полная замена штрихкодов товара
     */
    private void replaceBarcodes(Integer goodsId, List<BarcodeRequest> barcodes) {

        log.info("Syncing barcodes for goods {}", goodsId);

        for (BarcodeRequest b : barcodes) {

            // 🔍 Проверяем — есть ли уже такой ШК
            List<Integer> exists = jdbc.query(
                """
                SELECT 1
                FROM front.bar_codes
                WHERE id_goods = ?
                  AND bar_code = ?
                """,
                (rs, i) -> rs.getInt(1),
                goodsId,
                b.getBarcode()
            );

            if (!exists.isEmpty()) {
                log.info("Barcode {} already exists for goods {}", b.getBarcode(), goodsId);
                continue;
            }

            // 📦 Если нет — создаём
            Integer unitId = getOrCreateUnit(goodsId, b.getUnit());

            log.info("Creating barcode {} for goods {}", b.getBarcode(), goodsId);

            jdbc.update("""
                INSERT INTO front.bar_codes(
                    bar_code, id_unit, id_goods,
                    id_series, ext_bar_code, active
                )
                VALUES (?, ?, ?, '-', '-', true)
            """, b.getBarcode(), unitId, goodsId);
        }
    }

    Integer getMaxId(String columnName,String tableName){
        Integer maxId = jdbc.queryForObject(
                "SELECT COALESCE(MAX("+columnName+"), 0) FROM "+tableName,
                Integer.class
                ) + 1; 

        return maxId;
    }
    
    /**
     * Проверяет наличие записей в group_goods.
     * Если таблица пустая — создает базовую группу "Товары КСО".
     */
    private void ensureGroupGoodsExists() {

        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM front.group_goods",
                Integer.class
        );

        if (count == null || count == 0) {
            log.info("group_goods is empty. Creating default group...");

            jdbc.update(
                    "INSERT INTO front.group_goods (id_group, name_group, id_owner_group, active) " +
                    "VALUES (1, ?, NULL, true)",
                    "Товары КСО"
            );
        }


    }

}

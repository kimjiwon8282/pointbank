package com.pointbank.securities.product.mapper;

import com.pointbank.securities.product.domain.SecuritiesProduct;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

@Mapper
public interface SecuritiesProductMapper {
    Optional<SecuritiesProduct> findByStockCode(String stockCode);
    boolean existsActiveByStockCode(String stockCode);
    List<SecuritiesProduct> findActiveProducts();
}

package com.pointbank.securities.holding.mapper;

import com.pointbank.securities.holding.domain.SecuritiesHolding;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Mapper
public interface SecuritiesHoldingMapper {
    Optional<SecuritiesHolding> findByAccountIdAndStockCode(
            @Param("accountId") Long accountId,
            @Param("stockCode") String stockCode
    );
    Optional<SecuritiesHolding> findByAccountIdAndStockCodeForUpdate(
            @Param("accountId") Long accountId,
            @Param("stockCode") String stockCode
    );
    List<SecuritiesHolding> findByMemberId(Long memberId);
    int insert(SecuritiesHolding holding);
    int increaseBuyHolding(
            @Param("id") Long id,
            @Param("quantityDelta") long quantityDelta,
            @Param("buyAmountDelta") long buyAmountDelta,
            @Param("avgBuyPrice") BigDecimal avgBuyPrice
    );
    int decreaseSellHolding(
            @Param("id") Long id,
            @Param("quantityDelta") long quantityDelta,
            @Param("totalBuyAmountAfter") long totalBuyAmountAfter,
            @Param("avgBuyPriceAfter") BigDecimal avgBuyPriceAfter
    );
    int deleteIfZeroQuantity(Long id);
    int updateAfterBuy(
            @Param("id") Long id,
            @Param("quantity") long quantity,
            @Param("avgBuyPrice") BigDecimal avgBuyPrice,
            @Param("totalBuyAmount") long totalBuyAmount
    );
    int updateAfterSell(
            @Param("id") Long id,
            @Param("quantity") long quantity,
            @Param("avgBuyPrice") BigDecimal avgBuyPrice,
            @Param("totalBuyAmount") long totalBuyAmount
    );
}

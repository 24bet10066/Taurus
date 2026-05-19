package com.serviceos.parts.entity;

import java.math.BigDecimal;
import java.util.UUID;

/** JSON-serializable value object stored inside PartsSale.items (JSONB). */
public class SaleItem {
    private UUID partId;
    private String partName;
    private int qty;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;

    public SaleItem() {}

    public SaleItem(UUID partId, String partName, int qty, BigDecimal unitPrice, BigDecimal lineTotal) {
        this.partId = partId;
        this.partName = partName;
        this.qty = qty;
        this.unitPrice = unitPrice;
        this.lineTotal = lineTotal;
    }

    public UUID getPartId()          { return partId; }
    public void setPartId(UUID v)    { this.partId = v; }
    public String getPartName()      { return partName; }
    public void setPartName(String v){ this.partName = v; }
    public int getQty()              { return qty; }
    public void setQty(int v)        { this.qty = v; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal v){ this.unitPrice = v; }
    public BigDecimal getLineTotal() { return lineTotal; }
    public void setLineTotal(BigDecimal v){ this.lineTotal = v; }
}

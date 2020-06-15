package com.gft.dlp.model;

import java.io.Serializable;

public class JobExtended extends Job implements Serializable {

    public int count;
    public Float stockValue;

    public JobExtended() {
    }


    public JobExtended(String description, float salary, int count, Float stockValue) {
        super(description, salary);
        this.count = count;
        this.stockValue = stockValue;
    }


    public Float getStockValue() {
        return stockValue;
    }

    public void setStockValue(Float stockValue) {
        this.stockValue = stockValue;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(description).append(",");
        sb.append(salary).append(",");
        sb.append(count).append(",");
        sb.append(stockValue);

        return sb.toString();
    }
}

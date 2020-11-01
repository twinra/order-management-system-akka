package com.example.oms.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@ToString
public class OrderPosition {
    private final String name;
    private final BigDecimal cost;

    @JsonCreator
    public OrderPosition(@JsonProperty("name") String name, @JsonProperty("cost")BigDecimal cost) {
        this.name = name;
        this.cost = cost;
    }
}

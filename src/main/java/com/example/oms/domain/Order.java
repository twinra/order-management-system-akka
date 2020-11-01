package com.example.oms.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;

@Getter
@ToString
public class Order {
    private final String destination;
    private final List<OrderPosition> positions;

    @JsonCreator
    public Order(@JsonProperty("destination") String destination, @JsonProperty("positions") List<OrderPosition> positions) {
        this.destination = destination;
        this.positions = positions;
    }

    public BigDecimal getTotalValue() {
        return positions.stream().map(OrderPosition::getCost).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

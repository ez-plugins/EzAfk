package com.gyvex.ezafk.repository;

import com.github.ezframework.jaloquent.model.Factory;
import com.github.ezframework.jaker.Faker;

import java.util.Map;

public class AfkTimeModelFactory extends Factory<AfkTimeModel> {

    @Override
    protected Map<String, Object> definition(Faker faker) {
        return Map.of("seconds", (long) faker.number().numberBetween(0, 86_400));
    }
}

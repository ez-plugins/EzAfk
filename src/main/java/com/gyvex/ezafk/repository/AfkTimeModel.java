package com.gyvex.ezafk.repository;

import com.github.ezframework.jaloquent.model.Model;
import com.github.ezframework.jaloquent.model.ModelFactory;

public final class AfkTimeModel extends Model {

    public static final String TABLE_PREFIX = "afk_times";

    public static final ModelFactory<AfkTimeModel> FACTORY = (id, data) -> {
        AfkTimeModel m = new AfkTimeModel(id);
        m.fromMap(data);
        return m;
    };

    public AfkTimeModel(String id) {
        super(id);
    }

    public long getSeconds() {
        return getAs("seconds", Long.class, 0L);
    }

    public void setSeconds(long seconds) {
        set("seconds", seconds);
    }
}

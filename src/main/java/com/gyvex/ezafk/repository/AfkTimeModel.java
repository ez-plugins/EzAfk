package com.gyvex.ezafk.repository;

import com.github.ezframework.jaloquent.model.Factory;
import com.github.ezframework.jaloquent.model.HasFactory;
import com.github.ezframework.jaloquent.model.Model;
import com.github.ezframework.jaloquent.model.ModelFactory;

public final class AfkTimeModel extends Model implements HasFactory {

    public static final String TABLE_PREFIX = "afk_times";

    public static final ModelFactory<AfkTimeModel> FACTORY = (id, data) -> {
        AfkTimeModel m = new AfkTimeModel(id);
        m.fromMap(data);
        return m;
    };

    public AfkTimeModel(String id) {
        super(id);
        setFillable("seconds");
    }

    public static Factory<AfkTimeModel> factory() {
        return Factory.discover(AfkTimeModel.class);
    }

    public long getSeconds() {
        return getAs("seconds", Long.class, 0L);
    }

    public void setSeconds(long seconds) {
        set("seconds", seconds);
    }
}

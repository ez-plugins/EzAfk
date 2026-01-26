package com.gyvex.ezafk.integration;

import com.gyvex.ezafk.EzAfk;
import com.gyvex.ezafk.registry.Registry;
import com.gyvex.ezafk.manager.IntegrationManager;
import com.gyvex.ezafk.state.AfkState;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SingleLineChart;

public class MetricsIntegration extends Integration {
    private Metrics metrics;

    private void setupMetrics() {
        this.metrics = new Metrics(Registry.get().getPlugin(), 22316);
        this.metrics.addCustomChart(new SingleLineChart("tab_integration_count", () -> IntegrationManager.hasIntegration("tab") ? 1 : 0));
        this.metrics.addCustomChart(new SingleLineChart("worldguard_integration_count", () -> IntegrationManager.hasIntegration("worldguard") ? 1 : 0));
        this.metrics.addCustomChart(new SingleLineChart("afk_players", () -> AfkState.afkPlayers.size()));
    }

    public Metrics getMetrics() {
        return this.metrics;
    }

    @Override
    public void load() {
        this.setupMetrics();
        this.isSetup = true;
    }

    @Override
    public void unload() {
        metrics.shutdown();
    }
}

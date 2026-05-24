package com.kgm.ui.panel;

import com.kgm.dao.EmployeeRecordDao;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

/**
 * Home Stats Panel - Main dashboard container.
 * Composes KPI Panel and Charts Panel for a complete analytics view.
 */
public class HomeStatsPanel extends JPanel {
    private final HomeStatsKPIPanel kpiPanel;
    private final HomeStatsChartsPanel chartsPanel;

    public HomeStatsPanel(EmployeeRecordDao repo) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        setBackground(Color.WHITE);

        kpiPanel = new HomeStatsKPIPanel(repo);
        chartsPanel = new HomeStatsChartsPanel(repo);

        add(kpiPanel);
        
        JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        add(separator);
        
        add(chartsPanel);
    }

    public void setShowInTableHandler(Consumer<String> handler) {
        chartsPanel.setShowInTableHandler(handler);
    }

    public void reload() {
        kpiPanel.reload();
        chartsPanel.reload();
    }
}
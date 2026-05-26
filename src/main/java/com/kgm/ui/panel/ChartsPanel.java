package com.kgm.ui.panel;

import com.kgm.dao.EmployeeRecordDao;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

/**
 * Charts Panel - Displays dashboard charts and analytics.
 * This is a separate component that shows interactive charts for:
 * Department breakdown, Grades, Designations, Missing Data, Exit Trends.
 * 
 * Used by HomeView to display charts in the main dashboard layout after the table.
 */
public class ChartsPanel extends JPanel {
    private final HomeStatsChartsPanel chartsPanel;
    private EmployeeRecordDao repo;

    public ChartsPanel(EmployeeRecordDao repo) {
        this.repo = repo;
        setLayout(new BorderLayout());
        setOpaque(false);
        setBackground(Color.WHITE);

        chartsPanel = new HomeStatsChartsPanel(repo);

        add(chartsPanel, BorderLayout.CENTER);
    }

    public void setShowInTableHandler(Consumer<String> handler) {
        chartsPanel.setShowInTableHandler(handler);
    }

    public void setRepository(EmployeeRecordDao repo) {
        this.repo = repo;
        chartsPanel.setRepository(repo);
    }

    public void setStats(EmployeeRecordDao.DashboardStats stats) {
        chartsPanel.setStats(stats);
    }

    public void reload() {
        if (repo != null) {
            setStats(repo.dashboardStats());
        }
    }
}

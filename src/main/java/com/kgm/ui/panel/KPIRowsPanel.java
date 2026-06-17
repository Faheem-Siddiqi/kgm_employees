package com.kgm.ui.panel;

import com.kgm.dao.EmployeeRecordDao;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

/**
 * KPI Rows Panel - Displays key performance indicators (KPIs) as metric cards.
 * This is a separate component that shows: Total Employees, Departments, Grades, 
 * Designations, and Missing Documents.
 * 
 * Used by HomeView to display KPIs in the main dashboard layout after the header.
 */
public class KPIRowsPanel extends JPanel {
    private final HomeStatsKPIPanel kpiPanel;
    private EmployeeRecordDao repo;

    public KPIRowsPanel(EmployeeRecordDao repo) {
        this.repo = repo;
        setLayout(new BorderLayout());
        setOpaque(false);
        setBackground(Color.WHITE);

        kpiPanel = new HomeStatsKPIPanel(repo);

        add(kpiPanel, BorderLayout.CENTER);
    }

    public void setRepository(EmployeeRecordDao repo) {
        this.repo = repo;
        kpiPanel.setRepository(repo);
    }

    public void setStats(EmployeeRecordDao.DashboardStats stats) {
        kpiPanel.setStats(stats);
    }

    public void setChartTargetHandler(Consumer<String> handler) {
        kpiPanel.setChartTargetHandler(handler);
    }

    public void reload() {
        if (repo != null) {
            setStats(repo.dashboardStats());
        }
    }
}

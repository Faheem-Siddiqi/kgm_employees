package com.kgm.ui.panel;

import com.kgm.dao.EmployeeRecordDao;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.Component;
import java.util.Locale;

/**
 * KPI Panel - Displays key performance indicators as metric cards.
 * Shows: Total Employees, Departments, Grades, Designations, Missing Documents
 */
public class HomeStatsKPIPanel extends JPanel {
    private static final Color BACKGROUND = Color.WHITE;
    private static final Color CARD_BORDER = new Color(220, 226, 232);
    private static final Color TEXT_PRIMARY = new Color(35, 43, 54);
    private static final Color TEXT_SECONDARY = new Color(99, 115, 129);
    private static final Color BLUE = new Color(0, 112, 210);
    private static final Color TEAL = new Color(0, 150, 136);
    private static final Color ORANGE = new Color(226, 122, 47);
    private static final Color PURPLE = new Color(106, 90, 205);
    private static final Color RED = new Color(203, 75, 64);

    private EmployeeRecordDao repo;
    private final JPanel metricRow = new JPanel(new GridLayout(1, 5, 12, 0));
    private EmployeeRecordDao.DashboardStats stats;

    public HomeStatsKPIPanel(EmployeeRecordDao repo) {
        this.repo = repo;
        setLayout(new BorderLayout(0, 14));
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(16, 0, 8, 0));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Key Metrics");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(TEXT_PRIMARY);
        JLabel subtitle = new JLabel("Employee overview, department breakdown, and compliance status.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(TEXT_SECONDARY);

        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        copy.add(title);
        copy.add(Box.createVerticalStrut(4));
        copy.add(subtitle);
        header.add(copy, BorderLayout.WEST);

        metricRow.setOpaque(false);

        add(header, BorderLayout.NORTH);
        add(metricRow, BorderLayout.CENTER);
        rebuildMetrics();
    }

    public void setRepository(EmployeeRecordDao repo) {
        this.repo = repo;
    }

    public void setStats(EmployeeRecordDao.DashboardStats stats) {
        this.stats = stats;
        rebuildMetrics();
    }

    public void reload() {
        if (repo != null) {
            setStats(repo.dashboardStats());
        }
    }

    private void rebuildMetrics() {
        metricRow.removeAll();
        if (stats == null) {
            metricRow.add(metricCard("Total Ex-Employees:", "-", BLUE));
            metricRow.add(metricCard("Departments", "-", TEAL));
            metricRow.add(metricCard("Grades", "-", PURPLE));
            metricRow.add(metricCard("Designations", "-", ORANGE));
            metricRow.add(metricCard("Missing Data", "-", RED));
            metricRow.revalidate();
            metricRow.repaint();
            return;
        }

        int departments = stats.employeesByDepartment().size();
        int grades = stats.employeesByGrade().size();
        int designations = stats.employeesByDesignation().size();

        metricRow.add(metricCard("Total Ex-Employees:", formatNumber(stats.totalEmployees()), BLUE));
        metricRow.add(metricCard("Departments", formatNumber(departments), TEAL));
        metricRow.add(metricCard("Grades", formatNumber(grades), PURPLE));
        metricRow.add(metricCard("Designations", formatNumber(designations), ORANGE));
        metricRow.add(metricCard("Missing Data", formatNumber(missingEmployees()), RED));

        metricRow.revalidate();
        metricRow.repaint();
    }

    private JPanel metricCard(String title, String value, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(BACKGROUND);
        card.setBorder(new CompoundBorder(
                new RoundedBorder(8, CARD_BORDER),
                new EmptyBorder(14, 14, 14, 14)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        titleLabel.setForeground(TEXT_SECONDARY);

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        valueLabel.setForeground(accent);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private int missingEmployees() {
        return stats.employeesMissingAnyRequiredData();
    }

    private String formatNumber(int value) {
        return String.format(Locale.US, "%,d", value);
    }

    /**
     * Custom rounded border utility for card styling.
     */
    private static class RoundedBorder extends AbstractBorder {
        private final int radius;
        private final Color color;

        RoundedBorder(int radius, Color color) {
            this.radius = radius;
            this.color = color;
        }

        public void paintBorder(Component component, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
            g2.dispose();
        }

        public Insets getBorderInsets(Component component) {
            return new Insets(8, 8, 8, 8);
        }
    }
}

package com.kgm.ui.panel;

import com.kgm.dao.EmployeeRecordDao;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * KPI Panel - Displays key performance indicators as metric cards.
 * Shows: Total Employees, Departments, Grades, Designations, Missing Documents
 */
public class HomeStatsKPIPanel extends JPanel {
    private static final Color BACKGROUND = Color.WHITE;
    private static final Color TEXT_PRIMARY = new Color(35, 43, 54);
    private static final Color TEXT_SECONDARY = new Color(99, 115, 129);
    private static final MetricTheme TOTAL_THEME = new MetricTheme(new Color(37, 99, 235), new Color(30, 64, 175));
    private static final MetricTheme DEPARTMENT_THEME = new MetricTheme(new Color(20, 184, 166), new Color(15, 118, 110));
    private static final MetricTheme GRADE_THEME = new MetricTheme(new Color(124, 58, 237), new Color(91, 33, 182));
    private static final MetricTheme DESIGNATION_THEME = new MetricTheme(new Color(245, 158, 11), new Color(180, 83, 9));
    private static final MetricTheme MISSING_THEME = new MetricTheme(new Color(239, 68, 68), new Color(185, 28, 28));
    private static final int CARD_GAP = 12;
    private static final int CARD_HEIGHT = 116;
    private static final int CARD_MIN_WIDTH = 174;

    private EmployeeRecordDao repo;
    private final ResponsiveMetricGrid metricRow = new ResponsiveMetricGrid(CARD_GAP, CARD_HEIGHT, CARD_MIN_WIDTH);
    private EmployeeRecordDao.DashboardStats stats;
    private Consumer<String> chartTargetHandler;

    public HomeStatsKPIPanel(EmployeeRecordDao repo) {
        this.repo = repo;
        setLayout(new BorderLayout(0, 14));
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(0, 0, 8, 0));

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

    public void setChartTargetHandler(Consumer<String> handler) {
        this.chartTargetHandler = handler;
    }

    public void reload() {
        if (repo != null) {
            setStats(repo.dashboardStats());
        }
    }

    private void rebuildMetrics() {
        metricRow.removeAll();
        if (stats == null) {
            metricRow.add(metricCard("Total Ex-Employees", "-", TOTAL_THEME, "exitReasons"));
            metricRow.add(metricCard("Departments", "-", DEPARTMENT_THEME, "department"));
            metricRow.add(metricCard("Grades", "-", GRADE_THEME, "grade"));
            metricRow.add(metricCard("Designations", "-", DESIGNATION_THEME, "designation"));
            metricRow.add(metricCard("Missing Data", "-", MISSING_THEME, "missing"));
            metricRow.revalidate();
            metricRow.repaint();
            return;
        }

        int departments = stats.employeesByDepartment().size();
        int grades = stats.employeesByGrade().size();
        int designations = stats.employeesByDesignation().size();

        metricRow.add(metricCard("Total Ex-Employees", formatNumber(stats.totalEmployees()), TOTAL_THEME, "exitReasons"));
        metricRow.add(metricCard("Departments", formatNumber(departments), DEPARTMENT_THEME, "department"));
        metricRow.add(metricCard("Grades", formatNumber(grades), GRADE_THEME, "grade"));
        metricRow.add(metricCard("Designations", formatNumber(designations), DESIGNATION_THEME, "designation"));
        metricRow.add(metricCard("Missing Data", formatNumber(missingEmployees()), MISSING_THEME, "missing"));

        metricRow.revalidate();
        metricRow.repaint();
    }

    private JPanel metricCard(String title, String value, MetricTheme theme, String chartKey) {
        JPanel card = new GradientMetricCard(theme.start(), theme.end());
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(new EmptyBorder(16, 16, 15, 16));
        card.setPreferredSize(new Dimension(CARD_MIN_WIDTH, CARD_HEIGHT));
        card.setMinimumSize(new Dimension(150, CARD_HEIGHT));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.setToolTipText("Open related chart");
        card.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent event) {
                if (chartTargetHandler != null) {
                    chartTargetHandler.accept(chartKey);
                }
            }
        });

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        titleLabel.setForeground(new Color(255, 255, 255, 220));

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        valueLabel.setForeground(Color.WHITE);

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

    private static class GradientMetricCard extends JPanel {
        private static final int RADIUS = 8;

        private final Color start;
        private final Color end;

        private GradientMetricCard(Color start, Color end) {
            this.start = start;
            this.end = end;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(new GradientPaint(0, 0, start, getWidth(), getHeight(), end));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), RADIUS, RADIUS);

            g2.setColor(new Color(255, 255, 255, 70));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, RADIUS, RADIUS);
            g2.dispose();
            super.paintComponent(graphics);
        }
    }

    private static class ResponsiveMetricGrid extends JPanel {
        private final int gap;
        private final int cardHeight;
        private final int minCardWidth;

        private ResponsiveMetricGrid(int gap, int cardHeight, int minCardWidth) {
            super(null);
            this.gap = gap;
            this.cardHeight = cardHeight;
            this.minCardWidth = minCardWidth;
            setOpaque(false);
            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent event) {
                    revalidate();
                }
            });
        }

        @Override
        public void doLayout() {
            int count = getComponentCount();
            if (count == 0) {
                return;
            }

            int width = Math.max(1, getWidth());
            int columns = columnsFor(width, count);
            int cardWidth = Math.max(1, (width - gap * (columns - 1)) / columns);

            for (int index = 0; index < count; index++) {
                int row = index / columns;
                int column = index % columns;
                int x = column * (cardWidth + gap);
                int y = row * (cardHeight + gap);
                getComponent(index).setBounds(x, y, cardWidth, cardHeight);
            }
        }

        @Override
        public Dimension getPreferredSize() {
            int count = getComponentCount();
            if (count == 0) {
                return new Dimension(0, 0);
            }

            int width = getWidth();
            if (width <= 0 && getParent() != null) {
                width = getParent().getWidth();
            }
            if (width <= 0) {
                width = count * minCardWidth + gap * Math.max(0, count - 1);
            }

            int columns = columnsFor(width, count);
            int rows = (int) Math.ceil(count / (double) columns);
            return new Dimension(width, rows * cardHeight + gap * Math.max(0, rows - 1));
        }

        private int columnsFor(int width, int count) {
            int columns = Math.max(1, (width + gap) / (minCardWidth + gap));
            return Math.max(1, Math.min(count, columns));
        }
    }

    private record MetricTheme(Color start, Color end) {
    }
}

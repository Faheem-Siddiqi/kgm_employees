package com.kgm.ui.panel;

import com.kgm.dao.EmployeeRecordDao;
import com.kgm.ui.MissingDataView;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Charts Panel - Displays interactive dashboard charts.
 * Shows: Department breakdown, Grades, Designations, Missing Documents, Exit Trends
 */
public class HomeStatsChartsPanel extends JPanel {
    private static final Color BACKGROUND = Color.WHITE;
    private static final Color CARD_BORDER = new Color(220, 226, 232);
    private static final Color TEXT_PRIMARY = new Color(35, 43, 54);
    private static final Color TEXT_SECONDARY = new Color(99, 115, 129);
    private static final Color BLUE = new Color(0, 112, 210);
    private static final Color TEAL = new Color(0, 150, 136);
    private static final Color PURPLE = new Color(106, 90, 205);
    private static final Color ORANGE = new Color(226, 122, 47);
    private static final Color RED = new Color(203, 75, 64);
    private static final Color GREEN = new Color(51, 153, 102);

    private final EmployeeRecordDao repo;
    private final JPanel chartsPanel = new JPanel(new GridBagLayout());
    private final JLabel departmentTitle = new JLabel("Employees by Department");
    private final JButton departmentBack = new JButton("Back");
    private final JLabel missingTitle = new JLabel("Missing Required Data");
    private final JButton missingBack = new JButton("Back");
    private final JButton missingDetail = new JButton("View employees with missing required data");

    private final DashboardBarChart departmentChart = new DashboardBarChart();
    private final DashboardBarChart gradeChart = new DashboardBarChart();
    private final DashboardBarChart designationChart = new DashboardBarChart();
    private final DashboardBarChart missingDocsChart = new DashboardBarChart();
    private final DashboardBarChart exitTrendChart = new DashboardBarChart();

    private EmployeeRecordDao.DashboardStats stats;
    private String selectedDepartment;
    private String selectedMissingGroup;

    public HomeStatsChartsPanel(EmployeeRecordDao repo) {
        this.repo = repo;
        setLayout(new BorderLayout(0, 14));
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(16, 0, 8, 0));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Analytics & Charts");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(TEXT_PRIMARY);
        JLabel subtitle = new JLabel("Interactive charts for organizational structure, compliance, and exit analysis.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(TEXT_SECONDARY);

        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        copy.add(title);
        copy.add(Box.createVerticalStrut(4));
        copy.add(subtitle);
        header.add(copy, BorderLayout.WEST);

        chartsPanel.setOpaque(false);
        styleDepartmentBackButton();
        styleMissingControls();

        add(header, BorderLayout.NORTH);
        add(chartsPanel, BorderLayout.CENTER);
        reload();
    }

    public void reload() {
        stats = repo.dashboardStats();
        selectedDepartment = null;
        selectedMissingGroup = null;
        rebuildCharts();
    }

    private void rebuildCharts() {
        chartsPanel.removeAll();

        configureDepartmentChart();
        configureMissingChart();
        gradeChart.setItems(gradeItems(stats.employeesByGrade()));
        designationChart.setItems(countItems(stats.employeesByDesignation(), PURPLE));
        exitTrendChart.setItems(countItems(stats.exitTrends(), GREEN));

        addChartCard(chartsPanel, departmentCard(), 0, 0, 1, 1.0);
        addChartCard(chartsPanel, chartCard("Employees by Grade", gradeChart), 1, 0, 1, 1.0);
        addChartCard(chartsPanel, chartCard("Employees by Designation", designationChart), 0, 1, 1, 1.0);
        addChartCard(chartsPanel, missingDataCard(), 1, 1, 1, 1.0);
        addChartCard(chartsPanel, chartCard("Exit Reasons Overview", exitTrendChart), 0, 2, 2, 1.0);

        chartsPanel.revalidate();
        chartsPanel.repaint();
    }

    private JPanel departmentCard() {
        departmentBack.setVisible(selectedDepartment != null);

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.add(departmentTitle, BorderLayout.WEST);
        titleRow.add(departmentBack, BorderLayout.EAST);
        return chartCard(titleRow, departmentChart);
    }

    private void styleDepartmentBackButton() {
        departmentBack.setFocusPainted(false);
        departmentBack.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        departmentBack.addActionListener(event -> {
            selectedDepartment = null;
            configureDepartmentChart();
        });
    }

    private void styleMissingControls() {
        missingBack.setFocusPainted(false);
        missingBack.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        missingBack.addActionListener(event -> {
            selectedMissingGroup = null;
            configureMissingChart();
        });

        missingDetail.setBorderPainted(false);
        missingDetail.setContentAreaFilled(false);
        missingDetail.setFocusPainted(false);
        missingDetail.setForeground(BLUE);
        missingDetail.setFont(new Font("Segoe UI", Font.BOLD, 12));
        missingDetail.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        missingDetail.addActionListener(event -> openMissingDataView());
    }

    private void configureDepartmentChart() {
        if (selectedDepartment == null) {
            departmentTitle.setText("Employees by Department");
            departmentChart.setItems(countItems(stats.employeesByDepartment(), BLUE));
            departmentChart.setClickHandler(item -> {
                if (stats.sectionsByDepartment().containsKey(item.label())) {
                    selectedDepartment = item.label();
                    configureDepartmentChart();
                }
            });
        } else {
            departmentTitle.setText("Sections in " + selectedDepartment);
            departmentChart.setItems(countItems(
                    stats.sectionsByDepartment().getOrDefault(selectedDepartment, List.of()),
                    TEAL
            ));
            departmentChart.setClickHandler(null);
        }
        departmentBack.setVisible(selectedDepartment != null);
        departmentChart.revalidate();
        departmentChart.repaint();
    }

    private JPanel missingDataCard() {
        missingBack.setVisible(selectedMissingGroup != null);

        JPanel titleRow = new JPanel(new BorderLayout(8, 0));
        titleRow.setOpaque(false);
        titleRow.add(missingTitle, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(missingDetail);
        actions.add(missingBack);
        titleRow.add(actions, BorderLayout.EAST);

        return chartCard(titleRow, missingDocsChart);
    }

    private void configureMissingChart() {
        if (selectedMissingGroup == null) {
            missingTitle.setText("Missing Required Data");
            missingDocsChart.setItems(List.of(
                    new ChartItem(
                            "Documents",
                            stats.totalMissingRequiredDocuments(),
                            htmlTooltip(
                                    "Documents",
                                    stats.totalMissingRequiredDocuments(),
                                    List.of(stats.employeesMissingRequiredDocuments() + " employees affected")
                            ),
                            RED
                    ),
                    new ChartItem(
                            "Heading Fields",
                            stats.totalMissingRequiredFields(),
                            htmlTooltip(
                                    "Heading Fields",
                                    stats.totalMissingRequiredFields(),
                                    List.of(stats.employeesMissingRequiredFields() + " employees affected")
                            ),
                            PURPLE
                    )
            ));
            missingDocsChart.setClickHandler(item -> {
                selectedMissingGroup = item.label();
                configureMissingChart();
            });
        } else if ("Documents".equals(selectedMissingGroup)) {
            missingTitle.setText("Missing Documents");
            missingDocsChart.setItems(missingRequirementItems(stats.missingRequiredDocuments(), RED, "employees missing"));
            missingDocsChart.setClickHandler(null);
        } else {
            missingTitle.setText("Missing Required Fields");
            missingDocsChart.setItems(missingRequirementItems(stats.missingRequiredFields(), PURPLE, "employees missing"));
            missingDocsChart.setClickHandler(null);
        }
        missingBack.setVisible(selectedMissingGroup != null);
        missingDocsChart.revalidate();
        missingDocsChart.repaint();
    }

    private void openMissingDataView() {
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window != null) {
            window.dispose();
        }
        new MissingDataView();
    }

    private JPanel chartCard(String title, DashboardBarChart chart) {
        JLabel label = new JLabel(title);
        label.setFont(new Font("Segoe UI", Font.BOLD, 15));
        label.setForeground(TEXT_PRIMARY);
        return chartCard(label, chart);
    }

    private JPanel chartCard(JComponent title, DashboardBarChart chart) {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(BACKGROUND);
        card.setBorder(new CompoundBorder(
                new RoundedBorder(8, CARD_BORDER),
                new EmptyBorder(14, 14, 14, 14)
        ));
        card.add(title, BorderLayout.NORTH);
        card.add(chartScroller(chart), BorderLayout.CENTER);
        return card;
    }

    private JScrollPane chartScroller(DashboardBarChart chart) {
        JScrollPane scrollPane = new JScrollPane(chart);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(BACKGROUND);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(18);
        scrollPane.setPreferredSize(new Dimension(420, 315));
        return scrollPane;
    }

    private void addChartCard(
            JPanel target,
            JComponent component,
            int x,
            int y,
            int width,
            double weightx
    ) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = width;
        gbc.insets = new Insets(0, 0, 14, x == 0 && width == 1 ? 14 : 0);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = weightx;
        gbc.weighty = 1.0;
        target.add(component, gbc);
    }

    private List<ChartItem> countItems(List<EmployeeRecordDao.CountStat> stats, Color color) {
        List<ChartItem> items = new ArrayList<>();
        for (EmployeeRecordDao.CountStat stat : stats) {
            items.add(new ChartItem(
                    stat.label(),
                    stat.count(),
                    htmlTooltip(stat.label(), stat.count(), List.of()),
                    color
            ));
        }
        return items;
    }

    private List<ChartItem> gradeItems(List<EmployeeRecordDao.ContributionStat> stats) {
        List<ChartItem> items = new ArrayList<>();
        for (EmployeeRecordDao.ContributionStat stat : stats) {
            List<String> lines = new ArrayList<>();
            for (EmployeeRecordDao.CountStat contribution : stat.contributions()) {
                lines.add(contribution.label() + ": " + contribution.count());
            }
            items.add(new ChartItem(
                    stat.label(),
                    stat.count(),
                    htmlTooltip(stat.label(), stat.count(), lines),
                    ORANGE
            ));
        }
        return items;
    }

    private List<ChartItem> missingRequirementItems(
            List<EmployeeRecordDao.MissingRequirementStat> stats,
            Color color,
            String suffix
    ) {
        List<ChartItem> items = new ArrayList<>();
        for (EmployeeRecordDao.MissingRequirementStat stat : stats) {
            items.add(new ChartItem(
                    stat.label(),
                    stat.missingCount(),
                    htmlTooltip(stat.label(), stat.missingCount(), List.of(suffix)),
                    color
            ));
        }
        return items;
    }

    private String htmlTooltip(String label, int count, List<String> lines) {
        StringBuilder html = new StringBuilder("<html><b>")
                .append(escape(label))
                .append("</b><br>Total: ")
                .append(count);
        for (String line : lines) {
            html.append("<br>").append(escape(line));
        }
        html.append("</html>");
        return html.toString();
    }

    private String escape(String value) {
        return value == null
                ? ""
                : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private record ChartItem(String label, int count, String tooltip, Color color) {
    }

    /**
     * Interactive bar chart component with hover effects and click handlers.
     */
    private static class DashboardBarChart extends JPanel {
        private final List<ChartItem> items = new ArrayList<>();
        private final Map<Integer, Rectangle> barBounds = new LinkedHashMap<>();
        private int hoveredIndex = -1;
        private Consumer<ChartItem> clickHandler;

        DashboardBarChart() {
            setOpaque(true);
            setBackground(BACKGROUND);
            setToolTipText("");
            ToolTipManager.sharedInstance().registerComponent(this);
            addMouseMotionListener(new MouseAdapter() {
                public void mouseMoved(MouseEvent event) {
                    int hovered = itemAt(event.getPoint());
                    if (hovered != hoveredIndex) {
                        hoveredIndex = hovered;
                        setCursor(Cursor.getPredefinedCursor(
                                hoveredIndex >= 0 && clickHandler != null ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR
                        ));
                        repaint();
                    }
                }
            });
            addMouseListener(new MouseAdapter() {
                public void mouseExited(MouseEvent event) {
                    hoveredIndex = -1;
                    setCursor(Cursor.getDefaultCursor());
                    repaint();
                }

                public void mouseClicked(MouseEvent event) {
                    int index = itemAt(event.getPoint());
                    if (index >= 0 && clickHandler != null) {
                        clickHandler.accept(items.get(index));
                    }
                }
            });
        }

        void setItems(List<ChartItem> newItems) {
            items.clear();
            items.addAll(newItems == null ? List.of() : newItems);
            int preferredWidth = Math.max(420, 80 + items.size() * 58);
            setPreferredSize(new Dimension(preferredWidth, 300));
            revalidate();
            repaint();
        }

        void setClickHandler(Consumer<ChartItem> clickHandler) {
            this.clickHandler = clickHandler;
        }

        public String getToolTipText(MouseEvent event) {
            int index = itemAt(event.getPoint());
            return index >= 0 ? items.get(index).tooltip() : null;
        }

        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            barBounds.clear();

            if (items.isEmpty()) {
                drawEmptyState(g2);
                g2.dispose();
                return;
            }

            int width = getWidth();
            int height = getHeight();
            int left = 48;
            int right = 18;
            int top = 24;
            int bottom = 104;
            int chartHeight = Math.max(90, height - top - bottom);
            int chartWidth = Math.max(1, width - left - right);
            int max = 1;
            for (ChartItem item : items) {
                max = Math.max(max, item.count());
            }

            g2.setColor(new Color(242, 245, 248));
            for (int line = 0; line <= 4; line++) {
                int y = top + chartHeight - (chartHeight * line / 4);
                g2.drawLine(left, y, width - right, y);
            }

            int slot = Math.max(44, chartWidth / items.size());
            int barWidth = Math.max(18, Math.min(36, slot - 16));
            Font valueFont = new Font("Segoe UI", Font.BOLD, 11);
            Font labelFont = new Font("Segoe UI", Font.PLAIN, 11);

            for (int index = 0; index < items.size(); index++) {
                ChartItem item = items.get(index);
                int slotX = left + index * slot;
                int barX = slotX + (slot - barWidth) / 2;
                int barHeight = Math.max(2, (int) Math.round((item.count() / (double) max) * chartHeight));
                int barY = top + chartHeight - barHeight;
                Rectangle bounds = new Rectangle(barX, barY, barWidth, barHeight);
                barBounds.put(index, bounds);

                Color barColor = index == hoveredIndex ? item.color().darker() : item.color();
                g2.setColor(barColor);
                g2.fillRoundRect(barX, barY, barWidth, barHeight, 8, 8);

                g2.setFont(valueFont);
                g2.setColor(TEXT_PRIMARY);
                String value = String.valueOf(item.count());
                int valueX = barX + (barWidth - g2.getFontMetrics().stringWidth(value)) / 2;
                g2.drawString(value, valueX, Math.max(14, barY - 6));

                g2.setFont(labelFont);
                g2.setColor(TEXT_SECONDARY);
                drawVerticalLabel(g2, shortLabel(item.label()), slotX + slot / 2, height - 12);
            }

            g2.dispose();
        }

        private void drawEmptyState(Graphics2D g2) {
            g2.setColor(TEXT_SECONDARY);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            String text = "No data available";
            FontMetrics metrics = g2.getFontMetrics();
            g2.drawString(text, (getWidth() - metrics.stringWidth(text)) / 2, getHeight() / 2);
        }

        private void drawVerticalLabel(Graphics2D g2, String label, int centerX, int baselineY) {
            Graphics2D copy = (Graphics2D) g2.create();
            copy.rotate(-Math.PI / 2);
            copy.drawString(label, -baselineY, centerX + 4);
            copy.dispose();
        }

        private String shortLabel(String label) {
            String clean = label == null || label.isBlank() ? "Unassigned" : label.trim();
            return clean.length() <= 28 ? clean : clean.substring(0, 25) + "...";
        }

        private int itemAt(Point point) {
            for (Map.Entry<Integer, Rectangle> entry : barBounds.entrySet()) {
                if (entry.getValue().contains(point)) {
                    return entry.getKey();
                }
            }
            return -1;
        }
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

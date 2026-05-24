package com.kgm.ui.panel;

import com.kgm.dao.EmployeeRecordDao;
import com.kgm.ui.MissingDataView;
import com.kgm.ui.styling.HomeStatsChartHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Charts Panel - Displays interactive dashboard charts.
 * Shows: Department breakdown, Grades, Designations, Missing Documents, Exit Trends.
 *
 * UX features:
 * - Dense X-axis charts automatically take full width and scroll horizontally if content still overflows.
 * - Smaller charts render two per row on wide screens and one per row on narrow screens.
 * - Single chart in a row always occupies full width.
 * - Modern hover state, value badges, soft card styling and cleaner label placement.
 * - Zero-count items hidden from missing data bars.
 * - Click grade bar to drill into departments within that grade.
 * - Click designation bar to drill into departments with that designation.
 */
public class HomeStatsChartsPanel extends JPanel {
    private static final int CARD_GAP = HomeStatsChartHelper.CARD_GAP;
    private static final int SINGLE_COLUMN_WIDTH = HomeStatsChartHelper.SINGLE_COLUMN_WIDTH;

    private EmployeeRecordDao repo;
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
    private final DashboardPieChart exitTrendChart = new DashboardPieChart();

    private EmployeeRecordDao.DashboardStats stats;
    private String selectedDepartment;
    private String selectedMissingGroup;
    private String selectedGrade;
    private String selectedDesignation;
    private Consumer<String> showInTableHandler;

    public HomeStatsChartsPanel(EmployeeRecordDao repo) {
        this.repo = repo;
        setLayout(new BorderLayout(0, 14));
        setBackground(HomeStatsChartHelper.BACKGROUND);
        setBorder(new EmptyBorder(16, 0, 8, 0));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Analytics & Charts");
        title.setFont(HomeStatsChartHelper.TITLE_FONT);
        title.setForeground(HomeStatsChartHelper.TEXT_PRIMARY);
        JLabel subtitle = new JLabel("Interactive charts for organizational structure, compliance, and exit analysis.");
        subtitle.setFont(HomeStatsChartHelper.SUBTITLE_FONT);
        subtitle.setForeground(HomeStatsChartHelper.TEXT_SECONDARY);

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

        addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent event) {
                if (stats != null) {
                    rebuildCharts();
                }
            }
        });

        add(header, BorderLayout.NORTH);
        add(chartsPanel, BorderLayout.CENTER);
        rebuildCharts();
    }

    public void setShowInTableHandler(Consumer<String> handler) {
        this.showInTableHandler = handler;
    }

    public void setRepository(EmployeeRecordDao repo) {
        this.repo = repo;
    }

    public void setStats(EmployeeRecordDao.DashboardStats stats) {
        this.stats = stats;
        selectedDepartment = null;
        selectedMissingGroup = null;
        selectedGrade = null;
        selectedDesignation = null;
        rebuildCharts();
    }

    public void reload() {
        if (repo != null) {
            setStats(repo.dashboardStats());
        }
    }

    private void rebuildCharts() {
        chartsPanel.removeAll();
        if (stats == null) {
            chartsPanel.revalidate();
            chartsPanel.repaint();
            return;
        }

        configureDepartmentChart();
        configureMissingChart();
        configureGradeChart();
        configureDesignationChart();
        exitTrendChart.setItems(paletteItems(stats.exitTrends()));

        List<ChartCardSpec> cards = List.of(
                new ChartCardSpec(departmentCard(), departmentChart),
                new ChartCardSpec(gradeCard(), gradeChart),
                new ChartCardSpec(designationCard(), designationChart),
                new ChartCardSpec(missingDataCard(), missingDocsChart),
                new ChartCardSpec(chartCardWithFilter("Exit Reasons Overview", exitTrendChart, "RESIGN_REASON"), exitTrendChart)
        );

        addResponsiveChartCards(cards);

        chartsPanel.revalidate();
        chartsPanel.repaint();
    }

    private void addResponsiveChartCards(List<ChartCardSpec> cards) {
        int availableWidth = Math.max(0, getWidth());
        boolean singleColumn = availableWidth > 0 && availableWidth < HomeStatsChartHelper.SINGLE_COLUMN_WIDTH;
        int halfWidth = availableWidth > 0 ? Math.max(320, (availableWidth - CARD_GAP) / 2) : 420;
        int row = 0;
        int col = 0;

        for (int i = 0; i < cards.size(); i++) {
            ChartCardSpec spec = cards.get(i);
            // Single chart left in the row should always take full width
            boolean singleInRow = (col == 0) && (i == cards.size() - 1 || isChartFullWidth(cards.get(i + 1).chart()));
            boolean fullWidth = singleColumn || singleInRow || !((DashboardChart) spec.chart()).canFitInColumn(halfWidth);

            if (fullWidth && col == 1) {
                row++;
                col = 0;
            }

            int gridWidth = fullWidth ? 2 : 1;
            addChartCard(chartsPanel, spec.component(), col, row, gridWidth, 1.0);

            if (fullWidth || col == 1) {
                row++;
                col = 0;
            } else {
                col = 1;
            }
        }
    }

    private boolean isChartFullWidth(JComponent chart) {
        // Check if the next chart is a bar chart that can't fit in half width
        if (chart instanceof DashboardBarChart barChart) {
            return !barChart.canFitInColumn(320);
        }
        return false;
    }

    private JPanel departmentCard() {
        departmentBack.setVisible(selectedDepartment != null);

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        departmentTitle.setFont(HomeStatsChartHelper.CARD_TITLE_FONT);
        departmentTitle.setForeground(HomeStatsChartHelper.TEXT_PRIMARY);
        titleRow.add(departmentTitle, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        String deptFilterValue = selectedDepartment != null ? selectedDepartment : "ALL";
        JLabel showInTable = createShowInTableLink("DEPARTMENT", deptFilterValue, "Department");
        actions.add(showInTable);
        actions.add(departmentBack);
        titleRow.add(actions, BorderLayout.EAST);
        return chartCard(titleRow, departmentChart);
    }

    private void styleDepartmentBackButton() {
        styleSecondaryButton(departmentBack);
        departmentBack.addActionListener(event -> {
            selectedDepartment = null;
            rebuildCharts();
        });
    }

    private void styleMissingControls() {
        styleSecondaryButton(missingBack);
        missingBack.addActionListener(event -> {
            selectedMissingGroup = null;
            rebuildCharts();
        });

        missingDetail.setBorderPainted(false);
        missingDetail.setContentAreaFilled(false);
        missingDetail.setFocusPainted(false);
        missingDetail.setForeground(HomeStatsChartHelper.BLUE);
        missingDetail.setFont(HomeStatsChartHelper.BUTTON_FONT);
        missingDetail.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        missingDetail.addActionListener(event -> openMissingDataView());
    }

    private void styleSecondaryButton(JButton button) {
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setFont(HomeStatsChartHelper.BUTTON_FONT);
        button.setForeground(HomeStatsChartHelper.TEXT_PRIMARY);
        button.setBackground(HomeStatsChartHelper.SURFACE);
        button.setBorder(HomeStatsChartHelper.buttonBorder());
    }

    private void configureDepartmentChart() {
        if (selectedDepartment == null) {
            departmentTitle.setText("Employees by Department");
            departmentChart.setItems(countItems(stats.employeesByDepartment(), HomeStatsChartHelper.BLUE));
            departmentChart.setClickHandler(item -> {
                if (stats.sectionsByDepartment().containsKey(item.label())) {
                    selectedDepartment = item.label();
                    rebuildCharts();
                }
            });
        } else {
            departmentTitle.setText("Sections in " + selectedDepartment);
            departmentChart.setItems(countItems(
                    stats.sectionsByDepartment().getOrDefault(selectedDepartment, List.of()),
                    HomeStatsChartHelper.TEAL
            ));
            departmentChart.setClickHandler(null);
        }
        departmentBack.setVisible(selectedDepartment != null);
    }

    private JPanel gradeCard() {
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);

        JLabel gradeTitle = new JLabel(selectedGrade == null ? "Employees by Grade" : "Departments in Grade " + selectedGrade);
        gradeTitle.setFont(HomeStatsChartHelper.CARD_TITLE_FONT);
        gradeTitle.setForeground(HomeStatsChartHelper.TEXT_PRIMARY);
        titleRow.add(gradeTitle, BorderLayout.WEST);

        if (selectedGrade != null) {
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            actions.setOpaque(false);
            // Back button for grade drill-down
            JButton gradeBack = new JButton("Back");
            gradeBack.addActionListener(event -> {
                selectedGrade = null;
                rebuildCharts();
            });
            styleSecondaryButton(gradeBack);
            actions.add(gradeBack);
            titleRow.add(actions, BorderLayout.EAST);
        } else {
            JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            footer.setOpaque(false);
            footer.add(createShowInTableLink("GRADE", "ALL", "Employees by Grade"));
            titleRow.add(footer, BorderLayout.EAST);
        }

        return chartCard(titleRow, gradeChart);
    }

    private void configureGradeChart() {
        if (selectedGrade == null) {
            gradeChart.setItems(gradeItems(stats.employeesByGrade()));
            gradeChart.setClickHandler(item -> {
                // Check if this grade has department data
                Map<String, List<EmployeeRecordDao.CountStat>> deptsByGrade = getDepartmentsByGrade();
                if (deptsByGrade.containsKey(item.label()) && !deptsByGrade.get(item.label()).isEmpty()) {
                    selectedGrade = item.label();
                    rebuildCharts();
                }
            });
        } else {
            List<ChartItem> deptItems = new ArrayList<>();
            Map<String, List<EmployeeRecordDao.CountStat>> deptsByGrade = getDepartmentsByGrade();
            List<EmployeeRecordDao.CountStat> depts = deptsByGrade.getOrDefault(selectedGrade, List.of());
            for (EmployeeRecordDao.CountStat dept : depts) {
                deptItems.add(new ChartItem(
                        dept.label(),
                        dept.count(),
                        htmlTooltip(dept.label(), dept.count(),
                                List.of(dept.count() + " employee(s) in Grade " + selectedGrade)),
                        HomeStatsChartHelper.TEAL
                ));
            }
            gradeChart.setItems(deptItems);
            gradeChart.setClickHandler(null);
        }
    }

    private Map<String, List<EmployeeRecordDao.CountStat>> getDepartmentsByGrade() {
        Map<String, List<EmployeeRecordDao.CountStat>> result = new LinkedHashMap<>();
        for (EmployeeRecordDao.ContributionStat grade : stats.employeesByGrade()) {
            result.put(grade.label(), grade.contributions());
        }
        return result;
    }

    private JPanel designationCard() {
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);

        JLabel designationTitle = new JLabel(selectedDesignation == null
                ? "Employees by Designation"
                : "Departments with " + selectedDesignation);
        designationTitle.setFont(HomeStatsChartHelper.CARD_TITLE_FONT);
        designationTitle.setForeground(HomeStatsChartHelper.TEXT_PRIMARY);
        titleRow.add(designationTitle, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        if (selectedDesignation == null) {
            actions.add(createShowInTableLink("DESIGNATION", "ALL", "Employees by Designation"));
        } else {
            actions.add(createShowInTableLink("DESIGNATION", selectedDesignation, selectedDesignation));
            JButton designationBack = new JButton("Back");
            designationBack.addActionListener(event -> {
                selectedDesignation = null;
                rebuildCharts();
            });
            styleSecondaryButton(designationBack);
            actions.add(designationBack);
        }
        titleRow.add(actions, BorderLayout.EAST);

        return chartCard(titleRow, designationChart);
    }

    private void configureDesignationChart() {
        if (selectedDesignation == null) {
            designationChart.setItems(countItems(stats.employeesByDesignation(), HomeStatsChartHelper.PURPLE));
            designationChart.setClickHandler(item -> {
                List<EmployeeRecordDao.CountStat> departments = stats.departmentsByDesignation()
                        .getOrDefault(item.label(), List.of());
                if (!departments.isEmpty()) {
                    selectedDesignation = item.label();
                    rebuildCharts();
                }
            });
            return;
        }

        List<ChartItem> departmentItems = new ArrayList<>();
        List<EmployeeRecordDao.CountStat> departments = stats.departmentsByDesignation()
                .getOrDefault(selectedDesignation, List.of());
        for (EmployeeRecordDao.CountStat department : departments) {
            departmentItems.add(new ChartItem(
                    department.label(),
                    department.count(),
                    htmlTooltip(
                            department.label(),
                            department.count(),
                            List.of(department.count() + " employee(s) with designation " + selectedDesignation)
                    ),
                    HomeStatsChartHelper.TEAL
            ));
        }
        designationChart.setItems(departmentItems);
        designationChart.setClickHandler(null);
    }

    private JLabel createShowInTableLink(String columnName, String value, String displayLabel) {
        JLabel link = new JLabel("Show in Table");
        link.setFont(HomeStatsChartHelper.LINK_FONT);
        link.setForeground(HomeStatsChartHelper.BLUE);
        link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        link.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent event) {
                if (showInTableHandler != null) {
                    showInTableHandler.accept(columnName + "::" + value + "::" + displayLabel);
                }
            }
        });
        return link;
    }

    private JPanel missingDataCard() {
        missingBack.setVisible(selectedMissingGroup != null);

        JPanel titleRow = new JPanel(new BorderLayout(8, 0));
        titleRow.setOpaque(false);
        missingTitle.setFont(HomeStatsChartHelper.CARD_TITLE_FONT);
        missingTitle.setForeground(HomeStatsChartHelper.TEXT_PRIMARY);
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
            List<ChartItem> items = new ArrayList<>();
            int docMissing = stats.totalMissingRequiredDocuments();
            int fieldMissing = stats.totalMissingRequiredFields();
            if (docMissing > 0) {
                items.add(new ChartItem(
                        "Documents",
                        docMissing,
                        htmlTooltip(
                                "Documents",
                                docMissing,
                                List.of(stats.employeesMissingRequiredDocuments() + " employees affected")
                        ),
                        HomeStatsChartHelper.RED
                ));
            }
            if (fieldMissing > 0) {
                items.add(new ChartItem(
                        "Heading Fields",
                        fieldMissing,
                        htmlTooltip(
                                "Heading Fields",
                                fieldMissing,
                                List.of(stats.employeesMissingRequiredFields() + " employees affected")
                        ),
                        HomeStatsChartHelper.PURPLE
                ));
            }
            missingDocsChart.setItems(items);
            missingDocsChart.setClickHandler(item -> {
                selectedMissingGroup = item.label();
                rebuildCharts();
            });
        } else if ("Documents".equals(selectedMissingGroup)) {
            missingTitle.setText("Missing Documents");
            missingDocsChart.setItems(missingRequirementItems(stats.missingRequiredDocuments(), HomeStatsChartHelper.RED, "employees missing"));
            missingDocsChart.setClickHandler(null);
        } else {
            missingTitle.setText("Missing Required Fields");
            missingDocsChart.setItems(missingRequirementItems(stats.missingRequiredFields(), HomeStatsChartHelper.PURPLE, "employees missing"));
            missingDocsChart.setClickHandler(null);
        }
        missingBack.setVisible(selectedMissingGroup != null);
    }

    private void openMissingDataView() {
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window != null) {
            window.dispose();
        }
        new MissingDataView();
    }

    private JPanel chartCard(String title, JComponent chart) {
        JLabel label = new JLabel(title);
        label.setFont(HomeStatsChartHelper.CARD_TITLE_FONT);
        label.setForeground(HomeStatsChartHelper.TEXT_PRIMARY);
        return chartCard(label, chart);
    }

    private JPanel chartCard(JComponent title, JComponent chart) {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(HomeStatsChartHelper.SURFACE);
        card.setBorder(HomeStatsChartHelper.cardBorder());
        card.add(title, BorderLayout.NORTH);
        card.add(chartContainer(chart), BorderLayout.CENTER);
        return card;
    }

    private JPanel chartCardWithFilter(String title, JComponent chart, String columnName) {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(HomeStatsChartHelper.SURFACE);
        card.setBorder(HomeStatsChartHelper.cardBorder());

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        JLabel label = new JLabel(title);
        label.setFont(HomeStatsChartHelper.CARD_TITLE_FONT);
        label.setForeground(HomeStatsChartHelper.TEXT_PRIMARY);
        topRow.add(label, BorderLayout.WEST);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        footer.setOpaque(false);
        footer.add(createShowInTableLink(columnName, "ALL", title));
        topRow.add(footer, BorderLayout.EAST);

        card.add(topRow, BorderLayout.NORTH);
        card.add(chartContainer(chart), BorderLayout.CENTER);
        return card;
    }

    private JPanel chartContainer(JComponent chart) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        DashboardChart dashboardChart = (DashboardChart) chart;
        int preferredHeight = dashboardChart.chartPreferredHeight();
        if (chart instanceof DashboardBarChart) {
            preferredHeight += HomeStatsChartHelper.CHART_HORIZONTAL_SCROLLBAR_HEIGHT;
            panel.add(horizontalChartScrollPane(chart), BorderLayout.CENTER);
        } else {
            chart.setPreferredSize(new Dimension(dashboardChart.chartPreferredWidth(), preferredHeight));
            panel.add(chart, BorderLayout.CENTER);
        }
        panel.setPreferredSize(new Dimension(HomeStatsChartHelper.CHART_MIN_WIDTH, preferredHeight));
        return panel;
    }

    private JScrollPane horizontalChartScrollPane(JComponent chart) {
        DashboardChart dashboardChart = (DashboardChart) chart;
        chart.setPreferredSize(new Dimension(dashboardChart.chartPreferredWidth(), dashboardChart.chartPreferredHeight()));

        JScrollPane scrollPane = new JScrollPane(
                chart,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(HomeStatsChartHelper.CHART_SCROLL_UNIT);
        scrollPane.getHorizontalScrollBar().setBlockIncrement(Math.max(
                HomeStatsChartHelper.CHART_SCROLL_UNIT,
                HomeStatsChartHelper.CHART_MIN_WIDTH - HomeStatsChartHelper.CHART_SCROLL_BLOCK_GAP
        ));
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
        gbc.insets = new Insets(0, 0, CARD_GAP, x == 0 && width == 1 ? CARD_GAP : 0);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = weightx;
        gbc.weighty = 0.0;
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

    private List<ChartItem> paletteItems(List<EmployeeRecordDao.CountStat> stats) {
        Color[] palette = new Color[]{HomeStatsChartHelper.BLUE, HomeStatsChartHelper.TEAL, HomeStatsChartHelper.PURPLE, HomeStatsChartHelper.ORANGE, HomeStatsChartHelper.RED, HomeStatsChartHelper.GREEN};
        List<ChartItem> items = new ArrayList<>();
        int index = 0;
        for (EmployeeRecordDao.CountStat stat : stats) {
            Color color = palette[index % palette.length];
            items.add(new ChartItem(
                    stat.label(),
                    stat.count(),
                    htmlTooltip(stat.label(), stat.count(), List.of()),
                    color
            ));
            index++;
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
                    HomeStatsChartHelper.ORANGE
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
            if (stat.missingCount() > 0) {
                items.add(new ChartItem(
                        stat.label(),
                        stat.missingCount(),
                        htmlTooltip(stat.label(), stat.missingCount(), List.of(suffix)),
                        color
                ));
            }
        }
        return items;
    }

    private String htmlTooltip(String label, int count, List<String> lines) {
        StringBuilder html = new StringBuilder("<html><div style='padding:8px 10px;'><b>")
                .append(escape(label))
                .append("</b><br><span style='color:#637381;'>Total</span>: <b>")
                .append(formatNumber(count))
                .append("</b>");
        for (String line : lines) {
            html.append("<br>").append(escape(line));
        }
        html.append("</div></html>");
        return html.toString();
    }

    private static String formatNumber(int value) {
        return String.format("%,d", value);
    }

    private String escape(String value) {
        return value == null
                ? ""
                : value.replace("&", "&" + "amp;").replace("<", "&" + "lt;").replace(">", "&" + "gt;");
    }

    private record ChartItem(String label, int count, String tooltip, Color color) {
    }

    private record ChartCardSpec(JComponent component, JComponent chart) {
    }

    /**
     * Interactive bar chart component with responsive sizing, inside-bar values,
     * horizontal stacked labels, zero baseline, balanced bar spacing, X-axis scrolling, and color-only hover effects.
     */
    private interface DashboardChart {
        int chartPreferredHeight();

        int chartPreferredWidth();

        boolean canFitInColumn(int availableColumnWidth);
    }

    private static class DashboardBarChart extends JPanel implements DashboardChart, Scrollable {
        private static final int CHART_TOP = HomeStatsChartHelper.CHART_TOP;
        private static final int CHART_LEFT = HomeStatsChartHelper.CHART_LEFT;
        private static final int CHART_RIGHT = HomeStatsChartHelper.CHART_RIGHT;
        private static final int BAR_MIN_WIDTH = HomeStatsChartHelper.BAR_MIN_WIDTH;
        private static final int BAR_MAX_WIDTH = HomeStatsChartHelper.BAR_MAX_WIDTH;
        private static final int SLOT_MIN_WIDTH = HomeStatsChartHelper.SLOT_MIN_WIDTH;
        private static final int SLOT_MAX_WIDTH = HomeStatsChartHelper.SLOT_MAX_WIDTH;
        private static final double BAR_WIDTH_RATIO = HomeStatsChartHelper.BAR_WIDTH_RATIO;
        private static final int LABEL_LINE_HEIGHT = HomeStatsChartHelper.LABEL_LINE_HEIGHT;
        private static final int MAX_LABEL_LINES = HomeStatsChartHelper.MAX_LABEL_LINES;

        private final List<ChartItem> items = new ArrayList<>();
        private final Map<Integer, Rectangle> barBounds = new LinkedHashMap<>();
        private int hoveredIndex = -1;
        private Consumer<ChartItem> clickHandler;

        DashboardBarChart() {
            setOpaque(false);
            setBackground(HomeStatsChartHelper.SURFACE);
            setToolTipText("");
            ToolTipManager.sharedInstance().registerComponent(this);
            ToolTipManager.sharedInstance().setInitialDelay(120);
            ToolTipManager.sharedInstance().setDismissDelay(7000);

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

            setPreferredSize(new Dimension(chartPreferredWidth(), chartPreferredHeight()));
            revalidate();
            repaint();
        }

        public int chartPreferredHeight() {
            return 260 + dynamicBottomSpace();
        }

        public int chartPreferredWidth() {
            if (items.isEmpty()) {
                return HomeStatsChartHelper.CHART_MIN_WIDTH;
            }
            int slot = Math.max(SLOT_MIN_WIDTH, dynamicSlotWidth());
            return Math.max(
                    HomeStatsChartHelper.CHART_MIN_WIDTH,
                    CHART_LEFT + CHART_RIGHT + 28 + slot * items.size()
            );
        }

        public boolean canFitInColumn(int availableColumnWidth) {
            return items.isEmpty()
                    || (items.size() <= HomeStatsChartHelper.FULL_WIDTH_ITEM_LIMIT
                    && chartPreferredWidth() <= availableColumnWidth);
        }

        void setClickHandler(Consumer<ChartItem> clickHandler) {
            this.clickHandler = clickHandler;
        }

        public JToolTip createToolTip() {
            JToolTip tip = super.createToolTip();
            tip.setFont(HomeStatsChartHelper.LABEL_FONT);
            tip.setForeground(HomeStatsChartHelper.TEXT_PRIMARY);
            tip.setBackground(Color.WHITE);
            tip.setBorder(HomeStatsChartHelper.tooltipBorder());
            return tip;
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
            int left = CHART_LEFT;
            int right = CHART_RIGHT;
            int top = CHART_TOP;
            int bottom = dynamicBottomSpace();
            int chartHeight = Math.max(96, height - top - bottom);
            int chartRight = width - right;
            int chartWidth = Math.max(1, chartRight - left);
            int max = maxCount();

            drawGridAndZeroBaseline(g2, left, top, chartRight, chartHeight, max);
            drawBars(g2, left, top, chartHeight, chartWidth, max);
            g2.dispose();
        }

        private void drawGridAndZeroBaseline(Graphics2D g2, int left, int top, int right, int chartHeight, int max) {
            g2.setFont(HomeStatsChartHelper.GRID_FONT);
            FontMetrics metrics = g2.getFontMetrics();

            for (int line = 0; line <= 4; line++) {
                int y = top + chartHeight - (chartHeight * line / 4);
                int value = (int) Math.round(max * (line / 4.0));

                g2.setColor(line == 0 ? new Color(203, 213, 225) : HomeStatsChartHelper.GRID_LINE);
                g2.drawLine(left, y, right, y);

                g2.setColor(line == 0 ? HomeStatsChartHelper.TEXT_PRIMARY : new Color(100, 116, 139));
                String label = compactNumber(value);
                g2.drawString(label, left - metrics.stringWidth(label) - 8, y + 4);
            }
        }

        private void drawBars(Graphics2D g2, int left, int top, int chartHeight, int chartWidth, int max) {
            int itemCount = Math.max(1, items.size());

            int idealSlot = Math.min(SLOT_MAX_WIDTH, Math.max(SLOT_MIN_WIDTH, dynamicSlotWidth()));
            int fittedSlot = Math.max(idealSlot, chartWidth / itemCount);
            int slot = Math.max(44, Math.min(idealSlot, fittedSlot));
            int usedWidth = slot * itemCount;
            int startX = left + Math.max(0, (chartWidth - usedWidth) / 2);
            int barWidth = Math.max(BAR_MIN_WIDTH, Math.min(BAR_MAX_WIDTH, (int) Math.round(slot * BAR_WIDTH_RATIO)));

            FontMetrics labelMetrics = g2.getFontMetrics(HomeStatsChartHelper.LABEL_FONT);

            for (int index = 0; index < items.size(); index++) {
                ChartItem item = items.get(index);
                int slotX = startX + index * slot;
                int centerX = slotX + slot / 2;
                int barX = centerX - barWidth / 2;

                int rawBarHeight = (int) Math.round((item.count() / (double) max) * chartHeight);
                int barHeight = item.count() == 0 ? 4 : Math.max(22, rawBarHeight);
                barHeight = Math.min(chartHeight, barHeight);

                int zeroY = top + chartHeight;
                int barY = zeroY - barHeight;
                Rectangle bounds = new Rectangle(slotX, top, slot, chartHeight + dynamicBottomSpace());
                barBounds.put(index, bounds);

                boolean hovered = index == hoveredIndex;
                Color barColor = hovered ? HomeStatsChartHelper.hoverColor(item.color()) : item.color();

                g2.setColor(new Color(barColor.getRed(), barColor.getGreen(), barColor.getBlue(), hovered ? 245 : 220));
                g2.fillRoundRect(barX, barY, barWidth, barHeight, 4, 4);

                drawValueInsideBar(g2, compactNumber(item.count()), barX, barY, barWidth, barHeight, hovered);

                g2.setFont(HomeStatsChartHelper.LABEL_FONT);
                g2.setColor(hovered ? HomeStatsChartHelper.TEXT_PRIMARY : HomeStatsChartHelper.TEXT_SECONDARY);
                List<String> labelLines = stackedLabelLines(item.label(), labelMetrics, Math.max(54, slot - 12), MAX_LABEL_LINES);
                drawCenteredLabelLines(g2, labelLines, centerX, zeroY + 22, LABEL_LINE_HEIGHT);
            }
        }

        private void drawValueInsideBar(
                Graphics2D g2,
                String value,
                int barX,
                int barY,
                int barWidth,
                int barHeight,
                boolean hovered
        ) {
            g2.setFont(HomeStatsChartHelper.VALUE_FONT);
            FontMetrics metrics = g2.getFontMetrics();

            String fittedValue = fitText(g2, value, Math.max(10, barWidth - 8));
            int textW = metrics.stringWidth(fittedValue);
            int textX = barX + (barWidth - textW) / 2;
            int textY = barY + Math.max(metrics.getAscent() + 4, (barHeight + metrics.getAscent()) / 2 - 2);

            Shape oldClip = g2.getClip();
            g2.setClip(new Rectangle(barX, barY, barWidth, barHeight));

            g2.setColor(new Color(255, 255, 255, hovered ? 255 : 245));
            g2.drawString(fittedValue, textX, textY);

            g2.setClip(oldClip);
        }

        private void drawEmptyState(Graphics2D g2) {
            g2.setColor(new Color(246, 248, 251));
            g2.fillRoundRect(18, 18, Math.max(0, getWidth() - 36), Math.max(0, getHeight() - 36), 18, 18);
            g2.setColor(HomeStatsChartHelper.TEXT_SECONDARY);
            g2.setFont(HomeStatsChartHelper.EMPTY_FONT);
            String text = "No data available";
            FontMetrics metrics = g2.getFontMetrics();
            g2.drawString(text, (getWidth() - metrics.stringWidth(text)) / 2, getHeight() / 2);
        }

        private void drawCenteredLabelLines(Graphics2D g2, List<String> lines, int centerX, int startY, int lineHeight) {
            FontMetrics metrics = g2.getFontMetrics();
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                g2.drawString(line, centerX - metrics.stringWidth(line) / 2, startY + i * lineHeight);
            }
        }

        private List<String> stackedLabelLines(String label, FontMetrics metrics, int maxWidth, int maxLines) {
            String clean = cleanLabel(label);
            String[] words = clean.split("\\s+");
            List<String> lines = new ArrayList<>();

            for (String word : words) {
                addWrappedWord(lines, word, metrics, maxWidth, maxLines);
                if (lines.size() >= maxLines) {
                    break;
                }
            }

            if (lines.isEmpty()) {
                lines.add("Unassigned");
            }

            boolean hasMoreWords = words.length > lines.size();
            if (hasMoreWords || cleanWasTrimmed(clean, lines)) {
                int lastIndex = lines.size() - 1;
                lines.set(lastIndex, withEllipsis(lines.get(lastIndex), metrics, maxWidth));
            }

            return lines;
        }

        private void addWrappedWord(List<String> lines, String word, FontMetrics metrics, int maxWidth, int maxLines) {
            if (word == null || word.isBlank() || lines.size() >= maxLines) {
                return;
            }

            if (metrics.stringWidth(word) <= maxWidth) {
                lines.add(word);
                return;
            }

            String suffix = "...";
            int availableWidth = Math.max(8, maxWidth - metrics.stringWidth(suffix));
            StringBuilder part = new StringBuilder();

            for (char c : word.toCharArray()) {
                if (metrics.stringWidth(part.toString() + c) > availableWidth) {
                    break;
                }
                part.append(c);
            }

            lines.add(part + suffix);
        }

        private boolean cleanWasTrimmed(String clean, List<String> lines) {
            String visible = String.join(" ", lines).replace("...", "").trim();
            return visible.length() < clean.length() && lines.size() >= MAX_LABEL_LINES;
        }

        private String withEllipsis(String value, FontMetrics metrics, int maxWidth) {
            String suffix = "...";
            if (value.endsWith(suffix)) {
                return value;
            }

            if (metrics.stringWidth(value + suffix) <= maxWidth) {
                return value + suffix;
            }

            int availableWidth = Math.max(8, maxWidth - metrics.stringWidth(suffix));
            StringBuilder builder = new StringBuilder();
            for (char c : value.toCharArray()) {
                if (metrics.stringWidth(builder.toString() + c) > availableWidth) {
                    break;
                }
                builder.append(c);
            }
            return builder + suffix;
        }

        private String fitText(Graphics2D g2, String text, int maxWidth) {
            FontMetrics metrics = g2.getFontMetrics();
            if (metrics.stringWidth(text) <= maxWidth) {
                return text;
            }

            String suffix = "...";
            int available = Math.max(8, maxWidth - metrics.stringWidth(suffix));
            StringBuilder builder = new StringBuilder();
            for (char c : text.toCharArray()) {
                if (metrics.stringWidth(builder.toString() + c) > available) {
                    break;
                }
                builder.append(c);
            }
            return builder + suffix;
        }

        private int maxCount() {
            int max = 1;
            for (ChartItem item : items) {
                max = Math.max(max, item.count());
            }
            return niceAxisMax(max);
        }

        private int niceAxisMax(int value) {
            if (value <= 10) {
                return 10;
            }
            int magnitude = (int) Math.pow(10, String.valueOf(value).length() - 1);
            int rounded = (int) Math.ceil(value / (double) magnitude * 2.0) * magnitude / 2;
            return Math.max(value, rounded);
        }

        private int dynamicSlotWidth() {
            int longestWord = 0;
            for (ChartItem item : items) {
                for (String word : cleanLabel(item.label()).split("\\s+")) {
                    longestWord = Math.max(longestWord, word.length());
                }
            }
            return Math.max(SLOT_MIN_WIDTH, Math.min(SLOT_MAX_WIDTH, 64 + longestWord * 5));
        }

        private int dynamicBottomSpace() {
            int maxLines = 1;
            for (ChartItem item : items) {
                maxLines = Math.max(maxLines, Math.min(MAX_LABEL_LINES, cleanLabel(item.label()).split("\\s+").length));
            }
            return 42 + maxLines * LABEL_LINE_HEIGHT;
        }

        private static String compactNumber(int value) {
            if (value >= 1_000_000) {
                return String.format("%.1fM", value / 1_000_000.0).replace(".0M", "M");
            }
            if (value >= 1_000) {
                return String.format("%.1fk", value / 1_000.0).replace(".0k", "k");
            }
            return String.valueOf(value);
        }

        private static String cleanLabel(String label) {
            return label == null || label.isBlank() ? "Unassigned" : label.trim();
        }

        private int itemAt(Point point) {
            for (Map.Entry<Integer, Rectangle> entry : barBounds.entrySet()) {
                if (entry.getValue().contains(point)) {
                    return entry.getKey();
                }
            }
            return -1;
        }

        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return HomeStatsChartHelper.CHART_SCROLL_UNIT;
        }

        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(
                    HomeStatsChartHelper.CHART_SCROLL_UNIT,
                    visibleRect.width - HomeStatsChartHelper.CHART_SCROLL_BLOCK_GAP
            );
        }

        public boolean getScrollableTracksViewportWidth() {
            Container parent = getParent();
            return parent instanceof JViewport viewport && chartPreferredWidth() <= viewport.getWidth();
        }

        public boolean getScrollableTracksViewportHeight() {
            return true;
        }
    }


    /**
     * Pie chart for exit reasons. It avoids crowded X-axis labels and uses a readable legend.
     * Hover changes only the slice color and legend text weight; no extra shadow/detail box is drawn.
     */
    private static class DashboardPieChart extends JPanel implements DashboardChart {
        private static final int PIE_MIN_SIZE = HomeStatsChartHelper.PIE_MIN_SIZE;
        private static final int PIE_MAX_SIZE = HomeStatsChartHelper.PIE_MAX_SIZE;
        private static final int LEGEND_ROW_HEIGHT = HomeStatsChartHelper.LEGEND_ROW_HEIGHT;

        private final List<ChartItem> items = new ArrayList<>();
        private final Map<Integer, Shape> sliceBounds = new LinkedHashMap<>();
        private final Map<Integer, Rectangle> legendBounds = new LinkedHashMap<>();
        private int hoveredIndex = -1;
        private Consumer<ChartItem> clickHandler;

        DashboardPieChart() {
            setOpaque(false);
            setBackground(HomeStatsChartHelper.SURFACE);
            setToolTipText("");
            ToolTipManager.sharedInstance().registerComponent(this);
            ToolTipManager.sharedInstance().setInitialDelay(120);
            ToolTipManager.sharedInstance().setDismissDelay(7000);

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
            setPreferredSize(new Dimension(chartPreferredWidth(), chartPreferredHeight()));
            revalidate();
            repaint();
        }

        void setClickHandler(Consumer<ChartItem> clickHandler) {
            this.clickHandler = clickHandler;
        }

        public int chartPreferredHeight() {
            int legendRows = Math.max(2, Math.min(8, items.size()));
            return Math.max(260, 126 + legendRows * LEGEND_ROW_HEIGHT);
        }

        public int chartPreferredWidth() {
            return HomeStatsChartHelper.CHART_MIN_WIDTH;
        }

        public boolean canFitInColumn(int availableColumnWidth) {
            return items.size() <= 4 && availableColumnWidth >= 460;
        }

        public JToolTip createToolTip() {
            JToolTip tip = super.createToolTip();
            tip.setFont(HomeStatsChartHelper.LABEL_FONT);
            tip.setForeground(HomeStatsChartHelper.TEXT_PRIMARY);
            tip.setBackground(Color.WHITE);
            tip.setBorder(HomeStatsChartHelper.tooltipBorder());
            return tip;
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
            sliceBounds.clear();
            legendBounds.clear();

            if (items.isEmpty() || totalCount() <= 0) {
                drawEmptyState(g2);
                g2.dispose();
                return;
            }

            int width = getWidth();
            int height = getHeight();
            boolean compact = width < 560;
            int pieSize = Math.max(PIE_MIN_SIZE, Math.min(PIE_MAX_SIZE, compact ? width - 56 : height - 64));
            int pieX = compact ? (width - pieSize) / 2 : 36;
            int pieY = 28;

            drawPie(g2, pieX, pieY, pieSize);

            if (compact) {
                drawLegend(g2, 24, pieY + pieSize + 24, Math.max(120, width - 48));
            } else {
                drawLegend(g2, pieX + pieSize + 34, pieY + 8, Math.max(160, width - pieX - pieSize - 58));
            }

            g2.dispose();
        }

        private void drawPie(Graphics2D g2, int x, int y, int size) {
            int total = totalCount();
            double start = 90.0;

            for (int index = 0; index < items.size(); index++) {
                ChartItem item = items.get(index);
                double extent = -360.0 * item.count() / total;
                Arc2D.Double slice = new Arc2D.Double(x, y, size, size, start, extent, Arc2D.PIE);
                sliceBounds.put(index, slice);

                Color color = index == hoveredIndex ? HomeStatsChartHelper.hoverColor(item.color()) : item.color();
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), index == hoveredIndex ? 245 : 220));
                g2.fill(slice);
                g2.setColor(Color.WHITE);
                g2.draw(slice);
                start += extent;
            }

            int hole = Math.max(54, size / 3);
            int holeX = x + (size - hole) / 2;
            int holeY = y + (size - hole) / 2;
            g2.setColor(Color.WHITE);
            g2.fillOval(holeX, holeY, hole, hole);

            String totalText = compactNumber(total);
            g2.setFont(HomeStatsChartHelper.PIE_TOTAL_FONT);
            FontMetrics totalMetrics = g2.getFontMetrics();
            g2.setColor(HomeStatsChartHelper.TEXT_PRIMARY);
            g2.drawString(totalText, x + (size - totalMetrics.stringWidth(totalText)) / 2, y + size / 2 + 2);

            g2.setFont(HomeStatsChartHelper.PIE_LABEL_FONT);
            FontMetrics labelMetrics = g2.getFontMetrics();
            String label = "Total";
            g2.setColor(HomeStatsChartHelper.TEXT_SECONDARY);
            g2.drawString(label, x + (size - labelMetrics.stringWidth(label)) / 2, y + size / 2 + 18);
        }

        private void drawLegend(Graphics2D g2, int x, int y, int width) {
            int total = totalCount();
            Font normalFont = HomeStatsChartHelper.LABEL_FONT;
            Font hoverFont = HomeStatsChartHelper.LABEL_FONT_HOVER;

            for (int index = 0; index < items.size(); index++) {
                ChartItem item = items.get(index);
                int rowY = y + index * LEGEND_ROW_HEIGHT;
                Rectangle row = new Rectangle(x, rowY - 4, width, LEGEND_ROW_HEIGHT);
                legendBounds.put(index, row);

                boolean hovered = index == hoveredIndex;
                Color color = hovered ? HomeStatsChartHelper.hoverColor(item.color()) : item.color();
                g2.setColor(color);
                g2.fillRoundRect(x, rowY, 12, 12, 4, 4);

                double percent = total == 0 ? 0 : item.count() * 100.0 / total;
                String value = compactNumber(item.count()) + " \u2022 " + String.format("%.0f%%", percent);

                g2.setFont(hovered ? hoverFont : normalFont);
                FontMetrics metrics = g2.getFontMetrics();
                int valueW = metrics.stringWidth(value);
                int labelMax = Math.max(40, width - 30 - valueW - 12);
                String label = fitText(g2, cleanLabel(item.label()), labelMax);

                g2.setColor(hovered ? HomeStatsChartHelper.TEXT_PRIMARY : HomeStatsChartHelper.TEXT_SECONDARY);
                g2.drawString(label, x + 20, rowY + 11);
                g2.setColor(HomeStatsChartHelper.TEXT_PRIMARY);
                g2.drawString(value, x + width - valueW, rowY + 11);
            }
        }

        private void drawEmptyState(Graphics2D g2) {
            g2.setColor(new Color(246, 248, 251));
            g2.fillRoundRect(18, 18, Math.max(0, getWidth() - 36), Math.max(0, getHeight() - 36), 8, 8);
            g2.setColor(HomeStatsChartHelper.TEXT_SECONDARY);
            g2.setFont(HomeStatsChartHelper.EMPTY_FONT);
            String text = "No data available";
            FontMetrics metrics = g2.getFontMetrics();
            g2.drawString(text, (getWidth() - metrics.stringWidth(text)) / 2, getHeight() / 2);
        }

        private int totalCount() {
            int total = 0;
            for (ChartItem item : items) {
                total += Math.max(0, item.count());
            }
            return total;
        }

        private int itemAt(Point point) {
            for (Map.Entry<Integer, Rectangle> entry : legendBounds.entrySet()) {
                if (entry.getValue().contains(point)) {
                    return entry.getKey();
                }
            }
            for (Map.Entry<Integer, Shape> entry : sliceBounds.entrySet()) {
                if (entry.getValue().contains(point)) {
                    return entry.getKey();
                }
            }
            return -1;
        }

        private String fitText(Graphics2D g2, String text, int maxWidth) {
            FontMetrics metrics = g2.getFontMetrics();
            if (metrics.stringWidth(text) <= maxWidth) {
                return text;
            }

            String suffix = "...";
            int available = Math.max(8, maxWidth - metrics.stringWidth(suffix));
            StringBuilder builder = new StringBuilder();
            for (char c : text.toCharArray()) {
                if (metrics.stringWidth(builder.toString() + c) > available) {
                    break;
                }
                builder.append(c);
            }
            return builder + suffix;
        }

        private static String compactNumber(int value) {
            if (value >= 1_000_000) {
                return String.format("%.1fM", value / 1_000_000.0).replace(".0M", "M");
            }
            if (value >= 1_000) {
                return String.format("%.1fk", value / 1_000.0).replace(".0k", "k");
            }
            return String.valueOf(value);
        }

        private static String cleanLabel(String label) {
            return label == null || label.isBlank() ? "Unassigned" : label.trim();
        }
    }
}

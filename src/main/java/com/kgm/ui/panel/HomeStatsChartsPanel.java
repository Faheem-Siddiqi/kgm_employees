package com.kgm.ui.panel;

import com.kgm.dao.EmployeeRecordDao;
import com.kgm.ui.MissingDataView;
import com.kgm.ui.styling.ButtonStateHelper;
import com.kgm.ui.styling.HomeStatsChartHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
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
 * - Charts render two per row by default, with horizontal chart scrolling when dense labels overflow.
 * - Each chart can be expanded to a full row from an icon CTA in the card header.
 * - Modern hover state, value badges, soft card styling and cleaner label placement.
 * - Zero-count items hidden from missing data bars.
 * - Click grade bar to drill into departments within that grade.
 * - Click designation bar to drill into departments with that designation.
 */
public class HomeStatsChartsPanel extends JPanel {
    private static final Icon FULL_ROW_ICON = loadFullRowIcon();

    private EmployeeRecordDao repo;
    private final JPanel chartsPanel = new JPanel(new GridBagLayout());
    private final JLabel departmentTitle = new JLabel("Employees by Department");
    private final JButton departmentBack = new JButton("Back");
    private final JLabel missingTitle = new JLabel("Missing Required Data");
    private final JButton missingBack = new JButton("Back");
    private final JButton missingDetail = new JButton("");
    // View affected employees

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
    private String fullRowChartKey;
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
        fullRowChartKey = null;
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
        exitTrendChart.setPreferredChartHeight(averageChartContainerHeight(
                departmentChart,
                gradeChart,
                designationChart,
                missingDocsChart
        ));

        List<ChartCardSpec> cards = List.of(
                new ChartCardSpec("department", departmentCard(), departmentChart),
                new ChartCardSpec("grade", gradeCard(), gradeChart),
                new ChartCardSpec("designation", designationCard(), designationChart),
                new ChartCardSpec("missing", missingDataCard(), missingDocsChart),
                new ChartCardSpec("exitReasons", chartCardWithFilter(
                        "Exit Reasons Overview",
                        exitTrendChart,
                        "RESIGN_REASON",
                        "exitReasons"
                ), exitTrendChart)
        );

        addResponsiveChartCards(cards);

        chartsPanel.revalidate();
        chartsPanel.repaint();
    }

    private void addResponsiveChartCards(List<ChartCardSpec> cards) {
        int expandedIndex = expandedChartIndex(cards);
        if (expandedIndex < 0) {
            addGridChartCards(cards, 0);
            return;
        }

        int expandedRow = expandedIndex / 2;
        int row = addGridChartCards(cards.subList(0, expandedRow * 2), 0);

        ChartCardSpec expanded = cards.get(expandedIndex);
        addChartCard(chartsPanel, expanded.component(), 0, row, 2, 1.0);
        row++;

        List<ChartCardSpec> shiftedCards = new ArrayList<>();
        for (int index = expandedRow * 2; index < cards.size(); index++) {
            if (index != expandedIndex) {
                shiftedCards.add(cards.get(index));
            }
        }
        addGridChartCards(shiftedCards, row);
    }

    private int expandedChartIndex(List<ChartCardSpec> cards) {
        if (fullRowChartKey == null) {
            return -1;
        }

        for (int index = 0; index < cards.size(); index++) {
            if (fullRowChartKey.equals(cards.get(index).key())) {
                return index;
            }
        }
        return -1;
    }

    private int addGridChartCards(List<ChartCardSpec> cards, int startRow) {
        int row = startRow;
        int col = 0;

        for (ChartCardSpec spec : cards) {
            addChartCard(chartsPanel, spec.component(), col, row, 1, 1.0);

            if (col == 1) {
                row++;
                col = 0;
            } else {
                col = 1;
            }
        }
        if (col == 1) {
            addEmptyGridSlot(chartsPanel, 1, row);
        }
        return col == 0 ? row : row + 1;
    }

    private int averageChartContainerHeight(DashboardChart... charts) {
        if (charts == null || charts.length == 0) {
            return HomeStatsChartHelper.AVERAGE_CHART_HEIGHT;
        }

        int total = 0;
        int count = 0;
        for (DashboardChart chart : charts) {
            if (chart == null) {
                continue;
            }
            int height = chart.chartPreferredHeight();
            if (chart instanceof DashboardBarChart) {
                height += HomeStatsChartHelper.CHART_HORIZONTAL_SCROLLBAR_HEIGHT;
            }
            total += height;
            count++;
        }
        return count == 0 ? HomeStatsChartHelper.AVERAGE_CHART_HEIGHT : total / count;
    }

    private JPanel departmentCard() {
        departmentBack.setVisible(selectedDepartment != null);

        JPanel actions = chartActions();
        if (selectedDepartment != null) {
            actions.add(createShowInTableLink("DEPARTMENT", selectedDepartment, selectedDepartment));
        }
        actions.add(departmentBack);
        actions.add(createFullRowToggle("department"));
        styleChartTitle(departmentTitle);
        return chartCard(chartHeader(departmentTitle, departmentHelperText(), actions), departmentChart);
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
        missingDetail.addActionListener(event -> openMissingDataView(selectedMissingGroup));
    }

    private void styleSecondaryButton(JButton button) {
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setFont(HomeStatsChartHelper.BUTTON_FONT);
        button.setForeground(HomeStatsChartHelper.TEXT_PRIMARY);
        button.setBackground(HomeStatsChartHelper.SURFACE);
        button.setBorder(HomeStatsChartHelper.buttonBorder());
        ButtonStateHelper.installRounded(button, 8);
        ButtonStateHelper.setHoverBackground(button, HomeStatsChartHelper.MUTED_SURFACE, HomeStatsChartHelper.SOFT_HOVER);
    }

    private void configureDepartmentChart() {
        if (selectedDepartment == null) {
            departmentTitle.setText("Employees by Department");
            departmentChart.setItems(countItems(stats.employeesByDepartment(), HomeStatsChartHelper.BLUE));
            departmentChart.setClickHandler(item -> {
                if (stats.sectionsByDepartment().containsKey(item.label())
                        && !stats.sectionsByDepartment().get(item.label()).isEmpty()) {
                    selectedDepartment = item.label();
                    rebuildCharts();
                } else {
                    showInTable("DEPARTMENT", item.label(), item.label());
                }
            });
        } else {
            departmentTitle.setText("Sections in " + selectedDepartment);
            departmentChart.setItems(countItems(
                    stats.sectionsByDepartment().getOrDefault(selectedDepartment, List.of()),
                    HomeStatsChartHelper.TEAL
            ));
            departmentChart.setClickHandler(item -> showInTable(
                    selectedDepartment + " - " + item.label(),
                    "DEPARTMENT", selectedDepartment,
                    "SECTION", item.label()
            ));
        }
        departmentBack.setVisible(selectedDepartment != null);
    }

    private JPanel gradeCard() {
        JLabel gradeTitle = new JLabel(selectedGrade == null ? "Employees by Grade" : "Departments in Grade " + selectedGrade);
        styleChartTitle(gradeTitle);

        JPanel actions = chartActions();
        if (selectedGrade != null) {
            actions.add(createShowInTableLink("GRADE", selectedGrade, "Grade " + selectedGrade));
            // Back button for grade drill-down
            JButton gradeBack = new JButton("Back");
            gradeBack.addActionListener(event -> {
                selectedGrade = null;
                rebuildCharts();
            });
            styleSecondaryButton(gradeBack);
            actions.add(gradeBack);
        }
        actions.add(createFullRowToggle("grade"));
        return chartCard(chartHeader(gradeTitle, gradeHelperText(), actions), gradeChart);
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
                } else {
                    showInTable("GRADE", item.label(), "Grade " + item.label());
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
            gradeChart.setClickHandler(item -> showInTable(
                    "Grade " + selectedGrade + " / " + item.label(),
                    "GRADE", selectedGrade,
                    "DEPARTMENT", item.label()
            ));
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
        JLabel designationTitle = new JLabel(selectedDesignation == null
                ? "Employees by Designation"
                : "Departments with " + selectedDesignation);
        styleChartTitle(designationTitle);

        JPanel actions = chartActions();
        if (selectedDesignation != null) {
            actions.add(createShowInTableLink("DESIGNATION", selectedDesignation, selectedDesignation));
            JButton designationBack = new JButton("Back");
            designationBack.addActionListener(event -> {
                selectedDesignation = null;
                rebuildCharts();
            });
            styleSecondaryButton(designationBack);
            actions.add(designationBack);
        }
        actions.add(createFullRowToggle("designation"));
        return chartCard(chartHeader(designationTitle, designationHelperText(), actions), designationChart);
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
                } else {
                    showInTable("DESIGNATION", item.label(), item.label());
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
        designationChart.setClickHandler(item -> showInTable(
                selectedDesignation + " / " + item.label(),
                "DESIGNATION", selectedDesignation,
                "DEPARTMENT", item.label()
        ));
    }

    private JLabel createShowInTableLink(String columnName, String value, String displayLabel) {
        JLabel link = new JLabel("");
        // View affected employees button table
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

    private JPanel chartHeader(JLabel title, String helperText, JComponent actions) {
        styleChartTitle(title);

        JLabel helper = new JLabel(helperText == null ? "" : helperText);
        helper.setFont(HomeStatsChartHelper.CARD_HELPER_FONT);
        helper.setForeground(HomeStatsChartHelper.TEXT_SECONDARY);
        helper.setToolTipText(helper.getText());
        helper.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        copy.add(title);
        copy.add(Box.createVerticalStrut(4));
        copy.add(helper);

        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);
        header.add(copy, BorderLayout.CENTER);
        if (actions != null && actions.getComponentCount() > 0) {
            header.add(actions, BorderLayout.EAST);
        }
        return header;
    }

    private JPanel chartActions() {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        actions.setOpaque(false);
        actions.setAlignmentX(Component.RIGHT_ALIGNMENT);
        return actions;
    }

    private void styleChartTitle(JLabel label) {
        label.setFont(HomeStatsChartHelper.CARD_TITLE_FONT);
        label.setForeground(HomeStatsChartHelper.TEXT_PRIMARY);
        label.setToolTipText(label.getText());
    }

    private String departmentHelperText() {
        if (stats == null) {
            return "";
        }
        if (selectedDepartment == null) {
            int total = countTotal(stats.employeesByDepartment());
            return formatNumber(total) + " of " + formatNumber(stats.totalEmployees())
                    + " employees by department | Click a bar to drill into sections";
        }

        List<EmployeeRecordDao.CountStat> sections = stats.sectionsByDepartment()
                .getOrDefault(selectedDepartment, List.of());
        return formatNumber(countTotal(sections)) + " employees across "
                + formatNumber(sections.size()) + " sections | Click a bar to show employees";
    }

    private String gradeHelperText() {
        if (stats == null) {
            return "";
        }
        if (selectedGrade == null) {
            return formatNumber(contributionTotal(stats.employeesByGrade())) + " employees across "
                    + formatNumber(stats.employeesByGrade().size()) + " grades | Click a bar to view departments";
        }

        List<EmployeeRecordDao.CountStat> departments = getDepartmentsByGrade()
                .getOrDefault(selectedGrade, List.of());
        return formatNumber(countTotal(departments)) + " employees across "
                + formatNumber(departments.size()) + " departments | Click a bar to show employees";
    }

    private String designationHelperText() {
        if (stats == null) {
            return "";
        }
        if (selectedDesignation == null) {
            return formatNumber(countTotal(stats.employeesByDesignation())) + " employees across "
                    + formatNumber(stats.employeesByDesignation().size())
                    + " designations | Click a bar to view departments";
        }

        List<EmployeeRecordDao.CountStat> departments = stats.departmentsByDesignation()
                .getOrDefault(selectedDesignation, List.of());
        return formatNumber(countTotal(departments)) + " employees across "
                + formatNumber(departments.size()) + " departments | Click a bar to show employees";
    }

    private String missingHelperText() {
        if (stats == null) {
            return "";
        }
        if (selectedMissingGroup == null) {
            int missingItems = stats.totalMissingRequiredDocuments() + stats.totalMissingRequiredFields();
            return formatNumber(stats.employeesMissingAnyRequiredData()) + " of "
                    + formatNumber(stats.totalEmployees()) + " employees affected | "
                    + formatNumber(missingItems) + " missing required items | Click a bar for details";
        }
        if ("Documents".equals(selectedMissingGroup)) {
            return formatNumber(stats.employeesMissingRequiredDocuments()) + " employees affected | "
                    + formatNumber(stats.missingRequiredDocuments().size())
                    + " required documents tracked | Click a bar to view employees";
        }
        return formatNumber(stats.employeesMissingRequiredFields()) + " employees affected | "
                + formatNumber(stats.missingRequiredFields().size())
                + " required heading fields tracked | Click a bar to view employees";
    }

    private String exitReasonsHelperText() {
        if (stats == null) {
            return "";
        }
        return formatNumber(countTotal(stats.exitTrends())) + " of " + formatNumber(stats.totalEmployees())
                + " employees grouped by exit reason | Click a slice to filter table";
    }

    private int countTotal(List<EmployeeRecordDao.CountStat> items) {
        int total = 0;
        if (items != null) {
            for (EmployeeRecordDao.CountStat item : items) {
                total += Math.max(0, item.count());
            }
        }
        return total;
    }

    private int contributionTotal(List<EmployeeRecordDao.ContributionStat> items) {
        int total = 0;
        if (items != null) {
            for (EmployeeRecordDao.ContributionStat item : items) {
                total += Math.max(0, item.count());
            }
        }
        return total;
    }

    private JButton createFullRowToggle(String chartKey) {
        boolean expanded = chartKey.equals(fullRowChartKey);
        JButton button = new JButton(FULL_ROW_ICON != null ? FULL_ROW_ICON : new FullRowToggleIcon());
        String accessibleName = expanded ? "Back to grid" : "Use full row";
        button.setText(null);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setBorder(new EmptyBorder(4, 4, 4, 4));
        button.setPreferredSize(new Dimension(HomeStatsChartHelper.FULL_ROW_BUTTON_SIZE, HomeStatsChartHelper.FULL_ROW_BUTTON_SIZE));
        button.setMinimumSize(new Dimension(HomeStatsChartHelper.FULL_ROW_BUTTON_SIZE, HomeStatsChartHelper.FULL_ROW_BUTTON_SIZE));
        button.setMaximumSize(new Dimension(HomeStatsChartHelper.FULL_ROW_BUTTON_SIZE, HomeStatsChartHelper.FULL_ROW_BUTTON_SIZE));
        button.setForeground(HomeStatsChartHelper.BLUE);
        button.setBackground(expanded ? HomeStatsChartHelper.FULL_ROW_ACTIVE : HomeStatsChartHelper.SURFACE);
        button.setSelected(expanded);
        button.setIconTextGap(0);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.getAccessibleContext().setAccessibleName(accessibleName);
        button.setToolTipText(expanded
                ? "Place this chart back in the two-column grid."
                : "Give this chart the full row width.");
        ButtonStateHelper.installRounded(button, 8);
        ButtonStateHelper.setHoverBackground(
                button,
                HomeStatsChartHelper.FULL_ROW_ACTIVE,
                HomeStatsChartHelper.FULL_ROW_PRESSED
        );
        button.addActionListener(event -> {
            fullRowChartKey = expanded ? null : chartKey;
            rebuildCharts();
        });
        return button;
    }

    private static Icon loadFullRowIcon() {
        ImageIcon icon = new ImageIcon(HomeStatsChartHelper.FULL_ROW_ICON_PATH);
        if (icon.getIconWidth() <= 0) {
            java.net.URL resource = HomeStatsChartsPanel.class.getResource("/images/Full Screen.png");
            if (resource != null) {
                icon = new ImageIcon(resource);
            }
        }
        if (icon.getIconWidth() <= 0) {
            return null;
        }

        Image scaled = icon.getImage().getScaledInstance(
                HomeStatsChartHelper.FULL_ROW_ICON_SIZE,
                HomeStatsChartHelper.FULL_ROW_ICON_SIZE,
                Image.SCALE_SMOOTH
        );
        return new ImageIcon(scaled);
    }

    private static class FullRowToggleIcon implements Icon {
        @Override
        public int getIconWidth() {
            return HomeStatsChartHelper.FULL_ROW_ICON_SIZE;
        }

        @Override
        public int getIconHeight() {
            return HomeStatsChartHelper.FULL_ROW_ICON_SIZE;
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(component.getForeground());
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            int left = x + 2;
            int top = y + 2;
            int right = x + HomeStatsChartHelper.FULL_ROW_ICON_SIZE - 3;
            int bottom = y + HomeStatsChartHelper.FULL_ROW_ICON_SIZE - 3;
            int arm = 5;

            g2.drawLine(left, top, left + arm, top);
            g2.drawLine(left, top, left, top + arm);
            g2.drawLine(right - arm, top, right, top);
            g2.drawLine(right, top, right, top + arm);
            g2.drawLine(left, bottom - arm, left, bottom);
            g2.drawLine(left, bottom, left + arm, bottom);
            g2.drawLine(right - arm, bottom, right, bottom);
            g2.drawLine(right, bottom - arm, right, bottom);
            g2.dispose();
        }
    }

    private void showInTable(String columnName, String value, String displayLabel) {
        if (showInTableHandler != null) {
            showInTableHandler.accept(columnName + "::" + value + "::" + displayLabel);
        }
    }

    private void showInTable(String displayLabel, String... columnValuePairs) {
        if (showInTableHandler == null || columnValuePairs == null || columnValuePairs.length < 2) {
            return;
        }
        StringBuilder command = new StringBuilder("MULTI::").append(displayLabel);
        for (int index = 0; index + 1 < columnValuePairs.length; index += 2) {
            command.append("::").append(columnValuePairs[index]).append("::").append(columnValuePairs[index + 1]);
        }
        showInTableHandler.accept(command.toString());
    }

    private JPanel missingDataCard() {
        missingBack.setVisible(selectedMissingGroup != null);

        JPanel actions = chartActions();
        actions.add(missingDetail);
        actions.add(missingBack);
        actions.add(createFullRowToggle("missing"));
        styleChartTitle(missingTitle);
        return chartCard(chartHeader(missingTitle, missingHelperText(), actions), missingDocsChart);
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
            missingDocsChart.setClickHandler(item -> openMissingDataView(selectedMissingGroup, item.label()));
        } else {
            missingTitle.setText("Missing Required Fields");
            missingDocsChart.setItems(missingRequirementItems(stats.missingRequiredFields(), HomeStatsChartHelper.PURPLE, "employees missing"));
            missingDocsChart.setClickHandler(item -> openMissingDataView(selectedMissingGroup, item.label()));
        }
        missingBack.setVisible(selectedMissingGroup != null);
    }

    private void openMissingDataView(String missingGroup) {
        openMissingDataView(missingGroup, null);
    }

    private void openMissingDataView(String missingGroup, String initialMissingItem) {
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window != null) {
            window.dispose();
        }
        new MissingDataView("Documents".equals(missingGroup), initialMissingItem);
    }

    private JPanel chartCard(String title, JComponent chart) {
        JLabel label = new JLabel(title);
        return chartCard(chartHeader(label, "", null), chart);
    }

    private JPanel chartCard(JComponent title, JComponent chart) {
        JPanel card = new JPanel(new BorderLayout(0, 16));
        card.setBackground(HomeStatsChartHelper.SURFACE);
        card.setBorder(HomeStatsChartHelper.cardBorder());
        card.add(title, BorderLayout.NORTH);
        card.add(chartContainer(chart), BorderLayout.CENTER);
        return card;
    }

    private JPanel chartCardWithFilter(String title, JComponent chart, String columnName, String chartKey) {
        JPanel card = new JPanel(new BorderLayout(0, 16));
        card.setBackground(HomeStatsChartHelper.SURFACE);
        card.setBorder(HomeStatsChartHelper.cardBorder());

        JLabel label = new JLabel(title);

        JPanel actions = chartActions();
        actions.add(createFullRowToggle(chartKey));

        if ("RESIGN_REASON".equalsIgnoreCase(columnName) && chart instanceof DashboardPieChart pieChart) {
            pieChart.setClickHandler(item -> showInTable(columnName, item.label(), item.label()));
        }
        card.add(chartHeader(label, exitReasonsHelperText(), actions), BorderLayout.NORTH);
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
            installChartWheelForwardingOnce(chart);
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
        scrollPane.setWheelScrollingEnabled(false);
        installChartWheelForwarding(scrollPane, chart);
        HomeStatsChartHelper.styleHorizontalScrollBar(scrollPane.getHorizontalScrollBar());
        return scrollPane;
    }

    private void installChartWheelForwarding(JScrollPane chartScrollPane, JComponent chart) {
        chartScrollPane.addMouseWheelListener(event -> forwardChartWheel(event, chartScrollPane));
        chartScrollPane.getViewport().addMouseWheelListener(event -> forwardChartWheel(event, chartScrollPane));
        installChartWheelForwardingOnce(chart);
    }

    private void installChartWheelForwardingOnce(JComponent chart) {
        String key = "kgm.chartWheelForwarding";
        if (Boolean.TRUE.equals(chart.getClientProperty(key))) {
            return;
        }
        chart.putClientProperty(key, true);
        chart.addMouseWheelListener(event -> forwardChartWheel(event, nearestChartScrollPane(event.getComponent())));
    }

    private void forwardChartWheel(MouseWheelEvent event, JScrollPane chartScrollPane) {
        if (event.isConsumed()) {
            return;
        }
        if (chartScrollPane != null && event.isShiftDown() && scrollHorizontalChart(event, chartScrollPane)) {
            event.consume();
            return;
        }

        JScrollPane pageScroll = pageScrollPane(chartScrollPane != null ? chartScrollPane : event.getComponent());
        if (pageScroll == null) {
            return;
        }
        JScrollBar vertical = pageScroll.getVerticalScrollBar();
        if (vertical == null || !vertical.isVisible()) {
            return;
        }

        int direction = event.getWheelRotation() < 0 ? -1 : 1;
        int amount = event.getScrollType() == MouseWheelEvent.WHEEL_BLOCK_SCROLL
                ? event.getWheelRotation() * vertical.getBlockIncrement(direction)
                : event.getUnitsToScroll() * vertical.getUnitIncrement(direction);
        int maxValue = vertical.getMaximum() - vertical.getVisibleAmount();
        int nextValue = Math.max(vertical.getMinimum(), Math.min(maxValue, vertical.getValue() + amount));
        if (nextValue != vertical.getValue()) {
            vertical.setValue(nextValue);
            event.consume();
        }
    }

    private boolean scrollHorizontalChart(MouseWheelEvent event, JScrollPane chartScrollPane) {
        JScrollBar horizontal = chartScrollPane.getHorizontalScrollBar();
        if (horizontal == null || !horizontal.isVisible()) {
            return false;
        }
        int direction = event.getWheelRotation() < 0 ? -1 : 1;
        int amount = event.getScrollType() == MouseWheelEvent.WHEEL_BLOCK_SCROLL
                ? event.getWheelRotation() * horizontal.getBlockIncrement(direction)
                : event.getUnitsToScroll() * horizontal.getUnitIncrement(direction);
        int maxValue = horizontal.getMaximum() - horizontal.getVisibleAmount();
        int nextValue = Math.max(horizontal.getMinimum(), Math.min(maxValue, horizontal.getValue() + amount));
        if (nextValue == horizontal.getValue()) {
            return false;
        }
        horizontal.setValue(nextValue);
        return true;
    }

    private JScrollPane nearestChartScrollPane(Component component) {
        Container current = component instanceof Container container ? container : component.getParent();
        while (current != null) {
            if (current instanceof JScrollPane scrollPane
                    && scrollPane.getHorizontalScrollBarPolicy() != ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER) {
                return scrollPane;
            }
            current = current.getParent();
        }
        return null;
    }

    private JScrollPane pageScrollPane(Component component) {
        Container parent = component == null ? null : component.getParent();
        while (parent != null) {
            if (parent instanceof JScrollPane scrollPane) {
                return scrollPane;
            }
            parent = parent.getParent();
        }
        return null;
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
        gbc.insets = new Insets(
                0,
                0,
                HomeStatsChartHelper.CARD_GAP,
                x == 0 && width == 1 ? HomeStatsChartHelper.CARD_GAP : 0
        );
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = weightx;
        gbc.weighty = 0.0;
        target.add(component, gbc);
    }

    private void addEmptyGridSlot(JPanel target, int x, int y) {
        JPanel spacer = new JPanel();
        spacer.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(0, 0, HomeStatsChartHelper.CARD_GAP, 0);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;
        target.add(spacer, gbc);
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

    private record ChartCardSpec(String key, JComponent component, JComponent chart) {
    }

    /**
     * Interactive bar chart component with responsive sizing, inside-bar values,
     * horizontal stacked labels, zero baseline, balanced bar spacing, X-axis scrolling, and color-only hover effects.
     */
    private interface DashboardChart {
        int chartPreferredHeight();

        int chartPreferredWidth();
    }

    private static class DashboardBarChart extends JPanel implements DashboardChart, Scrollable {
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
            return HomeStatsChartHelper.BAR_CHART_BASE_HEIGHT + dynamicBottomSpace();
        }

        public int chartPreferredWidth() {
            if (items.isEmpty()) {
                return HomeStatsChartHelper.CHART_MIN_WIDTH;
            }
            int slot = Math.max(HomeStatsChartHelper.SLOT_MIN_WIDTH, dynamicSlotWidth());
            return Math.max(
                    HomeStatsChartHelper.CHART_MIN_WIDTH,
                    HomeStatsChartHelper.CHART_LEFT
                            + HomeStatsChartHelper.CHART_RIGHT
                            + HomeStatsChartHelper.BAR_CHART_EXTRA_WIDTH
                            + slot * items.size()
            );
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
            int left = HomeStatsChartHelper.CHART_LEFT;
            int right = HomeStatsChartHelper.CHART_RIGHT;
            int top = HomeStatsChartHelper.CHART_TOP;
            int bottom = dynamicBottomSpace();
            int chartHeight = Math.max(HomeStatsChartHelper.BAR_CHART_MIN_PLOT_HEIGHT, height - top - bottom);
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

                g2.setColor(line == 0 ? HomeStatsChartHelper.GRID_BASELINE : HomeStatsChartHelper.GRID_LINE);
                g2.drawLine(left, y, right, y);

                g2.setColor(line == 0 ? HomeStatsChartHelper.TEXT_PRIMARY : HomeStatsChartHelper.TEXT_SECONDARY);
                String label = compactNumber(value);
                g2.drawString(label, left - metrics.stringWidth(label) - 8, y + 4);
            }
        }

        private void drawBars(Graphics2D g2, int left, int top, int chartHeight, int chartWidth, int max) {
            int itemCount = Math.max(1, items.size());

            int idealSlot = Math.min(
                    HomeStatsChartHelper.SLOT_MAX_WIDTH,
                    Math.max(HomeStatsChartHelper.SLOT_MIN_WIDTH, dynamicSlotWidth())
            );
            int fittedSlot = Math.max(idealSlot, chartWidth / itemCount);
            int slot = Math.max(HomeStatsChartHelper.BAR_SLOT_FIT_MIN_WIDTH, Math.min(idealSlot, fittedSlot));
            int usedWidth = slot * itemCount;
            int startX = left + Math.max(0, (chartWidth - usedWidth) / 2);
            int barWidth = Math.max(
                    HomeStatsChartHelper.BAR_MIN_WIDTH,
                    Math.min(
                            HomeStatsChartHelper.BAR_MAX_WIDTH,
                            (int) Math.round(slot * HomeStatsChartHelper.BAR_WIDTH_RATIO)
                    )
            );

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

                g2.setColor(HomeStatsChartHelper.withAlpha(
                        barColor,
                        hovered ? HomeStatsChartHelper.BAR_HOVER_ALPHA : HomeStatsChartHelper.BAR_ALPHA
                ));
                g2.fillRoundRect(barX, barY, barWidth, barHeight, 4, 4);

                drawValueInsideBar(g2, compactNumber(item.count()), barX, barY, barWidth, barHeight, hovered);

                g2.setFont(HomeStatsChartHelper.LABEL_FONT);
                g2.setColor(hovered ? HomeStatsChartHelper.TEXT_PRIMARY : HomeStatsChartHelper.TEXT_SECONDARY);
                List<String> labelLines = stackedLabelLines(
                        item.label(),
                        labelMetrics,
                        Math.max(HomeStatsChartHelper.BAR_LABEL_MIN_WIDTH, slot - 12),
                        HomeStatsChartHelper.MAX_LABEL_LINES
                );
                drawCenteredLabelLines(
                        g2,
                        labelLines,
                        centerX,
                        zeroY + HomeStatsChartHelper.BAR_LABEL_Y_OFFSET,
                        HomeStatsChartHelper.LABEL_LINE_HEIGHT
                );
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

            g2.setColor(HomeStatsChartHelper.withAlpha(
                    HomeStatsChartHelper.SURFACE,
                    hovered ? HomeStatsChartHelper.BAR_VALUE_HOVER_ALPHA : HomeStatsChartHelper.BAR_VALUE_ALPHA
            ));
            g2.drawString(fittedValue, textX, textY);

            g2.setClip(oldClip);
        }

        private void drawEmptyState(Graphics2D g2) {
            g2.setColor(HomeStatsChartHelper.MUTED_SURFACE);
            g2.fillRoundRect(18, 18, Math.max(0, getWidth() - 36), Math.max(0, getHeight() - 36), 8, 8);
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
            return visible.length() < clean.length() && lines.size() >= HomeStatsChartHelper.MAX_LABEL_LINES;
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
            return Math.max(
                    HomeStatsChartHelper.SLOT_MIN_WIDTH,
                    Math.min(
                            HomeStatsChartHelper.SLOT_MAX_WIDTH,
                            HomeStatsChartHelper.BAR_DYNAMIC_SLOT_BASE
                                    + longestWord * HomeStatsChartHelper.BAR_DYNAMIC_SLOT_PER_CHAR
                    )
            );
        }

        private int dynamicBottomSpace() {
            int maxLines = 1;
            for (ChartItem item : items) {
                maxLines = Math.max(
                        maxLines,
                        Math.min(HomeStatsChartHelper.MAX_LABEL_LINES, cleanLabel(item.label()).split("\\s+").length)
                );
            }
            return HomeStatsChartHelper.BAR_LABEL_BOTTOM_BASE + maxLines * HomeStatsChartHelper.LABEL_LINE_HEIGHT;
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
        private final List<ChartItem> items = new ArrayList<>();
        private final Map<Integer, Shape> sliceBounds = new LinkedHashMap<>();
        private final Map<Integer, Rectangle> legendBounds = new LinkedHashMap<>();
        private int preferredChartHeight = HomeStatsChartHelper.AVERAGE_CHART_HEIGHT;
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

        void setPreferredChartHeight(int preferredChartHeight) {
            this.preferredChartHeight = Math.max(1, preferredChartHeight);
            setPreferredSize(new Dimension(chartPreferredWidth(), chartPreferredHeight()));
            revalidate();
            repaint();
        }

        public int chartPreferredHeight() {
            return preferredChartHeight;
        }

        public int chartPreferredWidth() {
            return HomeStatsChartHelper.CHART_MIN_WIDTH;
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
            int legendRows = Math.max(
                    HomeStatsChartHelper.PIE_MIN_LEGEND_ROWS,
                    Math.min(HomeStatsChartHelper.PIE_MAX_LEGEND_ROWS, items.size())
            );
            int legendHeight = legendRows * HomeStatsChartHelper.LEGEND_ROW_HEIGHT;
            int pieSize = Math.max(
                    HomeStatsChartHelper.PIE_MIN_SIZE,
                    Math.min(
                            HomeStatsChartHelper.PIE_MAX_SIZE,
                            height - HomeStatsChartHelper.PIE_TOP - HomeStatsChartHelper.PIE_BOTTOM
                    )
            );
            int availableLegendWidth = Math.max(
                    HomeStatsChartHelper.PIE_LEGEND_MIN_WIDTH,
                    width - HomeStatsChartHelper.PIE_LEFT - pieSize
                            - HomeStatsChartHelper.PIE_LEGEND_GAP
                            - HomeStatsChartHelper.PIE_LEGEND_RIGHT
            );
            int legendWidth = preferredLegendWidth(g2, availableLegendWidth);
            if (legendWidth < HomeStatsChartHelper.PIE_LEGEND_TARGET_MIN_WIDTH) {
                pieSize = Math.max(
                        HomeStatsChartHelper.PIE_MIN_SIZE,
                        width - HomeStatsChartHelper.PIE_LEFT
                                - HomeStatsChartHelper.PIE_LEGEND_GAP
                                - HomeStatsChartHelper.PIE_LEGEND_RIGHT
                                - HomeStatsChartHelper.PIE_LEGEND_TARGET_MIN_WIDTH
                );
                availableLegendWidth = Math.max(
                        HomeStatsChartHelper.PIE_LEGEND_MIN_WIDTH,
                        width - HomeStatsChartHelper.PIE_LEFT - pieSize
                                - HomeStatsChartHelper.PIE_LEGEND_GAP
                                - HomeStatsChartHelper.PIE_LEGEND_RIGHT
                );
                legendWidth = preferredLegendWidth(g2, availableLegendWidth);
            }
            int legendX = Math.max(
                    HomeStatsChartHelper.PIE_LEFT + pieSize + HomeStatsChartHelper.PIE_LEGEND_GAP,
                    width - HomeStatsChartHelper.PIE_LEGEND_RIGHT - legendWidth
            );
            int pieAreaWidth = Math.max(
                    pieSize,
                    legendX - HomeStatsChartHelper.PIE_LEGEND_GAP - HomeStatsChartHelper.PIE_LEFT
            );
            int pieX = HomeStatsChartHelper.PIE_LEFT + Math.max(0, (pieAreaWidth - pieSize) / 2);
            int pieY = HomeStatsChartHelper.PIE_TOP + Math.max(
                    0,
                    (height - HomeStatsChartHelper.PIE_TOP - HomeStatsChartHelper.PIE_BOTTOM - pieSize) / 2
            );
            int legendY = HomeStatsChartHelper.PIE_TOP + Math.max(
                    0,
                    (height - HomeStatsChartHelper.PIE_TOP - HomeStatsChartHelper.PIE_BOTTOM - legendHeight) / 2
            );

            drawPie(g2, pieX, pieY, pieSize);
            drawLegend(g2, legendX, legendY, legendWidth);

            g2.dispose();
        }

        private int preferredLegendWidth(Graphics2D g2, int maxWidth) {
            g2.setFont(HomeStatsChartHelper.LABEL_FONT);
            FontMetrics metrics = g2.getFontMetrics();
            int width = 96;
            int total = totalCount();
            for (ChartItem item : items) {
                double percent = total == 0 ? 0 : item.count() * 100.0 / total;
                String value = compactNumber(item.count()) + " / " + String.format("%.0f%%", percent);
                int rowWidth = HomeStatsChartHelper.PIE_LEGEND_ROW_PAD * 2
                        + HomeStatsChartHelper.PIE_LEGEND_DOT_SIZE
                        + HomeStatsChartHelper.PIE_LEGEND_DOT_GAP
                        + metrics.stringWidth(cleanLabel(item.label()))
                        + HomeStatsChartHelper.PIE_LEGEND_VALUE_GAP
                        + metrics.stringWidth(value);
                width = Math.max(width, rowWidth);
            }
            return Math.min(maxWidth, width);
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
                g2.setColor(HomeStatsChartHelper.withAlpha(
                        color,
                        index == hoveredIndex
                                ? HomeStatsChartHelper.PIE_SLICE_HOVER_ALPHA
                                : HomeStatsChartHelper.PIE_SLICE_ALPHA
                ));
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
                int rowY = y + index * HomeStatsChartHelper.LEGEND_ROW_HEIGHT;
                boolean hovered = index == hoveredIndex;
                Color color = hovered ? HomeStatsChartHelper.hoverColor(item.color()) : item.color();

                double percent = total == 0 ? 0 : item.count() * 100.0 / total;
                String value = compactNumber(item.count()) + " / " + String.format("%.0f%%", percent);

                g2.setFont(hovered ? hoverFont : normalFont);
                FontMetrics metrics = g2.getFontMetrics();
                int valueW = metrics.stringWidth(value);
                int labelMax = Math.max(
                        18,
                        width - HomeStatsChartHelper.PIE_LEGEND_ROW_PAD * 2
                                - HomeStatsChartHelper.PIE_LEGEND_DOT_SIZE
                                - HomeStatsChartHelper.PIE_LEGEND_DOT_GAP
                                - HomeStatsChartHelper.PIE_LEGEND_VALUE_GAP
                                - valueW
                );
                String label = fitText(g2, cleanLabel(item.label()), labelMax);
                int dotX = x + HomeStatsChartHelper.PIE_LEGEND_ROW_PAD;
                int labelX = dotX
                        + HomeStatsChartHelper.PIE_LEGEND_DOT_SIZE
                        + HomeStatsChartHelper.PIE_LEGEND_DOT_GAP;
                int valueX = x + width - HomeStatsChartHelper.PIE_LEGEND_ROW_PAD - valueW;

                Rectangle row = new Rectangle(x, rowY - 4, width, HomeStatsChartHelper.LEGEND_ROW_HEIGHT);
                legendBounds.put(index, row);

                if (hovered) {
                    g2.setColor(HomeStatsChartHelper.withAlpha(color, HomeStatsChartHelper.PIE_LEGEND_HOVER_ALPHA));
                    g2.fillRoundRect(row.x, row.y, row.width, row.height, 8, 8);
                }

                g2.setColor(color);
                g2.fillRoundRect(
                        dotX,
                        rowY,
                        HomeStatsChartHelper.PIE_LEGEND_DOT_SIZE,
                        HomeStatsChartHelper.PIE_LEGEND_DOT_SIZE,
                        4,
                        4
                );
                g2.setColor(hovered ? HomeStatsChartHelper.TEXT_PRIMARY : HomeStatsChartHelper.TEXT_SECONDARY);
                g2.drawString(label, labelX, rowY + 10);
                g2.setColor(HomeStatsChartHelper.TEXT_PRIMARY);
                g2.drawString(value, valueX, rowY + 10);
            }
        }

        private void drawEmptyState(Graphics2D g2) {
            g2.setColor(HomeStatsChartHelper.MUTED_SURFACE);
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

package com.kgm.ui.panel;

import com.kgm.model.Employee;
import com.kgm.ui.component.UniversalDatePicker;
import com.kgm.ui.styling.OtherDetailsPanelHelper;

import javax.swing.*;
import java.awt.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class OtherDetailsPanel extends JPanel {

    private Employee data;
    private JComponent topAnchor;

    // 🔥 NEW: store fields
    private Map<String, JTextField> fieldMap = new HashMap<>();
    private Map<String, UniversalDatePicker> dateFieldMap = new HashMap<>();
    private Map<String, Boolean> dateDirtyMap = new HashMap<>();
    private static final SimpleDateFormat DB_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    // ================= EMPTY =================
    public OtherDetailsPanel() {
        this(null);
    }

    // ================= WITH DATA =================
    public OtherDetailsPanel(Employee data) {
        this.data = data;

        OtherDetailsPanelHelper.stylePanel(this);

        add(OtherDetailsPanelHelper.createContent(buildUI()), BorderLayout.NORTH);
    }

    // ================= UI =================
    private JPanel buildUI() {

        JPanel root = OtherDetailsPanelHelper.createRootPanel();
        String[] breadcrumbLabels = {
                "Organization",
                "Personal",
                "Banking",
                "Compliance",
                "Emergency",
                "Vaccination"
        };
        JComponent[] sectionRefs = new JComponent[breadcrumbLabels.length];

        topAnchor = OtherDetailsPanelHelper.createBreadcrumbPanel();
        root.add(topAnchor);

        sectionRefs[0] = createSection("Organization / Structure",
                new String[][]{
                        {"ORG ID", get("ORG_ID")},
                        {"DIVISION", get("DIVISION")},
                        {"SHIFT", get("SHIFT")},
                        {"PROB PERIOD", get("PROB_PERIOD")},
                        {"CONFIRMING ON", get("CONFIRMING_ON")},
                        {"REP UNIT", get("REP_UNT")},
                        {"REP EMP ID", get("REP_EMP_ID")},
                        {"REP DESIG CODE", get("REP_EMP_DESIG_CODE")},
                        {"REP DEPT CODE", get("REP_EMP_DEPT_CODE")},
                        {"REP TYPE", get("REP_EMP_TYPE")},
                        {"BRANCH CODE", get("BRANCH_CODE")},
                        {"BRANCH NAME", get("BRANCH_NAME")}
                });
        root.add(sectionRefs[0]);

        root.add(Box.createVerticalStrut(18));

        sectionRefs[1] = createSection("Personal / HR Details",
                new String[][]{
                        {"DOB", get("DOB")},
                        {"City of Birth", get("CITY_OF_BIRTH")},
                        {"Nationality", get("NATIONALITY")},
                        {"Religion", get("RELIGION")},
                        {"Blood Group", get("BLOOD_GROUP")},
                        {"Marital Status", get("M_STATUS")},
                        {"Mother Name", get("MOTHER_NAME")}
                });
        root.add(sectionRefs[1]);

        root.add(Box.createVerticalStrut(18));

        sectionRefs[2] = createSection("Banking / Finance",
                new String[][]{
                        {"Bank Name", get("BANK_NAME")},
                        {"Account No", get("BANK_AC_NO")},
                        {"SS No", get("SS_NO")},
                        {"EOBI No", get("EOBI_NO")},
                        {"Tax No", get("TAX_NO")},
                        {"EFU", get("EFU")},
                        {"EFU No", get("EFU_NO")},
                        {"PFUND Code", get("CLIPPER_PFUND_CODE")}
                });
        root.add(sectionRefs[2]);

        root.add(Box.createVerticalStrut(18));

        sectionRefs[3] = createSection("Compliance / Status",
                new String[][]{
                        {"EOBI Status", get("EOBI_STATUS")},
                        {"NIC Verify", get("NIC_VERIFY")},
                        {"NIC Verify Date", get("NIC_VERIFY_DATE")},
                        {"HOD Check", get("HOD_CHECK")},
                        {"Clearance Status", get("CLEARANCE_STATUS")},
                        {"Dis Certificate", get("DIS_CERTIFICATE")}
                });
        root.add(sectionRefs[3]);

        root.add(Box.createVerticalStrut(18));

        sectionRefs[4] = createSection("Emergency / Misc",
                new String[][]{
                        {"Emergency No", get("EMERGENCY_NO")},
                        {"Attendance Category", get("ATT_CATEG")}
                });
        root.add(sectionRefs[4]);

        root.add(Box.createVerticalStrut(18));

        sectionRefs[5] = createSection("Vaccination / Wellness",
                new String[][]{
                        {"Wellness Club", get("WELLNESS_CLUB")},
                        {"Card Issue", get("WELLNESS_CARD_ISSUE")},
                        {"Card No", get("WELLNESS_CARD_NO")},
                        {"Valid Date", get("WELLNESS_CLUB_VALID_DATE")},
                        {"First Dose", get("FIRST_DOSE")},
                        {"Second Dose", get("SECOND_DOSE")},
                        {"First Vacc Date", get("FIRST_VACC_DATE")},
                        {"Second Vacc Date", get("SECOND_VACC_DATE")}
                });
        root.add(sectionRefs[5]);

        root.add(OtherDetailsPanelHelper.createReturnToTopPanel(() -> scrollToComponent(topAnchor)));
        installBreadcrumbLinks((JPanel) topAnchor, breadcrumbLabels, sectionRefs);

        return root;
    }

    private void installBreadcrumbLinks(JPanel breadcrumb, String[] labels, JComponent[] targets) {
        for (int index = 0; index < labels.length; index++) {
            JButton link = OtherDetailsPanelHelper.createBreadcrumbLink(labels[index]);
            JComponent target = targets[index];
            link.addActionListener(event -> scrollToComponent(target));
            breadcrumb.add(link);

            if (index < labels.length - 1) {
                breadcrumb.add(OtherDetailsPanelHelper.createBreadcrumbSeparator());
            }
        }
    }

    private void scrollToComponent(JComponent component) {
        if (component == null) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            Container parent = component.getParent();
            if (parent instanceof JComponent parentComponent) {
                Rectangle bounds = component.getBounds();
                bounds.y = Math.max(0, bounds.y - 12);
                bounds.height = component.getHeight() + 24;
                parentComponent.scrollRectToVisible(bounds);
                return;
            }

            component.scrollRectToVisible(new Rectangle(0, 0, component.getWidth(), component.getHeight()));
        });
    }

    // ================= SECTION =================
    private JPanel createSection(String title, String[][] data) {

        JPanel section = OtherDetailsPanelHelper.createSectionPanel();

        JLabel header = OtherDetailsPanelHelper.createSectionHeader(title);

        section.add(header, BorderLayout.NORTH);
        section.add(buildGrid(data), BorderLayout.CENTER);

        return section;
    }

    // ================= GRID =================
    private JPanel buildGrid(String[][] data) {

        JPanel grid = OtherDetailsPanelHelper.createGridPanel();

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 16, 8, 16);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        for (int i = 0; i < data.length; i += 2) {

            gbc.gridy = row++;

            gbc.gridx = 0;
            gbc.weightx = 0.5;
            grid.add(createField(data[i][0], data[i][1]), gbc);

            gbc.gridx = 1;
            gbc.weightx = 0.5;

            if (i + 1 < data.length) {
                grid.add(createField(data[i + 1][0], data[i + 1][1]), gbc);
            } else {
                grid.add(OtherDetailsPanelHelper.createGridFiller(), gbc);
            }
        }

        return grid;
    }

    // ================= FIELD =================
    private JPanel createField(String label, String value) {

        JPanel p = OtherDetailsPanelHelper.createFieldPanel();

        JLabel lbl = OtherDetailsPanelHelper.createFieldLabel(label);

        // 🔥 normalize empty -> N/A
        if (isEmpty(value)) {
            value = "N/A";
        }

        boolean editable = isEmpty(value);

        // 🔥 store field
        JComponent input;
        if (isDateField(label)) {
            UniversalDatePicker datePicker = OtherDetailsPanelHelper.createDateField(parseDate(value));
            datePicker.setEnabled(editable);
            dateDirtyMap.put(label, false);
            datePicker.addDateChangeListener(() -> dateDirtyMap.put(label, true));
            dateFieldMap.put(label, datePicker);
            input = datePicker;
        } else {
            JTextField field = OtherDetailsPanelHelper.createField(value);
            field.setEditable(editable);
            fieldMap.put(label, field);
            input = field;
        }

        p.add(lbl, BorderLayout.NORTH);
        p.add(input, BorderLayout.CENTER);

        return p;
    }

    private boolean isDateField(String label) {
        return "First Dose".equals(label)
                || "Second Dose".equals(label)
                || "First Vacc Date".equals(label)
                || "Second Vacc Date".equals(label);
    }

    private Date parseDate(String value) {
        if (isEmpty(value)) {
            return new Date();
        }

        String[] patterns = {"yyyy-MM-dd", "dd-MM-yyyy HH:mm", "dd-MM-yyyy", "yyyy/MM/dd"};
        for (String pattern : patterns) {
            try {
                return new SimpleDateFormat(pattern).parse(value.trim());
            } catch (ParseException ignored) {
            }
        }
        return new Date();
    }

    // ================= DATA GETTER =================
    private String get(String key) {
        if (data == null) return "";

        switch (key) {
            case "ORG_ID": return data.getORG_ID();
            case "DIVISION": return data.getDIVISION();
            case "SHIFT": return data.getSHIFT();
            case "PROB_PERIOD": return data.getPROB_PERIOD();
            case "CONFIRMING_ON": return data.getCONFIRMING_ON();

            case "REP_UNT": return data.getREP_UNT();
            case "REP_EMP_ID": return data.getREP_EMP_ID();
            case "REP_EMP_DESIG_CODE": return data.getREP_EMP_DESIG_CODE();
            case "REP_EMP_DEPT_CODE": return data.getREP_EMP_DEPT_CODE();
            case "REP_EMP_TYPE": return data.getREP_EMP_TYPE();

            case "BRANCH_CODE": return data.getBRANCH_CODE();
            case "BRANCH_NAME": return data.getBRANCH_NAME();

            case "DOB": return data.getDOB();
            case "CITY_OF_BIRTH": return data.getCITY_OF_BIRTH();
            case "NATIONALITY": return data.getNATIONALITY();
            case "RELIGION": return data.getRELIGION();
            case "BLOOD_GROUP": return data.getBLOOD_GROUP();
            case "M_STATUS": return data.getM_STATUS();
            case "MOTHER_NAME": return data.getMOTHER_NAME();

            case "BANK_NAME": return data.getBANK_NAME();
            case "BANK_AC_NO": return data.getBANK_AC_NO();
            case "SS_NO": return data.getSS_NO();
            case "EOBI_NO": return data.getEOBI_NO();
            case "TAX_NO": return data.getTAX_NO();
            case "EFU": return data.getEFU();
            case "EFU_NO": return data.getEFU_NO();
            case "CLIPPER_PFUND_CODE": return data.getCLIPPER_PFUND_CODE();

            case "EOBI_STATUS": return data.getEOBI_STATUS();
            case "NIC_VERIFY": return data.getNIC_VERIFY();
            case "NIC_VERIFY_DATE": return data.getNIC_VERIFY_DATE();
            case "HOD_CHECK": return data.getHOD_CHECK();
            case "CLEARANCE_STATUS": return data.getCLEARANCE_STATUS();
            case "DIS_CERTIFICATE": return data.getDIS_CERTIFICATE();

            case "EMERGENCY_NO": return data.getEMERGENCY_NO();
            case "ATT_CATEG": return data.getATT_CATEG();

            case "WELLNESS_CLUB": return data.getWELLNESS_CLUB();
            case "WELLNESS_CARD_ISSUE": return data.getWELLNESS_CARD_ISSUE();
            case "WELLNESS_CARD_NO": return data.getWELLNESS_CARD_NO();
            case "WELLNESS_CLUB_VALID_DATE": return data.getWELLNESS_CLUB_VALID_DATE();

            case "FIRST_DOSE": return data.getFIRST_DOSE();
            case "SECOND_DOSE": return data.getSECOND_DOSE();
            case "FIRST_VACC_DATE": return data.getFIRST_VACC_DATE();
            case "SECOND_VACC_DATE": return data.getSECOND_VACC_DATE();

            default: return "";
        }
    }

    // ================= EXTRACT UPDATED =================
    public Employee getUpdatedOtherDetails() {

        Employee emp = new Employee();

        fieldMap.forEach((label, field) -> {

            if (!field.isEditable()) return;

            String val = field.getText();

            if (isEmpty(val)) return;

            switch (label) {

                case "ORG ID": emp.setORG_ID(val); break;
                case "DIVISION": emp.setDIVISION(val); break;
                case "SHIFT": emp.setSHIFT(val); break;
                case "PROB PERIOD": emp.setPROB_PERIOD(val); break;
                case "CONFIRMING ON": emp.setCONFIRMING_ON(val); break;

                case "REP UNIT": emp.setREP_UNT(val); break;
                case "REP EMP ID": emp.setREP_EMP_ID(val); break;
                case "REP DESIG CODE": emp.setREP_EMP_DESIG_CODE(val); break;
                case "REP DEPT CODE": emp.setREP_EMP_DEPT_CODE(val); break;
                case "REP TYPE": emp.setREP_EMP_TYPE(val); break;

                case "BRANCH CODE": emp.setBRANCH_CODE(val); break;
                case "BRANCH NAME": emp.setBRANCH_NAME(val); break;

                case "DOB": emp.setDOB(val); break;
                case "City of Birth": emp.setCITY_OF_BIRTH(val); break;
                case "Nationality": emp.setNATIONALITY(val); break;
                case "Religion": emp.setRELIGION(val); break;
                case "Blood Group": emp.setBLOOD_GROUP(val); break;
                case "Marital Status": emp.setM_STATUS(val); break;
                case "Mother Name": emp.setMOTHER_NAME(val); break;

                case "Bank Name": emp.setBANK_NAME(val); break;
                case "Account No": emp.setBANK_AC_NO(val); break;
                case "SS No": emp.setSS_NO(val); break;
                case "EOBI No": emp.setEOBI_NO(val); break;
                case "Tax No": emp.setTAX_NO(val); break;
                case "EFU": emp.setEFU(val); break;
                case "EFU No": emp.setEFU_NO(val); break;
                case "PFUND Code": emp.setCLIPPER_PFUND_CODE(val); break;

                case "EOBI Status": emp.setEOBI_STATUS(val); break;
                case "NIC Verify": emp.setNIC_VERIFY(val); break;
                case "NIC Verify Date": emp.setNIC_VERIFY_DATE(val); break;
                case "HOD Check": emp.setHOD_CHECK(val); break;
                case "Clearance Status": emp.setCLEARANCE_STATUS(val); break;
                case "Dis Certificate": emp.setDIS_CERTIFICATE(val); break;

                case "Emergency No": emp.setEMERGENCY_NO(val); break;
                case "Attendance Category": emp.setATT_CATEG(val); break;

                case "Wellness Club": emp.setWELLNESS_CLUB(val); break;
                case "Card Issue": emp.setWELLNESS_CARD_ISSUE(val); break;
                case "Card No": emp.setWELLNESS_CARD_NO(val); break;
                case "Valid Date": emp.setWELLNESS_CLUB_VALID_DATE(val); break;

                case "First Dose": emp.setFIRST_DOSE(val); break;
                case "Second Dose": emp.setSECOND_DOSE(val); break;
                case "First Vacc Date": emp.setFIRST_VACC_DATE(val); break;
                case "Second Vacc Date": emp.setSECOND_VACC_DATE(val); break;
            }
        });

        dateFieldMap.forEach((label, picker) -> {
            if (!picker.isEnabled() || !Boolean.TRUE.equals(dateDirtyMap.get(label))) {
                return;
            }

            String val = DB_DATE_FORMAT.format(picker.getDate());
            switch (label) {
                case "First Dose": emp.setFIRST_DOSE(val); break;
                case "Second Dose": emp.setSECOND_DOSE(val); break;
                case "First Vacc Date": emp.setFIRST_VACC_DATE(val); break;
                case "Second Vacc Date": emp.setSECOND_VACC_DATE(val); break;
            }
        });

        return emp;
    }

    // ================= EMPTY CHECK =================
    private boolean isEmpty(String value) {
        if (value == null) return true;

        String v = value.trim();

        return v.isEmpty()
                || v.equalsIgnoreCase("N/A")
                || v.equalsIgnoreCase("NA");
    }
}

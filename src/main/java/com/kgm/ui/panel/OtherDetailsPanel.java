package com.kgm.ui.panel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import com.kgm.model.Employee;

public class OtherDetailsPanel extends JPanel {

    private Employee employee;

    // ================= SAFE EMPTY CONSTRUCTOR =================
    public OtherDetailsPanel() {
        this(null);
    }

    // ================= MAIN CONSTRUCTOR =================
    public OtherDetailsPanel(Employee employee) {
        this.employee = employee;

        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        JScrollPane scroll = new JScrollPane(buildUI());
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        add(scroll, BorderLayout.CENTER);
    }

    // ================= UI BUILDER =================
    private JPanel buildUI() {

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(Color.WHITE);
        root.setBorder(new EmptyBorder(20, 20, 20, 20));

        root.add(createSection("Organization / Structure",
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
                        {"REPORT TO EMP", get("REPORT_TO_EMP_ID")},
                        {"REPORT TO UNIT", get("REPORT_TO_UNT")},
                        {"BRANCH CODE", get("BRANCH_CODE")},
                        {"BRANCH NAME", get("BRANCH_NAME")}
                }));

        root.add(Box.createVerticalStrut(10));

        root.add(createSection("Personal / HR Details",
                new String[][]{
                        {"DOB", get("DOB")},
                        {"City of Birth", get("CITY_OF_BIRTH")},
                        {"Nationality", get("NATIONALITY")},
                        {"Religion", get("RELIGION")},
                        {"Blood Group", get("BLOOD_GROUP")},
                        {"Marital Status", get("M_STATUS")},
                        {"Mother Name", get("MOTHER_NAME")}
                }));

        root.add(Box.createVerticalStrut(10));

        root.add(createSection("Banking / Finance",
                new String[][]{
                        {"Bank Name", get("BANK_NAME")},
                        {"Account No", get("BANK_AC_NO")},
                        {"SS No", get("SS_NO")},
                        {"EOBI No", get("EOBI_NO")},
                        {"Tax No", get("TAX_NO")},
                        {"EFU", get("EFU")},
                        {"EFU No", get("EFU_NO")},
                        {"PFUND Code", get("CLIPPER_PFUND_CODE")}
                }));

        root.add(Box.createVerticalStrut(10));

        root.add(createSection("Compliance / Status",
                new String[][]{
                        {"EOBI Status", get("EOBI_STATUS")},
                        {"Union Deduction", get("DED_UNION")},
                        {"Rehiring Status", get("REHIRING_STATUS")},
                        {"NIC Verify", get("NIC_VERIFY")},
                        {"NIC Verify Date", get("NIC_VERIFY_DATE")},
                        {"HOD Check", get("HOD_CHECK")},
                        {"Clearance Status", get("CLEARANCE_STATUS")},
                        {"Dis Certificate", get("DIS_CERTIFICATE")}
                }));

        root.add(Box.createVerticalStrut(10));

        root.add(createSection("Emergency / Misc",
                new String[][]{
                        {"Emergency No", get("EMERGENCY_NO")},
                        {"Family CNIC", get("CNIC_FAMILY_NO")},
                        {"House No", get("COLONY_HOUSE_NUMBER")},
                        {"Attendance Category", get("ATT_CATEG")},
                        {"Reference", get("REFERENCE")}
                }));

        root.add(Box.createVerticalStrut(10));

        root.add(createSection("Vaccination / Wellness",
                new String[][]{
                        {"Wellness Club", get("WELLNESS_CLUB")},
                        {"Card Issue", get("WELLNESS_CARD_ISSUE")},
                        {"Card No", get("WELLNESS_CARD_NO")},
                        {"Valid Date", get("WELLNESS_CLUB_VALID_DATE")},
                        {"First Dose", get("FIRST_DOSE")},
                        {"Second Dose", get("SECOND_DOSE")},
                        {"First Vacc Date", get("FIRST_VACC_DATE")},
                        {"Second Vacc Date", get("SECOND_VACC_DATE")},
                        {"Vaccine ID", get("VAC_ID")}
                }));

        return root;
    }

    // ================= SAP STYLE SECTION =================
    private JPanel createSection(String title, String[][] data) {

        JPanel section = new JPanel(new BorderLayout());
        section.setBackground(Color.WHITE);
        section.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 230, 230)),
                new EmptyBorder(12, 12, 12, 12)
        ));

        JLabel header = new JLabel(title);
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setHorizontalAlignment(SwingConstants.LEFT);
        header.setForeground(new Color(60, 60, 60));

        section.add(header, BorderLayout.NORTH);
        section.add(buildGrid(data), BorderLayout.CENTER);

        return section;
    }

    // ================= 2-COLUMN GRID =================
    private JPanel buildGrid(String[][] data) {

        JPanel grid = new JPanel(new GridBagLayout());
        grid.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
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
                grid.add(new JPanel(), gbc);
            }
        }

        return grid;
    }

    // ================= FIELD =================
    private JPanel createField(String label, String value) {

        JPanel p = new JPanel(new BorderLayout(5, 3));
        p.setBackground(Color.WHITE);

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lbl.setHorizontalAlignment(SwingConstants.LEFT);

        JTextField field = new JTextField(value);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        setFieldEditability(field, value);

        field.setPreferredSize(new Dimension(200, 30));

        p.add(lbl, BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);

        return p;
    }

    // ================= DATA ACCESS =================
    private String get(String field) {
        if (employee == null) return "";

        try {
            return switch (field) {
                case "ORG_ID" -> employee.getORG_ID();
                case "DIVISION" -> employee.getDIVISION();
                case "SHIFT" -> employee.getSHIFT();
                case "PROB_PERIOD" -> employee.getPROB_PERIOD();
                case "CONFIRMING_ON" -> employee.getCONFIRMING_ON();
                case "REP_UNT" -> employee.getREP_UNT();
                case "REP_EMP_ID" -> employee.getREP_EMP_ID();
                case "REP_EMP_DESIG_CODE" -> employee.getREP_EMP_DESIG_CODE();
                case "REP_EMP_DEPT_CODE" -> employee.getREP_EMP_DEPT_CODE();
                case "REP_EMP_TYPE" -> employee.getREP_EMP_TYPE();
                case "REPORT_TO_EMP_ID" -> employee.getREPORT_TO_EMP_ID();
                case "REPORT_TO_UNT" -> employee.getREPORT_TO_UNT();
                case "BRANCH_CODE" -> employee.getBRANCH_CODE();
                case "BRANCH_NAME" -> employee.getBRANCH_NAME();

                case "DOB" -> employee.getDOB();
                case "CITY_OF_BIRTH" -> employee.getCITY_OF_BIRTH();
                case "NATIONALITY" -> employee.getNATIONALITY();
                case "RELIGION" -> employee.getRELIGION();
                case "BLOOD_GROUP" -> employee.getBLOOD_GROUP();
                case "M_STATUS" -> employee.getM_STATUS();
                case "MOTHER_NAME" -> employee.getMOTHER_NAME();

                case "BANK_NAME" -> employee.getBANK_NAME();
                case "BANK_AC_NO" -> employee.getBANK_AC_NO();
                case "SS_NO" -> employee.getSS_NO();
                case "EOBI_NO" -> employee.getEOBI_NO();
                case "TAX_NO" -> employee.getTAX_NO();
                case "EFU" -> employee.getEFU();
                case "EFU_NO" -> employee.getEFU_NO();
                case "CLIPPER_PFUND_CODE" -> employee.getCLIPPER_PFUND_CODE();

                case "EOBI_STATUS" -> employee.getEOBI_STATUS();
                case "DED_UNION" -> employee.getDED_UNION();
                case "REHIRING_STATUS" -> employee.getREHIRING_STATUS();
                case "NIC_VERIFY" -> employee.getNIC_VERIFY();
                case "NIC_VERIFY_DATE" -> employee.getNIC_VERIFY_DATE();
                case "HOD_CHECK" -> employee.getHOD_CHECK();
                case "CLEARANCE_STATUS" -> employee.getCLEARANCE_STATUS();
                case "DIS_CERTIFICATE" -> employee.getDIS_CERTIFICATE();

                case "EMERGENCY_NO" -> employee.getEMERGENCY_NO();
                case "CNIC_FAMILY_NO" -> employee.getCNIC_FAMILY_NO();
                case "COLONY_HOUSE_NUMBER" -> employee.getCOLONY_HOUSE_NUMBER();
                case "ATT_CATEG" -> employee.getATT_CATEG();
                case "REFERENCE" -> employee.getREFERENCE();

                case "WELLNESS_CLUB" -> employee.getWELLNESS_CLUB();
                case "WELLNESS_CARD_ISSUE" -> employee.getWELLNESS_CARD_ISSUE();
                case "WELLNESS_CARD_NO" -> employee.getWELLNESS_CARD_NO();
                case "WELLNESS_CLUB_VALID_DATE" -> employee.getWELLNESS_CLUB_VALID_DATE();
                case "FIRST_DOSE" -> employee.getFIRST_DOSE();
                case "SECOND_DOSE" -> employee.getSECOND_DOSE();
                case "FIRST_VACC_DATE" -> employee.getFIRST_VACC_DATE();
                case "SECOND_VACC_DATE" -> employee.getSECOND_VACC_DATE();
                case "VAC_ID" -> employee.getVAC_ID();

                default -> "";
            };
        } catch (Exception e) {
            return "";
        }
    }

    // ================= YOUR LOGIC =================
    private boolean isEmpty(String value) {
        if (value == null) return true;
        String t = value.trim();
        return t.isEmpty() || t.equalsIgnoreCase("N/A");
    }

    private void setFieldEditability(JTextField field, String value) {
        field.setEditable(isEmpty(value));
    }
}
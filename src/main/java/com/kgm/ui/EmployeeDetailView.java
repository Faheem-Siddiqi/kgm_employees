package com.kgm.ui;
import com.kgm.dao.EmployeeRepositoryDao;
import javax.swing.*;
import java.awt.*;
import javax.swing.border.EmptyBorder;
import com.kgm.ui.panel.HeaderPanel;
import com.kgm.ui.panel.FormViewPanel;
import com.kgm.model.Employee;
import com.kgm.ui.panel.DocumentViewPanel;
public class EmployeeDetailView extends JFrame {

    private String empCode;
    private JButton backBtn;
    private JButton nextBackBtn;
    private JButton updateBtn;

    // ✅ NEW CONSTRUCTOR (used when row is clicked)
    public EmployeeDetailView(String empCode) {
EmployeeRepositoryDao dao = new EmployeeRepositoryDao();
        this.empCode = empCode;

Employee emp = dao.getFullEmployeeByCode(empCode);


          if (emp != null) {
        System.out.println("===== EMPLOYEE CHECK =====");
      System.out.println("===== EMPLOYEE CHECK =====");

System.out.println("ID: " + emp.getID());
System.out.println("UNT_CODE: " + emp.getUNT_CODE());
System.out.println("EMPLOYEE_CODE: " + emp.getEMPLOYEE_CODE());

System.out.println("EMP_NAME: " + emp.getEMP_NAME());
System.out.println("FATHER_NAME: " + emp.getFATHER_NAME());
System.out.println("MOTHER_NAME: " + emp.getMOTHER_NAME());

System.out.println("GENDER: " + emp.getGENDER());
System.out.println("DOB: " + emp.getDOB());
System.out.println("CITY_OF_BIRTH: " + emp.getCITY_OF_BIRTH());
System.out.println("NATIONALITY: " + emp.getNATIONALITY());
System.out.println("RELIGION: " + emp.getRELIGION());
System.out.println("BLOOD_GROUP: " + emp.getBLOOD_GROUP());
System.out.println("M_STATUS: " + emp.getM_STATUS());
System.out.println("NID: " + emp.getNID());

System.out.println("DEPARTMENT: " + emp.getDEPARTMENT());
System.out.println("DESIG_CODE: " + emp.getDESIG_CODE());
System.out.println("DESIGNATION: " + emp.getDESIGNATION());
System.out.println("GRADE: " + emp.getGRADE());
System.out.println("JOINING_DATE: " + emp.getJOINING_DATE());
System.out.println("CONFIRMING_ON: " + emp.getCONFIRMING_ON());
System.out.println("EMP_STATUS: " + emp.getEMP_STATUS());
System.out.println("SHIFT: " + emp.getSHIFT());
System.out.println("PROB_PERIOD: " + emp.getPROB_PERIOD());
System.out.println("EXP_IN_KTML: " + emp.getEXP_IN_KTML());
System.out.println("APPLICATION_DATE: " + emp.getAPPLICATION_DATE());
System.out.println("RESIGN_REASON: " + emp.getRESIGN_REASON());
System.out.println("RESIGN_DATE: " + emp.getRESIGN_DATE());

System.out.println("ORG_ID: " + emp.getORG_ID());
System.out.println("DIVISION: " + emp.getDIVISION());
System.out.println("BRANCH_CODE: " + emp.getBRANCH_CODE());
System.out.println("BRANCH_NAME: " + emp.getBRANCH_NAME());
System.out.println("DESCR: " + emp.getDESCR());

System.out.println("GROSS_SALARY: " + emp.getGROSS_SALARY());
System.out.println("PAY_CATEGORY: " + emp.getPAY_CATEGORY());
System.out.println("BASIC: " + emp.getBASIC());
System.out.println("COLA1: " + emp.getCOLA1());
System.out.println("COLA2: " + emp.getCOLA2());
System.out.println("COLA3: " + emp.getCOLA3());
System.out.println("COLA4: " + emp.getCOLA4());
System.out.println("COLA5: " + emp.getCOLA5());
System.out.println("COLA6_7: " + emp.getCOLA6_7());
System.out.println("COLA8: " + emp.getCOLA8());
System.out.println("COLA9: " + emp.getCOLA9());
System.out.println("COLA10: " + emp.getCOLA10());
System.out.println("COLA11: " + emp.getCOLA11());

System.out.println("PB_SPECIAL1_2: " + emp.getPB_SPECIAL1_2());
System.out.println("PB_SPECIAL3: " + emp.getPB_SPECIAL3());
System.out.println("PB_SPECIAL4: " + emp.getPB_SPECIAL4());
System.out.println("SPECIAL: " + emp.getSPECIAL());

System.out.println("OTHER1: " + emp.getOTHER1());
System.out.println("OTHER2: " + emp.getOTHER2());
System.out.println("OTHER3: " + emp.getOTHER3());

System.out.println("MEDICAL: " + emp.getMEDICAL());
System.out.println("CONVEYANCE: " + emp.getCONVEYANCE());
System.out.println("UTILITY: " + emp.getUTILITY());
System.out.println("ENTERTAINMENT: " + emp.getENTERTAINMENT());

System.out.println("PAY_GROUP: " + emp.getPAY_GROUP());
System.out.println("PAY_GROUP_DESC: " + emp.getPAY_GROUP_DESC());
System.out.println("PAY_AT_JOINING: " + emp.getPAY_AT_JOINING());
System.out.println("EXTRA_DUTY: " + emp.getEXTRA_DUTY());
System.out.println("PAYROLL_FLAG: " + emp.getPAYROLL_FLAG());

System.out.println("BANK_NAME: " + emp.getBANK_NAME());
System.out.println("BANK_AC_NO: " + emp.getBANK_AC_NO());
System.out.println("SS_NO: " + emp.getSS_NO());
System.out.println("EOBI_NO: " + emp.getEOBI_NO());
System.out.println("TAX_NO: " + emp.getTAX_NO());
System.out.println("PFUND_DEDUCTION: " + emp.getPFUND_DEDUCTION());
System.out.println("PF_INTEREST: " + emp.getPF_INTEREST());
System.out.println("PFUND_CODE: " + emp.getPFUND_CODE());
System.out.println("CLIPPER_PFUND_CODE: " + emp.getCLIPPER_PFUND_CODE());
System.out.println("EFU: " + emp.getEFU());
System.out.println("EFU_NO: " + emp.getEFU_NO());
System.out.println("EOBI_STATUS: " + emp.getEOBI_STATUS());

System.out.println("EMP_CONTNO: " + emp.getEMP_CONTNO());
System.out.println("CURRENT_ADR: " + emp.getCURRENT_ADR());
System.out.println("PERMANENT_ADR: " + emp.getPERMANENT_ADR());
System.out.println("PERSONAL_EMAIL: " + emp.getPERSONAL_EMAIL());
System.out.println("OFFICIAL_EMAIL: " + emp.getOFFICIAL_EMAIL());
System.out.println("EMERGENCY_NO: " + emp.getEMERGENCY_NO());

System.out.println("REP_UNT: " + emp.getREP_UNT());
System.out.println("REP_EMP_ID: " + emp.getREP_EMP_ID());
System.out.println("REP_EMP_DESIG_CODE: " + emp.getREP_EMP_DESIG_CODE());
System.out.println("REP_EMP_DEPT_CODE: " + emp.getREP_EMP_DEPT_CODE());
System.out.println("REP_EMP_TYPE: " + emp.getREP_EMP_TYPE());

System.out.println("FIRST_DOSE: " + emp.getFIRST_DOSE());
System.out.println("SECOND_DOSE: " + emp.getSECOND_DOSE());
System.out.println("FIRST_VACC_DATE: " + emp.getFIRST_VACC_DATE());
System.out.println("SECOND_VACC_DATE: " + emp.getSECOND_VACC_DATE());

System.out.println("WELLNESS_CLUB: " + emp.getWELLNESS_CLUB());
System.out.println("WELLNESS_CARD_NO: " + emp.getWELLNESS_CARD_NO());
System.out.println("WELLNESS_CARD_ISSUE: " + emp.getWELLNESS_CARD_ISSUE());
System.out.println("WELLNESS_CLUB_VALID_DATE: " + emp.getWELLNESS_CLUB_VALID_DATE());

System.out.println("EMP_IMG: " + emp.getEMP_IMG());
    } else {
        System.out.println("Employee not found: " + empCode);
    }

        setTitle("Employee Form");
        setSize(1100, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // ================= TOP =================
        JPanel topContainer = new JPanel(new BorderLayout());

        // ✅ dynamic header
        topContainer.add(new HeaderPanel("Employee Record - " + empCode), BorderLayout.NORTH);

        JPanel backRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        backRow.setBackground(Color.WHITE);

        backBtn = new JButton("← Back");
        backBtn.setBorderPainted(false);
        backBtn.setContentAreaFilled(false);
        backBtn.setFocusPainted(false);
        backBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        backBtn.addActionListener(e -> {
            this.dispose();
            new HomeView();
        });

        backRow.add(backBtn);
        topContainer.add(backRow, BorderLayout.CENTER);
        add(topContainer, BorderLayout.NORTH);

        // ================= CENTER =================
        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setBorder(new EmptyBorder(10, 20, 10, 20));
        centerWrapper.setBackground(Color.WHITE);

        JTabbedPane tabs = new JTabbedPane();

        FormViewPanel formPanel = new FormViewPanel();
        DocumentViewPanel documentPanel = new DocumentViewPanel();

        tabs.addTab("Basic", formPanel);
        tabs.addTab("Organizational ", formPanel);
        tabs.addTab("Personal ", formPanel);
        tabs.addTab("Finance", formPanel);
        tabs.addTab("Compliance", formPanel);
        tabs.addTab("Emergency", formPanel);
        tabs.addTab("Wellness", documentPanel);
        tabs.addTab("Documents", documentPanel);

        centerWrapper.add(tabs, BorderLayout.CENTER);
        add(centerWrapper, BorderLayout.CENTER);

        // ================= FOOTER =================
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        footer.setBackground(Color.WHITE);

        nextBackBtn = new JButton("Next");
        updateBtn = new JButton("Update");

        Dimension btnSize = new Dimension(110, 32);
        nextBackBtn.setPreferredSize(btnSize);
        updateBtn.setPreferredSize(btnSize);

        nextBackBtn.setFocusPainted(false);
        updateBtn.setFocusPainted(false);

        nextBackBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        updateBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        nextBackBtn.setForeground(Color.WHITE);
        updateBtn.setForeground(Color.WHITE);

        nextBackBtn.setBackground(new Color(0, 38, 77));
        updateBtn.setBackground(new Color(0, 38, 77));

        updateBtn.setEnabled(false);

        footer.add(nextBackBtn);
        footer.add(updateBtn);

        add(footer, BorderLayout.SOUTH);

        // ================= TAB CONTROL =================
        tabs.addChangeListener(e -> {
            int index = tabs.getSelectedIndex();

            if (index == 0) {
                nextBackBtn.setText("Next");
                updateBtn.setEnabled(false);
            } else {
                nextBackBtn.setText("Back");
                updateBtn.setEnabled(true);
            }
        });

        nextBackBtn.addActionListener(e -> {
            int index = tabs.getSelectedIndex();
            tabs.setSelectedIndex(index == 0 ? 1 : 0);
        });

        // ================= UPDATE =================
        updateBtn.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Update logic goes here");
        });

        setVisible(true);
    }

    // ✅ OLD CONSTRUCTOR (kept untouched)
    public EmployeeDetailView() {
        setTitle("Employee Form");
        setSize(1100, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel topContainer = new JPanel(new BorderLayout());
        topContainer.add(new HeaderPanel("Employee Record"), BorderLayout.NORTH);

        JPanel backRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        backRow.setBackground(Color.WHITE);

        backBtn = new JButton("← Back");
        backBtn.setBorderPainted(false);
        backBtn.setContentAreaFilled(false);
        backBtn.setFocusPainted(false);
        backBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        backBtn.addActionListener(e -> {
            this.dispose();
            new HomeView();
        });

        backRow.add(backBtn);
        topContainer.add(backRow, BorderLayout.CENTER);
        add(topContainer, BorderLayout.NORTH);

        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setBorder(new EmptyBorder(10, 20, 10, 20));
        centerWrapper.setBackground(Color.WHITE);

        JTabbedPane tabs = new JTabbedPane();

        FormViewPanel formPanel = new FormViewPanel();
        DocumentViewPanel documentPanel = new DocumentViewPanel();

        tabs.addTab("Basic", formPanel);
        tabs.addTab("Organizational ", formPanel);
        tabs.addTab("Personal ", formPanel);
        tabs.addTab("Finance", formPanel);
        tabs.addTab("Compliance", formPanel);
        tabs.addTab("Emergency", formPanel);
        tabs.addTab("Wellness", documentPanel);
        tabs.addTab("Wellness", documentPanel);

        centerWrapper.add(tabs, BorderLayout.CENTER);
        add(centerWrapper, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        footer.setBackground(Color.WHITE);

        nextBackBtn = new JButton("Next");
        updateBtn = new JButton("Update");

        Dimension btnSize = new Dimension(110, 32);
        nextBackBtn.setPreferredSize(btnSize);
        updateBtn.setPreferredSize(btnSize);

        nextBackBtn.setForeground(Color.WHITE);
        updateBtn.setForeground(Color.WHITE);

        nextBackBtn.setBackground(new Color(0, 38, 77));
        updateBtn.setBackground(new Color(0, 38, 77));

        updateBtn.setEnabled(false);

        footer.add(nextBackBtn);
        footer.add(updateBtn);

        add(footer, BorderLayout.SOUTH);

        setVisible(true);
    }
}
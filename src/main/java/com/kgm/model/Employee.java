package com.kgm.model;

public class Employee {

    // ================= EMPLOYEES =================
    private int ID;
    private String UNT_CODE;
    private String EMPLOYEE_CODE;
    private String EMP_NAME;
    private String FATHER_NAME;
    private String MOTHER_NAME;
    private String GENDER;
    private String DOB;
    private String CITY_OF_BIRTH;
    private String NATIONALITY;
    private String RELIGION;
    private String BLOOD_GROUP;
    private String M_STATUS;
    private String NID;

    // ================= EMPLOYMENT =================
    private String DEPARTMENT;
    private String DESIG_CODE;
    private String DESIGNATION;
    private String GRADE;
    private String JOINING_DATE;
    private String CONFIRMING_ON;
    private String EMP_STATUS;
    private String SHIFT;
    private String PROB_PERIOD;
    private String EXP_IN_KTML;
    private String APPLICATION_DATE;
    private String RESIGN_REASON;
    private String RESIGN_DATE;

    // ================= ORGANIZATION =================
    private String ORG_ID;
    private String DIVISION;
    private String BRANCH_CODE;
    private String BRANCH_NAME;
    private String DESCR;

    // ================= PAYROLL =================
    private String GROSS_SALARY;
    private String PAY_CATEGORY;
    private String BASIC;
    private String COLA1;
    private String COLA2;
    private String COLA3;
    private String COLA4;
    private String COLA5;
    private String COLA6_7;
    private String COLA8;
    private String COLA9;
    private String COLA10;
    private String COLA11;

    private String PB_SPECIAL1_2;
    private String PB_SPECIAL3;
    private String PB_SPECIAL4;
    private String SPECIAL;

    private String OTHER1;
    private String OTHER2;
    private String OTHER3;

    private String MEDICAL;
    private String CONVEYANCE;
    private String UTILITY;
    private String ENTERTAINMENT;

    private String PAY_GROUP;
    private String PAY_GROUP_DESC;
    private String PAY_AT_JOINING;
    private String EXTRA_DUTY;
    private String PAYROLL_FLAG;

    // ================= BANKING =================
    private String BANK_NAME;
    private String BANK_AC_NO;
    private String SS_NO;
    private String EOBI_NO;
    private String TAX_NO;
    private String PFUND_DEDUCTION;
    private String PF_INTEREST;
    private String PFUND_CODE;
    private String CLIPPER_PFUND_CODE;
    private String EFU;
    private String EFU_NO;
    private String EOBI_STATUS;

    // ================= CONTACT =================
    private String EMP_CONTNO;
    private String CURRENT_ADR;
    private String PERMANENT_ADR;
    private String PERSONAL_EMAIL;
    private String OFFICIAL_EMAIL;
    private String EMERGENCY_NO;

    // ================= REPORTING =================
    private String REP_UNT;
    private String REP_EMP_ID;
    private String REP_EMP_DESIG_CODE;
    private String REP_EMP_DEPT_CODE;
    private String REP_EMP_TYPE;

    // ================= COMPLIANCE =================
    private String FLAG;
    private String CLEARANCE_STATUS;
    private String HOD_CHECK;
    private String SEC_HEAD_CHK;
    private String NIC_VERIFY;
    private String NIC_VERIFY_DATE;
    private String ATT_CATEG;
    private String DIS_CERTIFICATE;

    // ================= BENEFITS =================
    private String WELLNESS_CLUB;
    private String WELLNESS_CARD_ISSUE;
    private String WELLNESS_CARD_NO;
    private String WELLNESS_CLUB_VALID_DATE;

    // ================= VACCINATION =================
    private String FIRST_DOSE;
    private String SECOND_DOSE;
    private String FIRST_VACC_DATE;
    private String SECOND_VACC_DATE;

    // ================= DOCUMENTS =================
    private String CNIC_COPY;
    private String SS_CARD_COPY;
    private String EOBI_CARD_COPY;
    private String FINAL_SETTLEMENT;
    private String CLEARANCE_CERT;
    private String JOB_APPOINTMENT;
    private String APPLICATION_DOC;
    private String ISSUANCE_DOC;
    private String SETTLEMENT_DOC;
    private String TRIAL_CARD;
    private String INTERVIEW_DOC;
    private String SERVICE_LETTER;
    private String EXTENSION_LETTER;
    private String RETIREMENT_LETTER;
    private String COVID_CERT;
    private String DISCIPLINARY_I;
    private String DISCIPLINARY_II;
    private String DISCIPLINARY_III;
    private String EMP_IMG;

    public Employee() {}

    // ================= GETTERS & SETTERS (sample core only) =================

    public int getID() { return ID; }
    public void setID(int ID) { this.ID = ID; }

    public String getEMPLOYEE_CODE() { return EMPLOYEE_CODE; }
    public void setEMPLOYEE_CODE(String EMPLOYEE_CODE) { this.EMPLOYEE_CODE = EMPLOYEE_CODE; }

    public String getEMP_NAME() { return EMP_NAME; }
    public void setEMP_NAME(String EMP_NAME) { this.EMP_NAME = EMP_NAME; }

    public String getFATHER_NAME() { return FATHER_NAME; }
    public void setFATHER_NAME(String FATHER_NAME) { this.FATHER_NAME = FATHER_NAME; }

    public String getDEPARTMENT() { return DEPARTMENT; }
    public void setDEPARTMENT(String DEPARTMENT) { this.DEPARTMENT = DEPARTMENT; }

    public String getDESIGNATION() { return DESIGNATION; }
    public void setDESIGNATION(String DESIGNATION) { this.DESIGNATION = DESIGNATION; }

    public String getJOINING_DATE() { return JOINING_DATE; }
    public void setJOINING_DATE(String JOINING_DATE) { this.JOINING_DATE = JOINING_DATE; }

    public String getEMP_STATUS() { return EMP_STATUS; }
    public void setEMP_STATUS(String EMP_STATUS) { this.EMP_STATUS = EMP_STATUS; }

    public String getGROSS_SALARY() { return GROSS_SALARY; }
    public void setGROSS_SALARY(String GROSS_SALARY) { this.GROSS_SALARY = GROSS_SALARY; }

    public String getBANK_NAME() { return BANK_NAME; }
    public void setBANK_NAME(String BANK_NAME) { this.BANK_NAME = BANK_NAME; }

    public String getBANK_AC_NO() { return BANK_AC_NO; }
    public void setBANK_AC_NO(String BANK_AC_NO) { this.BANK_AC_NO = BANK_AC_NO; }

    public String getEMP_CONTNO() { return EMP_CONTNO; }
    public void setEMP_CONTNO(String EMP_CONTNO) { this.EMP_CONTNO = EMP_CONTNO; }

    public String getPERSONAL_EMAIL() { return PERSONAL_EMAIL; }
    public void setPERSONAL_EMAIL(String PERSONAL_EMAIL) { this.PERSONAL_EMAIL = PERSONAL_EMAIL; }

    public String getOFFICIAL_EMAIL() { return OFFICIAL_EMAIL; }
    public void setOFFICIAL_EMAIL(String OFFICIAL_EMAIL) { this.OFFICIAL_EMAIL = OFFICIAL_EMAIL; }

    public String getEMERGENCY_NO() { return EMERGENCY_NO; }
    public void setEMERGENCY_NO(String EMERGENCY_NO) { this.EMERGENCY_NO = EMERGENCY_NO; }

    public String getEMP_IMG() { return EMP_IMG; }
    public void setEMP_IMG(String EMP_IMG) { this.EMP_IMG = EMP_IMG; }
}
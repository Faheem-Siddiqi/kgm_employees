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

        // ================= BASIC GETTERS =================

        public int getID() { return ID; }
        public void setID(int ID) { this.ID = ID; }

        public String getNID() { return NID; }
        public void setNID(String NID) { this.NID = NID; }

        public String getEMPLOYEE_CODE() { return EMPLOYEE_CODE; }
        public void setEMPLOYEE_CODE(String EMPLOYEE_CODE) { this.EMPLOYEE_CODE = EMPLOYEE_CODE; }

        public String getEMP_NAME() { return EMP_NAME; }
        public void setEMP_NAME(String EMP_NAME) { this.EMP_NAME = EMP_NAME; }


        public String getGENDER() {return GENDER;}

    public void setGENDER(String GENDER) {this.GENDER = GENDER;}

        public String getFATHER_NAME() { return FATHER_NAME; }
        public void setFATHER_NAME(String FATHER_NAME) { this.FATHER_NAME = FATHER_NAME; }

        public String getDEPARTMENT() { return DEPARTMENT; }
        public void setDEPARTMENT(String DEPARTMENT) { this.DEPARTMENT = DEPARTMENT; }

        public String getDESIGNATION() { return DESIGNATION; }
        public void setDESIGNATION(String DESIGNATION) { this.DESIGNATION = DESIGNATION; }

        public String getJOINING_DATE() { return JOINING_DATE; }
        public void setJOINING_DATE(String JOINING_DATE) { this.JOINING_DATE = JOINING_DATE; }

        public String getRESIGN_DATE() { return RESIGN_DATE; }
        public void setRESIGN_DATE(String RESIGN_DATE) { this.RESIGN_DATE = RESIGN_DATE; }

        public String getRESIGN_REASON() { return RESIGN_REASON; }
        public void setRESIGN_REASON(String RESIGN_REASON) { this.RESIGN_REASON = RESIGN_REASON; }

        public String getUNT_CODE() { return UNT_CODE; }
        public void setUNT_CODE(String UNT_CODE) { this.UNT_CODE = UNT_CODE; }

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

        public String getPERMANENT_ADR() { return PERMANENT_ADR; }
        public void setPERMANENT_ADR(String PERMANENT_ADR) { this.PERMANENT_ADR = PERMANENT_ADR; }

        // ================= DOCUMENT GETTERS =================

        public String getCNIC_COPY() { return CNIC_COPY; }
        public void setCNIC_COPY(String CNIC_COPY) { this.CNIC_COPY = CNIC_COPY; }

        public String getSS_CARD_COPY() { return SS_CARD_COPY; }
        public void setSS_CARD_COPY(String SS_CARD_COPY) { this.SS_CARD_COPY = SS_CARD_COPY; }

        public String getEOBI_CARD_COPY() { return EOBI_CARD_COPY; }
        public void setEOBI_CARD_COPY(String EOBI_CARD_COPY) { this.EOBI_CARD_COPY = EOBI_CARD_COPY; }

        public String getEMP_IMG() { return EMP_IMG; }
        public void setEMP_IMG(String EMP_IMG) { this.EMP_IMG = EMP_IMG; }



        public String getFINAL_SETTLEMENT() {
        return FINAL_SETTLEMENT;
    }
    public void setFINAL_SETTLEMENT(String FINAL_SETTLEMENT) {
        this.FINAL_SETTLEMENT = FINAL_SETTLEMENT;
    }

    public String getCLEARANCE_CERT() {
        return CLEARANCE_CERT;
    }
    public void setCLEARANCE_CERT(String CLEARANCE_CERT) {
        this.CLEARANCE_CERT = CLEARANCE_CERT;
    }

    public String getJOB_APPOINTMENT() {
        return JOB_APPOINTMENT;
    }
    public void setJOB_APPOINTMENT(String JOB_APPOINTMENT) {
        this.JOB_APPOINTMENT = JOB_APPOINTMENT;
    }

    public String getAPPLICATION_DOC() {
        return APPLICATION_DOC;
    }
    public void setAPPLICATION_DOC(String APPLICATION_DOC) {
        this.APPLICATION_DOC = APPLICATION_DOC;
    }

    public String getISSUANCE_DOC() {
        return ISSUANCE_DOC;
    }
    public void setISSUANCE_DOC(String ISSUANCE_DOC) {
        this.ISSUANCE_DOC = ISSUANCE_DOC;
    }

    public String getSETTLEMENT_DOC() {
        return SETTLEMENT_DOC;
    }
    public void setSETTLEMENT_DOC(String SETTLEMENT_DOC) {
        this.SETTLEMENT_DOC = SETTLEMENT_DOC;
    }

    public String getTRIAL_CARD() {
        return TRIAL_CARD;
    }
    public void setTRIAL_CARD(String TRIAL_CARD) {
        this.TRIAL_CARD = TRIAL_CARD;
    }

    public String getINTERVIEW_DOC() {
        return INTERVIEW_DOC;
    }
    public void setINTERVIEW_DOC(String INTERVIEW_DOC) {
        this.INTERVIEW_DOC = INTERVIEW_DOC;
    }

    public String getSERVICE_LETTER() {
        return SERVICE_LETTER;
    }
    public void setSERVICE_LETTER(String SERVICE_LETTER) {
        this.SERVICE_LETTER = SERVICE_LETTER;
    }

    public String getEXTENSION_LETTER() {
        return EXTENSION_LETTER;
    }
    public void setEXTENSION_LETTER(String EXTENSION_LETTER) {
        this.EXTENSION_LETTER = EXTENSION_LETTER;
    }

    public String getRETIREMENT_LETTER() {
        return RETIREMENT_LETTER;
    }
    public void setRETIREMENT_LETTER(String RETIREMENT_LETTER) {
        this.RETIREMENT_LETTER = RETIREMENT_LETTER;
    }

    public String getCOVID_CERT() {
        return COVID_CERT;
    }
    public void setCOVID_CERT(String COVID_CERT) {
        this.COVID_CERT = COVID_CERT;
    }

    public String getDISCIPLINARY_I() {
        return DISCIPLINARY_I;
    }
    public void setDISCIPLINARY_I(String DISCIPLINARY_I) {
        this.DISCIPLINARY_I = DISCIPLINARY_I;
    }

    public String getDISCIPLINARY_II() {
        return DISCIPLINARY_II;
    }
    public void setDISCIPLINARY_II(String DISCIPLINARY_II) {
        this.DISCIPLINARY_II = DISCIPLINARY_II;
    }

    public String getDISCIPLINARY_III() {
        return DISCIPLINARY_III;
    }
    public void setDISCIPLINARY_III(String DISCIPLINARY_III) {
        this.DISCIPLINARY_III = DISCIPLINARY_III;
    }
    }
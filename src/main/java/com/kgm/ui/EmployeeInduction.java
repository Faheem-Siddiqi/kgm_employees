
package com.kgm.ui;
import javax.swing.*;
import java.awt.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import com.kgm.ui.panel.HeaderPanel;
import com.kgm.ui.panel.FormPanel;
import com.kgm.ui.panel.DocumentPanel;

public class EmployeeInduction extends JFrame {

    private JButton nextBackBtn;
    private JButton submitBtn;

    public EmployeeInduction() {
        setTitle("Employee Form");
        setSize(1100, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setLayout(new BorderLayout());

        add(new HeaderPanel("Employee Induction"), BorderLayout.NORTH);

        // ================= CENTER =================
        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setBorder(new EmptyBorder(10, 20, 10, 20));
        centerWrapper.setOpaque(false);

        JTabbedPane tabs = new JTabbedPane();

        FormPanel formPanel = new FormPanel();
        DocumentPanel documentPanel = new DocumentPanel();

        tabs.addTab("Form", formPanel);
        tabs.addTab("Documents", documentPanel);

        centerWrapper.add(tabs, BorderLayout.CENTER);
        add(centerWrapper, BorderLayout.CENTER);

        // ================= FOOTER =================
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        footer.setBackground(Color.WHITE);

        // Next / Back Button
        nextBackBtn = new JButton("Next");

        // Submit Button
        submitBtn = new JButton("Submit");

        footer.add(nextBackBtn);
        footer.add(submitBtn);

        add(footer, BorderLayout.SOUTH);

        // ================= LOGIC =================
        tabs.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                int index = tabs.getSelectedIndex();

                if (index == 0) {
                    // FORM TAB
                    nextBackBtn.setText("Next");
                    nextBackBtn.setEnabled(true);
                    submitBtn.setEnabled(false);
                } else {
                    // DOCUMENT TAB
                    nextBackBtn.setText("Back");
                    nextBackBtn.setEnabled(true);
                    submitBtn.setEnabled(true);
                }
            }
        });

        // Navigation logic
        nextBackBtn.addActionListener(e -> {
            int index = tabs.getSelectedIndex();
            if (index == 0) {
                tabs.setSelectedIndex(1); // go to Documents
            } else {
                tabs.setSelectedIndex(0); // back to Form
            }
        });

        setVisible(true);
    }
}
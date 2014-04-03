import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.MatteBorder;
import javax.swing.border.TitledBorder;

public class Reporter extends JPanel implements ActionListener {

    private JButton parseClipBoardButton, copy;
    private Parser parser = new Parser();
    private JScrollPane scrollData = new JScrollPane();
    private JTable data;
    private static JFrame frame;
    private JPanel results = new JPanel();

    private final JComboBox<String> comboBoxOSes;
    private final JComboBox<String> comboBoxJDKs;
    private final JComboBox<String> comboBoxStacks;
    private final JComboBox<String> comboBoxDBs;

    public Reporter() {
        this.setLayout(new FlowLayout());

        JPanel options = new JPanel();
        options.setLayout(new GridLayout(2, 1));
        add(options);
        results.setLayout(new BorderLayout());
        add(results);

        JPanel wholeRun = new JPanel();
        options.add(wholeRun);

        //Create the log first, because the action listeners
        //need to refer to it.
        JTextArea log = new JTextArea(0, 0);
        log.setMargin(new Insets(0, 0, 0, 0));
        log.setEditable(false);

        comboBoxOSes = new JComboBox<String>(new String[]{"CentOS 5.9", "CentOS 6.4", "SUSE 11.1"});
        comboBoxOSes.setEditable(true);

        comboBoxJDKs = new JComboBox<String>(new String[]{"OracleJDK 1.7", "OpenJDK 7", "Oracle JDK 1.6"});
        comboBoxJDKs.setEditable(true);

        comboBoxStacks = new JComboBox<String>(new String[]{"2.1.1", "2.1", "2.0.6", "2.0", "1.3.4", "1.3.3", "1.3"});
        comboBoxStacks.setEditable(true);

        comboBoxDBs = new JComboBox<String>(new String[]{"Postgres", "MySQL", "Oracle"});
        comboBoxDBs.setEditable(true);

        parseClipBoardButton = new JButton("PARSE ANALYSIS");
        parseClipBoardButton.addActionListener(this);
        parseClipBoardButton.setAlignmentX(CENTER_ALIGNMENT);

        wholeRun.setAlignmentX(Component.CENTER_ALIGNMENT);
        wholeRun.setAlignmentY(Component.CENTER_ALIGNMENT);
        wholeRun.setLayout(new BoxLayout(wholeRun, BoxLayout.Y_AXIS));
        TitledBorder tb = new TitledBorder(new MatteBorder(1, 1, 1, 1, Color.GRAY), "Analysis reporter", TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION);
        wholeRun.setBorder(tb);
        wholeRun.add(comboBoxOSes);
        wholeRun.add(comboBoxJDKs);
        wholeRun.add(comboBoxStacks);
        wholeRun.add(comboBoxDBs);
        wholeRun.add(parseClipBoardButton);

        // results pane
        copy = new JButton("Copy to clipboard");
        copy.addActionListener(this);
        results.setVisible(false);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == parseClipBoardButton) {
            try {
                showResults(parser.parseClipBoard(
                        comboBoxOSes.getSelectedItem().toString(),
                        comboBoxJDKs.getSelectedItem().toString(),
                        comboBoxDBs.getSelectedItem().toString(),
                        comboBoxStacks.getSelectedItem().toString()));
            } catch (Exception e1) {
                showErrorDialog("Please copy to the clipboard the whole run table.\n\n" +
                        "ClipBoard chould contain whole pack of runs in such order:\n" +
                        " - installer\n" +
                        " - unsecure monitoring\n" +
                        " - secure monitoring\n" +
                        " - heavyweight\n");
            }
        } else if (e.getSource() == copy) {
            data.selectAll();
            Action copy = data.getActionMap().get("copy");
            ActionEvent ae = new ActionEvent(data, ActionEvent.ACTION_PERFORMED, "");
            copy.actionPerformed(ae);
        }
    }

    private void showResults(Object[][] result) {
        data = new JTable(result, new String[]{"Configuration", "Date", "Statistics", "Environment", "Comments"});

        results.remove(scrollData);
        results.removeAll();
        results.add(data, BorderLayout.NORTH);
        results.add(copy, BorderLayout.SOUTH);
        results.setVisible(true);

        frame.setPreferredSize(new Dimension(600, 420));
        frame.pack();
        frame.repaint();
    }


    private static void createAndShowGUI() {
        //Create and set up the window.
        frame = new JFrame("Analysis Reporter");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setPreferredSize(new Dimension(180, 200));
        //Add content to the window.
        frame.add(new Reporter());

        //Display the window.
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

    }

    private void showErrorDialog(String message) {
        final JOptionPane pane = new JOptionPane(message);
        final JDialog d = pane.createDialog((JFrame) null, "Error");
        d.setLocationRelativeTo(this);
        d.setVisible(true);
    }

    public static void main(String... args) {

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Reporter.createAndShowGUI();
            }
        });

    }

}
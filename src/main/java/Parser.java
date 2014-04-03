import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {

    private static final String CONFIGURATION_STRING = "ambari-%s - %s - %s - multiple - n/a - %s - %s - %s";
    private static final String CELL_BREAK = "\t";
    private static final String RESULT_SEPARATOR = "                    ";

    private static final Clipboard CLIPBOARD = Toolkit.getDefaultToolkit().getSystemClipboard();

    public Object[][] parseClipBoard(String os, String jdk, String db, String stack) throws IOException, UnsupportedFlavorException {
        Object[][] fullRunReport = new Object[21][5];

        String clipboardText = (String) CLIPBOARD.getData(DataFlavor.stringFlavor);
        List<String> cells = Arrays.asList(clipboardText.split(CELL_BREAK));

        Object[][][] runsResults = new Object[4][][];
        int lastHeaderId = 0;
        for (int i = 1; i < cells.size(); i++) { // starting from 2 'cause installer header is always the 1st.
            String cellNormalized = cells.get(i).replaceAll("[^0-9a-zA-Z:]", "");
            if (cellNormalized.contains("Totaltestcases")) {
                for (int j = 0; j < runsResults.length; j++) {
                    if (runsResults[j] == null) {
                        runsResults[j] = parseSingleRun(cells.subList(lastHeaderId, i + 1));
                        lastHeaderId = i;
                        break;
                    }
                }
            }
        }
        runsResults[3] = parseSingleRun(cells.subList(lastHeaderId, cells.size()));

        fullRunReport[0][0] = "Component, OS, Secure, # Of Datanodes, Installer, JDK, DB, HDP";
        fullRunReport[0][1] = new SimpleDateFormat("MM/dd/yyyy").format(Calendar.getInstance().getTime());
        fullRunReport[0][2] = "Passed/Failed/Aborted";
        fullRunReport[0][3] = "Environment";
        fullRunReport[0][4] = "Failure Comments";

        for (int i = 0; i < 4; i++) {
            String suiteName = "";
            String isSecure = "";
            switch (i) {
                case 0:
                    suiteName = "installer";
                    isSecure = "FALSE";
                    break;
                case 1:
                    suiteName = "monitoring";
                    isSecure = "FALSE";
                    break;
                case 2:
                    suiteName = "monitoring";
                    isSecure = "TRUE";
                    break;
                case 3:
                    suiteName = "heavyweight";
                    isSecure = "FALSE";
                    break;
            }
            for (int j = 0; j < 4; j++) {
                runsResults[i][0][0] = String.format(CONFIGURATION_STRING, suiteName, os, isSecure, jdk, db, stack);
                fullRunReport[1 + 5 * i + j] = runsResults[i][j];
            }
        }

        return fullRunReport;
    }


    private Object[][] parseSingleRun(List<String> runCells) throws IOException, UnsupportedFlavorException {
        Object[][] runReport = new Object[4][5];

        String newSummaryCell = runCells.get(0).replaceAll("[^0-9a-zA-Z:]", "");
        runCells.set(0, newSummaryCell);

        runReport[0][0] = " ";

        // Total test cases ran:
        Pattern p = Pattern.compile("(?<=ran:).*?(\\d+)");
        Matcher m = p.matcher(runCells.get(0));
        while (m.find()) runReport[0][1] = m.group();
        runReport[0][2] = "Total ran";
        runReport[0][3] = "Nano";
        runReport[0][4] = " ";

        p = Pattern.compile("(?<=passed:).*?(\\d+)");
        m = p.matcher(runCells.get(0));
        while (m.find()) runReport[1][1] = m.group();
        runReport[1][2] = "Passed";
        runReport[1][3] = " ";
        runReport[1][4] = " ";

        p = Pattern.compile("(?<=failed:).*?(\\d+)");
        m = p.matcher(runCells.get(0));
        while (m.find()) runReport[2][1] = m.group();
        runReport[2][2] = "Failed";
        runReport[2][3] = " ";
        runReport[2][4] = " ";

        p = Pattern.compile("(?<=cy:).*?(\\d+)");
        m = p.matcher(runCells.get(0));
        while (m.find()) runReport[3][1] = m.group();
        runReport[3][2] = "Aborted";
        runReport[3][3] = " ";
        runReport[3][4] = " ";


        int underAnalysis = Integer.parseInt(runReport[3][1].toString()) + Integer.parseInt(runReport[2][1].toString());
        Map<String, Integer> reasonsMap = new HashMap<String, Integer>();
        for (int i = 0; i < runCells.size(); i++) {
            String cellValue = runCells.get(i);
            p = Pattern.compile("((not app issue)|(app issue)|(test issue))");
            m = p.matcher(cellValue);
            while (m.find()) {
                if (m.group().contains("issue")) {
                    int similarFailures = 0;
                    if (runCells.get(i - 1).contains("similar")) {
                        String similarString = runCells.get(i - 1);
                        Pattern p1 = Pattern.compile("(?<=...)(\\d+)(?= similar)");
                        Matcher m1 = p1.matcher(similarString);
                        while (m1.find())
                            similarFailures = Integer.parseInt(m1.group());
                    }
                    Integer count = reasonsMap.get(m.group());
                    if (count == null)
                        reasonsMap.put(m.group(), 1 + similarFailures);
                    else
                        reasonsMap.put(m.group(), count + 1 + similarFailures);
                    underAnalysis = underAnalysis - 1 - similarFailures;
                }
            }
        }

        Map<String, Integer> bugsMap = new HashMap<String, Integer>();
        for (int i = 0; i < runCells.size(); i++) {
            String cellValue = runCells.get(i);
            p = Pattern.compile("\\s*(BUG-\\d+)");
            m = p.matcher(cellValue);
            while (m.find()) {
                if (m.group().startsWith("BUG-")) {
                    int similarFailures = 0;
                    if (runCells.get(i - 2).contains("similar")) {
                        String similarString = runCells.get(i - 2);
                        Pattern p1 = Pattern.compile("(?<=...)(\\d+)(?= similar)");
                        Matcher m1 = p1.matcher(similarString);
                        while (m1.find())
                            similarFailures = Integer.parseInt(m1.group());
                    }
                    Integer count = bugsMap.get(m.group());
                    if (count == null)
                        bugsMap.put(m.group(), 1 + similarFailures);
                    else
                        bugsMap.put(m.group(), count + 1 + similarFailures);
                }
            }
        }

        StringBuilder failuresReport = new StringBuilder("");
        Iterator it = bugsMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            failuresReport.append(pair.getValue());
            failuresReport.append(" - ");
            failuresReport.append(pair.getKey());
            failuresReport.append("                    ");
            it.remove(); // avoids a ConcurrentModificationException
        }
        it = reasonsMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            if (pair.getKey().equals("app issue")) continue;
            failuresReport.append(pair.getValue());
            failuresReport.append(" - ");
            failuresReport.append(pair.getKey());
            failuresReport.append(RESULT_SEPARATOR);
            it.remove(); // avoids a ConcurrentModificationException
        }
        if (underAnalysis > 0) {
            failuresReport.append(underAnalysis);
            failuresReport.append(" - ");
            failuresReport.append("under analysis");
            failuresReport.append(RESULT_SEPARATOR);
        }

        runReport[2][4] = failuresReport.toString();

        return runReport;
    }
}

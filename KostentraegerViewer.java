import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class KostentraegerViewer {

    public static void main(String[] args) throws Exception {
        File folder = new File(".");
        File[] xmlFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".xml"));
        if (xmlFiles == null || xmlFiles.length == 0) {
            JOptionPane.showMessageDialog(null, "Keine XML-Datei gefunden!");
            return;
        }

        File xmlFile = xmlFiles[0];
        List<List<String>> data = parseKostentraeger(xmlFile);
        showTable(data, xmlFile.getName());
    }

    private static List<List<String>> parseKostentraeger(File xmlFile) throws Exception {
        List<List<String>> rows = new ArrayList<>();
        List<String> header = List.of("Kostenträger-ID", "Name", "Kurzname", "Ort", "IK");
        rows.add(header);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true); // wichtig wegen ehd: Präfix
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(xmlFile);

        NodeList list = doc.getElementsByTagName("kostentraeger");
        for (int i = 0; i < list.getLength(); i++) {
            Element kt = (Element) list.item(i);
            String id = kt.getAttribute("V");

            String name = getAttr(kt, "name");
            String kurz = getAttr(kt, "kurzname");
            String ort = getAttr(kt, "CTY");
            String ik = getAttr(kt, "ik");

            rows.add(List.of(id, name, kurz, ort, ik));
        }

        return rows;
    }

    private static String getAttr(Element parent, String tagName) {
        NodeList list = parent.getElementsByTagName(tagName);
        if (list.getLength() > 0) {
            return ((Element) list.item(0)).getAttribute("V");
        }
        return "";
    }

    private static void showTable(List<List<String>> data, String title) {
        if (data.size() <= 1) {
            JOptionPane.showMessageDialog(null, "Keine Kostenträger-Daten gefunden!");
            return;
        }

        String[] columns = data.get(0).toArray(new String[0]);
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        for (int i = 1; i < data.size(); i++) {
            model.addRow(data.get(i).toArray(new String[0]));
        }

        JTable table = new JTable(model);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        // Das Suchfeld funktioniert "Live" während der Eingabe
        JTextField searchField = new JTextField();
        searchField.setToolTipText("Suchbegriff eingeben (z. B. AOK, Kiel, LKK...)");
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void filter() {
                String text = searchField.getText();
                if (text.trim().isEmpty()) sorter.setRowFilter(null);
                else sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
            }

            public void insertUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filter(); }
        });

        JButton exportButton = new JButton("Als CSV exportieren");
        exportButton.addActionListener(e -> exportToCSV(table));

        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.add(searchField, BorderLayout.CENTER);
        topPanel.add(exportButton, BorderLayout.EAST);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JFrame frame = new JFrame("Kostenträgerliste – " + title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(panel);
        frame.setSize(950, 550);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    
    private static void exportToCSV(JTable table) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("CSV-Datei speichern");
        chooser.setSelectedFile(new File("kostentraeger.csv"));
        if (chooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();

        String delimiter = JOptionPane.showInputDialog(null,
                "Bitte Trennzeichen eingeben (z. B. ; oder ,):", ";");

        if (delimiter == null || delimiter.isEmpty()) delimiter = ";";

        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            
            for (int i = 0; i < table.getColumnCount(); i++) {
                writer.print(table.getColumnName(i));
                if (i < table.getColumnCount() - 1) writer.print(delimiter);
            }
            writer.println();

            for (int r = 0; r < table.getRowCount(); r++) {
                for (int c = 0; c < table.getColumnCount(); c++) {
                    Object value = table.getValueAt(r, c);
                    String text = value == null ? "" : value.toString().replace("\"", "\"\"");
                    writer.print("\"" + text + "\"");
                    if (c < table.getColumnCount() - 1) writer.print(delimiter);
                }
                writer.println();
            }

            writer.flush();
            JOptionPane.showMessageDialog(null, "CSV erfolgreich exportiert:\n" + file.getAbsolutePath());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Fehler beim Export: " + ex.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }
}

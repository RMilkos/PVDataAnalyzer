package pvda;

import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.Scanner;


public class MainWindow {

    @FXML LineChart<String,Double> pchart, tchart, ichart;
    @FXML BarChart<String,Double> echart;
    @FXML private TextArea resultArea;
    @FXML private TextField fromValue, toValue, energyTo, energyFrom;
    @FXML TextField modifierField;
    @FXML CheckBox toggleChart;
    @FXML ChoiceBox chooseModifier;
    @FXML NumberAxis powery;
    @FXML Label sumDisplay0, sumDisplay1, sumDisplay2, sumDisplay3;

    File file;
    Scanner work_sheet;
    String FieldDelimiter = ";|/", chosenModifier = "";
    FileChooser chooser = new FileChooser();
    double[] modifier = {1, 1, 1, 1}, overallEnergy = {0, 0, 0, 0}, partEnergy = new double[96];
    int fileCount = 0, antiOverlap = 0;
    String[] paths = new String[4], name = new String[4];
    Stage modifierStage;
    DecimalFormat df = new DecimalFormat("#.##");

    public void chooseClick(ActionEvent e) throws IOException {
        if (antiOverlap > 0 && toggleChart.isSelected())
        {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Information Dialog");
            alert.setHeaderText(null);
            alert.setContentText("You can't overlap close-up charts with full charts!");
            alert.showAndWait();
            toggleChart.setSelected(false);
            return;
        }
        if (fileCount == 4)
            fileCount = 0;
        XYChart.Series power = new XYChart.Series();
        XYChart.Series temp = new XYChart.Series();
        XYChart.Series irr = new XYChart.Series();

        chooser.setTitle("Choose file");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );
        file = chooser.showOpenDialog(null);
        paths[fileCount] = file.getPath();
        double prevValue = 0;
        if(!(toggleChart.isSelected()) || fileCount == 4)
        {
            pchart.getData().clear();
            tchart.getData().clear();
            ichart.getData().clear();
            fileCount = 0;
        }
        if (fileCount == 0)
            resultArea.setText("");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Modifier.fxml"));
            loader.setController(this);
            Parent root;
            root = loader.load();
            modifierStage = new Stage();
            modifierStage.setTitle("Modifier");
            modifierStage.setScene(new Scene(root, 290, 75));
            modifierStage.setResizable(false);
            if (fileCount == 0)
            chooseModifier.setValue("Nominal power [W]");
            else
            {
                chooseModifier.setValue(chosenModifier);
                chooseModifier.setDisable(true);
            }
            modifierStage.showAndWait();
            name[fileCount] = file.getName().substring(0, file.getName().lastIndexOf("."));
            work_sheet = new Scanner(file);
            work_sheet.nextLine();
            int i = 0;
            while(work_sheet.hasNext()) {
                String[] data = work_sheet.next().split(FieldDelimiter);
                double B = Double.parseDouble(data[2].replaceAll(",","."));
                double C = Double.parseDouble(data[3].replaceAll(",","."));
                double D = Double.parseDouble(data[4].replaceAll(",","."));

                power.getData().add(new XYChart.Data(data[1], B/modifier[fileCount]));
                irr.getData().add(new XYChart.Data(data[1], C));
                temp.getData().add(new XYChart.Data(data[1], D));

                if (B < 0)
                    resultArea.appendText("["+ name[fileCount] + "] " + data[1] + " Power less than 0 \n");
                if (C < 0)
                    resultArea.appendText("["+ name[fileCount] + "] " + data[1] + " Solar irradiation less than 0 \n");
                if (B > 10 && (B < 0.95 * prevValue | B > 1.05 * prevValue))
                    resultArea.appendText("["+ name[fileCount] + "] " + data[1] + " Power output change more than 5% \n");
                if (B == 0 && C == 0 && D == 0)
                {
                    resultArea.setStyle("-fx-text-fill: red;");
                    resultArea.appendText("["+ name[fileCount] + "] " + data[1] + " Possible sensors failure \n");
                }
                if (B == 0 && C > 10)
                    i++;
                else
                    i = 0;
                if (B == 0 && C > 10 && i > 10)
                    resultArea.appendText("["+ name[fileCount] + "] " + data[1] + " No expected output power \n");
                prevValue = B;
            }
            power.setName(file.getName().substring(0, file.getName().lastIndexOf(".")));
            if(chooseModifier.getValue().equals("Nominal power [W]")) {
                powery.setLabel("Power output per nominal power");
                chosenModifier = chooseModifier.getValue().toString();
            }
            else {
                powery.setLabel("Power output per surface [W/m²]");
                chosenModifier = chooseModifier.getValue().toString();
            }
            pchart.getData().add(power);
            ichart.getData().add(irr);
            tchart.getData().add(temp);

            work_sheet.close();
            fileCount += 1;
            if (toggleChart.isSelected() && fileCount == 4)
            {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Information Dialog");
                    alert.setHeaderText(null);
                    alert.setContentText("You have reached maximum number of series.");
                    alert.showAndWait();
                    toggleChart.setSelected(false);
            }
            antiOverlap = 0;
        }
        catch(IOException eIO) {
            eIO.printStackTrace();
        }
        catch(RuntimeException eR){
            eR.printStackTrace();
        }
    }
    public void closeClick(ActionEvent e) {
        if (fileCount == 0)
        {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Information Dialog");
            alert.setHeaderText(null);
            alert.setContentText("No file chosen!");
            alert.showAndWait();
            return;
        }
        if (fromValue.getText().trim().equals("") || toValue.getText().trim().equals(""))
        {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Information Dialog");
            alert.setHeaderText(null);
            alert.setContentText("Fields must be filled.");
            alert.showAndWait();
            return;
        }
        int closeCount = 0;
        antiOverlap++;
        String f = fromValue.getText();
        String t = toValue.getText();
        String[] closeCompf = f.split(":");
        String[] closeCompt = t.split(":");
        if(Integer.parseInt(closeCompf[0]) > Integer.parseInt(closeCompt[0]) || (Integer.parseInt(closeCompf[0]) == Integer.parseInt(closeCompt[0]) && Integer.parseInt(closeCompf[1]) > Integer.parseInt(closeCompt[1])))
        {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Information Dialog");
            alert.setHeaderText(null);
            alert.setContentText("Value 'from' must be less than value 'to'.");
            alert.showAndWait();
            return;
        }
        if(closeCompf.equals(closeCompt))
        {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Information Dialog");
            alert.setHeaderText(null);
            alert.setContentText("Value 'from' must be different than value 'to'.");
            alert.showAndWait();
            return;
        }
        pchart.getData().clear();
        tchart.getData().clear();
        ichart.getData().clear();
        while(closeCount < fileCount) {
            XYChart.Series closepower = new XYChart.Series();
            XYChart.Series closetemp = new XYChart.Series();
            XYChart.Series closeirr = new XYChart.Series();
            closepower.setName(name[closeCount]);
            try {
                work_sheet = new Scanner(new File(paths[closeCount]));
            } catch (FileNotFoundException eFNF) {
                eFNF.printStackTrace();
            }
            work_sheet.nextLine();
            String checkDate = "";
            String firstCloseHour = "";
            double firstCloseB = 0, firstCloseC = 0, firstCloseD = 0;
            try {
                while (!(checkDate.equals(f))) {
                    String[] data = work_sheet.next().split(FieldDelimiter);
                    checkDate = firstCloseHour = data[1];
                    firstCloseB = Double.parseDouble(data[2].replaceAll(",", "."));
                    firstCloseC = Double.parseDouble(data[3].replaceAll(",", "."));
                    firstCloseD = Double.parseDouble(data[4].replaceAll(",", "."));
                    String[] hour = checkDate.split(":");
                    String[] fhour = f.split(":");
                    if(Integer.parseInt(hour[0]) > Integer.parseInt(fhour[0]) || (Integer.parseInt(hour[0]) == Integer.parseInt(fhour[0]) && Integer.parseInt(hour[1]) > Integer.parseInt(fhour[1])))
                    {
                        f = firstCloseHour;
                    }
                }
                closepower.getData().add(new XYChart.Data(firstCloseHour, firstCloseB/modifier[closeCount]));
                closetemp.getData().add(new XYChart.Data(firstCloseHour, firstCloseC));
                closeirr.getData().add(new XYChart.Data(firstCloseHour, firstCloseD));
                while (!(checkDate.equals(t))) {
                    String[] data = work_sheet.next().split(FieldDelimiter);
                    double B = Double.parseDouble(data[2].replaceAll(",", "."));
                    double C = Double.parseDouble(data[3].replaceAll(",", "."));
                    double D = Double.parseDouble(data[4].replaceAll(",", "."));
                    closepower.getData().add(new XYChart.Data(data[1], B/modifier[closeCount]));
                    closetemp.getData().add(new XYChart.Data(data[1], C));
                    closeirr.getData().add(new XYChart.Data(data[1], D));
                    checkDate = data[1];
                    String[] hour = checkDate.split(":");
                    String[] thour = t.split(":");
                    if(Integer.parseInt(hour[0]) > Integer.parseInt(thour[0]) || (Integer.parseInt(hour[0]) == Integer.parseInt(thour[0]) && Integer.parseInt(hour[1]) > Integer.parseInt(hour[1])-1))
                    {
                        t = checkDate;
                    }
                }
                pchart.getData().add(closepower);
                ichart.getData().add(closetemp);
                tchart.getData().add(closeirr);
            } catch (java.util.NoSuchElementException eNSE) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error Dialog");
                alert.setHeaderText(null);
                alert.setContentText("No such record in a working file!");
                alert.showAndWait();
                return;
            } catch (RuntimeException eR) {
                eR.printStackTrace();
            }
        closeCount++;
        }
        toggleChart.setSelected(false);
    }
    public void snapClick(ActionEvent e) throws IOException {
        if (fileCount == 0) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Information Dialog");
            alert.setHeaderText(null);
            alert.setContentText("No file chosen");
            alert.showAndWait();
            return;
        }
        SnapshotParameters parameters = new SnapshotParameters();
        WritableImage wi = new WritableImage(745, 485);
        WritableImage snapshot = pchart.snapshot(new SnapshotParameters(), wi);
        File output = new File("Snapshot" + new Date().getTime() + ".png");
        ImageIO.write(SwingFXUtils.fromFXImage(snapshot, null), "png", output);
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information Dialog");
        alert.setHeaderText(null);
        alert.setContentText("Snapshot saved");
        alert.showAndWait();
    }
    public void modifierClick(ActionEvent e) {
        if (modifierField.getText().trim().equals(""))
        {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Information Dialog");
            alert.setHeaderText(null);
            alert.setContentText("Please add a value.");
            alert.showAndWait();
            return;
        }

        //
        //Add value not double check
        //

        if (Double.parseDouble(modifierField.getText()) <= 0)
        {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Information Dialog");
            alert.setHeaderText(null);
            alert.setContentText("Surface cannot be 0 or less!");
            alert.showAndWait();
            return;
        }
        modifier[fileCount] = Double.parseDouble(modifierField.getText());
        modifierStage.close();
    }
    public void energyClick(ActionEvent e)
    {
    if (fileCount == 0)
    {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information Dialog");
        alert.setHeaderText(null);
        alert.setContentText("No file chosen");
        alert.showAndWait();
        return;
    }
    try {
        //Window popup
        FXMLLoader loader = new FXMLLoader(getClass().getResource("EnergyWindow.fxml"));
        loader.setController(this);
        Parent root;
        root = loader.load();
        Stage stage = new Stage();
        stage.setTitle("Energy chart");
        stage.setScene(new Scene(root, 1200, 700));
        stage.setResizable(false);
        stage.show();
        //Energy counting
        double energyAmount = 0;
        int energyCount = 0;
        double divider = 1;
        int i = 0;
        int u = 1;
        while (energyCount<fileCount)
        {
            try {
                work_sheet = new Scanner(new File(paths[energyCount]));
            } catch (FileNotFoundException eFNF) {
                eFNF.printStackTrace();
            }
            overallEnergy[energyCount] = 0;
            XYChart.Series energy = new XYChart.Series();
            energy.setName(name[energyCount]);
            work_sheet.nextLine();
            String[] data = work_sheet.next().split(FieldDelimiter);
            energyAmount += Double.parseDouble(data[2].replaceAll(",", "."));
            while (work_sheet.hasNext())
            {
                energyAmount = 0;
                divider = 0;
                while (i != u && work_sheet.hasNext())
                {
                    String[] energyData = work_sheet.next().split(FieldDelimiter);
                    energyAmount += Double.parseDouble(energyData[2].replaceAll(",", "."));
                    divider++;
                    String[] hour = energyData[1].split(":");
                    i = Integer.parseInt(hour[0]);
                }
                overallEnergy[energyCount] += energyAmount/divider;
                String[] hours = {"00:00", "01:00", "02:00", "03:00", "04:00", "05:00", "06:00", "07:00", "08:00", "09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00", "18:00", "19:00", "20:00", "21:00", "22:00", "23:00", "24:00"};
                if (energyCount == 0)
                    {
                        partEnergy[u-1] = energyAmount/divider;
                    }
                    else if (energyCount == 1)
                    {
                        partEnergy[u+23] = energyAmount/divider;
                    }
                    else if (energyCount == 2)
                    {
                        partEnergy[u+47] = energyAmount/divider;
                    }
                    else if (energyCount == 3)
                    {
                        partEnergy[u+71] = energyAmount/divider;
                    }
                energy.getData().add(new XYChart.Data(hours[u-1], energyAmount/divider));
                u++;
                if (u == 25)
                u = 1;
            }
            if (energyCount == 0)
                    sumDisplay0.setText(name[energyCount] + ": Total " + df.format(overallEnergy[energyCount]) + " Wh");
                else if (energyCount == 1)
                    sumDisplay1.setText(name[energyCount] + ": Total " + df.format(overallEnergy[energyCount]) + " Wh");
                else if (energyCount == 2)
                    sumDisplay2.setText(name[energyCount] + ": Total " + df.format(overallEnergy[energyCount]) + " Wh");
                else if (energyCount == 3)
                    sumDisplay3.setText(name[energyCount] + ": Total " + df.format(overallEnergy[energyCount]) + " Wh");
            energyCount++;
            echart.getData().addAll(energy);
        }
    }
    catch (IOException eIO) {
        eIO.printStackTrace();
    }
    catch (java.lang.RuntimeException eR)
    {
        eR.printStackTrace();
    }
}
    public void energySnap(ActionEvent e) throws IOException {
        SnapshotParameters parameters = new SnapshotParameters();
        WritableImage wi = new WritableImage(1200, 650);
        WritableImage snapshot = echart.snapshot(new SnapshotParameters(), wi);
        File output = new File("Energysnapshot" + new Date().getTime() + ".png");
        ImageIO.write(SwingFXUtils.fromFXImage(snapshot, null), "png", output);
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information Dialog");
        alert.setHeaderText(null);
        alert.setContentText("Snapshot saved");
        alert.showAndWait();
    }
    public void energySumClick (ActionEvent e) {
        if (energyTo.getText().trim().equals("") || energyFrom.getText().trim().equals(""))
        {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Information Dialog");
            alert.setHeaderText(null);
            alert.setContentText("Fields must be filled.");
            alert.showAndWait();
            return;
        }
        String f = energyFrom.getText();
        String t = energyTo.getText();
        int sumCount = 0;
        if(Integer.parseInt(f) > Integer.parseInt(t))
        {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Information Dialog");
            alert.setHeaderText(null);
            alert.setContentText("Value 'from' must be less than value 'to'.");
            alert.showAndWait();
            return;
        }
        while (sumCount < fileCount) {
                double energySum = 0;
                int modifier = 0;
                if (sumCount == 1)
                    modifier = 24;
                else if (sumCount == 2)
                    modifier = 48;
                else if (sumCount == 3)
                    modifier = 72;
                for (int i = Integer.parseInt(f); i < Integer.parseInt(t); i++)
                {
                    energySum += partEnergy[i+modifier];
                }
                if (sumCount == 0)
                    sumDisplay0.setText(name[sumCount] + ": " + "Part " + df.format(energySum) + " Wh Total " + df.format(overallEnergy[sumCount]) + " Wh");
                else if (sumCount == 1)
                    sumDisplay1.setText(name[sumCount] + ": " + "Part " + df.format(energySum) + " Wh Total "+ df.format(overallEnergy[sumCount]) + " Wh");
                else if (sumCount == 2)
                    sumDisplay2.setText(name[sumCount] + ": " + "Part " + df.format(energySum) + " Wh Total "+ df.format(overallEnergy[sumCount]) + " Wh");
                else if (sumCount == 3)
                    sumDisplay3.setText(name[sumCount] + ": " + "Part " + df.format(energySum) + " Wh Total "+ df.format(overallEnergy[sumCount]) + " Wh");
                sumCount++;
            }
        }
    }
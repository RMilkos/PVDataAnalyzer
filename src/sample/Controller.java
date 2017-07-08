package sample;

import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Scanner;

public class Controller {

    @FXML LineChart<String,Double> pchart, tchart, ichart;
    @FXML private BarChart<String,Double> echart;
    @FXML private TextArea resultArea;
    @FXML private TextField fromValue, toValue;
    @FXML CheckBox toggleChart;

    File file;
    Scanner work_sheet;
    String FieldDelimiter = ";|/";
    FileChooser chooser = new FileChooser();
    long[] rowsNumber = new long[4];
    int fileCount = 0, antiOverlap = 0;
    String[] paths = new String[4];
    String[] name = new String[4];

    @FXML
    public void chooseClick(ActionEvent e)
    {
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
        Path path = Paths.get(file.getPath());
        paths[fileCount] = file.getPath();
        try
        {
            rowsNumber[fileCount] = Files.lines(path).count();
        } catch (IOException eIO) {
            eIO.printStackTrace();
        }
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
            name[fileCount] = file.getName().substring(0, file.getName().lastIndexOf("."));
            work_sheet = new Scanner(file);
            work_sheet.nextLine();
            int i = 0;
            while(work_sheet.hasNext()) {
                String[] data = work_sheet.next().split(FieldDelimiter);
                double B = Double.parseDouble(data[2].replaceAll(",","."));
                double C = Double.parseDouble(data[3].replaceAll(",","."));
                double D = Double.parseDouble(data[4].replaceAll(",","."));

                power.getData().add(new XYChart.Data(data[1], B));
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
        }
        catch(IOException eIO) {
            eIO.printStackTrace();
        }
        catch(RuntimeException eR){
            eR.printStackTrace();
        }
    }
    @FXML
    public void closeClick(ActionEvent e) {
        int closeCount = 0;
        antiOverlap++;
        String f = fromValue.getText();
        String t = toValue.getText();
        pchart.getData().clear();
        tchart.getData().clear();
        ichart.getData().clear();
        while(closeCount<fileCount) {
            XYChart.Series closepower = new XYChart.Series();
            XYChart.Series closetemp = new XYChart.Series();
            XYChart.Series closeirr = new XYChart.Series();
            closepower.setName(name[closeCount]);
            try {
                work_sheet = new Scanner(new File(paths[closeCount]));
            } catch (FileNotFoundException eFNF) {
                eFNF.printStackTrace();
            }
            work_sheet.next();
            String checkDate = " ";
            String firstCloseHour = "";
            double firstCloseB = 0, firstCloseC = 0, firstCloseD = 0;
            try {
                while (!(checkDate.equals(f))) {
                    String[] data = work_sheet.next().split(FieldDelimiter);
                    checkDate = data[1];
                    firstCloseHour = data[1];
                    firstCloseB = Double.parseDouble(data[2].replaceAll(",", "."));
                    firstCloseC = Double.parseDouble(data[3].replaceAll(",", "."));
                    firstCloseB = Double.parseDouble(data[4].replaceAll(",", "."));
                }
                closepower.getData().add(new XYChart.Data(firstCloseHour, firstCloseB));
                closetemp.getData().add(new XYChart.Data(firstCloseHour, firstCloseC));
                closeirr.getData().add(new XYChart.Data(firstCloseHour, firstCloseD));
                while (!(checkDate.equals(t))) {
                    String[] data = work_sheet.next().split(FieldDelimiter);
                    double B = Double.parseDouble(data[2].replaceAll(",", "."));
                    double C = Double.parseDouble(data[3].replaceAll(",", "."));
                    double D = Double.parseDouble(data[4].replaceAll(",", "."));
                    closepower.getData().add(new XYChart.Data(data[1], B));
                    closetemp.getData().add(new XYChart.Data(data[1], C));
                    closeirr.getData().add(new XYChart.Data(data[1], D));
                    checkDate = data[1];
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("window.fxml"));
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
                        String[] hours = {"00:00", "01:00", "02:00", "03:00", "04:00", "05:00", "06:00", "07:00", "08:00", "09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00", "18:00", "19:00", "20:00", "21:00", "22:00", "23:00", "24:00"};
                        energy.getData().add(new XYChart.Data(hours[u], energyAmount/divider));
                        u++;
                        if (u == 25)
                            u = 0;
                    }
                    energyCount++;
                    echart.getData().addAll(energy);
            }
        }
        catch (IOException eIO) {
            eIO.printStackTrace();
        }
        catch (RuntimeException eR)
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
}
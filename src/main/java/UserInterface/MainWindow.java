package UserInterface;

import Commands.ShowPreviousCommand;
import Commands.WeekCommand;
import Commands.UpdateProgressIndicatorCommand;
import Commons.Duke;
import Commons.LookupTable;
import Commons.Storage;
import Commons.WeekList;
import DukeExceptions.DukeIOException;
import Tasks.Assignment;
import Tasks.TaskList;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;
import javafx.util.Pair;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for MainWindow. Provides the layout for the other controls.
 */
public class MainWindow extends BorderPane implements Initializable {
    private static final String NO_FIELD = "void";
    @FXML
    private Text currentTime;
    @FXML
    private Label currentWeek;
    @FXML
    private TextField userInput;
    @FXML
    private HBox progressContainer;
    @FXML
    private ListView<Text> sunEventView;
    @FXML
    private ListView<Text> monEventView;
    @FXML
    private ListView<Text> tueEventView;
    @FXML
    private ListView<Text> wedEventView;
    @FXML
    private ListView<Text> thuEventView;
    @FXML
    private ListView<Text> friEventView;
    @FXML
    private ListView<Text> satEventView;
    @FXML
    private TableView<DeadlineView> overdueTable;
    @FXML
    private TableColumn<DeadlineView, String> overdueDateColumn;
    @FXML
    private TableColumn<DeadlineView, String> overdueTaskColumn;
    @FXML
    private TableView<DukeResponseView> dukeResponseTable;
    @FXML
    private TableColumn<DukeResponseView, String> dukeResponseColumn;

    private Duke duke;
    private Storage storage;
    private ArrayList<Assignment> events;
    private ArrayList<Assignment> deadlines;
    private ArrayList<Assignment> todos;
    private ArrayList<Assignment> overdue;
    private TaskList eventsList;
    private TaskList deadlinesList;
    private static LookupTable LT;
    public static ArrayList<String> outputList = new ArrayList<>();
    private static WeekList outputWeekList = new WeekList();
    private static final Logger LOGGER = Logger.getLogger(MainWindow.class.getName());
    static {
        LT = new LookupTable();
    }


    /**
     * This method initializes the display in the window of the GUI.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            events = new ArrayList<>();
            todos = new ArrayList<>();
            deadlines = new ArrayList<>();
            setWeek(true, NO_FIELD);
            displayQuoteOfTheDay();

            retrieveList();
            openReminderBox();
            setDeadlineTable();

            overdueDateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
            overdueTaskColumn.setCellValueFactory(new PropertyValueFactory<>("task"));
            overdueTable.setItems(setOverdueTable());
            setProgressContainer();
        } catch (NullPointerException | IOException e) {
            LOGGER.log(Level.SEVERE, e.toString(), e);
        }
    }

    private void displayQuoteOfTheDay(){
        try {
            ArrayList<String> listOfQuotes = new ArrayList<>();
            InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("documents/quotes.txt");
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            StringBuffer sb = new StringBuffer();
            String firstLine;
            String line;
            while ((line = bufferedReader.readLine()) != null){
                listOfQuotes.add(line);
            }
            Random random = new Random();
            int result = random.nextInt(68);
            firstLine = listOfQuotes.get(result);
            AlertBox.display("Quote of the day", "Quote of the day !!", firstLine, Alert.AlertType.INFORMATION);
            bufferedReader.close();
            inputStreamReader.close();
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * This method creates the progress indicator for the different modules.
     * @throws IOException On reading error in the lines of the file
     */
    private void setProgressContainer() throws IOException {
        progressContainer.getChildren().clear();

        UpdateProgressIndicatorCommand updateProgressIndicatorCommand = new UpdateProgressIndicatorCommand(eventsList, deadlinesList);
        Pair<HashMap<String, String>, ArrayList<Pair<String, Pair<String, String>>>> wholeData = updateProgressIndicatorCommand.getWholeDate(eventsList, deadlinesList);
        HashMap<String, String> moduleMap = updateProgressIndicatorCommand.getModuleMap(wholeData);

        HashMap<String, Pair<Integer, Integer>> progressIndicatorValues = updateProgressIndicatorCommand.getValues(moduleMap, wholeData);
        for (String module : progressIndicatorValues.keySet()) {
            FXMLLoader fxmlLoad = new FXMLLoader(getClass().getResource("/view/ProgressIndicator.fxml"));
            Parent loads = null;
            try {
                loads = fxmlLoad.load();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, e.toString(), e);
            }
            int totalNumOfTasks = progressIndicatorValues.get(module).getKey();
            int completedValue = progressIndicatorValues.get(module).getValue();
            fxmlLoad.<ProgressController>getController().getData(module, totalNumOfTasks, completedValue);
            progressContainer.getChildren().add(loads);
        }
    }

    /**
     * Initialize Duke object in MainWindow controller with Duke object from Main.
     * @param d Duke object from Main bridge
     */
    public void setDuke(Duke d) {
        duke = d;
    }

    static ArrayList<String> filteredInput = new ArrayList<>();

    /**
     * Pulls the list from storage data and stores here.
     * @throws IOException On input error reading lines in the file
     * @throws ParseException On conversion error from string to Task object
     */
    private void retrieveList() throws DukeIOException {
        storage = new Storage();
        eventsList = new TaskList();
        deadlinesList = new TaskList();
        overdue = new ArrayList<>();
        storage.readEventList(eventsList);
        storage.readDeadlineList(deadlinesList);
        events = eventsList.getList();
        deadlines = deadlinesList.getList();
    }

    private ObservableList<DukeResponseView> betterDukeResponse = FXCollections.observableArrayList();

    private ObservableList<DeadlineView> setDeadlineTable()  {
        String to;
        String description;
        String activity;
        ObservableList<DeadlineView> deadlineViews = FXCollections.observableArrayList();
        for (Assignment task : deadlines) {
            activity = task.toString();
            DateFormat dateFormat = new SimpleDateFormat("E dd/MM/yyyy hh:mm a");
            DateFormat timeFormat= new SimpleDateFormat("dd/MM/yyyy HH:mm");
            Date date = null;
            try {
                date = dateFormat.parse(activity.substring(activity.indexOf("by:") + 4, activity.indexOf(')')));
            } catch (ParseException e) {
                LOGGER.log(Level.SEVERE, e.toString(), e);
            }
            to = timeFormat.format(date);
            description = task.getDescription();
            if (overdueCheck(date) && activity.contains("\u2718")) {
                overdue.add(task);
            } else {
                deadlineViews.add(new DeadlineView(to, description));
            }
        }
        return deadlineViews;
    }

    private ObservableList<DeadlineView> setOverdueTable() {
        String daysDue;
        String description;
        String activity;
        ObservableList<DeadlineView> overdueViews = FXCollections.observableArrayList();
        for (Assignment task : overdue) {
            activity = task.toString();
            DateFormat dateFormat = new SimpleDateFormat("E dd/MM/yyyy hh:mm a");
            Date date = null;
            try {
                date = dateFormat.parse(activity.substring(activity.indexOf("by:") + 4, activity.indexOf(')')));
            } catch (ParseException e) {
                LOGGER.log(Level.SEVERE, e.toString(), e);
            }
            daysDue = String.valueOf(daysBetween(date));
            description = task.getDescription();
            overdueViews.add(new DeadlineView(daysDue, description));
        }
        return overdueViews;
    }

    private void openReminderBox() {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        Date date = new Date();
        for (int i = 0; i < todos.size(); i++) {
            if (todos.get(i).getDescription().contains("(from") && todos.get(i).getDescription().contains("to")) {
                String description = todos.get(i).getDescription();
                int index = description.indexOf("(from");
                String taskDescription = description.substring(0, index);
                description = description.replace(taskDescription, "");
                description = description.replace("(from", "").trim();
                String[] dateString = description.split(" to ", 2);
                String startDate = dateString[0];
                String endDate = dateString[1].replace(")", "").trim();

                if (formatter.format(date).equals(startDate)) {
                    AlertBox.display("Reminder Alert", " To Do Within Period Task: " + taskDescription,
                            "Reminder starts today. On: " + startDate, Alert.AlertType.INFORMATION);

                } else if(formatter.format(date).equals(endDate)) {
                    AlertBox.display("Reminder Alert", "To Do Within Period Task: " + taskDescription,
                            "Reminder ends today. On: " + endDate, Alert.AlertType.INFORMATION);

                }
            }
        }
    }

    private void setDukeResponse() {
        dukeResponseTable.getColumns().clear();
//        TableColumn dukeIndexColumn = new TableColumn<>();
//        dukeIndexColumn.setText("Index");
//        dukeIndexColumn.setMinWidth(35);
//        dukeIndexColumn.setCellValueFactory(new PropertyValueFactory<>("index"));
        dukeResponseColumn = new TableColumn<>();
        dukeResponseColumn.setText("Duke Response");
        dukeResponseColumn.setSortable(false);
        dukeResponseColumn.setCellValueFactory(new PropertyValueFactory("response"));
        dukeResponseTable.setItems(betterDukeResponse);
        dukeResponseTable.getColumns().add(dukeResponseColumn);
    }

    @FXML
    private void handleUserInput() throws IOException {
        String input = userInput.getText();
        String response = duke.getResponse(input);
        if(input.startsWith("Week")) {
            Integer digit = -1;
            boolean isDigit = true;
            try {
                String strInput = input.replaceFirst("Week", "");
                if(!strInput.isEmpty()) {
                    if(strInput.charAt(0) == ' ') {
                        strInput = strInput.trim();
                        digit = Integer.parseInt(strInput);
                    } else {
                        isDigit = false;
                    }
                }
            } catch (NumberFormatException e){
                isDigit = false;
                userInput.clear();
                throw new NumberFormatException("Invalid Week");
            } finally {
                if (isDigit && digit > 0 && digit < 14) {
                    week = "Week " + digit;
                    setWeek(false, week);
                }
            }
        }

        duke.getResponse(week);
        outputWeekList = WeekCommand.getWeekList();
        updateListView();

        outputList = ShowPreviousCommand.getOutputList();
        setDeadlineTable();
        overdueDateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        overdueTaskColumn.setCellValueFactory(new PropertyValueFactory<>("task"));
        overdueTable.setItems(setOverdueTable());

        retrieveList();
        setProgressContainer();
        if(!response.isEmpty()) {
            Text temp = new Text(response);
            temp.setWrappingWidth(dukeResponseColumn.getWidth() - 15);
            Integer index = betterDukeResponse.size() + 1;
            betterDukeResponse.add(new DukeResponseView(index.toString(), temp));
            setDukeResponse();
        }

        if (userInput.getText().equals("bye")) {
            PauseTransition delay = new PauseTransition(Duration.seconds(1));
            delay.setOnFinished( event -> Platform.exit() );
            delay.play();
        }
        userInput.clear();

        if (input.contains("retrieve/previous")) {
            String previousInput = Duke.getPreviousInput();
            userInput.setText(previousInput);
        } else if (input.startsWith("retrieve/ft ")) {
            String selectedOption = Duke.getSelectedOption();
            userInput.setText(selectedOption);
        }
    }

    private boolean overdueCheck(Date date) {
        Calendar c = Calendar.getInstance();
        Date startOfWeek = c.getTime();
        if (date.before(startOfWeek)) {
            return true;
        } else return false;
    }

    private long daysBetween(Date date) {
        Date currentDate = new Date();
        return (currentDate.getTime() - date.getTime()) / (1000 * 60 * 60 * 24);
    }

    private String week = NO_FIELD;

    /**
     * This method updates currentWeek Label.
     * @param onStart The flag which indicates program startup
     * @param selectedWeek The week selected
     */
    private void setWeek(Boolean onStart,String selectedWeek){
        if(onStart){
            Date dateTime = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
            String date = dateFormat.format(dateTime);
            selectedWeek = LT.getValue(date);
            currentWeek.setText(selectedWeek + " ( " + LT.getValue(selectedWeek.toLowerCase()) + " )");
            week = selectedWeek;
            currentWeek.setFont(Font.font("Verdana", FontWeight.BOLD, FontPosture.ITALIC,30));
            currentWeek.setTextFill(Color.GOLDENROD);
        }
        else{
            currentWeek.setText(selectedWeek + " ( " + LT.getValue(selectedWeek.toLowerCase()) + " )");
            week = selectedWeek;
        }
    }

    private void updateListView(){
        monEventView.setItems(outputWeekList.getMonList());
        tueEventView.setItems(outputWeekList.getTueList());
        wedEventView.setItems(outputWeekList.getWedList());
        thuEventView.setItems(outputWeekList.getThuList());
        friEventView.setItems(outputWeekList.getFriList());
        satEventView.setItems(outputWeekList.getSatList());
        sunEventView.setItems(outputWeekList.getSunList());
    }
}
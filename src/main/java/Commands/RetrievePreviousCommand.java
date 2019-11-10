package Commands;

import Commons.DukeConstants;
import Commons.DukeLogger;
import Commons.Storage;
import Commons.UserInteraction;
import DukeExceptions.DukeInvalidCommandException;
import DukeExceptions.DukeNoValidDataException;
import Tasks.TaskList;
import java.util.ArrayList;
import java.util.logging.Logger;

public class RetrievePreviousCommand extends Command {
    private String fullCommand;
    public static String retrievedOutput;
    private final Logger LOGGER = DukeLogger.getLogger(RetrievePreviousCommand.class);
    private static boolean isValid = true;

    /**
     * Creates a RetrievePreviousCommand object.
     * @param fullCommand The user's input
     */
    public RetrievePreviousCommand(String fullCommand) {
        this.fullCommand = fullCommand;
    }

    /**
     * Retrieves the chosen input that the user wish to get.
     * @param ui The Ui object to display the message for chosen input
     * @return This returns the method in the Ui object which returns the string to display retrieve previous message
     * @throws DukeInvalidCommandException on emtpy list and invalid index input
     */
    @Override
    public String execute(TaskList events, TaskList deadlines, UserInteraction ui, Storage storage) throws DukeInvalidCommandException, DukeNoValidDataException {
        fullCommand = fullCommand.replace("retrieve/previous", "");

        if (!fullCommand.isEmpty()) {
            char checkSpace = fullCommand.charAt(0);
            if (checkSpace != ' ') {
                throw new DukeInvalidCommandException(DukeConstants.INVALID_WITHOUT_SPACE);
            }
        }

        fullCommand = fullCommand.trim();
        if (fullCommand.length() == 0) {
            isValid = false;
            throw new DukeInvalidCommandException(DukeConstants.INVALID_EMPTY_NUMBER);
        }

        ArrayList<String> retrievedList;
        retrievedList = ShowPreviousCommand.getOutputList();
        int size = retrievedList.size();
        if (size == 0) {
            isValid = false;
            throw new DukeNoValidDataException(DukeConstants.NO_PREVIOUS_COMMAND_TO_GET_LIST);
        }

        boolean isNumber = true;
        int intFullCommand = 0;
        try {
            intFullCommand = Integer.parseInt(fullCommand);
        } catch (NumberFormatException e) {
            LOGGER.severe("Unable to parse string to integer");
            isNumber = false;
            isValid = false;
            throw new DukeInvalidCommandException(DukeConstants.INVALID_STRING_SHOULD_BE_INTEGER);
        }

        if (isNumber) {
            if (intFullCommand <= 0 ) {
                isValid = false;
                throw new DukeInvalidCommandException("Please enter a valid integer x between 0 to " + size);
            } else if (intFullCommand > size) {
                isValid = false;
                throw new DukeInvalidCommandException("There are only " + size + " of previous commands."
                        + "Please enter a valid number less than or equal to " + size + " .");
            }
            int index = intFullCommand - 1;
            retrievedOutput = retrievedList.get(index);
            isValid = true;
        }
        return ui.showChosenPreviousChoice(retrievedOutput);
    }

    public static String getChosenOutput() {
        return retrievedOutput;
    }

    public static boolean isValid() {
        return isValid;
    }
}
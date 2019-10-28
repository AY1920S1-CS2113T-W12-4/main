package Commands;

import Commons.LookupTable;
import Commons.Storage;
import Commons.Ui;
import Tasks.Event;
import Tasks.TaskList;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Represents the command to add recurring tasks to the events TaskList.
 */
public class RecurringCommand extends Command {
    private final String description;
    private final String startTimeString;
    private final String endTimeString;
    private final String endDateString;
    private String startDateString;

    /**
     * Creates RecurringCommand object.
     * @param description Description of a task
     * @param startDateString Start date of a task
     * @param endDateString End date of a task
     * @param startTimeString Start time of a task
     * @param endTimeString End time of a task
     */
    public RecurringCommand(String description, String startDateString, String endDateString, String startTimeString, String endTimeString) {
        this.description = description;
        this.startDateString = startDateString;
        this.endDateString = endDateString;
        this.startTimeString = startTimeString;
        this.endTimeString = endTimeString;
    }

    private Date getNextWeekDate (Date inDate) {
        Date nextWeek = new Date(inDate.getTime() + 7 * 24 * 60 * 60 * 1000);
        return nextWeek;
    }

    @Override
    public String execute(LookupTable LT,TaskList events, TaskList deadlines, Ui ui, Storage storage) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("E dd/MM/yyyy");
        Date startDate = dateFormat.parse(startDateString);
        Date endDate = dateFormat.parse(endDateString);
        String oldStartDateString = startDateString;
        Date startOfFollowingWeek;

        do {
            events.addTask(new Event(description, startDateString, startTimeString, endTimeString));
            storage.updateEventList(events);
            startOfFollowingWeek = getNextWeekDate(startDate);
            startDateString = dateFormat.format(startOfFollowingWeek);
            startDate = startOfFollowingWeek;
        }
        while (startOfFollowingWeek.before(endDate) || startOfFollowingWeek.equals(endDate));

        return ui.showRecurring(description, oldStartDateString, endDateString);
    }
}

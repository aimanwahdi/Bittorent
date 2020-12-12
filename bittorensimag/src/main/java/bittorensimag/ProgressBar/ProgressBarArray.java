package bittorensimag.ProgressBar;

import java.text.DecimalFormat;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

import org.apache.log4j.Logger;

public class ProgressBarArray implements AutoCloseable {
    private static final Logger LOG = Logger.getLogger(ProgressBarArray.class);
    ArrayList<ProgressBar> pbArray = new ArrayList<ProgressBar>();

    /**
     * Creates a progress bar array with specific taskArray names and initial
     * maximum value.
     * 
     * @param task       Task name
     * @param initialMax Initial maximum value
     */
    public ProgressBarArray(ArrayList<String> taskArray, long initialMax) {
        for (int i = 0; i < taskArray.size(); i++) {
            pbArray.add(new ProgressBar(taskArray.get(i), initialMax));
        }
    }

    /**
     * Creates a progress bar with the specific name, initial maximum value,
     * customized update interval (default 1s), and the provided progress bar
     * renderer ({@link ProgressBarRenderer}) and consumer
     * ({@link ProgressBarConsumer}).
     * 
     * @param taskArray            ArrayList of task names
     * @param initialMax           Initial maximum value
     * @param updateIntervalMillis Update time interval (default value 1000ms)
     * @param processed            Initial completed process value
     * @param elapsed              Initial elapsedBeforeStart second before
     * @param speedUnit
     * @param speedFormat
     * @param showSpeed
     * @param unitSize
     * @param unitName
     * @param style
     * @param consumer
     */
    public ProgressBarArray(ArrayList<String> taskArray, long initialMax, int updateIntervalMillis, long processed,
            Duration elapsed, ProgressBarStyle style, String unitName, long unitSize, boolean showSpeed,
            DecimalFormat speedFormat, ChronoUnit speedUnit, ProgressBarConsumer consumer) {
        for (int i = 0; i < taskArray.size(); i++) {
            pbArray.add(new ProgressBar(taskArray.get(i), initialMax, updateIntervalMillis, processed, elapsed,
                    new DefaultProgressBarRenderer(style, unitName, unitSize, showSpeed, speedFormat, speedUnit),
                    consumer == null ? Util.createConsoleConsumer() : consumer));
        }
    }

    /**
     * Sets the extra message at the end of the progress bar.
     * 
     * @param msg New message
     */
    public ProgressBarArray setExtraMessage(String msg) {
        for (ProgressBar progressBar : pbArray) {
            progressBar.setExtraMessage(msg);
        }
        return this;
    }

    /**
     * Sets the extra message at the end of the progress bar.
     * 
     * @param pbBuilder Base builder
     * @param task      New message
     */
    public void add(ProgressBarBuilder pbBuilder, String task) {
        pbArray.add(pbBuilder.setTaskName(task).build());
    }

    /**
     * <p>
     * Stops this progress bar, effectively stops tracking the underlying process.
     * </p>
     * <p>
     * Implements the {@link AutoCloseable} interface which enables the
     * try-with-resource pattern with progress bars.
     * </p>
     * 
     * @since 0.7.0
     */
    @Override
    public void close() {
        for (ProgressBar pb : pbArray) {
            pb.close();
        }

    }

    public ProgressBar getByName(String string) {
        for (ProgressBar progressBar : pbArray) {
            if (progressBar.getTaskName().compareTo(string) == 0) {
                return progressBar;
            }
        }
        LOG.error("Could not get the progressBar by its taskName");
        return null;
    }
}

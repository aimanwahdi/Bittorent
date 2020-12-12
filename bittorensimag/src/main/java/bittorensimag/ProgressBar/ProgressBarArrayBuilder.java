package bittorensimag.ProgressBar;

import java.text.DecimalFormat;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

import bittorensimag.ProgressBar.DefaultProgressBarRenderer;
import bittorensimag.ProgressBar.ProgressBarConsumer;
import bittorensimag.ProgressBar.ProgressBarStyle;

public class ProgressBarArrayBuilder {

    private ArrayList<String> taskArray = new ArrayList<String>();
    private long initialMax = -1;
    private int updateIntervalMillis = 1000;
    private ProgressBarStyle style = ProgressBarStyle.COLORFUL_UNICODE_BLOCK;
    private ProgressBarConsumer consumer = null;
    private String unitName = "";
    private long unitSize = 1;
    private boolean showSpeed = false;
    private DecimalFormat speedFormat;
    private ChronoUnit speedUnit = ChronoUnit.SECONDS;
    private long processed = 0;
    private Duration elapsed = Duration.ZERO;

    public ProgressBarArrayBuilder() {
    }

    public ProgressBarArrayBuilder setTaskName(ArrayList<String> taskArray) {
        this.taskArray = taskArray;
        return this;
    }

    public ProgressBarArrayBuilder setInitialMax(long initialMax) {
        this.initialMax = initialMax;
        return this;
    }

    public ProgressBarArrayBuilder setStyle(ProgressBarStyle style) {
        this.style = style;
        return this;
    }

    public ProgressBarArrayBuilder setUpdateIntervalMillis(int updateIntervalMillis) {
        this.updateIntervalMillis = updateIntervalMillis;
        return this;
    }

    public ProgressBarArrayBuilder setConsumer(ProgressBarConsumer consumer) {
        this.consumer = consumer;
        return this;
    }

    public ProgressBarArrayBuilder setUnit(String unitName, long unitSize) {
        this.unitName = unitName;
        this.unitSize = unitSize;
        return this;
    }

    public ProgressBarArrayBuilder showSpeed() {
        return showSpeed(new DecimalFormat("#.0"));
    }

    public ProgressBarArrayBuilder showSpeed(DecimalFormat speedFormat) {
        this.showSpeed = true;
        this.speedFormat = speedFormat;
        return this;
    }

    public ProgressBarArrayBuilder setSpeedUnit(ChronoUnit speedUnit) {
        this.speedUnit = speedUnit;
        return this;
    }

    /**
     * Sets elapsedBeforeStart duration and number of processed units.
     * 
     * @param processed amount of processed units
     * @param elapsed   duration of
     */
    public ProgressBarArrayBuilder startsFrom(long processed, Duration elapsed) {
        this.processed = processed;
        this.elapsed = elapsed;
        return this;
    }

    public ProgressBarArray build() {
        return new ProgressBarArray(taskArray, initialMax, updateIntervalMillis, processed, elapsed, style, unitName,
                unitSize, showSpeed, speedFormat, speedUnit, consumer);
    }
}

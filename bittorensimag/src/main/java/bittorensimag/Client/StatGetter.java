package bittorensimag.Client;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Scanner;

import com.sun.management.OperatingSystemMXBean;

import org.apache.log4j.Logger;

public class StatGetter {
    private static final Logger LOG = Logger.getLogger(StatGetter.class);
    private static final int KB = 1024;
    private static final int MB = 1024 * 1024;
    private static final int GB = 1024 * 1024 * 1024;

    public static double getUsedMemory() {
        double totalMemory = 0, usedMemory = 0;

        if (isWindows()) {
            OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            totalMemory = (double) (osBean.getTotalPhysicalMemorySize() / MB);
            usedMemory = (double) ((totalMemory - (osBean.getFreePhysicalMemorySize() / MB)));
        } else {
            String fName = "/proc/meminfo";
            try {
                FileInputStream f = new FileInputStream(fName);

                /*
                 * $ cat /proc/meminfo MemTotal: 2056964 kB MemFree: 16716 kB Buffers: 9776 kB
                 * Cached: 127220 kB
                 */
                Scanner scanner = new Scanner(f).useDelimiter("\\D+");
                try {
                    long memTotal = scanner.nextLong();
                    long memFree = scanner.nextLong();
                    long memAvailable = scanner.nextLong();
                    long buffers = scanner.nextLong();
                    long cached = scanner.nextLong();
                    long swapCached = scanner.nextLong();
                    long active = scanner.nextLong();
                    long inactive = scanner.nextLong();
                    long activeAnon = scanner.nextLong();
                    long inactiveAnon = scanner.nextLong();
                    long activeFile = scanner.nextLong();
                    long inactiveFile = scanner.nextLong();
                    long unevitable = scanner.nextLong();
                    long mlocked = scanner.nextLong();
                    long swapTotal = scanner.nextLong();
                    long swapFree = scanner.nextLong();
                    long dirty = scanner.nextLong();
                    long writeback = scanner.nextLong();
                    long anonPages = scanner.nextLong();
                    long mapped = scanner.nextLong();
                    long shmem = scanner.nextLong();
                    long kReclamable = scanner.nextLong();
                    long slab = scanner.nextLong();

                    totalMemory = memTotal / KB;
                    usedMemory = (memTotal - (memFree + buffers + cached + slab)) / KB;

                    // Truncate doubles to 3 digits
                    totalMemory = StatGetter.truncate(totalMemory, 3);
                    usedMemory = StatGetter.truncate(usedMemory, 3);
                } catch (Exception ex) {
                    LOG.error("Could not calculate memory usage.", ex);
                } finally {
                    scanner.close();
                }
            } catch (IOException ex) {
                LOG.error("Could not calculate memory usage.", ex);
            }
        }

        // if (Logger.getRootLogger().getLevel() == Level.INFO) {
        // LOG.info("[Total Memory Used] " + usedMemory + " / " + totalMemory + " GB");
        // }
        // double memoryPercent = (usedMemory / totalMemory) * 100;
        // // truncate to 3 digits
        // double truncatedMemoryPercent = StatGetter.truncate(memoryPercent, 3);
        // if (Logger.getRootLogger().getLevel() == Level.INFO) {
        // LOG.info("[Total Memory Used] " + truncatedMemoryPercent + " %");
        // }
        return usedMemory;
    }

    public static double getTotalMemory() {
        double totalMemory = 0;

        if (isWindows()) {
            OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            totalMemory = (double) (osBean.getTotalPhysicalMemorySize() / MB);
        } else {
            String fName = "/proc/meminfo";
            try {
                FileInputStream f = new FileInputStream(fName);

                /*
                 * $ cat /proc/meminfo MemTotal: 2056964 kB MemFree: 16716 kB Buffers: 9776 kB
                 * Cached: 127220 kB
                 */
                Scanner scanner = new Scanner(f).useDelimiter("\\D+");
                try {
                    long memTotal = scanner.nextLong();

                    totalMemory = memTotal / KB;

                    // Truncate doubles to 3 digits
                    totalMemory = StatGetter.truncate(totalMemory, 3);
                } catch (Exception ex) {
                    LOG.error("Could not get total memory", ex);
                } finally {
                    scanner.close();
                }
            } catch (IOException ex) {
                LOG.error("Could not get total memory", ex);
            }
        }
        return totalMemory;
    }

    private static double truncate(double toBeTruncated, int digits) {
        double truncatedDouble = BigDecimal.valueOf(toBeTruncated).setScale(digits, RoundingMode.HALF_UP).doubleValue();
        return truncatedDouble;
    }

    public static double getLoadAverage() {
        double loadAvg = (double) ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
        // assume system cores = available cores to JVM
        int cores = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();

        // if (Logger.getRootLogger().getLevel() == Level.INFO) {
        // LOG.info("[CPU Average Load] " + loadAvg + " on " + cores + " cores");
        // }

        double loadAvgPercentage = (loadAvg / cores) * 100;
        // if (Logger.getRootLogger().getLevel() == Level.INFO) {
        // LOG.info("[CPU Percentage Load] " + loadAvgPercentage + " % on " + cores + "
        // cores");
        // }
        return loadAvgPercentage;
    }

    private static boolean isWindows() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.indexOf("win") >= 0;
    }

    public static void clearScreen() {
        // Clears Screen in java
        try {
            if (System.getProperty("os.name").contains("Windows"))
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            else
                // Runtime.getRuntime().exec("clear");
                System.out.print("\033[H\033[2J");
            System.out.flush();

        } catch (IOException | InterruptedException ex) {
            LOG.error("Error trying to clear screen");
        }
    }
}
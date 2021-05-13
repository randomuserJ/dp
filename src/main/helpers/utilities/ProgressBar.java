package main.helpers.utilities;

public class ProgressBar {
    private static final int DEFAULT_BAR_SIZE = 20;
    private final double stepSize;
    private final String runningProcess;
    private final int iterationCount;
    private final boolean simpleMode;
    private String progressBar;
    private double printedProgress;

    /**
     * Creates a new instance of ProgressBar.
     * @param iterations the number of expected iterations
     * @param runningProcess the name of the running process *OPTIONAL*
     * @param simpleMode true for activating multiple lined progressBar with
     *                  pre-defined format *OPTIONAL*
     */
    public ProgressBar(int iterations, String runningProcess, boolean simpleMode) {
        this.runningProcess = runningProcess;
        this.iterationCount = iterations;
        this.printedProgress = 0;
        this.simpleMode = simpleMode;
        this.stepSize = (double)100 / DEFAULT_BAR_SIZE;
        this.progressBar = initBar(simpleMode);
    }

    public ProgressBar(int barSize, String runningProcess) {
        this(barSize, runningProcess, true);
    }

    public ProgressBar(int barSize) {
        this(barSize, "Process");
    }

    /**
     * Prints the actual state of running process. The loaded value of progress bar will
     * be calculated from the current iteration.
     * @param iteration the number of current running iteration
     */
    public void updateBar(int iteration) {
        double percentage = (double)iteration / (iterationCount-1) * 100;

        if (percentage >= this.printedProgress + this.stepSize)
            showProgress(percentage);
        if (percentage >= 100)
            System.out.println();
    }

    /**
     * Shows the percentage value of process by progress bar.
     * @param progress percentage value of process [0-100]
     */
    private void showProgress(double progress) {
        double percentagePrinted = this.printedProgress;
        if (simpleMode) {
            while (progress >= (percentagePrinted += stepSize)) {
                System.out.print('#');
                this.printedProgress = percentagePrinted;
            }
            return;
        }

        int barIndex = 0;
        StringBuilder sb = new StringBuilder(this.progressBar);
        System.out.print(runningProcess + " in progress: [");

        while (progress >= (percentagePrinted += stepSize)) {
            sb.setCharAt(barIndex++, '#');
            this.printedProgress = percentagePrinted;
        }

        this.progressBar = sb.toString();
        System.out.println(this.progressBar + "]");
    }

    /**
     * Initialize the progress bar. Required only for advanced mode.
     * @return the string value of progress bar of pre-defined length.
     */
    private String initBar(boolean simpleMod) {
        if (simpleMod) {
            System.out.printf("%s in progress [%d#]: ", this.runningProcess, DEFAULT_BAR_SIZE);
            return "";
        }

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < DEFAULT_BAR_SIZE; i++) {
            sb.append(' ');
        }

        return sb.toString();
    }
}

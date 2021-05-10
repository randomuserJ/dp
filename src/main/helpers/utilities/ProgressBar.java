package main.helpers.utilities;

public class ProgressBar {
    private static final int DEFAULT_BAR_SIZE = 20;
    private final double stepSize;
    private final String runningProcess;
    private final int iterationCount;
    private final boolean simpleMod;
    private String progressBar;
    private double printedProgress;

    public ProgressBar(int barSize, String runningProcess, boolean simpleMod) {
        this.runningProcess = runningProcess;
        this.iterationCount = barSize;
        this.printedProgress = 0;
        this.simpleMod = simpleMod;
        this.stepSize = (double)100 / DEFAULT_BAR_SIZE;
        this.progressBar = initBar(simpleMod);
    }

    public ProgressBar(int barSize, String runningProcess) {
        this(barSize, runningProcess, true);
    }

    public ProgressBar(int barSize) {
        this(barSize, "Process");
    }

    public void updateBar(int iteration) {
        double percentage = (double)iteration / (iterationCount-1) * 100;

        if (percentage >= this.printedProgress + this.stepSize)
            showProgress(percentage);
        if (percentage >= 100)
            System.out.println();
    }

    private void showProgress(double progress) {
        double percentagePrinted = this.printedProgress;
        if (simpleMod) {
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

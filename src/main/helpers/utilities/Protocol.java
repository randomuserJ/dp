package main.helpers.utilities;

public class Protocol {

    private static final int LINE_LENGTH = 80;

    /**
     * Prints the information message. The message format will lok like "INFO: {message}".
     */
    public static void printInfoMessage(String message) {
        printMessage("info", message, false);
    }

    /**
     * Prints the warning message. The message format will look like "WARNING: {message}".
     */
    public static void printWarningMessage(String message) {
        printMessage("warning", message, false);
    }

    /**
     * Prints the error message. The message format will look like "ERROR: {message}".
     * Some tools even add a red color to error messages.
     */
    public static void printErrorMessage(String message) {
        printMessage("error", message, true);
    }

    /**
     * Prints the border of section of pre-defined length.
     * User may specify a name of section, which will be printed in the middle of border line.
     * The format of section may look like:
     * <p>---- SEC 1 ----</p>
     * <p>content</p>
     * <p>---------------</p>
     * @param caption the caption of the new section or empty string for ending line
     */
    public static void printSection(String caption) {
        StringBuilder sb = new StringBuilder();

        if (caption == null || caption.isEmpty()) {
            for (int i = 0; i < LINE_LENGTH; i++)
                sb.append("-");

            System.out.println(sb.toString());
            return;
        }

        caption = "  " + caption + "  ";
        int remainingSpace = LINE_LENGTH - caption.length();

        for (int i = 0; i < remainingSpace - remainingSpace / 2; i++)
            sb.append("-");

        sb.append(caption);

        for (int i = 0; i < remainingSpace / 2; i++)
            sb.append("-");

        System.out.println(sb.toString());
    }

    /**
     * Prints the message with specific prefix.
     */
    private static void printMessage(String prefix, String message, boolean error) {
        if (error)
            System.err.println(prefix.toUpperCase() + ": " + message);
        else
            System.out.println(prefix.toUpperCase() + ": " + message);
    }
}

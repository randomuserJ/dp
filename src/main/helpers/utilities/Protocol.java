package main.helpers.utilities;

public class Protocol {

    private static final int LINE_LENGTH = 80;

    public static void printInfoMessage(String message) {
        printMessage("info", message, false);
    }

    public static void printWarningMessage(String message) {
        printMessage("warning", message, false);
    }

    public static void printErrorMessage(String message) {
        printMessage("error", message, true);
    }

    public static void printSection(String message) {
        StringBuilder sb = new StringBuilder();

        if (message == null || message.isEmpty()) {
            for (int i = 0; i < LINE_LENGTH; i++)
                sb.append("-");

            System.out.println(sb.toString());
            return;
        }

        message = "  " + message + "  ";
        int remainingSpace = LINE_LENGTH - message.length();

        for (int i = 0; i < remainingSpace - remainingSpace / 2; i++)
            sb.append("-");

        sb.append(message);

        for (int i = 0; i < remainingSpace / 2; i++)
            sb.append("-");

        System.out.println(sb.toString());
    }

    private static void printMessage(String prefix, String message, boolean error) {
        if (error)
            System.err.println(prefix.toUpperCase() + ": " + message);
        else
            System.out.println(prefix.toUpperCase() + ": " + message);
    }
}

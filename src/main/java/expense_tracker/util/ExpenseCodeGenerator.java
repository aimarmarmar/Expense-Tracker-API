package expense_tracker.util;

import java.security.SecureRandom;

public class ExpenseCodeGenerator {

    private static final String characterSet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 4;
    private static final int TOTAL_SEGMENTS = 4;
    private static final SecureRandom random = new SecureRandom();

    public static String generate() {

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < TOTAL_SEGMENTS; i++) {
            for (int j = 0; j < CODE_LENGTH; j++) {
                int randomIndex = random.nextInt(characterSet.length());
                sb.append(characterSet.charAt(randomIndex));
            }
            if (i < TOTAL_SEGMENTS - 1) {
                sb.append("-");
            }
        }
        return sb.toString();
    }
}

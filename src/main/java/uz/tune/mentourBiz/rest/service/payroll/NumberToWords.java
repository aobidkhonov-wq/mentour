package uz.tune.mentourBiz.rest.service.payroll;

/**
 * Spells an amount out for the payslip ("Two million seven hundred sixty thousand sum only"). Payslips
 * are handed to people and signed, and the written amount is what stops a printed figure being altered.
 */
final class NumberToWords {

    private static final String[] UNITS = {
            "", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
            "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen",
            "nineteen"};

    private static final String[] TENS = {
            "", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety"};

    private static final String[] SCALES = {"", "thousand", "million", "billion", "trillion"};

    private NumberToWords() {
    }

    static String spell(long amount, String currencyWord) {
        if (amount == 0) return capitalise("zero " + currencyWord + " only");

        StringBuilder sb = new StringBuilder();
        if (amount < 0) {
            sb.append("minus ");
            amount = -amount;
        }
        sb.append(spellNumber(amount)).append(' ').append(currencyWord).append(" only");
        return capitalise(sb.toString());
    }

    private static String spellNumber(long amount) {
        // Split into groups of three digits and name each with its scale, biggest first.
        StringBuilder sb = new StringBuilder();
        int scale = 0;
        java.util.Deque<String> parts = new java.util.ArrayDeque<>();
        while (amount > 0) {
            int group = (int) (amount % 1000);
            if (group > 0) {
                String words = spellUnderThousand(group);
                parts.push(scale > 0 ? words + " " + SCALES[scale] : words);
            }
            amount /= 1000;
            scale++;
        }
        while (!parts.isEmpty()) {
            sb.append(parts.pop());
            if (!parts.isEmpty()) sb.append(' ');
        }
        return sb.toString();
    }

    private static String spellUnderThousand(int value) {
        StringBuilder sb = new StringBuilder();
        if (value >= 100) {
            sb.append(UNITS[value / 100]).append(" hundred");
            value %= 100;
            if (value > 0) sb.append(' ');
        }
        if (value >= 20) {
            sb.append(TENS[value / 10]);
            value %= 10;
            if (value > 0) sb.append('-').append(UNITS[value]);
        } else if (value > 0) {
            sb.append(UNITS[value]);
        }
        return sb.toString();
    }

    private static String capitalise(String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}

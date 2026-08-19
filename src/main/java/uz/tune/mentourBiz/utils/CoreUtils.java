package uz.tune.mentourBiz.utils;

import com.google.gson.reflect.TypeToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Type;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CoreUtils {
    public static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }

    public static boolean isEmpty(String str) {
        return !StringUtils.hasText(str);
    }

    public static boolean isEmpty(Object obj) {
        return obj == null;
    }

    public static boolean isEmpty(Collection<?> col) {
        return col == null || col.isEmpty();
    }

    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    public static boolean isPresent(String str) {
        return StringUtils.hasText(str);
    }

    public static boolean isPresent(Object obj) {
        return obj != null;
    }

    public static boolean isPresent(Collection<?> col) {
        return col != null && !col.isEmpty();
    }

    public static boolean isPresent(Map<?, ?> map) {
        return map != null && !map.isEmpty();
    }

    public static boolean validateAccountNumberSize(String accountNumber) {
        return isPresent(accountNumber) && accountNumber.length() == 20;
    }

    public static boolean validateBankCodeSize(String bankCode) {
        return isPresent(bankCode) && bankCode.length() == 5;
    }

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+";
    private static final int PASSWORD_LENGTH = 16;
    @Value("${app.processing-server.host}")
    private static String processingServerHost;
    private static String fileServerStatic;

    @Value("${app.file-server.base-url}")
    public void setFileServerStatic(String fileServerStatic) {
        CoreUtils.fileServerStatic = fileServerStatic;
    }
    public static String getBaseFileUrl() {
        return fileServerStatic;
    }

    public static String generatePassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder(PASSWORD_LENGTH);

        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            int index = random.nextInt(CHARACTERS.length());
            password.append(CHARACTERS.charAt(index));
        }

        return password.toString();
    }

    public static String normalizeCharacters(String text) {
        if (text == null) return "";
        return text
                .replaceAll("[\\u200B\\u200C\\u200D\\uFEFF]", "")
                .replaceAll("[‘’‚‛´`′ʻʼʹʽˈ＇]", "'")
                .replaceAll("[“”„‟″＂]", "\"")
                .replaceAll("[\\u00A0\\u1680\\u2000-\\u200A\\u202F\\u205F\\u3000]", " ")
                .trim();
    }

    public static boolean validatePhoneNumber(String phoneNumber) {
        Pattern pattern = Pattern.compile("^(\\+998)?(33|55|77|88|50|9[0-9])\\d{7}$");
        Matcher matcher = pattern.matcher(phoneNumber);
        return matcher.matches();
    }

    public static boolean validateEmail(String email) {
        try {
            String regex = "^(.+)@(.+)$";
            Pattern pattern = Pattern.compile(regex);
            return pattern.matcher(email).matches();
        } catch (Throwable th) {
            return false;
        }
    }

    public static Type getStringListType() {
        return new TypeToken<List<String>>() {
        }.getType();
    }

    public static Type getLongListType() {
        return new TypeToken<List<Long>>() {
        }.getType();
    }

    public static boolean isVersionOutdated(String currentVersion, String latestVersion) {
        if (currentVersion == null || latestVersion == null) return false;

        String[] currentParts = currentVersion.split("\\.");
        String[] latestParts = latestVersion.split("\\.");
        int length = Math.max(currentParts.length, latestParts.length);

        for (int i = 0; i < length; i++) {
            int v1 = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
            int v2 = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;

            if (v1 < v2) return true;
            if (v1 > v2) return false;
        }
        return false;
    }



    public static boolean validatePassword(String password) {
        if (!StringUtils.hasText(password) || password.length() < 6 || password.length() > 20) {
            return true;
        }
        if (checkContinuous(password)) {
            return true;
        }
        if (checkCyrilic(password)) {
            return true;
        }
        return checkContainDigitString(password);
    }

    public static boolean checkContinuous(String given) {
        byte digitSum = 0;
        byte charSum = 0;
        short digit = 0;
        short letter = 0;
        for (int i = 0; i < given.length(); i++) {
            short code = (short) given.codePointAt(i);
            if (Character.isDigit(given.charAt(i))) {
                if (code - digit == 1) {
                    digitSum++;
                } else {
                    if (digitSum < 2) digitSum = 0;
                }
                digit = code;
            }

            if (Character.isLetter(given.charAt(i))) {
                if (code - letter == 1) {
                    charSum++;
                } else {
                    if (charSum < 2) charSum = 0;
                }
                letter = code;
            }
        }
        return digitSum > 2 && charSum > 2;
    }

    public static boolean checkCyrilic(String text) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (Character.UnicodeBlock.of(text.charAt(i)).equals(Character.UnicodeBlock.CYRILLIC)) {
                count++;
            }
        }
        return count > 0;
    }

    public static String generateRandomNum(int length) {
        Random random = new Random();
        StringBuilder randomNumber = new StringBuilder();

        randomNumber.append(random.nextInt(9) + 1);

        for (int i = 1; i < length; i++) {
            randomNumber.append(random.nextInt(10));
        }

        return randomNumber.toString();
    }

    public static boolean checkContainDigitString(String given) {
        if (!StringUtils.hasText(given)) {
            int countDigit = 0;
            int countString = 0;
            for (int i = 0; i < given.length(); i++) {
                if (Character.isSpaceChar(given.charAt(i))) {
                    return false;
                }
                if (Character.isDigit(given.charAt(i))) {
                    countDigit++;
                }
                if (Character.isLetter(given.charAt(i))) {
                    countString++;
                }
            }
            return countDigit > 0 && countString > 0;
        } else {
            return false;
        }
    }

    public static boolean checkMaxSubscriberCounts(Integer maxSubscriberCount) {
        return maxSubscriberCount < 30;
    }

    public static String makeFullName(String firstName, String lastName, String middleName) {
        return (CoreUtils.isPresent(lastName) ? lastName : "")
                + (CoreUtils.isPresent(firstName) ? firstName : "")
                + (CoreUtils.isPresent(middleName) ? middleName : "");
    }

}

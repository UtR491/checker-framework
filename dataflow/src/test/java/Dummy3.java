import org.checkerframework.checker.regex.qual.EnhancedRegex;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Arrays;

public class Dummy3 {

    public static void main(String[] args) {
        @EnhancedRegex String s = "^.(x)?((x.*)|.*).$";
        Pattern PATTERN = Pattern.compile("^.(x)?((x.*)|.*).$");
        Matcher matcher = PATTERN.matcher("hello");
        matcher.matches();
        System.out.printf("group count: %d%n", matcher.groupCount());
        for (int i = 0; i<=matcher.groupCount(); i++) {
            System.out.printf("%d: %s%n", i, matcher.group(i));
        }
    }

}

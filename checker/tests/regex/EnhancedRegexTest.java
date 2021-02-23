import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.regex.qual.EnhancedRegex;
import org.checkerframework.checker.regex.util.RegexUtil;

public class EnhancedRegexTest {
    void hierarchyTest() {
        System.out.println("Does this run? If you see this you know.");
        // legal
        @EnhancedRegex String regexp1 = "(a?).(abc)";
        // legal

        @EnhancedRegex(groups = 2)
        String regexp2 = "(a?).(abc)";
        // legal
        @EnhancedRegex(
                groups = 2,
                nonNullGroups = {1, 2})
        String regexp3 = "(a?).(abc)";
        @EnhancedRegex(
                groups = 3,
                nonNullGroups = {1, 2})
        // :: error: (assignment.type.incompatible)
        String regexp4 = "(a?).(abc)";
        // legal
        @EnhancedRegex(
                groups = 2,
                nonNullGroups = {2})
        String regexp5 = "(a)?(abc)";
        @EnhancedRegex(
                groups = 2,
                nonNullGroups = {1, 2})
        // :: error: (assignment.type.incompatible)
        String regexp6 = "(a)?(abc)";
    }

    String s = "(abc)(def)?(?<alpha>alpha)?.";
    Pattern p = Pattern.compile(s);

    // This will throw an error for now.
    @NonNull String regexUtilRefinementTest() {
        Matcher m = p.matcher("abc");
        if (m.matches()) {
            List<Integer> nonNull = new ArrayList<>(Arrays.asList(1, 2, 3));
            if (RegexUtil.isRegex(s, nonNull)) return m.group(3);
        }
        return "";
    }

    String doSomethingOdd(String input) {
        p = Pattern.compile("^.(.*).$");
        Matcher matcher = p.matcher(input);
        if (!matcher.matches()) {
            return "";
        }

        // The regex checker concludes that this call is legal, therefore its return type
        // can be @NonNull.
        @NonNull String a = matcher.group(1);

        // This call is not legal because the number of groups in the pattern is 1, therefore
        // its return type cannot be @NonNull.
        // :: error: (group.count.invalid)
        @NonNull String b = matcher.group(2);
        return a + b;
    }
}

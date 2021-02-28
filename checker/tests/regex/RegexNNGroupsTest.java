import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.regex.qual.RegexNNGroups;
import org.checkerframework.checker.regex.util.RegexUtil;

public class RegexNNGroupsTest {
    void hierarchyTest() {
        // legal
        @RegexNNGroups String regexp1 = "(a?).(abc)";
        // legal

        @RegexNNGroups(groups = 2)
        String regexp2 = "(a?).(abc)";
        // legal
        @RegexNNGroups(
                groups = 2,
                nonNullGroups = {1, 2})
        String regexp3 = "(a?).(abc)";
        @RegexNNGroups(
                groups = 3,
                nonNullGroups = {1, 2})
        // :: error: (assignment.type.incompatible)
        String regexp4 = "(a?).(abc)";
        // legal
        @RegexNNGroups(
                groups = 2,
                nonNullGroups = {2})
        String regexp5 = "(a)?(abc)";
        @RegexNNGroups(
                groups = 2,
                nonNullGroups = {1, 2})
        // :: error: (assignment.type.incompatible)
        String regexp6 = "(a)?(abc)";
    }

    void getNonNullImplementationTest() {
        @RegexNNGroups(
                groups = 1,
                nonNullGroups = {1})
        // :: error: (assignment.type.incompatible)
        String s3 = "(?>abc)";
        @RegexNNGroups String s2 = "(?>abc)";
        @RegexNNGroups(
                groups = 2,
                nonNullGroups = {1, 2})
        String s4 = "(?>(abc)(<alpha>alpha))";
        @RegexNNGroups(groups = 2)
        // :: error: (assignment.type.incompatible)
        String s5 = "[abc(def)](ghi)\\Q(abc)\\E";
        @RegexNNGroups(
                groups = 1,
                nonNullGroups = {1})
        String s7 = "[abc(def)](ghi)\\Q(abc)\\E";
        @RegexNNGroups String s6 = "[a[a[a]]]";
        @RegexNNGroups(
                groups = 1,
                nonNullGroups = {1})
        // :: error: (assignment.type.incompatible)
        String s8 = "(abc){0}";
        @RegexNNGroups(
                groups = 4,
                nonNullGroups = {1, 2, 4})
        String s9 = "(?<g1>([abcdef]\\Qhello\\E))(?>(abc)*(def)).a\\Q(xyz)\\E";
        @RegexNNGroups(groups = 1)
        // :: error: (assignment.type.incompatible)
        String s12 = "[(abc)[xyz[^p-q]]]";
        // legal
        @RegexNNGroups String s13c = "[a[a](abc)]]"; // (abc) is not a group.
        @RegexNNGroups(groups = 1)
        // :: error: (assignment.type.incompatible)
        String s13 = "[a[a](abc)]]";
        // legal
        @RegexNNGroups(
                groups = 1,
                nonNullGroups = {1})
        String s = "\\\\include\\{(.*)\\}";
    }

    String s = "(abc)(def)?(?<alpha>alpha)?.";
    Pattern p = Pattern.compile(s);

    @NonNull String regexUtilRefinementTest() {
        String s = "abc";
        if (RegexUtil.isRegex(s, 3, 1, 2, 3)) {
            Pattern p = Pattern.compile(s);
            Matcher m = p.matcher("abc");
            if (m.matches()) {
                // legal
                return m.group(3);
            }
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

import org.checkerframework.checker.regex.qual.EnhancedRegex;

public class EnhancedRegexTest {
    void fun() {
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
}

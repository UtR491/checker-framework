import org.checkerframework.checker.regex.qual.EnhancedRegex;

public class EnhancedRegexTest {
    void fun() {
        System.out.println("Does this run? If you see this you know.");
        // legal
        @EnhancedRegex String regexp1 = "(a?).(abc)";
        // legal
        @EnhancedRegex({0, 2})
        String regexp2 = "(a?).(abc)";
        // legal
        @EnhancedRegex({0, 1, 2, 2})
        String regexp3 = "(a?).(abc)";
        // :: error: (assignment.type.incompatible)
        @EnhancedRegex({0, 1, 2, 3})
        String regexp4 = "(a?).(abc)";
        // legal
        @EnhancedRegex({0, 2, 2})
        String regexp5 = "(a)?(abc)";
        // :: error: (assignment.type.incompatible)
        @EnhancedRegex({0, 1, 2, 2})
        String regexp6 = "(a)?(abc)";
    }
}

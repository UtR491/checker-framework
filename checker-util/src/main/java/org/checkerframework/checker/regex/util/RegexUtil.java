// This class should be kept in sync with org.plumelib.util.RegexUtil in the plume-util project.
// (Warning suppressions may differ.)

package org.checkerframework.checker.regex.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.checkerframework.checker.index.qual.GTENegativeOne;
import org.checkerframework.checker.lock.qual.GuardSatisfied;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.regex.qual.Regex;
import org.checkerframework.checker.regex.qual.RegexNNGroups;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.framework.qual.AnnotatedFor;
import org.checkerframework.framework.qual.EnsuresQualifierIf;

/**
 * Utility methods for regular expressions, most notably for testing whether a string is a regular
 * expression.
 *
 * <p>For an example of intended use, see section <a
 * href="https://checkerframework.org/manual/#regexutil-methods">Testing whether a string is a
 * regular expression</a> in the Checker Framework manual.
 *
 * <p><b>Runtime Dependency</b>: If you use this class, you must distribute (or link to) {@code
 * checker-qual.jar}, along with your binaries. Or, you can can copy this class into your own
 * project.
 */
@AnnotatedFor("nullness")
public final class RegexUtil {

    /** This class is a collection of methods; it does not represent anything. */
    private RegexUtil() {
        throw new Error("do not instantiate");
    }

    /**
     * A checked version of {@link PatternSyntaxException}.
     *
     * <p>This exception is useful when an illegal regex is detected but the contextual information
     * to report a helpful error message is not available at the current depth in the call stack. By
     * using a checked PatternSyntaxException the error must be handled up the call stack where a
     * better error message can be reported.
     *
     * <p>Typical usage is:
     *
     * <pre>
     * void myMethod(...) throws CheckedPatternSyntaxException {
     *   ...
     *   if (! isRegex(myString)) {
     *     throw new CheckedPatternSyntaxException(...);
     *   }
     *   ... Pattern.compile(myString) ...
     * </pre>
     *
     * Simply calling {@code Pattern.compile} would have a similar effect, in that {@code
     * PatternSyntaxException} would be thrown at run time if {@code myString} is not a regular
     * expression. There are two problems with such an approach. First, a client of {@code myMethod}
     * might forget to handle the exception, since {@code PatternSyntaxException} is not checked.
     * Also, the Regex Checker would issue a warning about the call to {@code Pattern.compile} that
     * might throw an exception. The above usage pattern avoids both problems.
     *
     * @see PatternSyntaxException
     */
    public static class CheckedPatternSyntaxException extends Exception {

        private static final long serialVersionUID = 6266881831979001480L;

        /** The PatternSyntaxException that this is a wrapper around. */
        private final PatternSyntaxException pse;

        /**
         * Constructs a new CheckedPatternSyntaxException equivalent to the given {@link
         * PatternSyntaxException}.
         *
         * <p>Consider calling this constructor with the result of {@link RegexUtil#regexError}.
         *
         * @param pse the PatternSyntaxException to be wrapped
         */
        public CheckedPatternSyntaxException(PatternSyntaxException pse) {
            this.pse = pse;
        }

        /**
         * Constructs a new CheckedPatternSyntaxException.
         *
         * @param desc a description of the error
         * @param regex the erroneous pattern
         * @param index the approximate index in the pattern of the error, or {@code -1} if the
         *     index is not known
         */
        public CheckedPatternSyntaxException(String desc, String regex, @GTENegativeOne int index) {
            this(new PatternSyntaxException(desc, regex, index));
        }

        /**
         * Retrieves the description of the error.
         *
         * @return the description of the error
         */
        public String getDescription() {
            return pse.getDescription();
        }

        /**
         * Retrieves the error index.
         *
         * @return the approximate index in the pattern of the error, or {@code -1} if the index is
         *     not known
         */
        public int getIndex() {
            return pse.getIndex();
        }

        /**
         * Returns a multi-line string containing the description of the syntax error and its index,
         * the erroneous regular-expression pattern, and a visual indication of the error index
         * within the pattern.
         *
         * @return the full detail message
         */
        @Override
        @Pure
        public String getMessage(@GuardSatisfied CheckedPatternSyntaxException this) {
            return pse.getMessage();
        }

        /**
         * Retrieves the erroneous regular-expression pattern.
         *
         * @return the erroneous pattern
         */
        public String getPattern() {
            return pse.getPattern();
        }
    }

    /**
     * Returns true if the argument is a syntactically valid regular expression.
     *
     * @param s string to check for being a regular expression
     * @return true iff s is a regular expression
     */
    @Pure
    @EnsuresQualifierIf(result = true, expression = "#1", qualifier = Regex.class)
    public static boolean isRegex(String s) {
        return isRegex(s, 0);
    }

    /**
     * Returns true if the argument is a syntactically valid regular expression with at least the
     * given number of groups.
     *
     * @param s string to check for being a regular expression
     * @param groups number of groups expected
     * @return true iff s is a regular expression with {@code groups} groups
     */
    @SuppressWarnings("regex") // RegexUtil; for purity, catches an exception
    @Pure
    // @EnsuresQualifierIf annotation is extraneous because this method is special-cased
    // in RegexTransfer.
    @EnsuresQualifierIf(result = true, expression = "#1", qualifier = Regex.class)
    public static boolean isRegex(String s, int groups) {
        Pattern p;
        try {
            p = Pattern.compile(s);
        } catch (PatternSyntaxException e) {
            return false;
        }
        return getGroupCount(p) >= groups;
    }

    /**
     * Returns true if the argument is a syntactically valid regular expression with specified
     * groups as definitely non-null.
     *
     * @param s string to check for being a regular expression
     * @param groups minimum number of groups
     * @param nonNullGroups groups that are guaranteed to match some part of the target string
     *     provided it matches the (possible) regular expression {@code s}
     * @return true iff s is a regular expression with {@code groups} groups and groups in {@code
     *     nonNullGroups} are definitely non-null when a string matches the regex
     */
    @Pure
    @EnsuresQualifierIf(result = true, expression = "#1", qualifier = RegexNNGroups.class)
    public static boolean isRegex(String s, int groups, int... nonNullGroups) {
        Pattern p;
        try {
            p = Pattern.compile(s);
        } catch (PatternSyntaxException e) {
            return false;
        }
        List<Integer> computedNonNullGroups = getNonNullGroups(p.pattern(), getGroupCount(p));
        if (groups <= getGroupCount(p)) {
            for (int e : nonNullGroups) {
                if (!computedNonNullGroups.contains(e)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Returns true if the argument is a syntactically valid regular expression.
     *
     * @param c char to check for being a regular expression
     * @return true iff c is a regular expression
     */
    @SuppressWarnings({
        "regex", "lock"
    }) // RegexUtil; temp value used in pure method is equal up to equals but not up to ==
    @Pure
    @EnsuresQualifierIf(result = true, expression = "#1", qualifier = Regex.class)
    public static boolean isRegex(final char c) {
        return isRegex(Character.toString(c));
    }

    /**
     * Returns null if the argument is a syntactically valid regular expression. Otherwise returns a
     * string describing why the argument is not a regex.
     *
     * @param s string to check for being a regular expression
     * @return null, or a string describing why the argument is not a regex
     */
    @SideEffectFree
    public static @Nullable String regexError(String s) {
        return regexError(s, 0);
    }

    /**
     * Returns null if the argument is a syntactically valid regular expression with at least the
     * given number of groups. Otherwise returns a string describing why the argument is not a
     * regex.
     *
     * @param s string to check for being a regular expression
     * @param groups number of groups expected
     * @return null, or a string describing why the argument is not a regex
     */
    @SuppressWarnings({"regex", "not.sef"}) // RegexUtil;
    @SideEffectFree
    public static @Nullable String regexError(String s, int groups) {
        try {
            Pattern p = Pattern.compile(s);
            int actualGroups = getGroupCount(p);
            if (actualGroups < groups) {
                return regexErrorMessage(s, groups, actualGroups);
            }
        } catch (PatternSyntaxException e) {
            return e.getMessage();
        }
        return null;
    }

    /**
     * Returns null if the argument is a syntactically valid regular expression. Otherwise returns a
     * PatternSyntaxException describing why the argument is not a regex.
     *
     * @param s string to check for being a regular expression
     * @return null, or a PatternSyntaxException describing why the argument is not a regex
     */
    @SideEffectFree
    public static @Nullable PatternSyntaxException regexException(String s) {
        return regexException(s, 0);
    }

    /**
     * Returns null if the argument is a syntactically valid regular expression with at least the
     * given number of groups. Otherwise returns a PatternSyntaxException describing why the
     * argument is not a regex.
     *
     * @param s string to check for being a regular expression
     * @param groups number of groups expected
     * @return null, or a PatternSyntaxException describing why the argument is not a regex
     */
    @SuppressWarnings("regex") // RegexUtil
    @SideEffectFree
    public static @Nullable PatternSyntaxException regexException(String s, int groups) {
        try {
            Pattern p = Pattern.compile(s);
            int actualGroups = getGroupCount(p);
            if (actualGroups < groups) {
                return new PatternSyntaxException(
                        regexErrorMessage(s, groups, actualGroups), s, -1);
            }
        } catch (PatternSyntaxException pse) {
            return pse;
        }
        return null;
    }

    /**
     * Returns the argument as a {@code @Regex String} if it is a regex, otherwise throws an error.
     * The purpose of this method is to suppress Regex Checker warnings. It should be very rarely
     * needed.
     *
     * @param s string to check for being a regular expression
     * @return its argument
     * @throws Error if argument is not a regex
     */
    @SideEffectFree
    // The return type annotation is a conservative bound.
    public static @Regex String asRegex(String s) {
        return asRegex(s, 0);
    }

    /**
     * Returns the argument as a {@code @RegexNNGroups(groups = groups) String} if it is a regex
     * with at least the given number of groups, otherwise throws an error. The purpose of this
     * method is to suppress Regex Checker warnings. It should be very rarely needed.
     *
     * @param s string to check for being a regular expression
     * @param groups number of groups expected
     * @return its argument
     * @throws Error if argument is not a regex with at least the given number of groups
     */
    @SuppressWarnings("regex") // RegexUtil
    @SideEffectFree
    // The return type annotation is irrelevant; it is special-cased by
    // RegexAnnotatedTypeFactory.
    public static @RegexNNGroups String asRegex(String s, int groups) {
        try {
            Pattern p = Pattern.compile(s);
            int actualGroups = getGroupCount(p);
            if (actualGroups < groups) {
                throw new Error(regexErrorMessage(s, groups, actualGroups));
            }
            return s;
        } catch (PatternSyntaxException e) {
            throw new Error(e);
        }
    }

    /**
     * Returns the argument as a {@code @RegexNNGroups(groups = groups, nonNullGroups =
     * nonNullGroups} if it is a regex with at least the given number of groups and the groups in
     * {@code nonNullGroups} are guaranteed to match provided that the regex matches a string,
     * otherwise throws an error. The purpose of this method is to suppress Regex Checker warnings.
     * It should be rarely needed.
     *
     * @param s string to check for being a regular expression
     * @param groups number of groups expected
     * @param nonNullGroups groups expected to be match some part of a target string when the regex
     *     matches
     * @return its argument
     * @throws Error if argument is not a regex with the specified characteristics
     */
    @SuppressWarnings("regex")
    @SideEffectFree
    public static @RegexNNGroups String asRegex(String s, int groups, int... nonNullGroups) {
        try {
            Pattern p = Pattern.compile(s);
            int actualGroups = getGroupCount(p);
            List<Integer> actualNonNullGroups = getNonNullGroups(p.pattern(), actualGroups);
            boolean containsAll = true;
            int failingGroup = -1;
            for (int e : nonNullGroups) {
                if (!actualNonNullGroups.contains(e)) {
                    containsAll = false;
                    failingGroup = e;
                    break;
                }
            }
            if (actualGroups < groups) {
                throw new Error(regexErrorMessage(s, groups, actualGroups));
            } else if (!containsAll) {
                throw new Error(regexNNGroupsErrorMessage(s, failingGroup));
            }
            return s;
        } catch (PatternSyntaxException e) {
            throw new Error(e);
        }
    }

    /**
     * Generates an error message for s when expectedGroups are needed, but s only has actualGroups.
     *
     * @param s string to check for being a regular expression
     * @param expectedGroups the number of needed capturing groups
     * @param actualGroups the number of groups that {@code s} has
     * @return an error message for s when expectedGroups groups are needed, but s only has
     *     actualGroups groups
     */
    @SideEffectFree
    private static String regexErrorMessage(String s, int expectedGroups, int actualGroups) {
        return "regex \""
                + s
                + "\" has "
                + actualGroups
                + " groups, but "
                + expectedGroups
                + " groups are needed.";
    }

    private static String regexNNGroupsErrorMessage(String s, int nullableGroup) {
        return "for regex \""
                + s
                + "\", call to group("
                + nullableGroup
                + ") can return a possibly-null string "
                + " but is expected to return a non-null string.";
    }

    /**
     * Return the count of groups in the argument.
     *
     * @param p pattern whose groups to count
     * @return the count of groups in the argument
     */
    @SuppressWarnings("lock") // does not depend on object identity
    @Pure
    private static int getGroupCount(Pattern p) {
        return p.matcher("").groupCount();
    }

    /**
     * Returns a list of groups other than 0, that are guaranteed to be non-null given that the
     * regular expression matches the String. The String argument passed has to be a valid regular
     * expression.
     *
     * @param regexp pattern to be analysed
     * @param n number of capturing groups in the pattern
     * @return a {@code List} of groups that are guaranteed to match some part of a string that
     *     matches {@code regexp}
     * @throws Error if the argument is not a regex
     */
    public static List<Integer> getNonNullGroups(String regexp, int n) {
        try {
            Pattern.compile(regexp);
        } catch (PatternSyntaxException e) {
            throw new Error(e);
        }
        List<Integer> nonNullGroups = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            nonNullGroups.add(i);
        }

        // Holds all indices of opening parentheses that were openings of a capturing group.
        ArrayDeque<Integer> openingIndices = new ArrayDeque<>();
        // Holds whether the last occurrence of a non-literal '(' was a capturing group (true)
        // or was some other special construct (false). This helps in determining whether a
        // non-literal
        // ')' closes a capturing group or some other special construct. If it closes a capturing
        // group, the top element from openingIndices has to be removed.
        ArrayDeque<Boolean> openingWasGroup = new ArrayDeque<>();
        // If true, the character just before the current one was '\', i.e. the current character
        // has to be considered either in a literal sense or as some special character or flag.
        boolean escaped = false;
        int group = 0;

        /**
         * If you encounter '(' and it is not preceded by a '\', then it is a special group. If it
         * is followed by a '?', it will either be a named capturing group or some other special
         * construct. A named capturing group has a '<' followed by the '?'. The boolean stack is to
         * figure out whether a ')' closes a capturing group or a special construct.
         *
         * <p>If you encounter a '\' it defines some sort of flag or special character. If the '\'
         * is preceded by another '\', it represents the literal '\'.
         *
         * <p>If you encounter a '[' and it was not preceded by a '\', it marks the beginning of a
         * literal list. Traverse inside the list till you encounter the closing ']', then resume
         * normal traversal.
         *
         * <p>If you encounter a 'Q' which is preceded by a '\', it marks the beginning of a quote.
         * Traverse until you find the corresponding '\E', then resume normal traversal.
         */
        final int length = regexp.length();
        for (int i = 0; i < length; i++) {
            if (regexp.charAt(i) == '(') {
                if (!escaped) {
                    if (i != length - 1) {
                        if (regexp.charAt(i + 1) == '?') {
                            if (i < length - 2
                                    && regexp.charAt(i + 2) == '<') { // named capturing group.
                                group += 1;
                                if (i != 0 && regexp.charAt(i - 1) == '|') {
                                    nonNullGroups.remove(Integer.valueOf(group));
                                }
                                openingIndices.push(group);
                                openingWasGroup.push(true);
                            } else { // non capturing group.
                                openingWasGroup.push(false);
                            }
                        } else { // unnamed capturing group.
                            group += 1;
                            if (i != 0 && regexp.charAt(i - 1) == '|') {
                                nonNullGroups.remove(Integer.valueOf(group));
                            }
                            openingIndices.push(group);
                            openingWasGroup.push(true);
                        }
                    }
                } else {
                    escaped = false;
                }
            } else if (regexp.charAt(i) == ')') {
                if (!escaped) { // ending of a construct.
                    boolean closesGroup = openingWasGroup.pop();
                    if (closesGroup) {
                        int value = openingIndices.pop();
                        if (i != regexp.length() - 1
                                && "?|*".contains(Character.toString(regexp.charAt(i + 1)))) {
                            nonNullGroups.remove(Integer.valueOf(value));
                        } else if (i < length - 2
                                && regexp.charAt(i + 1) == '{'
                                && regexp.charAt(i + 2) == '0') {
                            nonNullGroups.remove(Integer.valueOf(value));
                        }
                    }
                } else {
                    escaped = false;
                }
            } else if (regexp.charAt(i) == '\\') {
                escaped = !escaped;
            } else if (regexp.charAt(i) == '[') {
                if (!escaped) {
                    int balance = 1;
                    int j;
                    for (j = i + 1; balance > 0; j++) {
                        if (regexp.charAt(j) == '\\') {
                            escaped = !escaped;
                        } else if (regexp.charAt(j) == ']' && !escaped) {
                            balance -= 1;
                        } else if (escaped) {
                            escaped = false;
                        }
                    }
                    i = j - 1;
                } else {
                    escaped = false;
                }
            } else if (regexp.charAt(i) == 'Q') {
                if (escaped) {
                    escaped = false;
                }
                i = regexp.indexOf("\\E", i) + 1;
            } else { // any other character.
                if (escaped) {
                    escaped = false;
                }
            }
        }
        return nonNullGroups;
    }
}

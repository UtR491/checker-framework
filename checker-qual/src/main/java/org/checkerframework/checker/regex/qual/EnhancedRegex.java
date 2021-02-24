package org.checkerframework.checker.regex.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * If a type is annotated as {@code EnhancedRegex(groups = n, nonNullGroups = {a, b, c ... })}, then
 * the runtime value is a regular expression with at least n groups where groups numbered a, b, c...
 * are guaranteed to match some (possibly empty) part of a matching String provided that the regular
 * expression itself matched the String (i.e. group 0 has matched).
 *
 * <p>For example the regular expression {@code "(abc)?(cde)"} has type {@code EnhancedRegex(groups
 * = 2, nonNullGroups = {2})}
 *
 * @checker_framework.manual #regex-checker Regex Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(Regex.class)
public @interface EnhancedRegex {
    /**
     * The number of groups in the regular expression. Defaults to 0.
     *
     * @return number of capturing groups in the pattern
     */
    int groups() default 0;

    /**
     * The list of groups other than 0, that are guaranteed to be non-null provided the input String
     * matches the regular expression. Defaults to an empty list.
     *
     * @return list of non-null groups.
     */
    int[] nonNullGroups() default {};
}

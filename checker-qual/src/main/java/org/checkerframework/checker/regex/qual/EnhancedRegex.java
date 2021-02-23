package org.checkerframework.checker.regex.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * If a type is annotated as {@code EnhancedRegex({0, a, b, c ... , n})}, then the runtime value is
 * a regular expression with n groups where groups numbered 0, a, b, c ... are guaranteed to match
 * some (possibly empty) part of a matching String provided that the regular expression itself
 * matched the String (i.e. group 0 has matched).
 *
 * <p>For example the regular expression {@code "(abc)?(cde)"} will be annotated with {@code
 * EnhancedRegex({0, 2, 2})}
 *
 * @checker_framework.manual #regex-checker Regex Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(Regex.class)
public @interface EnhancedRegex {
    int groups() default 0;

    int[] nonNullGroups() default {};
}

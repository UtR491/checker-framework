package org.checkerframework.checker.regex;

import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.dataflow.analysis.ConditionalTransferResult;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.ArrayCreationNode;
import org.checkerframework.dataflow.cfg.node.ClassNameNode;
import org.checkerframework.dataflow.cfg.node.GreaterThanNode;
import org.checkerframework.dataflow.cfg.node.GreaterThanOrEqualNode;
import org.checkerframework.dataflow.cfg.node.IntegerLiteralNode;
import org.checkerframework.dataflow.cfg.node.LessThanNode;
import org.checkerframework.dataflow.cfg.node.LessThanOrEqualNode;
import org.checkerframework.dataflow.cfg.node.MethodAccessNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.dataflow.util.NodeUtils;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;

/** The transfer function for the Regex Checker. */
public class RegexTransfer extends CFTransfer {

    // isRegex and asRegex are tested as signatures (string name plus formal parameters), not
    // ExecutableElement, because they exist in two packages:
    // org.checkerframework.checker.regex.util.RegexUtil.isRegex(String,int)
    // org.plumelib.util.RegexUtil.isRegex(String,int)
    // and org.plumelib.util might not be on the classpath.
    private static final String IS_REGEX_METHOD_NAME = "isRegex";
    private static final String AS_REGEX_METHOD_NAME = "asRegex";

    /** The MatchResult.groupCount() method. */
    private final ExecutableElement matchResultgroupCount;

    /** Create the transfer function for the Regex Checker. */
    public RegexTransfer(CFAbstractAnalysis<CFValue, CFStore, CFTransfer> analysis) {
        super(analysis);
        this.matchResultgroupCount =
                TreeUtils.getMethod(
                        "java.util.regex.MatchResult",
                        "groupCount",
                        0,
                        analysis.getTypeFactory().getProcessingEnv());
    }

    // TODO: These are special cases for isRegex(String, int) and asRegex(String, int).
    // They should be replaced by adding an @EnsuresQualifierIf annotation that supports
    // specifying attributes.
    @Override
    public TransferResult<CFValue, CFStore> visitMethodInvocation(
            MethodInvocationNode n, TransferInput<CFValue, CFStore> in) {
        TransferResult<CFValue, CFStore> result = super.visitMethodInvocation(n, in);

        // refine result for some helper methods
        MethodAccessNode target = n.getTarget();
        ExecutableElement method = target.getMethod();
        Node receiver = target.getReceiver();
        if (receiver instanceof ClassNameNode) {
            ClassNameNode cnn = (ClassNameNode) receiver;
            String receiverName = cnn.getElement().toString();
            if (isRegexUtil(receiverName)) {
                result = handleRegexUtil(n, method, result);
            }
        }
        return result;
    }

    private TransferResult<CFValue, CFStore> handleRegexUtil(
            MethodInvocationNode n,
            ExecutableElement method,
            TransferResult<CFValue, CFStore> result) {
        RegexAnnotatedTypeFactory factory = (RegexAnnotatedTypeFactory) analysis.getTypeFactory();
        if (ElementUtils.matchesElement(method, IS_REGEX_METHOD_NAME, String.class, int.class)) {
            return getModifiedStore(n, result, factory);
        } else if (ElementUtils.matchesElement(
                method, IS_REGEX_METHOD_NAME, String.class, int.class, int[].class)) {
            return getModifiedStore(n, result, factory);
        } else if (ElementUtils.matchesElement(
                method, AS_REGEX_METHOD_NAME, String.class, int.class)) {
            return getAsRegexModifiedStore(n, result, factory);
        } else if (ElementUtils.matchesElement(
                method, AS_REGEX_METHOD_NAME, String.class, int.class, int[].class)) {
            return getAsRegexModifiedStore(n, result, factory);
        }
        return result;
    }

    @Override
    public TransferResult<CFValue, CFStore> visitLessThan(
            LessThanNode n, TransferInput<CFValue, CFStore> in) {
        // Look for: constant < mat.groupCount()
        // Make mat be @Regex(constant + 1)
        TransferResult<CFValue, CFStore> res = super.visitLessThan(n, in);
        return handleMatcherGroupCount(n.getRightOperand(), n.getLeftOperand(), false, res);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitLessThanOrEqual(
            LessThanOrEqualNode n, TransferInput<CFValue, CFStore> in) {
        // Look for: constant <= mat.groupCount()
        // Make mat be @Regex(constant)
        TransferResult<CFValue, CFStore> res = super.visitLessThanOrEqual(n, in);
        return handleMatcherGroupCount(n.getRightOperand(), n.getLeftOperand(), true, res);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitGreaterThan(
            GreaterThanNode n, TransferInput<CFValue, CFStore> in) {

        TransferResult<CFValue, CFStore> res = super.visitGreaterThan(n, in);
        return handleMatcherGroupCount(n.getLeftOperand(), n.getRightOperand(), false, res);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitGreaterThanOrEqual(
            GreaterThanOrEqualNode n, TransferInput<CFValue, CFStore> in) {
        // Look for: mat.groupCount() >= constant
        // Make mat be @Regex(constant)
        TransferResult<CFValue, CFStore> res = super.visitGreaterThanOrEqual(n, in);
        return handleMatcherGroupCount(n.getLeftOperand(), n.getRightOperand(), true, res);
    }

    /**
     * See whether possibleMatcher is a call of groupCount on a Matcher and possibleConstant is a
     * constant. If so, annotate the matcher as constant + 1 if !isAlsoEqual constant if isAlsoEqual
     *
     * @param possibleMatcher the Node that might be a call of Matcher.groupCount()
     * @param possibleConstant the Node that might be a constant
     * @param isAlsoEqual whether the comparison operation is strict or reflexive
     * @param resultIn TransferResult
     * @return the possibly refined output TransferResult
     */
    private TransferResult<CFValue, CFStore> handleMatcherGroupCount(
            Node possibleMatcher,
            Node possibleConstant,
            boolean isAlsoEqual,
            TransferResult<CFValue, CFStore> resultIn) {
        if (!(possibleMatcher instanceof MethodInvocationNode)) {
            return resultIn;
        }
        if (!(possibleConstant instanceof IntegerLiteralNode)) {
            return resultIn;
        }

        if (!NodeUtils.isMethodInvocation(
                possibleMatcher,
                matchResultgroupCount,
                analysis.getTypeFactory().getProcessingEnv())) {
            return resultIn;
        }

        MethodAccessNode methodAccessNode = ((MethodInvocationNode) possibleMatcher).getTarget();
        Node receiver = methodAccessNode.getReceiver();

        JavaExpression matcherReceiver = JavaExpression.fromNode(receiver);

        IntegerLiteralNode iln = (IntegerLiteralNode) possibleConstant;
        int groupCount;
        if (isAlsoEqual) {
            groupCount = iln.getValue();
        } else {
            groupCount = iln.getValue() + 1;
        }

        CFStore thenStore = resultIn.getRegularStore();
        CFStore elseStore = thenStore.copy();
        ConditionalTransferResult<CFValue, CFStore> newResult =
                new ConditionalTransferResult<>(resultIn.getResultValue(), thenStore, elseStore);
        RegexAnnotatedTypeFactory factory = (RegexAnnotatedTypeFactory) analysis.getTypeFactory();

        AnnotationMirror regexAnnotation = factory.createRegexAnnotation(groupCount);
        thenStore.insertValue(matcherReceiver, regexAnnotation);

        return newResult;
    }

    /**
     * Returns true if the given receiver is a class named "RegexUtil". Examples of such classes are
     * org.checkerframework.checker.regex.util.RegexUtil and org.plumelib.util.RegexUtil, and the
     * user might copy one into their own project.
     *
     * @param receiver some string
     * @return true if the given receiver is a class named "RegexUtil"
     */
    private boolean isRegexUtil(String receiver) {
        return receiver.equals("RegexUtil") || receiver.endsWith(".RegexUtil");
    }

    /**
     * The new store with types modified on the basis of {@code isRegex} test.
     *
     * @param n the {@code RegexUtil.isRegex} method invocation.
     * @param result the store just before the invocation is encountered.
     * @param factory the type factory to create relevant types.
     * @return the new store after refining the type of the string that was checked.
     */
    private ConditionalTransferResult<CFValue, CFStore> getModifiedStore(
            MethodInvocationNode n,
            TransferResult<CFValue, CFStore> result,
            RegexAnnotatedTypeFactory factory) {
        CFStore thenStore = result.getRegularStore();
        CFStore elseStore = thenStore.copy();
        ConditionalTransferResult<CFValue, CFStore> newResult =
                new ConditionalTransferResult<>(result.getResultValue(), thenStore, elseStore);
        JavaExpression firstParam = JavaExpression.fromNode(n.getArgument(0));

        AnnotationMirror regexNNGroupsAnnotation = getRegexNNGroupsAnno(factory, n.getArguments());
        if (thenStore.getValue(firstParam) != null) thenStore.clearValue(firstParam);
        thenStore.insertValue(firstParam, regexNNGroupsAnnotation);
        return newResult;
    }

    /**
     * The new store with modified on the basis of {@code asRegex} refinement.
     *
     * @param n the {@code RegexUtil.isRegex} method invocation.
     * @param result the store just before the invocation is encountered.
     * @param factory the type factory to create relevant types.
     * @return the new store after refining the type of the string that was refined.
     */
    private TransferResult<CFValue, CFStore> getAsRegexModifiedStore(
            MethodInvocationNode n,
            TransferResult<CFValue, CFStore> result,
            RegexAnnotatedTypeFactory factory) {
        AnnotationMirror regexNNGroupsAnnotation = getRegexNNGroupsAnno(factory, n.getArguments());
        CFValue newResultValue =
                analysis.createSingleAnnotationValue(
                        regexNNGroupsAnnotation, result.getResultValue().getUnderlyingType());
        return new RegularTransferResult<>(newResultValue, result.getRegularStore());
    }

    /**
     * Returns the {@code @RegexNNGroups} annotation with relevant field values.
     *
     * @param factory the type factory to create types.
     * @param nodes arguments passed to the {@code RegexUtil} functions.
     * @return the {@code @RegexNNGroups} annotation.
     */
    private AnnotationMirror getRegexNNGroupsAnno(
            RegexAnnotatedTypeFactory factory, List<Node> nodes) {
        int groupCount = 0;
        List<Integer> nonNullGroups = new ArrayList<>();
        for (Node e : nodes) {
            if (e instanceof IntegerLiteralNode) {
                groupCount = ((IntegerLiteralNode) e).getValue();
            } else if (e instanceof ArrayCreationNode) {
                List<Node> nonNullNodes = ((ArrayCreationNode) e).getInitializers();
                for (Node inits : nonNullNodes) {
                    if (inits instanceof IntegerLiteralNode) {
                        nonNullGroups.add(((IntegerLiteralNode) inits).getValue());
                    }
                }
            }
        }
        return factory.createRegexNNGroupsAnnotation(groupCount, nonNullGroups);
    }
}

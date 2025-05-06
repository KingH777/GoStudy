package org.example;

public class Problem6 {
    public static void main(String[] args) {
        // 测试用例
        String[] testCases = {
                "((a+b)+((c)))",
                "a+(b+c)",
                "(a*b)+c/d",
                "a+b/(c-d)",
                "(a+b)*c",
                "a*(b+c)",
                "(a+b+c)",
                "((a+b))",
                "a+((b+c))",
                "(a+b)+(c+d)",
                "a*(b/(c-d))",
                "(a+b)*(c-d)",
                "a/(b+c*d)",
                "(a+b*c)+d"
        };

        for (String test : testCases) {
            String result = removeRedundantParentheses(test);
            System.out.println("原始表达式: " + test);
            System.out.println("精简表达式: " + result);
            System.out.println();
        }
    }

    /**
     * 去除表达式中的多余括号
     * @param expr 输入表达式
     * @return 去除多余括号后的表达式
     */
    public static String removeRedundantParentheses(String expr) {
        // 解析表达式为语法树
        Node root = parseExpression(expr, 0).node;
        // 将语法树转换回字符串
        return treeToString(root, null, null);
    }

    // 解析结果类，包含节点和解析位置
    private static class ParseResult {
        Node node;
        int pos;

        ParseResult(Node node, int pos) {
            this.node = node;
            this.pos = pos;
        }
    }

    // 表达式节点类型
    private static class Node {
        enum Type {
            VARIABLE, // 变量
            OPERATOR, // 运算符
            EXPRESSION, // 表达式
            BRACKETED // 括号包围的表达式
        }

        Type type;
        char value; // 变量或运算符的值
        Node left; // 左子节点
        Node right; // 右子节点
        Node inner; // 括号内的表达式

        // 变量或运算符构造函数
        Node(Type type, char value) {
            this.type = type;
            this.value = value;
        }

        // 表达式构造函数
        Node(Node left, char operator, Node right) {
            this.type = Type.EXPRESSION;
            this.value = operator;
            this.left = left;
            this.right = right;
        }

        // 括号表达式构造函数
        Node(Node inner) {
            this.type = Type.BRACKETED;
            this.inner = inner;
        }
    }

    /**
     * 解析表达式为语法树
     * @param expr 表达式字符串
     * @param pos 当前解析位置
     * @return 解析结果，包含节点和解析位置
     */
    private static ParseResult parseExpression(String expr, int pos) {
        Node result = null;
        char lastOp = '\0';

        while (pos < expr.length()) {
            char c = expr.charAt(pos);

            if (c == '(') {
                // 处理括号内的表达式
                ParseResult innerResult = parseExpression(expr, pos + 1);
                Node bracketed = new Node(innerResult.node);
                pos = innerResult.pos;

                if (result == null) {
                    result = bracketed;
                } else {
                    result = new Node(result, lastOp, bracketed);
                }
            } else if (c == ')') {
                // 结束当前括号内的解析
                return new ParseResult(result, pos + 1);
            } else if (c == '+' || c == '-' || c == '*' || c == '/') {
                // 记录运算符
                lastOp = c;
                pos++;
            } else if (Character.isLowerCase(c)) {
                // 处理变量
                Node var = new Node(Node.Type.VARIABLE, c);

                if (result == null) {
                    result = var;
                } else {
                    result = new Node(result, lastOp, var);
                }
                pos++;
            } else {
                // 跳过空格等其他字符
                pos++;
            }
        }

        return new ParseResult(result, pos);
    }

    /**
     * 获取运算符优先级
     * @param op 运算符
     * @return 优先级数值
     */
    private static int getPriority(char op) {
        switch (op) {
            case '+':
            case '-':
                return 1;
            case '*':
            case '/':
                return 2;
            default:
                return 0;
        }
    }

    /**
     * 判断是否需要保留括号
     * @param node 当前节点
     * @param parentOp 父运算符
     * @param position 在父表达式中的位置（left或right）
     * @return 是否需要保留括号
     */
    private static boolean isBracketNeeded(Node node, Character parentOp, String position) {
        if (node == null) return false;

        switch (node.type) {
            case VARIABLE:
                return false;

            case EXPRESSION:
                // 检查当前节点的运算符优先级是否低于父运算符
                if (parentOp != null && getPriority(node.value) < getPriority(parentOp)) {
                    return true;
                }

                // 检查右侧的减法和除法
                if ("right".equals(position) && (node.value == '-' || node.value == '/') && parentOp != null) {
                    return true;
                }

                return false;

            case BRACKETED:
                return isBracketNeeded(node.inner, parentOp, position);

            default:
                return false;
        }
    }

    /**
     * 将语法树转换为字符串
     * @param node 当前节点
     * @param parentOp 父运算符
     * @param position 在父表达式中的位置
     * @return 表达式字符串
     */
    private static String treeToString(Node node, Character parentOp, String position) {
        if (node == null) return "";

        switch (node.type) {
            case VARIABLE:
                return String.valueOf(node.value);

            case OPERATOR:
                return String.valueOf(node.value);

            case EXPRESSION:
                String leftStr = treeToString(node.left, node.value, "left");
                String rightStr = treeToString(node.right, node.value, "right");
                String exprStr = leftStr + node.value + rightStr;

                // 检查是否需要括号
                if (isBracketNeeded(node, parentOp, position)) {
                    return "(" + exprStr + ")";
                }
                return exprStr;

            case BRACKETED:
                String innerStr = treeToString(node.inner, parentOp, position);

                // 检查是否需要保留括号
                if (isBracketNeeded(node.inner, parentOp, position)) {
                    return "(" + innerStr + ")";
                }
                return innerStr;

            default:
                return "";
        }
    }
}
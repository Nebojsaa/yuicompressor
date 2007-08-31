/*
 * YUI Compressor
 * Author: Julien Lecomte <jlecomte@yahoo-inc.com>
 * Copyright (c) 2007, Yahoo! Inc. All rights reserved.
 * Code licensed under the BSD License:
 *     http://developer.yahoo.net/yui/license.txt
 */

package com.yahoo.platform.yui.compressor;

import org.mozilla.javascript.*;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.*;

public class JavaScriptCompressor {

    static final ArrayList ones;
    static final ArrayList twos;
    static final ArrayList threes;

    static final Set builtin = new HashSet();
    static final Map literals = new Hashtable();

    static {

        // This list contains all the 3 characters or less built-in global
        // symbols available in a browser. Please add to this list if you
        // see anything missing.
        builtin.add("NaN");
        builtin.add("top");

        ones = new ArrayList();
        for (char c = 'A'; c <= 'Z'; c++)
            ones.add(Character.toString(c));
        for (char c = 'a'; c <= 'z'; c++)
            ones.add(Character.toString(c));

        twos = new ArrayList();
        for (int i = 0; i < ones.size(); i++) {
            String one = (String) ones.get(i);
            for (char c = 'A'; c <= 'Z'; c++)
                twos.add(one + Character.toString(c));
            for (char c = 'a'; c <= 'z'; c++)
                twos.add(one + Character.toString(c));
            for (char c = '0'; c <= '9'; c++)
                twos.add(one + Character.toString(c));
        }

        // Remove two-letter JavaScript reserved words and built-in globals...
        twos.remove("as");
        twos.remove("is");
        twos.remove("do");
        twos.remove("if");
        twos.remove("in");
        twos.removeAll(builtin);

        threes = new ArrayList();
        for (int i = 0; i < twos.size(); i++) {
            String two = (String) twos.get(i);
            for (char c = 'A'; c <= 'Z'; c++)
                threes.add(two + Character.toString(c));
            for (char c = 'a'; c <= 'z'; c++)
                threes.add(two + Character.toString(c));
            for (char c = '0'; c <= '9'; c++)
                threes.add(two + Character.toString(c));
        }

        // Remove three-letter JavaScript reserved words and built-in globals...
        threes.remove("for");
        threes.remove("int");
        threes.remove("new");
        threes.remove("try");
        threes.remove("use");
        threes.remove("var");
        threes.removeAll(builtin);

        // That's up to ((26+26)*(1+(26+26+10)))*(1+(26+26+10))-8
        // (206,380 symbols per scope)

        // The following list comes from org/mozilla/javascript/Decompiler.java...
        literals.put(new Integer(Token.GET), "get ");
        literals.put(new Integer(Token.SET), "set ");
        literals.put(new Integer(Token.TRUE), "true");
        literals.put(new Integer(Token.FALSE), "false");
        literals.put(new Integer(Token.NULL), "null");
        literals.put(new Integer(Token.THIS), "this");
        literals.put(new Integer(Token.FUNCTION), "function ");
        literals.put(new Integer(Token.COMMA), ",");
        literals.put(new Integer(Token.LC), "{");
        literals.put(new Integer(Token.RC), "}");
        literals.put(new Integer(Token.LP), "(");
        literals.put(new Integer(Token.RP), ")");
        literals.put(new Integer(Token.LB), "[");
        literals.put(new Integer(Token.RB), "]");
        literals.put(new Integer(Token.DOT), ".");
        literals.put(new Integer(Token.NEW), "new ");
        literals.put(new Integer(Token.DELPROP), "delete ");
        literals.put(new Integer(Token.IF), "if");
        literals.put(new Integer(Token.ELSE), "else");
        literals.put(new Integer(Token.FOR), "for");
        literals.put(new Integer(Token.IN), " in ");
        literals.put(new Integer(Token.WITH), "with");
        literals.put(new Integer(Token.WHILE), "while");
        literals.put(new Integer(Token.DO), "do");
        literals.put(new Integer(Token.TRY), "try");
        literals.put(new Integer(Token.CATCH), "catch");
        literals.put(new Integer(Token.FINALLY), "finally");
        literals.put(new Integer(Token.THROW), "throw ");
        literals.put(new Integer(Token.SWITCH), "switch");
        literals.put(new Integer(Token.BREAK), "break ");
        literals.put(new Integer(Token.CONTINUE), "continue ");
        literals.put(new Integer(Token.CASE), "case ");
        literals.put(new Integer(Token.DEFAULT), "default");
        literals.put(new Integer(Token.RETURN), "return ");
        literals.put(new Integer(Token.VAR), "var ");
        literals.put(new Integer(Token.SEMI), ";");
        literals.put(new Integer(Token.ASSIGN), "=");
        literals.put(new Integer(Token.ASSIGN_ADD), "+=");
        literals.put(new Integer(Token.ASSIGN_SUB), "-=");
        literals.put(new Integer(Token.ASSIGN_MUL), "*=");
        literals.put(new Integer(Token.ASSIGN_DIV), "/=");
        literals.put(new Integer(Token.ASSIGN_MOD), "%=");
        literals.put(new Integer(Token.ASSIGN_BITOR), "|=");
        literals.put(new Integer(Token.ASSIGN_BITXOR), "^=");
        literals.put(new Integer(Token.ASSIGN_BITAND), "&=");
        literals.put(new Integer(Token.ASSIGN_LSH), "<<=");
        literals.put(new Integer(Token.ASSIGN_RSH), ">>=");
        literals.put(new Integer(Token.ASSIGN_URSH), ">>>=");
        literals.put(new Integer(Token.HOOK), "?");
        literals.put(new Integer(Token.OBJECTLIT), ":");
        literals.put(new Integer(Token.COLON), ":");
        literals.put(new Integer(Token.OR), "||");
        literals.put(new Integer(Token.AND), "&&");
        literals.put(new Integer(Token.BITOR), "|");
        literals.put(new Integer(Token.BITXOR), "^");
        literals.put(new Integer(Token.BITAND), "&");
        literals.put(new Integer(Token.SHEQ), "===");
        literals.put(new Integer(Token.SHNE), "!==");
        literals.put(new Integer(Token.EQ), "==");
        literals.put(new Integer(Token.NE), "!=");
        literals.put(new Integer(Token.LE), "<=");
        literals.put(new Integer(Token.LT), "<");
        literals.put(new Integer(Token.GE), ">=");
        literals.put(new Integer(Token.GT), ">");
        literals.put(new Integer(Token.INSTANCEOF), " instanceof ");
        literals.put(new Integer(Token.LSH), "<<");
        literals.put(new Integer(Token.RSH), ">>");
        literals.put(new Integer(Token.URSH), ">>>");
        literals.put(new Integer(Token.TYPEOF), "typeof ");
        literals.put(new Integer(Token.VOID), "void ");
        literals.put(new Integer(Token.CONST), "const ");
        literals.put(new Integer(Token.NOT), "!");
        literals.put(new Integer(Token.BITNOT), "~");
        literals.put(new Integer(Token.POS), "+");
        literals.put(new Integer(Token.NEG), "-");
        literals.put(new Integer(Token.INC), "++");
        literals.put(new Integer(Token.DEC), "--");
        literals.put(new Integer(Token.ADD), "+");
        literals.put(new Integer(Token.SUB), "-");
        literals.put(new Integer(Token.MUL), "*");
        literals.put(new Integer(Token.DIV), "/");
        literals.put(new Integer(Token.MOD), "%");
        literals.put(new Integer(Token.COLONCOLON), "::");
        literals.put(new Integer(Token.DOTDOT), "..");
        literals.put(new Integer(Token.DOTQUERY), ".(");
        literals.put(new Integer(Token.XMLATTR), "@");
    }

    private static ArrayList readTokens(String source) {
        int offset = 0;
        ArrayList tokens = new ArrayList();
        int length = source.length();
        StringBuffer sb = new StringBuffer();
        while (offset < length) {
            int token = source.charAt(offset++);
            switch (token) {

                case Token.NAME:
                case Token.REGEXP:
                    sb.setLength(0);
                    offset = printSourceString(source, offset, false, sb);
                    tokens.add(new JavaScriptToken(token, sb.toString()));
                    break;

                case Token.STRING:
                    sb.setLength(0);
                    offset = printSourceString(source, offset, true, sb);
                    tokens.add(new JavaScriptToken(token, sb.toString()));
                    break;

                case Token.NUMBER:
                    sb.setLength(0);
                    offset = printSourceNumber(source, offset, sb);
                    tokens.add(new JavaScriptToken(token, sb.toString()));
                    break;

                default:
                    String literal = (String) literals.get(new Integer(token));
                    if (literal != null) {
                        tokens.add(new JavaScriptToken(token, literal));
                    }
                    break;
            }
        }
        return tokens;
    }

    private static int printSourceString(String source, int offset,
            boolean asQuotedString, StringBuffer sb) {
        int length = source.charAt(offset);
        ++offset;
        if ((0x8000 & length) != 0) {
            length = ((0x7FFF & length) << 16) | source.charAt(offset);
            ++offset;
        }
        if (sb != null) {
            String str = source.substring(offset, offset + length);
            if (!asQuotedString) {
                sb.append(str);
            } else {
                sb.append('"');
                sb.append(escapeString(str, '"'));
                sb.append('"');
            }
        }
        return offset + length;
    }

    private static String escapeString(String s, char escapeQuote) {

        assert escapeQuote == '"' || escapeQuote == '\'';

        if (s == null) {
            return null;
        }

        StringBuffer sb = new StringBuffer();
        for (int i = 0, L = s.length(); i < L; i++) {
            int c = s.charAt(i);
            switch (c) {
                case'\b':
                    sb.append("\\b");
                    break;
                case'\f':
                    sb.append("\\f");
                    break;
                case'\n':
                    sb.append("\\n");
                    break;
                case'\r':
                    sb.append("\\r");
                    break;
                case'\t':
                    sb.append("\\t");
                    break;
                case 0xb:
                    sb.append("\\v");
                    break;
                case'\\':
                    sb.append("\\\\");
                    break;
                case'"':
                    sb.append("\\\"");
                    break;
                default:
                    if (c < ' ') {
                        // Control character: 2-digit hex. Note: Can I ever get
                        // in this situation? Shouldn't rhino report an error?
                        sb.append("\\x");
                        // Append hexadecimal form of c left-padded with 0.
                        int hexSize = 2;
                        for (int shift = (hexSize - 1) * 4; shift >= 0; shift -= 4) {
                            int digit = 0xf & (c >> shift);
                            int hc = (digit < 10) ? '0' + digit : 'a' - 10 + digit;
                            sb.append((char) hc);
                        }
                    } else {
                        sb.append((char) c);
                    }
                    break;
            }
        }

        return sb.toString();
    }

    private static int printSourceNumber(String source,
            int offset, StringBuffer sb) {
        double number = 0.0;
        char type = source.charAt(offset);
        ++offset;
        if (type == 'S') {
            if (sb != null) {
                number = source.charAt(offset);
            }
            ++offset;
        } else if (type == 'J' || type == 'D') {
            if (sb != null) {
                long lbits;
                lbits = (long) source.charAt(offset) << 48;
                lbits |= (long) source.charAt(offset + 1) << 32;
                lbits |= (long) source.charAt(offset + 2) << 16;
                lbits |= (long) source.charAt(offset + 3);
                if (type == 'J') {
                    number = lbits;
                } else {
                    number = Double.longBitsToDouble(lbits);
                }
            }
            offset += 4;
        } else {
            // Bad source
            throw new RuntimeException();
        }
        if (sb != null) {
            sb.append(ScriptRuntime.numberToString(number, 10));
        }
        return offset;
    }

    private ErrorReporter logger;

    private boolean munge;
    private boolean warn;

    private static final int BUILDING_SYMBOL_TREE = 1;
    private static final int CHECKING_SYMBOL_TREE = 2;

    private int mode;
    private int offset;
    private int braceNesting;
    private ArrayList tokens;
    private Stack scopes = new Stack();
    private ScriptOrFnScope globalScope = new ScriptOrFnScope(-1, null);
    private Hashtable indexedScopes = new Hashtable();

    public JavaScriptCompressor(Reader in, ErrorReporter reporter)
            throws IOException, EvaluatorException {
        this.logger = reporter;
        CompilerEnvirons env = new CompilerEnvirons();
        Parser parser = new Parser(env, reporter);
        parser.parse(in, null, 1);
        String encodedSource = parser.getEncodedSource();
        this.tokens = readTokens(encodedSource);
    }

    public void compress(Writer out, int linebreak, boolean munge,
            boolean warn, boolean preserveAllSemiColons)
            throws IOException {

        this.munge = munge;
        this.warn = warn;

        buildSymbolTree();
        mungeSymboltree();
        StringBuffer sb = printSymbolTree(linebreak, preserveAllSemiColons);
        out.write(sb.toString());
    }

    private ScriptOrFnScope getCurrentScope() {
        return (ScriptOrFnScope) scopes.peek();
    }

    private void enterScope(ScriptOrFnScope scope) {
        scopes.push(scope);
    }

    private void leaveCurrentScope() {
        scopes.pop();
    }

    private JavaScriptToken consumeToken() {
        return (JavaScriptToken) tokens.get(offset++);
    }

    private JavaScriptToken getToken(int delta) {
        return (JavaScriptToken) tokens.get(offset + delta);
    }

    /*
     * Returns the identifier for the specified symbol defined in
     * the specified scope or in any scope above it. Returns null
     * if this symbol does not have a corresponding identifier.
     */
    private JavaScriptIdentifier getIdentifier(String symbol, ScriptOrFnScope scope) {
        JavaScriptIdentifier javaScriptIdentifier;
        while (scope != null) {
            javaScriptIdentifier = scope.getIdentifier(symbol);
            if (javaScriptIdentifier != null)
                return javaScriptIdentifier;
            scope = scope.getParentScope();
        }
        return null;
    }

    /*
     * If either 'eval' or 'with' is used in a local scope, we must make
     * sure that all containing local scopes don't get munged. Otherwise,
     * the obfuscation would potentially introduce bugs.
     */
    private void protectScopeFromObfuscation(ScriptOrFnScope scope) {
        assert scope != null;

        if (scope == globalScope) {
            // The global scope does not get obfuscated,
            // so we don't need to worry about it...
            return;
        }

        // Find the highest local scope containing the specified scope.
        while (scope.getParentScope() != globalScope) {
            scope = scope.getParentScope();
        }

        assert scope.getParentScope() == globalScope;
        scope.preventMunging();
    }

    private String getDebugString(int max) {
        assert max > 0;
        StringBuffer result = new StringBuffer();
        int start = Math.max(offset - max, 0);
        int end = Math.min(offset + max, tokens.size());
        for (int i = start; i < end; i++) {
            JavaScriptToken token = (JavaScriptToken) tokens.get(i);
            if (i == offset)
                result.append(" ---> ");
            result.append(token.getValue());
            if (i == offset)
                result.append(" <--- ");
        }
        return result.toString();
    }

    private void warn(String message, boolean showDebugString) {
        if (warn) {
            if (showDebugString) {
                message = message + "\n" + getDebugString(10);
            }
            logger.warning(message, null, -1, null, -1);
        }
    }

    private void parseFunctionDeclaration() {

        String symbol;
        JavaScriptToken token;
        ScriptOrFnScope currentScope, fnScope;

        currentScope = getCurrentScope();

        token = consumeToken();
        if (token.getType() == Token.NAME) {
            if (mode == BUILDING_SYMBOL_TREE) {
                // Get the name of the function and declare it in the current scope.
                symbol = token.getValue();
                if (currentScope.getIdentifier(symbol) != null) {
                    warn("[WARNING] The function " + symbol + " has already been declared in the same scope...", true);
                }
                currentScope.declareIdentifier(symbol);
            }
            token = consumeToken();
        }

        assert token.getType() == Token.LP;
        if (mode == BUILDING_SYMBOL_TREE) {
            fnScope = new ScriptOrFnScope(braceNesting, currentScope);
            indexedScopes.put(new Integer(offset), fnScope);
        } else {
            fnScope = (ScriptOrFnScope) indexedScopes.get(new Integer(offset));
        }

        // Parse function arguments.
        while ((token = consumeToken()).getType() != Token.RP) {
            assert token.getType() == Token.NAME ||
                    token.getType() == Token.COMMA;
            if (token.getType() == Token.NAME && mode == BUILDING_SYMBOL_TREE) {
                symbol = token.getValue();
                fnScope.declareIdentifier(symbol);
            }
        }

        parseScope(fnScope);
    }

    private void parseCatch() {

        String symbol;
        JavaScriptToken token;
        ScriptOrFnScope currentScope;
        JavaScriptIdentifier javaScriptIdentifier;

        token = getToken(-1);
        assert token.getType() == Token.CATCH;
        token = consumeToken();
        assert token.getType() == Token.LP;
        token = consumeToken();
        assert token.getType() == Token.NAME;

        symbol = token.getValue();
        currentScope = getCurrentScope();

        if (mode == BUILDING_SYMBOL_TREE) {
            // We must declare the exception javaScriptIdentifier in the containing function
            // scope to avoid errors related to the obfuscation process. No need to
            // display a warning if the symbol was already declared here...
            currentScope.declareIdentifier(symbol);
        } else {
            javaScriptIdentifier = getIdentifier(symbol, currentScope);
            javaScriptIdentifier.incrementRefcount();
        }

        token = consumeToken();
        assert token.getType() == Token.RP;
    }

    private void parseExpression() {

        // Parse the expression until we encounter a comma or a semi-colon
        // in the same brace nesting, bracket nesting and paren nesting.
        // Parse functions if any...

        String symbol;
        JavaScriptToken token;
        ScriptOrFnScope currentScope;
        JavaScriptIdentifier javaScriptIdentifier;

        int expressionBraceNesting = braceNesting;
        int bracketNesting = 0;
        int parensNesting = 0;

        int length = tokens.size();

        while (offset < length) {

            token = consumeToken();
            currentScope = getCurrentScope();

            switch (token.getType()) {

                case Token.SEMI:
                case Token.COMMA:
                    if (braceNesting == expressionBraceNesting &&
                            bracketNesting == 0 &&
                            parensNesting == 0) {
                        return;
                    }
                    break;

                case Token.FUNCTION:
                    parseFunctionDeclaration();
                    break;

                case Token.LC:
                    braceNesting++;
                    break;

                case Token.RC:
                    braceNesting--;
                    assert braceNesting >= expressionBraceNesting;
                    break;

                case Token.LB:
                    bracketNesting++;
                    break;

                case Token.RB:
                    bracketNesting--;
                    break;

                case Token.LP:
                    parensNesting++;
                    break;

                case Token.RP:
                    parensNesting--;
                    break;

                case Token.NAME:
                    symbol = token.getValue();

                    if (mode == BUILDING_SYMBOL_TREE) {

                        if (symbol.equals("eval")) {
                            protectScopeFromObfuscation(currentScope);
                            warn("[WARNING] Using 'eval' is not recommended..." + (munge ? "\nNote: Using 'eval' reduces the level of compression!" : ""), true);
                        }

                    } else if (mode == CHECKING_SYMBOL_TREE) {

                        if ((offset < 2 || getToken(-2).getType() != Token.DOT) &&
                                getToken(0).getType() != Token.OBJECTLIT) {

                            javaScriptIdentifier = getIdentifier(symbol, currentScope);

                            if (javaScriptIdentifier == null) {

                                if (symbol.length() <= 3 && !builtin.contains(symbol)) {
                                    // Here, we found an undeclared and un-namespaced symbol that is
                                    // 3 characters or less in length. Declare it in the global scope.
                                    // We don't need to declare longer symbols since they won't cause
                                    // any conflict with other munged symbols.
                                    globalScope.declareIdentifier(symbol);
                                    warn("[WARNING] Found an undeclared symbol: " + symbol, true);
                                }

                            } else {

                                javaScriptIdentifier.incrementRefcount();
                            }
                        }
                    }
                    break;
            }
        }
    }

    private void parseScope(ScriptOrFnScope scope) {

        String symbol;
        JavaScriptToken token;
        JavaScriptIdentifier javaScriptIdentifier;

        int length = tokens.size();

        enterScope(scope);

        while (offset < length) {

            token = consumeToken();

            switch (token.getType()) {

                case Token.VAR:
                case Token.CONST:

                    // The var keyword is followed by at least one symbol name.
                    // If several symbols follow, they are comma separated.
                    for (; ;) {
                        token = consumeToken();

                        assert token.getType() == Token.NAME;

                        if (mode == BUILDING_SYMBOL_TREE) {
                            symbol = token.getValue();
                            if (scope.getIdentifier(symbol) == null) {
                                scope.declareIdentifier(symbol);
                            } else {
                                warn("[WARNING] The variable " + symbol + " has already been declared in the same scope...", true);
                            }
                        }

                        token = getToken(0);

                        assert token.getType() == Token.SEMI ||
                                token.getType() == Token.ASSIGN ||
                                token.getType() == Token.COMMA ||
                                token.getType() == Token.IN;

                        if (token.getType() == Token.IN) {
                            break;
                        } else {
                            parseExpression();
                            token = getToken(-1);
                            if (token.getType() == Token.SEMI) {
                                break;
                            }
                        }
                    }
                    break;

                case Token.FUNCTION:
                    parseFunctionDeclaration();
                    break;

                case Token.LC:
                    braceNesting++;
                    break;

                case Token.RC:
                    braceNesting--;
                    assert braceNesting >= scope.getBraceNesting();
                    if (braceNesting == scope.getBraceNesting()) {
                        leaveCurrentScope();
                        return;
                    }
                    break;

                case Token.WITH:
                    if (mode == BUILDING_SYMBOL_TREE) {
                        // Inside a 'with' block, it is impossible to figure out
                        // statically whether a symbol is a local variable or an
                        // object member. As a consequence, the only thing we can
                        // do is turn the obfuscation off for the highest scope
                        // containing the 'with' block.
                        protectScopeFromObfuscation(scope);
                        warn("[WARNING] Using 'with' is not recommended" + (munge ? "(and it reduces the level of compression)" : ""), true);
                    }
                    break;

                case Token.CATCH:
                    parseCatch();
                    break;

                case Token.NAME:
                    symbol = token.getValue();

                    if (mode == BUILDING_SYMBOL_TREE) {

                        if (symbol.equals("eval")) {
                            protectScopeFromObfuscation(scope);
                            warn("[WARNING] Using 'eval' is not recommended..." + (munge ? "\nNote: Using 'eval' reduces the level of compression!" : ""), true);
                        }

                    } else if (mode == CHECKING_SYMBOL_TREE) {

                        if ((offset < 2 || getToken(-2).getType() != Token.DOT) &&
                                getToken(0).getType() != Token.OBJECTLIT) {

                            javaScriptIdentifier = getIdentifier(symbol, scope);

                            if (javaScriptIdentifier == null) {

                                if (symbol.length() <= 3 && !builtin.contains(symbol)) {
                                    // Here, we found an undeclared and un-namespaced symbol that is
                                    // 3 characters or less in length. Declare it in the global scope.
                                    // We don't need to declare longer symbols since they won't cause
                                    // any conflict with other munged symbols.
                                    globalScope.declareIdentifier(symbol);
                                    warn("[WARNING] Found an undeclared symbol: " + symbol, true);
                                }

                            } else {

                                javaScriptIdentifier.incrementRefcount();
                            }
                        }
                    }
                    break;
            }
        }
    }

    private void buildSymbolTree() {
        offset = 0;
        braceNesting = 0;
        scopes.clear();
        indexedScopes.clear();
        indexedScopes.put(new Integer(0), globalScope);
        mode = BUILDING_SYMBOL_TREE;
        parseScope(globalScope);
    }

    private void mungeSymboltree() {

        if (!munge) {
            return;
        }

        // One problem with obfuscation resides in the use of undeclared
        // and un-namespaced global symbols that are 3 characters or less
        // in length. Here is an example:
        //
        //     var declaredGlobalVar;
        //
        //     function declaredGlobalFn() {
        //         var localvar;
        //         localvar = abc; // abc is an undeclared global symbol
        //     }
        //
        // In the example above, there is a slim chance that localvar may be
        // munged to 'abc', conflicting with the undeclared global symbol
        // abc, creating a potential bug. The following code detects such
        // global symbols. This must be done AFTER the entire file has been
        // parsed, and BEFORE munging the symbol tree. Note that declaring
        // extra symbols in the global scope won't hurt.
        //
        // Note: Since we go through all the tokens to do this, we also use
        // the opportunity to count how many times each identifier is used.

        offset = 0;
        braceNesting = 0;
        scopes.clear();
        mode = CHECKING_SYMBOL_TREE;
        parseScope(globalScope);
        globalScope.munge();
    }

    private StringBuffer printSymbolTree(int linebreakpos, boolean preserveAllSemiColons)
            throws IOException {

        offset = 0;
        braceNesting = 0;
        scopes.clear();

        String symbol;
        JavaScriptToken token;
        ScriptOrFnScope currentScope;
        JavaScriptIdentifier javaScriptIdentifier;

        int length = tokens.size();
        StringBuffer result = new StringBuffer();

        int linestartpos = 0;

        enterScope(globalScope);

        while (offset < length) {

            token = consumeToken();
            symbol = token.getValue();
            currentScope = getCurrentScope();

            switch (token.getType()) {

                case Token.NAME:

                    if (offset >= 2 && getToken(-2).getType() == Token.DOT ||
                            getToken(0).getType() == Token.OBJECTLIT) {

                        result.append(symbol);

                    } else {

                        javaScriptIdentifier = getIdentifier(symbol, currentScope);
                        if (javaScriptIdentifier != null) {
                            if (javaScriptIdentifier.getMungedValue() != null) {
                                result.append(javaScriptIdentifier.getMungedValue());
                            } else {
                                result.append(symbol);
                            }
                            if (currentScope != globalScope && javaScriptIdentifier.getRefcount() == 0) {
                                warn("[WARNING] The symbol " + symbol + " is declared but is apparently never used.\nThis code can probably be written in a more efficient way.", true);
                            }
                        } else {
                            result.append(symbol);
                        }
                    }
                    break;

                case Token.REGEXP:
                case Token.NUMBER:
                case Token.STRING:
                    result.append(symbol);
                    break;

                case Token.ADD:
                    if (offset >= 2 && offset < length &&
                            getToken(-2).getType() == Token.STRING &&
                            getToken(0).getType() == Token.STRING &&
                            (offset == length - 1 || getToken(1).getType() != Token.DOT)) {
                        // Here, we have two string literal that are being appended.
                        // Note that we take care of the case "a" + "b".toUpperCase()
                        // Also note that the different quoting styles are normalized
                        // in printSourceString, and the strings are already escaped...

                        // Remove the first string's terminating quote...
                        result.deleteCharAt(result.length() - 1);

                        // Append the second string without its starting quote...
                        result.append(getToken(0).getValue().substring(1));

                        consumeToken(); // skip the second string...
                        break;
                    }

                    /* FALLSTHROUGH */

                case Token.SUB:
                    result.append((String) literals.get(new Integer(token.getType())));
                    if (offset < length) {
                        token = getToken(0);
                        if (token.getType() == Token.INC ||
                                token.getType() == Token.DEC ||
                                token.getType() == Token.ADD ||
                                token.getType() == Token.DEC) {
                            // Handle the case x +/- ++/-- y
                            // We must keep a white space here. Otherwise, x +++ y would be
                            // interpreted as x ++ + y by the compiler, which is a bug (due
                            // to the implicit assignment being done on the wrong variable)
                            result.append(" ");
                        } else if (token.getType() == Token.POS && getToken(-1).getType() == Token.ADD ||
                                token.getType() == Token.NEG && getToken(-1).getType() == Token.SUB) {
                            // Handle the case x + + y and x - - y
                            result.append(" ");
                        }
                    }
                    break;

                case Token.FUNCTION:
                    result.append("function");
                    token = consumeToken();
                    if (token.getType() == Token.NAME) {
                        result.append(" ");
                        symbol = token.getValue();
                        javaScriptIdentifier = getIdentifier(symbol, currentScope);
                        assert javaScriptIdentifier != null;
                        if (javaScriptIdentifier.getMungedValue() != null) {
                            result.append(javaScriptIdentifier.getMungedValue());
                        } else {
                            result.append(symbol);
                        }
                        if (currentScope != globalScope && javaScriptIdentifier.getRefcount() == 0) {
                            warn("[WARNING] The symbol " + symbol + " is declared but is apparently never used.\nThis code can probably be written in a more efficient way.", true);
                        }
                        token = consumeToken();
                    }
                    assert token.getType() == Token.LP;
                    result.append("(");
                    currentScope = (ScriptOrFnScope) indexedScopes.get(new Integer(offset));
                    enterScope(currentScope);
                    while ((token = consumeToken()).getType() != Token.RP) {
                        assert token.getType() == Token.NAME || token.getType() == Token.COMMA;
                        if (token.getType() == Token.NAME) {
                            symbol = token.getValue();
                            javaScriptIdentifier = getIdentifier(symbol, currentScope);
                            assert javaScriptIdentifier != null;
                            if (javaScriptIdentifier.getMungedValue() != null) {
                                result.append(javaScriptIdentifier.getMungedValue());
                            } else {
                                result.append(symbol);
                            }
                        } else if (token.getType() == Token.COMMA) {
                            result.append(",");
                        }
                    }
                    result.append(")");
                    token = consumeToken();
                    assert token.getType() == Token.LC;
                    result.append("{");
                    braceNesting++;
                    break;

                case Token.RETURN:
                    result.append("return");
                    // No space needed after 'return' when followed
                    // by '(', '[', '{', a string or a regexp.
                    if (offset < length) {
                        token = getToken(0);
                        if (token.getType() != Token.LP &&
                                token.getType() != Token.LB &&
                                token.getType() != Token.LC &&
                                token.getType() != Token.STRING &&
                                token.getType() != Token.REGEXP) {
                            result.append(" ");
                        }
                    }
                    break;

                case Token.CASE:
                    result.append("case");
                    // White-space needed after 'case' when not followed by a string.
                    if (offset < length && getToken(0).getType() != Token.STRING) {
                        result.append(" ");
                    }
                    break;

                case Token.THROW:
                    // White-space needed after 'throw' when not followed by a string.
                    result.append("throw");
                    if (offset < length && getToken(0).getType() != Token.STRING) {
                        result.append(" ");
                    }
                    break;

                case Token.BREAK:
                    result.append("break");
                    if (offset < length && getToken(0).getType() != Token.SEMI) {
                        // If 'break' is not followed by a semi-colon, it must be
                        // followed by a label, hence the need for a white space.
                        result.append(" ");
                    }
                    break;

                case Token.CONTINUE:
                    result.append("continue");
                    if (offset < length && getToken(0).getType() != Token.SEMI) {
                        // If 'continue' is not followed by a semi-colon, it must be
                        // followed by a label, hence the need for a white space.
                        result.append(" ");
                    }
                    break;

                case Token.LC:
                    result.append("{");
                    braceNesting++;
                    break;

                case Token.RC:
                    result.append("}");
                    braceNesting--;
                    assert braceNesting >= currentScope.getBraceNesting();
                    if (braceNesting == currentScope.getBraceNesting()) {
                        leaveCurrentScope();
                    }
                    break;

                case Token.SEMI:
                    // No need to output a semi-colon if the next character is a right-curly...
                    if (preserveAllSemiColons || offset < length && getToken(0).getType() != Token.RC)
                        result.append(";");
                    if (linebreakpos >= 0 && result.length() - linestartpos > linebreakpos) {
                        // Some source control tools don't like it when files containing lines longer
                        // than, say 8000 characters, are checked in. The linebreak option is used in
                        // that case to split long lines after a specific column.
                        result.append("\n");
                        linestartpos = result.length();
                    }
                    break;

                default:
                    String literal = (String) literals.get(new Integer(token.getType()));
                    if (literal != null) {
                        result.append(literal);
                    } else {
                        warn("[WARNING] This symbol cannot be printed: " + symbol, true);
                    }
                    break;
            }
        }

        return result;
    }
}

package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

public class Util {
    // ANSI color codes from https://stackoverflow.com/questions/5762491/how-to-print-color-in-console-using-system-out-println
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";
    public static final String ANSI_GREY = "\u001B[90m";

    private static final Map<Character, String> escapeSequences;

    static {
        escapeSequences = new HashMap<>();
        escapeSequences.put('b', "\\b");
        escapeSequences.put('t', "\t");
        escapeSequences.put('n', "\n");
        escapeSequences.put('f', "\f");
        escapeSequences.put('r', "\r");
        escapeSequences.put('"', "\"");
        escapeSequences.put('\'', "'");
        escapeSequences.put('\\', "\\");
    }

    /** 
     *  scans through string, replacing any instances of supported escape characters with the character they represent.
     * 
     *  e.g. "a\tb" becomes "a   b"
     * 
     *  supported characters: \b (passed through), \t, \n, \f, \r, \", \', \\
     * 
     *  @returns the unescaped string.  does not mutate s.
     * 
     *  @throws Exception if an unrecognized escape sequence is encountered.
     */
    public static final String unescapeString(String s) throws Exception {
        StringBuffer ret = new StringBuffer(s.length());
        for (int i = 0; i < s.length(); i ++) {
            char curr = s.charAt(i);
            if (curr == '\\') {
                if (i < s.length() - 1) {
                    char next = s.charAt(++i);
                    String unescaped = escapeSequences.get(next);
                    if (unescaped == null) {
                        throw new Exception("Unrecognized escape sequence \\" + next);
                    }
                    ret.append(unescaped);
                } else {
                   throw new Exception("Could not parse escape sequence at end of string literal");
                }
            } else {
                ret.append(curr);
            }
        }

        return ret.toString();
    }    

    /**
     * Transforms an object into a Lox-appropriate string representation.
     * Generally returns object.toString().
     * If object is null, returns "nil"
     * If object is a Double representing an integer value, the fractional part is removed.
     * @param object Object to stringify.
     * @return String representation of object.
     */
    public static final String stringify(Object object) {
        if (object == null) return "nil";

        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }

            return text;
        }

        return object.toString();
    }

    /**
     * Tests if the provided Double represents an integer value.
     * Useful for interpreting variable accesses where you must differentiate between integer and decimal values.
     */
    public static final boolean isInteger(Double n) {
        return (Math.rint(n) == n) && Double.isFinite(n);
    }
}

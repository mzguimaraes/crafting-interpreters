package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

public class StringUtils {
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

    // scans through string, replacing any instances of supported escape characters with the character they represent.
    // e.g. "a\tb" becomes "a   b"
    // returns the unescaped string.  does not mutate s.
    // throws if an unrecognized escape sequence is encountered.
    // supported characters: \b (passed through), \t, \n, \f, \r, \", \', \\
    public static final String unescapeString(String s) throws Exception {
        // initialize return string
        // iterate through chars in string:
            // if curr is \:
                // identify escape sequence by looking ahead 1
                // if escape sequence recognized, append unescaped value to return string
                // else throw error
                // consume both characters
            // else:
                // append curr to return string

        StringBuffer ret = new StringBuffer(s.length());
        for (int i = 0; i < s.length(); i ++) {
            char curr = s.charAt(i);
            // String q = "foo\\";
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
}

/**
 *  Copyright 2011, 2012 Bill Dortch / RunningReds.org
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.runningreds.horatio.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility methods called by the generated parsers.
 */
public class ParseUtil {
    private ParseUtil() {} // making a Util would be futile
    
    public static String parseStringLiteral(String image) throws StringFormatException {
        char[] buf = image.toCharArray();
        int limit = buf.length;
        int i;
        if (limit >= 2 && buf[0] == '"' && buf[limit - 1] == '"') {
            i = 1;
            --limit;
        } else {
            i = 0;
        }
        StringBuilder sb = new StringBuilder(limit);
        for ( ; i < limit ; i++) {
            char c;
            if ((c = buf[i]) != '\\') {
                sb.append(c);
            } else {
                if (++i < limit) {
                    switch(c = buf[i]) {
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'f': sb.append('\f'); break;
                    case 'b': sb.append('\b'); break;
                    case '\\': sb.append('\\'); break;
                    case '\"': sb.append('\"'); break;
                    case '\'': sb.append('\''); break;
                    default:
                        // should not occur with values passed by ThriftParser
                        throw new StringFormatException("Invalid String literal (unsuppored escape" +
                        		" sequence \\"+c+"): \"" + image + "\"");
                    }
                    
                } else {
                    // should not occur with values passed by ThriftParser
                    throw new StringFormatException("Invalid String literal (trailing \\): \"" + image + "\"");
                }
            }
                
        }
        return sb.toString();
    }

    public static char parseCharLiteral(String image) throws StringFormatException {
        char[] buf = image.toCharArray();
        int limit = buf.length;
        int i;
        if (limit >= 2 && buf[0] == '\'' && buf[limit - 1] == '\'') {
            i = 1;
            --limit;
        } else {
            i = 0;
        }
        if (i < limit) {
            char c;
            if ((c = buf[i]) != '\\') {
                if (limit - i == 1) {
                    return c;
                }
            } else {
                if (limit - ++i == 1) {
                    switch(c = buf[i]) {
                    case 'n': return '\n';
                    case 'r': return '\r';
                    case 't': return '\t';
                    case 'f': return '\f';
                    case 'b': return '\b';
                    case '\\': return '\\';
                    case '\"': return '"';
                    case '\'': return '\'';
                    }
                }
            }
        }
        throw new StringFormatException("Invalid char literal: " + image);
    }

    public static byte parseByteLiteral(String image) throws NumberFormatException {
        if (image.charAt(0) == '0') {
            int len;
            if ((len = image.length()) == 1) {
                return 0;
            } else if (len > 2 && (image.charAt(1) | 0x20) == 'x') {
                int value = Integer.parseInt(image.substring(2), 16);
                if (value <= 0xff) {
                    return (byte)value;
                }
                throw new  NumberFormatException("Value out of range of byte ("+image+")");
            }
        }
        return Byte.parseByte(image);
    }

    public static short parseShortLiteral(String image) throws NumberFormatException {
        if (image.charAt(0) == '0') {
            int len;
            if ((len = image.length()) == 1) {
                return 0;
            } else if (len > 2 && (image.charAt(1) | 0x20) == 'x') {
                int value = Integer.parseInt(image.substring(2), 16);
                if (value <= 0xffff) {
                    return (short)value;
                }
                throw new  NumberFormatException("Value out of range of short ("+image+")");
            }
        }
        return Short.parseShort(image);
    }

    public static int parseIntLiteral(String image) throws NumberFormatException {
        if (image.charAt(0) == '0') {
            int len;
            if ((len = image.length()) == 1) {
                return 0;
            } else if (len > 2 && (image.charAt(1) | 0x20) == 'x') {
                long value = Long.parseLong(image.substring(2), 16);
                if (value <= 0xffffffffL) {
                    return (int)value;
                }
                throw new  NumberFormatException("Value out of range of int ("+image+")");
            }
        }
        return Integer.parseInt(image);
    }
    
    public static long parseLongLiteral(String image) throws NumberFormatException {
        if (image.charAt(0) == '0') {
            int len;
            if ((len = image.length()) == 1) {
                return 0;
            } else if (len > 2 && (image.charAt(1) | 0x20) == 'x') {
                // FIXME: MAYBE: SOMEDAY: deal with high bit set for 64-bit hex values
                return Long.parseLong(image.substring(2), 16);
            }
        }
        return Long.parseLong(image);
    }
    
    public static List<String> parseDocComments(String image) {
        if (!image.startsWith("/**")) {
            return null;
        }
        ArrayList<String> list = new ArrayList<String>();
        try {
            BufferedReader lines = new BufferedReader(new StringReader(image));
            String line;
            while ((line = lines.readLine()) != null) {
                // remove doc comment artifacts
                line = line.trim();
                if (line.startsWith("/*")) {
                    line = line.substring(2);
                }
                while (line.startsWith("*") && !line.startsWith("*/")) {
                    line = line.substring(1);
                }
                if (line.endsWith("*/")) {
                    line = line.substring(0, line.length() - 2);
                }
                list.add(line);
            }
        } catch(IOException e) {
            // won't happen
            throw new RuntimeException(e);
        }
        // remove leading/trailing blank lines
        while (!list.isEmpty() && list.get(0).trim().length() == 0) {
            list.remove(0);
        }
        while (!list.isEmpty() && list.get(list.size() - 1).trim().length() == 0) {
            list.remove(list.size() - 1);
        }
        return list.isEmpty() ? null : list;
    }
}

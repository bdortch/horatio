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

package org.runningreds.horatio.generator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.runningreds.horatio.GenerationException;
import org.runningreds.horatio.model.CompositeType;
import org.runningreds.horatio.model.FieldDef;
import org.runningreds.horatio.model.StructType;
import org.runningreds.horatio.model.Type;

public class GenUtil {
    
    // FIXME: rethink this subst thing.  may want more detailed spec, e.g.
    //  html -> HTML in most cases, but -> html for initial seg in lcc.
    // same for id/ID, :  "int myID", but "int id" 

    private final Map<String,String[]> nameSegMap = new HashMap<String,String[]>();
    
    
    public void addNameSegMapping(String lcval, String ccval) {
        nameSegMap.put(lcval.toLowerCase(), new String[] {lcval, ccval});
    }
    
    public void addNameSegMapping(String lcval, String lcseg, String ccseg) {
        nameSegMap.put(lcval.toLowerCase(), new String[] {lcseg, ccseg});
    }
    
    public String lcseg(String seg) {
        String[] mapping = nameSegMap.get(seg.toLowerCase());
        if (mapping == null) {
            return decap(seg);
        } else {
            return mapping[0];
        }
    }
    
    public String ccseg(String seg) {
        String[] mapping = nameSegMap.get(seg.toLowerCase());
        if (mapping == null) {
            return cap(seg);
        } else {
            return mapping[1];
        }
    }
    
    
    public String cap(String value) {
        if (value != null && !value.isEmpty()) {
            if (!Character.isUpperCase(value.charAt(0))) {
                return new StringBuilder(value.length())
                    .append(Character.toUpperCase(value.charAt(0)))
                    .append(value.substring(1))
                    .toString();
            }
        }
        return value;
    }
    
    public String decap(String value) {
        if (value != null && !value.isEmpty()) {
            if (!Character.isLowerCase(value.charAt(0))) {
                return new StringBuilder(value.length())
                    .append(Character.toLowerCase(value.charAt(0)))
                    .append(value.substring(1))
                    .toString();
            }
        }
        return value;
    }
    
    public String uc(String value) {
        return value == null ? value : value.toUpperCase();
    }
    
    public String lc(String value) {
        return value == null ? null : value.toLowerCase();
    }
    
    public String ucamel(String value) {
        if (value != null && !value.isEmpty()) {
            if (value.indexOf('_') < 0) {
                return ccseg(value);
            }
            String[] segs = value.split("_");
            StringBuilder b = new StringBuilder(value.length());
            for (String seg : segs) {
                b.append(ccseg(seg));
            }
            return b.toString();
        }
        return value;
    }

    public String lcamel(String value) {
        if (value != null && !value.isEmpty()) {
            if (value.indexOf('_') < 0) {
                return lcseg(value);
            }
            String[] segs = value.split("_");
            StringBuilder b = new StringBuilder(value.length());
            b.append(lcseg(segs[0]));
            for (int i = 1, limit = segs.length; i < limit; i++) {
                b.append(ccseg(segs[i]));
            }
            return b.toString();
        }
        return value;
    }
    
    private static final Pattern CSPLIT_CASE = Pattern.compile("([a-z][0-9]*)([A-Z])");
    private static final Pattern CSPLIT_MULT = Pattern.compile("([A-Z])([A-Z][a-z])");
    
    public String decamel(String value) {
        // the double-match handles cases of sequences of upper-case
        // characters, e.g,,  MyHTMLParser -> my_html_parser
        if (value != null && !value.isEmpty()) {
            return CSPLIT_MULT.matcher(CSPLIT_CASE.matcher(value).replaceAll("$1_$2"))
                .replaceAll("$1_$2")
                .toLowerCase();
        }
        return value;
    }
    
    public int maxNameWidth(StructType struct) {
        int max = 0;
        for (FieldDef field : struct.getFields()) {
            int w;
            if ((w = decamel(field.getName()).length()) > max) {
                max = w;
            }
        }
        return max;
    }

    // 64 spaces
    private static final String SPACES = "                                                                ";
    
    public String pad(String value, int width) {
        if (value == null) {
            value = "";
        }
        int size = Math.min(Math.max(width - value.length(), 1), SPACES.length());
        return SPACES.substring(0, size);
    }

    
    // FIXME: NEXT: primValue/objValue
    
    public String objType(Type type) {
        Type base = type.getBase();
        switch(type.getCategory()) {
        case BOOLEAN:
            return "Boolean";
        case INTEGER:
            if (base == Type.BYTE) {
                return "Byte";
            } else if (base == Type.I16) {
                return "Short";
            } else if (base == Type.I32) {
                return "Integer";
            } else {
                return "Long";
            }
        case FLOATING_POINT:
            return "Double";
        case STRING:
            return "String";
        case BINARY:
            return "byte[]";
        case CONTAINER:
            StringBuilder sb = new StringBuilder(32);
            if (type.isList()) {
                sb.append("List<");
            } else if (type.isMap()) {
                sb.append("Map<");
            } else {
                sb.append("Set<");
            }
            List<Type> types = ((CompositeType)type).getElementTypes();
            for (int i = 0, max = types.size() - 1; i <= max; i++) {
                sb.append(objType(types.get(i)));
                if (i < max) {
                    sb.append(", ");
                }
            }
            return sb.append('>').toString();
        default:
            return ucamel(type.getName());
        }
    }
    
    public String primType(Type type) {
        Type base = type.getBase();
         if (!base.isPrimitive()) {
             return objType(type);
         }
        switch(type.getCategory()) {
        case BOOLEAN:
            return "boolean";
        case INTEGER:
            if (base == Type.BYTE) {
                return "byte";
            } else if (base == Type.I16) {
                return "short";
            } else if (base == Type.I32) {
                return "int";
            } else {
                return "long";
            }
        case FLOATING_POINT:
            return "double";
        default:
            // should not get here
            throw new GenerationException();
        }
    }
    
    public String thriftType(Type type) {
        Type base = type.getBase();
        switch(base.getCategory()) {
        case VOID:
            return "VOID";
        case BOOLEAN:
            return "BOOL";
        case INTEGER:
            if (base == Type.BYTE) {
                return "BYTE";
            } else if (base == Type.I16) {
                return "I16";
            } else if (base == Type.I32) {
                return "I32";
            } else {
                return "I64";
            }
        case FLOATING_POINT:
            return "DOUBLE";
        case STRING:
            return "STRING";
        case BINARY:
            return "BINARY";
        case STRUCT:
            return "STRUCT";
        case CONTAINER:
            if (base.isList()) {
                return "LIST";
            } else if (base.isMap()) {
                return "MAP";
            } else {
                return "SET";
            }
        case ENUM:
            return "ENUM";
        default:
            return "???";
        }
    }

    static void p(Object o) {
        System.out.println(o);
    }
    public static void main(String[] args) {
        GenUtil j = new GenUtil();
        j.addNameSegMapping("id", "ident", "Ident");
        j.addNameSegMapping("html", "HTML");
        p(j.lcamel("object_id"));
        p(j.ucamel("object_id"));
        p(j.lcamel("html_parser"));
        p(j.ucamel("html_parser"));
        p(j.lcamel("foo"));
        p(j.ucamel("foo"));
        p(j.decamel("STypedRoleRelIDList"));
        
    }

















}

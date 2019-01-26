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

package org.runningreds.horatio.model;

import java.util.List;

public class Type {
    
    private static final long LONG2DOUB_MAX = 1L << 53;
    private static final long LONG2DOUB_MIN = -LONG2DOUB_MAX;
    
    public enum Category { // FIXME: PRIMITIVE vs. String/bytes
        BOOLEAN,
        INTEGER,
        FLOATING_POINT,
        STRING,
        BINARY,
        ENUM,
        STRUCT,
        CONTAINER,
        EXCEPTION,
        FUNCTION,
        SERVICE,
        VOID,
    }
    
    
    public Object validate(Object value) throws ModelException {
        if (value instanceof ConstDef) {
            ConstDef cdef = (ConstDef)value;
            if (cdef.type == this) {
                // must be valid
                return cdef.value;
            }
            value = cdef.value;
        }
        if (value == null) {
            if (isPrimitive() || isEnum()) {
                throw new ModelException("null not permitted for type " + name);
            }
            return null;
        }
        outer:
        switch(category) {
        case VOID:
            break;
        case BOOLEAN:
            if (value instanceof Boolean) {
                return value;
            }
            if (value instanceof Number) {
                return ((Number)value).longValue() != 0;
            }
            break;
        case INTEGER:
            if (value instanceof Number) {
                if (value instanceof Long || value instanceof Integer || value instanceof Short || value instanceof Byte) {
                    long numval = ((Number)value).longValue();
                    if ((this == BYTE && numval <= Byte.MAX_VALUE && numval >= Byte.MIN_VALUE) ||
                            (this == I16 && numval <= Short.MAX_VALUE && numval >= Short.MIN_VALUE) ||
                            (this == I32 && numval <= Integer.MAX_VALUE && numval >= Integer.MIN_VALUE) ||
                            (this == I64)) {
                        return value;
                    }
                    throw new ModelException("Value outside range of type " + name + ": " + value);
                }
            } else if (value instanceof Character) {
                if (this == BYTE && ((Character)value).charValue() > Byte.MAX_VALUE) {
                    throw new ModelException("Character value outside range of type byte: " + value);
                }
                return value;
            }
            break;
        case FLOATING_POINT:
            if (value instanceof Double){
                return value;
            }
            if (value instanceof Number) {
                long numval = ((Number)value).longValue();
                if (numval <= LONG2DOUB_MAX && numval >= LONG2DOUB_MIN) {
                    return Double.valueOf((double)numval);
                }
                throw new ModelException("Value exceeds precision of double: " + value);
            }
            break;
        case STRING:
            if (value instanceof String) {
                return value;
            }
            break;
        case BINARY:
            if (value instanceof byte[]) {
                return value;
            } else if (value instanceof List) {
                for (Object elem : ((List<?>)value)) {
                    if (!(elem instanceof Byte || 
                            (elem instanceof Character && ((Character)elem).charValue() <= Byte.MAX_VALUE))) {
                        break outer;
                    }
                }
                return value;
            }
            break;
        default:
            // Enum, Struct, List, Set and Map validated by their respective Type subclasses
            break;
        }
        throw new ModelException("Illegal value for type " + name + ": " + value + 
                " (" + value.getClass().getSimpleName() + ")");
    }
    
    public static final Type BOOL    = new Type("bool",   Category.BOOLEAN);
    public static final Type BYTE    = new Type("byte",   Category.INTEGER);
    public static final Type I16     = new Type("i16",    Category.INTEGER);
    public static final Type I32     = new Type("i32",    Category.INTEGER);
    public static final Type I64     = new Type("i64",    Category.INTEGER);
    public static final Type DOUBLE  = new Type("double", Category.FLOATING_POINT);
    public static final Type BINARY  = new Type("binary", Category.BINARY);
    public static final Type STRING  = new Type("string", Category.STRING);
    public static final Type VOID    = new Type("void",   Category.VOID);
    
//    static final Map<String, Type> typeMap = 
//        Collections.synchronizedMap(new HashMap<String, Type>());
//    
//    static {
//        // we do not register VOID with other types, as it is only valid with functions
//        for (Type type : new Type[] { BOOL, BYTE, I16, I32, I64, DOUBLE, BINARY, STRING } ) {
//            registerTypeAs(type.getName(), type);
//        }
//    }
//    
//    public static Type get(String name) {
//        return typeMap.get(name);
//    }
//    
//    public static boolean isRegistered(String name) {
//        return typeMap.containsKey(name);
//    }
//    
//    public static void registerType(Type type) throws ModelException {
//        registerTypeAs(type.getName(), type);
//    }
//    
//    public static void registerTypeAs(String name, Type type) throws ModelException {
//        synchronized(typeMap) {
//            if (typeMap.containsKey(name)) {
//                throw new ModelException("Type \"" + name + "\" already registered");
//            }
//            typeMap.put(name, type);
//        }
//    }
//    
//    public static List<Type> getRegisteredTypes() {
//        synchronized(typeMap) {
//            return new ArrayList<Type>(typeMap.values());
//        }
//    }

    public static void registerPrimitiveTypes(ModelSet modelSet) {
        // we do not register VOID with other types, as it is only valid with functions
        for (Type type : new Type[] { BOOL, BYTE, I16, I32, I64, DOUBLE, BINARY, STRING } ) {
            modelSet.registerType(type.getName(), type);
        }
    }
    
    final Type base;
    final ThriftModel schema;
    final String name;
    final Category category;
    
    protected Type(ThriftModel schema, Type base, String name, Category category) {
        this.schema = schema;
        this.base = base == null ? this : base;
        this.name = name;
        this.category = category;
    }
    
    public Type(String name, Category category) {
        this((ThriftModel)null, null, name, category);
    }
    
    public Type(ThriftModel schema, String name, Category category) {
        this(schema, null, name, category);
    }
    
    public ThriftModel getSchema() {
        return schema;
    }

    public Type getBase() {
        return base;
    }

    public String getName() {
        return name;
    }
    
    public String getQName() {
        if (schema != null && schema.getName() != null) {
            return schema.getName() + '.' + getName();
        }
        return getName();
    }
    
    public final Category getCategory() {
        return category;
    }

    public final boolean isBoolean() {
        return category == Category.BOOLEAN;
    }
    
    public final boolean isInteger() {
        return category == Category.INTEGER;
    }
    
    public final boolean isEnum() {
        return category == Category.ENUM;
    }
    
    public final boolean isFloatingPoint() {
        return category == Category.FLOATING_POINT;
    }
    
    public final boolean isNumeric() {
        return category == Category.INTEGER || category == Category.FLOATING_POINT;
    }
    
    public final boolean isPrimitive() {
        return isNumeric() || isBoolean();
    }
    
    public final boolean isBinary() {
        return category == Category.BINARY;
    }
    
    public final boolean isString() {
        return category == Category.STRING;
    }
    
    public final boolean isContainer() {
        return category == Category.CONTAINER;
    }
    
    public final boolean isMap() {
        return this instanceof MapType;
    }
    
    public final boolean isSet() {
        return this instanceof SetType;
    }
    
    public final boolean isList() {
        return this instanceof ListType;
    }
    
    public boolean isStruct() {
        return category == Category.STRUCT;
    }
    
    public boolean isException() {
        return category == Category.EXCEPTION;
    }
    
    public boolean isVoid() {
        return category == Category.VOID;
    }
    
    public String toString() {
        return getName();
    }

}

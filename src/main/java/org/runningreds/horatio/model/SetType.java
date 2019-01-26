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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class SetType extends CompositeType {

    private SetType(String name, Type elemType) {
        super(name, Category.CONTAINER, elemType);
    }
    
    public Type getElementType() {
        return elemTypes.get(0);
    }

    public static final String nameForElemType(Type elemType) {
        return "set<"+elemType.getName()+">";
    }

    public static final SetType valueOf(Type elemType, ModelSet modelSet) {
        String name = nameForElemType(elemType);
        return (SetType)modelSet.registerTypeIfNew(name, new SetType(name, elemType));
    }

    @Override
    public Object validate(Object value) throws ModelException {
        if (value instanceof ConstDef) {
            ConstDef cdef = (ConstDef)value;
            value = cdef.value;
            if (cdef.type == this) {
                return value;
            }
        }
        if (value instanceof Collection) {
            Set<Object> set = new HashSet<Object>(((Collection<?>)value).size());
            Type elemType = getElementType();
            for (Object elem : (Collection<?>)value) {
                set.add(elemType.validate(elem));
            }
            return set;
        } else {
            return super.validate(value); // will throw
        }
    }

}

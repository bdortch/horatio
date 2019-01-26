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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FieldSet {
    public enum SetType {
        STRUCT,
        UNION,
        EXCEPTION,
        ARGS,
    }
    private final List<FieldDef> fieldList = Collections.synchronizedList(new ArrayList<FieldDef>());
    private final Map<String, FieldDef> fieldMap = new HashMap<String, FieldDef>();
    private final Set<Short> idSet = new HashSet<Short>();
    
    private final String ownerName;
    
    public FieldSet(String ownerName) {
        this.ownerName = ownerName;
    }

    public synchronized void addField(FieldDef field) throws ModelException {
        if (idSet.contains(field.id)) {
            throw new ModelException("Duplicate field id (" + field.id +
                    ") in  \"" + ownerName + "\"");
        }
        if (fieldMap.containsKey(field.name)) {
            throw new ModelException("Duplicate field name \"" + field.name +
                    "\" in  \"" + ownerName + "\"");
        }
        fieldList.add(field);
        fieldMap.put(field.name, field);
        idSet.add(field.id);
    }
    
    public synchronized List<FieldDef> getFields() {
        return Collections.unmodifiableList(fieldList);
    }
    
    public boolean hasField(String name) {
        return fieldMap.containsKey(name);
    }
    
    public FieldDef getField(String name) {
        return fieldMap.get(name);
    }
    
    public boolean hasFields() {
        return !fieldList.isEmpty();
    }
    
}

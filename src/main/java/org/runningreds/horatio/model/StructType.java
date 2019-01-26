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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StructType extends Type implements Named {

    private final boolean isUnion;
    private final FieldSet fields;
    private final Map<String, Object> annotations = Collections.synchronizedMap(new HashMap<String,Object>(2,2));
    private final List<String> docComments;
    
    public StructType(ThriftModel schema, String name, boolean isUnion, List<String> docComments) {
        super(schema, name, Category.STRUCT);
        this.isUnion = isUnion;
        this.fields = new FieldSet(getQName());
        this.docComments = docComments;
    }

    public StructType(ThriftModel schema, String name, boolean isUnion) {
        this(schema, name, isUnion, (List<String>)null);
    }
    
    public boolean isUnion() {
        return isUnion;
    }

    public void addField(FieldDef field) throws ModelException {
        fields.addField(field);
    }

    public List<FieldDef> getFields() {
        return fields.getFields();
    }

    public boolean hasField(String name) {
        return fields.hasField(name);
    }

    public FieldDef getField(String name) {
        return fields.getField(name);
    }

    public boolean hasFields() {
        return fields.hasFields();
    }

    public void addAnnotation(String name, Object value) {
        annotations.put(name, value);
    }
    
    public boolean hasAnnotations() {
        return !annotations.isEmpty();
    }

    public Map<String, Object> getAnnotations() {
        return annotations;
    }
    
    public List<String> getDocComments() {
        return docComments;
    }
    
}

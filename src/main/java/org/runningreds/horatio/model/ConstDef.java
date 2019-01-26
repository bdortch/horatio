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

public class ConstDef extends ModelComponent {
    final String name;
    final Type type;
    final Object value;
    private final Map<String, Object> annotations = Collections.synchronizedMap(new HashMap<String,Object>(2,2));
    private final List<String> docComments;
    
    public ConstDef(ThriftModel schema, String name, Type type, Object value, List<String> docComments) throws ModelException {
        super(schema);
        value = type.validate(value);
        this.name = name;
        this.type = type;
        this.value = value;
        this.docComments = docComments;
    }
 
    public ConstDef(ThriftModel schema, String name, Type type, Object value) throws ModelException {
        this(schema, name, type, value, (List<String>)null);
    }
    
    @Override
    public String getQName() {
        return schema.qname(name);
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }
    
    public boolean isEnumElement() {
        return false;
    }
    
    public EnumType getEnclosingEnum() {
        return null;
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

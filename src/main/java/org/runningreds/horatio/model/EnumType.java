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

import java.util.*;

public class EnumType extends Type implements Named {

    public class Elem {

        final String name;
        final Integer value;
        
        private Elem(String name, Integer value) {
            this.name = name;
            this.value = value;
        }
        
        public String getName() {
            return name;
        }
        
        public Integer getValue() {
            return value;
        }
        
        public EnumType getEnclosingEnum() {
            return EnumType.this;
        }
        
        public String getQName() {
            return EnumType.this.getQName() + '.' + name;
        }
        
        @Override
        public String toString() {
            return getQName();
        }

    }
    

    private final List<Elem> elemList = Collections.synchronizedList(new ArrayList<Elem>());
    private final Map<String, Elem> elemMap = new HashMap<String,Elem>();
    private final Set<Integer> idSet = new HashSet<Integer>();
    private final List<String> docComments;
    private volatile int nextID = 0;
    private volatile boolean complete;
    
    
    
    public EnumType(ThriftModel schema, String name, List<String> docComments) {
        super(schema, name, Category.ENUM);
        this.docComments = docComments;
    }

    public EnumType(ThriftModel schema, String name) {
        this(schema, name, (List<String>)null);
    }

    public void setComplete() {
        this.complete = true;
    }

    public String getName() {
        return name;
    }
    
    public String getQName() {
        return schema.qname(name);
    }
    
    public void addElem(String name) throws ModelException {
        addElem(name, nextID);
    }
    
    public synchronized void addElem(String name, int id) throws ModelException {
        if (complete) {
            throw new ModelException("Can't add element to completed EnumDef " + name);
        }
        if (elemMap.containsKey(name)) {
            throw new ModelException("Duplicate enum element name \"" + name +
                    "\" in enum \"" + getQName() + "\"");
        }
        if (idSet.contains(id)) {
            throw new ModelException("Duplicate enum element id (" + id + 
                    ") in enum \"" + getQName() + "\"");
        }
        if (id >= nextID) {
            nextID = id + 1;
        }
        Elem elem = new Elem(name, id);
        elemList.add(elem);
        elemMap.put(name, elem);
        idSet.add(id);
    }
    
    public synchronized List<Elem> getElements() {
        return Collections.unmodifiableList(elemList);
    }
    
    // would create a map by name, except that I expect this usage to be rare. 
    public synchronized Elem getElement(String name) {
        return elemMap.get(name);
    }

    @Override
    public Object validate(Object value) throws ModelException{
        if (value instanceof ConstDef) {
            ConstDef cdef = (ConstDef)value;
            value = cdef.value;
            if (cdef.type == this) {
                return value;
            }
        }
        if (value instanceof EnumType.Elem && ((EnumType.Elem)value).getEnclosingEnum() == this) {
            return value;
        }
        return super.validate(value); // throws
    }

    public List<String> getDocComments() {
        return docComments;
    }

    
    
}

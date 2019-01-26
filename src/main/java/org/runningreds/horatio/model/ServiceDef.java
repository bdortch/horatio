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
import java.util.List;
import java.util.Map;

public class ServiceDef extends ModelComponent implements Named {

    final String name;
    final ServiceDef superDef;
    private final List<FunctionDef> funcList = Collections.synchronizedList(new ArrayList<FunctionDef>());
    private final Map<String, FunctionDef> funcMap = new HashMap<String, FunctionDef>();
    private final List<String> docComments;
    
    public ServiceDef(ThriftModel schema, String name, ServiceDef superDef, List<String> docComments) {
        super(schema);
        this.name = name;
        this.superDef = superDef;
        this.docComments = docComments;
    }
    
    public ServiceDef(ThriftModel schema, String name, ServiceDef superDef) {
        this(schema, name, superDef, (List<String>)null);
    }

    public String getName() {
        return name;
    }

    public ServiceDef getSuperDef() {
        return superDef;
    }

    @Override
    public String getQName() {
        return  schema.qname(name);
    }
    
    public synchronized void addFunction(FunctionDef def) throws ModelException {
        String name = def.getName();
        
        // FIXME: allow override by subtypes? check Thrift impl
        if (isFunctionDefined(name)) {
            throw new ModelException("Duplicate function name \"" + name +
                    "\" in  \"" + getQName() + "\"");
        }
        funcList.add(def);
        funcMap.put(name, def);
    }
    
    /**
     * Returns only the functions defined by this service, excluding
     * those defined in ancestor services.
     * @return the functions defined by this service, excluding
     *         those defined in ancestor services.
     */
    public synchronized List<FunctionDef> getDefinedFunctions() {
        return Collections.unmodifiableList(funcList);
    }
    
    /**
     * Returns the functions defined  by this service, plus any
     * defined by ancestor services.
     * @return the functions defined  by this service, plus any
     *         defined by ancestor services.
     */
    public synchronized List<FunctionDef> getFunctions() {
        if (superDef == null) {
            return new ArrayList<FunctionDef>(funcList);
        }
        List<FunctionDef> list = superDef.getFunctions();
        list.addAll(funcList);
        return list;
    }
    
    public boolean isFunctionDefined(String name) {
        return getFunction(name) != null;
    }
    
    public synchronized FunctionDef getFunction(String name) {
        FunctionDef def;
        if ((def = funcMap.get(name))  != null) {
            return def;
        }
        if (superDef != null) {
            return superDef.getFunction(name);
        }
        return null;
    }

    public List<String> getDocComments() {
        return docComments;
    }
    
    

}

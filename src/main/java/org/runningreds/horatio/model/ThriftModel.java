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


public class ThriftModel {
    
    private final Map<String, NamespaceDef> namespaceDefs = Collections.synchronizedMap(new HashMap<String, NamespaceDef>());
    private final Map<String, ConstDef> constDefs = Collections.synchronizedMap(new HashMap<String, ConstDef>());
    private final Map<String, EnumType.Elem> enumElemDefs = Collections.synchronizedMap(new HashMap<String, EnumType.Elem>());
    private final Map<String, EnumType> enumDefs = Collections.synchronizedMap(new HashMap<String, EnumType>());
    private final Map<String, StructType> structDefs = Collections.synchronizedMap(new HashMap<String, StructType>());
    private final Map<String, ExceptionType> excepDefs = Collections.synchronizedMap(new HashMap<String, ExceptionType>());
    private final Map<String, ServiceDef> serviceDefs  = Collections.synchronizedMap(new HashMap<String, ServiceDef>());
    
    private final ModelSet models;
    final String name;
    
    ThriftModel(ModelSet modelSet, String name) {
        this.models = modelSet;
        this.name = name;
    }

    public synchronized Object lookup(String name) {
        Object val;
        // local cases first:
        if ((val = constDefs.get(name)) != null ||
                (val = enumElemDefs.get(name)) != null ||
                (val = enumDefs.get(name)) != null ||
                (val = structDefs.get(name)) != null ||
                (val = excepDefs.get(name)) != null ||
                (val = serviceDefs.get(name)) != null)
        {
            return val;
        }
        // not here, try other (included) models
        int index;
        if ((index = name.indexOf('.')) > 0) {
            ThriftModel model;
            if ((model = models.getModel(name.substring(0, index))) != null) {
                return model.lookup(name.substring(index + 1));
            }
        }
        return null;
    }
    
    public Type lookupType(String name) {
        Type type;
        if ((type = models.getType(name)) != null || (type = models.getType(qname(name))) != null) {
            return type;
        }
        return null;
    }
    
    
    public ModelSet getModelSet() {
        return models;
    }

    public String getName() {
        return name;
    }

    public void addNamespace(NamespaceDef def) throws ModelException {
        String gen = def.generator;
        synchronized(namespaceDefs) {
            if (namespaceDefs.containsKey(gen)) {
                throw new ModelException("Duplicate namespace defined for generator \"" + gen + "\"");
            }
            namespaceDefs.put(gen, def);
        }
    }
    
    public Map<String, NamespaceDef> getNamespaces() {
        return namespaceDefs;
    }
    
    public NamespaceDef getNamespace(String generator) {
        return namespaceDefs.get(generator);
    }
    
    private void checkName(String name) throws ModelException {
        Object named;
        if ((named = lookup(name)) != null) {
            throw new ModelException("Name \"" + name + "\" already defined by " + named);
        }
    }
    
    public synchronized void addConst(ConstDef def) throws ModelException {
        String name = def.name;
        checkName(name);
        constDefs.put(name, def);
    }
    
    public Map<String, ConstDef> getConsts() {
        return constDefs;
    }
    
    public synchronized void addEnum(EnumType def) throws ModelException {
        String name = def.name;
        checkName(name);
        enumDefs.put(name, def);
        models.registerType(def.getQName(), def);
        for (EnumType.Elem elem : def.getElements()) {
            String elemName = name + '.' + elem.getName();
            enumElemDefs.put(elemName, elem);
        }
    }
    
    public Map<String, EnumType> getEnums()  {
        return enumDefs;
    }
    
    public synchronized void addStruct(StructType def) throws ModelException {
        String name = def.name;
        checkName(name);
        structDefs.put(name, def);
        models.registerType(def.getQName(), def);
    }
    
    public Map<String, StructType> getStructs() {
        return structDefs;
    }
    
    public synchronized void addException(ExceptionType def) throws ModelException {
        String name = def.name;
        checkName(name);
        excepDefs.put(name, def);
        models.registerType(def.getQName(), def);
    }
    
    public Map<String, ExceptionType> getExceptions() {
        return excepDefs;
    }

    public synchronized void addService(ServiceDef def) throws ModelException {
        String name = def.name;
        checkName(name);
        serviceDefs.put(name, def);
    }
    
    public Map<String, ServiceDef> getServices() {
        return serviceDefs;
    }
    
    public ServiceDef getService(String name) throws ModelException {
        Object named;
        if ((named = lookup(name)) != null) {
            if (named instanceof ServiceDef) {
                return (ServiceDef)named;
            }
            throw new ModelException("Object \"" + name + "\" is not a service: " + named);
        }
        throw new ModelException("Service \"" + name + "\" not defined");
    }

    
    public String qname(String name) {
        return this.name == null ? name : this.name + "." + name;
    }


    static void p(Object o) {
        System.out.println(o);
    }








}

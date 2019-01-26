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

public class FunctionDef extends ModelComponent implements Named {

    private final ServiceDef service;
    private final String name;
    private final Type type;
    private final boolean oneway;
    private final FieldSet argFields;
    private final FieldSet throwsFields;
    private final List<String> docComments;
    
    public FunctionDef(ServiceDef service, String name, Type type, boolean oneway, List<String> docComments) {
        super(service.getSchema());
        this.service = service;
        this.name  = name;
        this.type = type;
        this.oneway = oneway;
        this.argFields = new FieldSet(getQName() + ".<args>");
        this.throwsFields = new FieldSet(getQName() + ".<throws>");
        this.docComments = docComments;
    }
    
    public FunctionDef(ServiceDef service, String name, Type type, boolean oneway) {
        this(service, name, type, oneway, (List<String>)null);
    }

    public ServiceDef getService() {
        return service;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public boolean isOneway() {
        return oneway;
    }

    @Override
    public String getQName() {
        return service.getQName() + '.' + name;
    }

    public void addField(FieldDef field) throws ModelException {
        argFields.addField(field);
    }

    public List<FieldDef> getFields() {
        return argFields.getFields();
    }

    public boolean hasField(String name) {
        return argFields.hasField(name);
    }

    public FieldDef getField(String name) {
        return argFields.getField(name);
    }

    public boolean hasFields() {
        return argFields.hasFields();
    }


    public void addThrowsField(FieldDef field) throws ModelException {
        throwsFields.addField(field);
    }

    public List<FieldDef> getThrowsFields() {
        return throwsFields.getFields();
    }

    public boolean hasThrowsField(String name) {
        return throwsFields.hasField(name);
    }

    public FieldDef getThrowsField(String name) {
        return throwsFields.getField(name);
    }

    public boolean hasThrowsFields() {
        return throwsFields.hasFields();
    }

    public List<String> getDocComments() {
        return docComments;
    }
    
    

}

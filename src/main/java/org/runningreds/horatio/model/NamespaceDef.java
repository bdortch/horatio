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

public class NamespaceDef extends ModelComponent {

    final String generator;
    final String namespace;
    
    
    public NamespaceDef(ThriftModel schema, String generator, String namespace) {
        super(schema);
        this.generator = generator;
        this.namespace = namespace;
    }
    
    public boolean isWildcardGenerator() {
        return "*".equals(generator);
    }

    public String getGenerator() {
        return generator;
    }

    public String getNamespace() {
        return namespace;
    }
    
    
    public String getQName() {
        return generator;
    }

    
    

}

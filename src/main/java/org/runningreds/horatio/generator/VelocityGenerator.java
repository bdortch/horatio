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

package org.runningreds.horatio.generator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.runningreds.horatio.FileRef;
import org.runningreds.horatio.GenerationException;
import org.runningreds.horatio.model.EnumType;
import org.runningreds.horatio.model.NamespaceDef;
import org.runningreds.horatio.model.ServiceDef;
import org.runningreds.horatio.model.StructType;
import org.runningreds.horatio.model.ThriftModel;
import org.runningreds.horatio.parser.Parsers;

import static org.runningreds.horatio.Horatio.*;
import static org.runningreds.horatio.GenspecUtil.*;

public class VelocityGenerator implements Generator {
    
    static void p(Object o) {
        System.out.println(o);
    }
    
    public String getName() {
        return "Standard Velocity Template-Based Generator";
    }
    
    public void generate(Map<String, Object> genspec, String target, int genIndex) throws GenerationException {
        Map<String, Object> opts = getMap(SECTION_OPTIONS, genspec);
        FileRef modelRef = getFileRef(null, null, getString(OPT_MODEL_FILE, opts));
        if (modelRef.isEmpty()) {
            printWarning("No model_file defined for target " + getTargetGenId(target, genIndex));
            return;
        }
        FileRef templateRef = getFileRef(getString(OPT_BASE_INPUT_DIR, opts),
                getString(OPT_INPUT_DIR, opts), getString(OPT_TEMPLATE, genspec));
        if (templateRef.isEmpty()) {
            printWarning("No template defined for target " + getTargetGenId(target, genIndex));
            return;
        }
        ThriftModel model;
        try {
            if (modelRef.isFile()) {
                model = Parsers.parseThrift(modelRef.getFile());
            } else {
                model = Parsers.parseThrift(modelRef.getUrl());
            }
        } catch (Exception e) {
            e.printStackTrace();
            printError("Error parsing Thrift model for target " + getTargetGenId(target, genIndex));
            return;
        }

        File outputDir = getOutputDir(opts, model);
        if (!outputDir.isDirectory()) {
            printError("Invalid output directory " + outputDir.getAbsolutePath() +
                    " for target " + getTargetGenId(target, genIndex));
            return;
        }
        
        VelocityEngine ve = new VelocityEngine();
        String templateName;
        if (templateRef.isFile()) {
            File file = templateRef.getFile();
            templateName = file.getName();
            Properties props = new Properties();
            props.put("resource.loader", "file");
            props.put("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.FileResourceLoader");
            props.put("file.resource.loader.path", file.getParentFile().getAbsolutePath());
            props.put("file.resource.loader.cache", "true");
            ve.init(props);
        } else {
            String url = templateRef.getUrl().toString();
            templateName = url.substring(url.lastIndexOf('/') + 1);
            Properties props = new Properties();
            props.put("resource.loader", "url");
            props.put("url.resource.loader.class", "org.apache.velocity.runtime.resource.loader.URLResourceLoader");
            props.put("url.resource.loader.root", url.substring(0, url.lastIndexOf('/')));
            props.put("url.resource.loader.cache", "true");
            ve.init(props);
        }
        
        String charsetName = getString(OPT_CHARSET, opts, "UTF8");
        Charset charset = Charset.forName(charsetName);
        
        GenUtil util = new GenUtil();
        String filename = getString(OPT_FILENAME, genspec, "");
        String filenamePrefix = getString(OPT_FILENAME_PREFIX, genspec, "");
        String filenameSuffix = getString(OPT_FILENAME_SUFFIX, genspec, "");
        String filenameExtension = getString(OPT_FILENAME_EXTENSION, genspec, getString(OPT_FILENAME_EXTENSION, opts, "no_extension_defined"));
        String filenameCase = getString(OPT_FILENAME_CASE, genspec, getString(OPT_FILENAME_CASE, opts, "CamelCase"));
        if (filenameExtension.startsWith(".")) {
            filenameExtension = filenameExtension.substring(1);
        }
        
        HashMap<String, Object> helpers = new HashMap<String, Object>();
        Map<String, Object> helperClasses = getMap(SECTION_HELPERS, genspec);
        for (Map.Entry<String,Object> entry : helperClasses.entrySet()) {
            try {
                helpers.put(entry.getKey(), Class.forName((String)entry.getValue()).newInstance());
            } catch (Exception e) {
                printWarning("Error loading helper class " + entry.getValue() + " for target " + getTargetGenId(target, genIndex));
            }
        }
        HashMap<String, Object> params = getMap(SECTION_PARAMS, genspec);
        
        String iterate = getString(OPT_ITERATE, genspec, "none");
        
        VelocityContext ctx = new VelocityContext();
        ctx.put("model", model);
        ctx.put("helpers", helpers.clone());
        ctx.put("params", params.clone());
       
        
        if ("struct".equals(iterate)) {
            for (StructType struct : model.getStructs().values()) {
                if (struct.isUnion()) {
                    continue;
                }
                ctx.put("struct", struct);
                String structName = struct.getName();
                String baseName;
                if ("underscore_case".equals(filenameCase)) {
                    baseName= filenamePrefix + util.decamel(structName) + filenameSuffix;
                } else {
                    baseName = filenamePrefix + util.ucamel(structName) + filenameSuffix;
                }
                filename = baseName + '.' + filenameExtension;
                ctx.put("filename", filename);
                ctx.put("base_name", baseName);
                File outputFile = new File(outputDir, filename);
                Writer writer;
                try {
                    FileOutputStream out = new FileOutputStream(outputFile);
                    writer = new BufferedWriter(new OutputStreamWriter(out, charset));
                    ve.mergeTemplate(templateName, charsetName, ctx, writer);
                    writer.flush();
                    writer.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    printError("Error creating struct output file " + outputFile.getAbsolutePath() +
                            " for target " + getTargetGenId(target, genIndex));
                }
            }
        } else if ("union".equals(iterate)) {
            for (StructType struct : model.getStructs().values()) {
                if (!struct.isUnion()) {
                    continue;
                }
                ctx.put("union", struct);
                String structName = struct.getName();
                String baseName;
                if ("underscore_case".equals(filenameCase)) {
                    baseName= filenamePrefix + util.decamel(structName) + filenameSuffix;
                } else {
                    baseName = filenamePrefix + util.ucamel(structName) + filenameSuffix;
                }
                filename = baseName + '.' + filenameExtension;
                ctx.put("filename", filename);
                ctx.put("base_name", baseName);
                File outputFile = new File(outputDir, filename);
                Writer writer;
                try {
                    FileOutputStream out = new FileOutputStream(outputFile);
                    writer = new BufferedWriter(new OutputStreamWriter(out, charset));
                    ve.mergeTemplate(templateName, charsetName, ctx, writer);
                    writer.flush();
                    writer.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    printError("Error creating union output file " + outputFile.getAbsolutePath() +
                            " for target " + getTargetGenId(target, genIndex));
                }
            }
        } else if ("enum".equals(iterate)) {
            for (EnumType etype : model.getEnums().values()) {
                ctx.put("enum", etype);
                String enumName = etype.getName();
                String baseName;
                if ("underscore_case".equals(filenameCase)) {
                    baseName= filenamePrefix + util.decamel(enumName) + filenameSuffix;
                } else {
                    baseName = filenamePrefix + util.ucamel(enumName) + filenameSuffix;
                }
                filename = baseName + '.' + filenameExtension;
                ctx.put("filename", filename);
                ctx.put("base_name", baseName);
                File outputFile = new File(outputDir, filename);
                Writer writer;
                try {
                    FileOutputStream out = new FileOutputStream(outputFile);
                    writer = new BufferedWriter(new OutputStreamWriter(out, charset));
                    ve.mergeTemplate(templateName, charsetName, ctx, writer);
                    writer.flush();
                    writer.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    printError("Error creating enum output file " + outputFile.getAbsolutePath() +
                            " for target " + getTargetGenId(target, genIndex));
                }
            }
            
        } else if ("service".equals(iterate)) {
            for (ServiceDef service : model.getServices().values()) {
                ctx.put("service", service);
                String serviceName = service.getName();
                String baseName;
                if ("underscore_case".equals(filenameCase)) {
                    baseName= filenamePrefix + util.decamel(serviceName) + filenameSuffix;
                } else {
                    baseName = filenamePrefix + util.ucamel(serviceName) + filenameSuffix;
                }
                filename = baseName + '.' + filenameExtension;
                ctx.put("filename", filename);
                ctx.put("base_name", baseName);
                File outputFile = new File(outputDir, filename);
                Writer writer;
                try {
                    FileOutputStream out = new FileOutputStream(outputFile);
                    writer = new BufferedWriter(new OutputStreamWriter(out, charset));
                    ve.mergeTemplate(templateName, charsetName, ctx, writer);
                    writer.flush();
                    writer.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    printError("Error creating service output file " + outputFile.getAbsolutePath() +
                            " for target " + getTargetGenId(target, genIndex));
                }
            }
        } else if ("none".equals(iterate)){
            if (filename == null) {
                printWarning("No filename specified for iterate:none for target " + getTargetGenId(target, genIndex));
            } else {
                int extidx = filename.lastIndexOf('.');
                String baseName = extidx > 0 ? filename.substring(0, extidx) : filename;
                ctx.put("filename", filename);
                ctx.put("base_name", baseName);
                File outputFile = new File(outputDir, filename);
                Writer writer;
                try {
                    FileOutputStream out = new FileOutputStream(outputFile);
                    writer = new BufferedWriter(new OutputStreamWriter(out, charset));
                    ve.mergeTemplate(templateName, charsetName, ctx, writer);
                    writer.flush();
                    writer.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    printError("Error creating output file " + outputFile.getAbsolutePath() +
                            " for target " + getTargetGenId(target, genIndex));
                }
            }
        } else {
            printWarning("Invalid iterate type " + iterate + " for target  " + getTargetGenId(target, genIndex));
        }
    }
    

    private static File getOutputDir(Map<String, Object> opts, ThriftModel model) {
        StringBuilder sb = new StringBuilder(128);
        String basedir = getString(OPT_BASE_OUTPUT_DIR, opts);
        String subdir = getString(OPT_OUTPUT_DIR, opts);
        if (subdir != null) {
            if (basedir == null || isAbsolutePath(subdir)) {
                sb.append(subdir);
            } else {
                sb.append(basedir);
                if (!basedir.endsWith("/") || basedir.endsWith("\\")) {
                    sb.append('/');
                }
                sb.append(subdir);
            }
        } else if (basedir != null) {
            sb.append(basedir);
        }
        if (sb.length() > 0) {
            char last = sb.charAt(sb.length() - 1);
            if (last == '/' || last == '\\') {
                sb.deleteCharAt(sb.length() - 1);
            }
        }
        String nsname = getString(OPT_NAMESPACE, opts);
        if (nsname != null) {
            NamespaceDef nsdef = model.getNamespace(nsname);
            if (nsdef != null) {
                String ns = nsdef.getNamespace();
                if ("java".equals(nsname)) {
                    String nspath = ns.replaceAll("\\.", "\\/");
                    if (sb.length() > 0) {
                        sb.append('/');
                    }
                    sb.append(nspath);
                } else {
                    // FIXME: other namespace path effects
                }
            }
        }
        if (sb.length() == 0) {
            return new File(".");
        } else {
            File dir = new File(sb.toString());
            dir.mkdirs();
            return dir;
        }
    }

}

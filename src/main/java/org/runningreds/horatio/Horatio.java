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

package org.runningreds.horatio;

import static org.runningreds.horatio.GenspecUtil.*;

import java.io.File;
import java.io.FilenameFilter;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.runningreds.horatio.generator.Generator;
import org.runningreds.horatio.generator.VelocityGenerator;
import org.runningreds.horatio.parser.Parsers;


public class Horatio {
    
    public static final String SECTION_GLOBAL          = "global";
    public static final String SECTION_OPTIONS         = "options";
    public static final String SECTION_PARAMS          = "params";
    public static final String SECTION_TARGETS         = "targets";
    public static final String SECTION_HELPERS         = "helper_classes";
    public static final String SECTION_GENERATORS      = "generators";
    
    public static final String OPT_MODEL_FILE          = "model_file";
    public static final String OPT_GENSPEC_FILE        = "genspec_file";
    public static final String OPT_BASE_INPUT_DIR      = "base_input_dir";
    public static final String OPT_BASE_OUTPUT_DIR     = "base_output_dir";
    public static final String OPT_INPUT_DIR           = "input_dir";
    public static final String OPT_OUTPUT_DIR          = "output_dir";
    public static final String OPT_INPUT_FILE          = "input_file";
    public static final String OPT_OUTPUT_FILE         = "output_file";
    public static final String OPT_CHARSET             = "output_charset";
    public static final String OPT_GENERATOR_CLASS     = "generator_class";
    public static final String OPT_TARGET              = "target";
    public static final String OPT_MODEL               = "model";
    public static final String OPT_NAMESPACE           = "namespace";    
    public static final String OPT_TEMPLATE            = "template";
    public static final String OPT_ITERATE             = "iterate";
    public static final String OPT_FILENAME            = "filename";
    public static final String OPT_FILENAME_CASE       = "filename_case";
    public static final String OPT_FILENAME_PREFIX     = "filename_prefix";
    public static final String OPT_FILENAME_SUFFIX     = "filename_suffix";
    public static final String OPT_FILENAME_EXTENSION  = "filename_extension";
    
    
    private final Map<String, Object> mainOptions;
    private final Map<String, Object> genspec;
    
    
    public Horatio(Map<String, Object> mainOptions) throws HoratioException {
        if (mainOptions == null) {
            mainOptions = new HashMap<String, Object>(mainOptions);
        }
        this.mainOptions = mainOptions;
        
        String genspecFilename = getString(OPT_GENSPEC_FILE, mainOptions);
        if (genspecFilename == null) {
            String[] filenames = new File(".").list(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(".gen");
                }
            });
            if (filenames.length == 1) {
                genspecFilename = filenames[0];
                mainOptions.put(OPT_GENSPEC_FILE, genspecFilename);
            } else if (filenames.length == 0) {
                throw new GenspecException("No genspec file specified and none found in current directory");
            } else {
                throw new GenspecException("No genspec file specified and " + filenames.length + " found in current directory");
            }
        }
        if (genspecFilename.startsWith("http:") || genspecFilename.startsWith("https:")) {
            try {
                this.genspec = Parsers.parseGenspec(new URL(genspecFilename));
            } catch (Exception e) {
                throw new GenspecException("Error reading genspec", e);
            }
        } else {
            File genspecFile = new File(genspecFilename);
            checkFileReadable(genspecFile, "Genspec file");
            this.genspec = Parsers.parseGenspec(genspecFile);
        }
    }
    
    @SuppressWarnings("unchecked")
    public void run() throws HoratioException {
        Map<String, Object> globalSection = getMap(SECTION_GLOBAL, genspec);
        HashMap<String, Object> globalOptions = new HashMap<String, Object>(getMap(SECTION_OPTIONS, globalSection));
        // command line options override matching global options (and later, below, target options)
        globalOptions.putAll(mainOptions);
        HashMap<String, Object> globalParams = getMap(SECTION_PARAMS, globalSection);
        
        List<Object> targetNames = getList(OPT_TARGET, globalOptions);
        boolean allTargets = targetNames.isEmpty() || targetNames.contains("all");
        
        Map<String, Object> targets = getMap(SECTION_TARGETS, genspec);
        for (Map.Entry<String, Object> targetEntry : targets.entrySet()) {
            String targetName = targetEntry.getKey();
            if (allTargets || targetNames.contains(targetName)) {
                Map<String, Object> target = getMap(targetName, targets);
                
                // merge target options into copy of global options, overriding any that match
                HashMap<String, Object> targetOpts = (HashMap<String, Object>)globalOptions.clone();
                targetOpts.putAll(getMap(SECTION_OPTIONS, target));
                
                // merge target params into copy of global params, overriding any that match
                HashMap<String, Object> targetParams = (HashMap<String, Object>)globalParams.clone();
                targetParams.putAll(getMap(SECTION_PARAMS, target));
                
                HashMap<String, Object> targetHelpers = getMap(SECTION_HELPERS, target);

                List<Map<String, Object>> generators = (List<Map<String, Object>>)(Object)getList(SECTION_GENERATORS, target);
                for (int i = 0, limit = generators.size(); i < limit; i++) {
                    Map<String, Object> gen = generators.get(i);
                    // merge generator options into copy of target options, overriding any that match
                    HashMap<String, Object> genOpts = (HashMap<String, Object>)targetOpts.clone();
                    genOpts.putAll(getMap(SECTION_OPTIONS, gen));
                    
                    // merge generator params into copy of target params, overriding any that match
                    HashMap<String, Object> genParams = (HashMap<String, Object>)targetParams.clone();
                    genParams.putAll(getMap(SECTION_PARAMS, gen));
                    
                    // merge generator helpers into copy of target helpers, overriding any that match
                    HashMap<String, Object> genHelpers = (HashMap<String, Object>)targetHelpers.clone();
                    genHelpers.putAll(getMap(SECTION_HELPERS, gen));
                    
                    // put merged maps back into generator map
                    gen.put(SECTION_OPTIONS, genOpts);
                    gen.put(SECTION_PARAMS, genParams);
                    gen.put(SECTION_HELPERS, genHelpers);
                    
                    // instantiate generator class
                    Generator generator;
                    String genClassName = getString(OPT_GENERATOR_CLASS, gen);
                    if (genClassName == null || (genClassName = genClassName.trim()).isEmpty()) {
                        generator = new VelocityGenerator();
//                        printWarning("No generator class specified for target[generator] = " +
//                                targetName + "[" + i + "] -- using VelocityGenerator");
                    } else {
                        try {
                            Class<?> genClass = Class.forName(genClassName);
                            generator = (Generator)genClass.getConstructor(new Class[]{}).newInstance(new Object[]{});
                        } catch (Exception e) {
                            printError("Invalid generator class specified for target[generator] = " + 
                                    targetName + "[" + i + "] -- skipping");
                            throw new GenspecException(e);
                        }
                    }
                    
                    generator.generate(gen, targetName, i);
                }
            }
        }
    }
    
    
    // FIXME: return version number
    private static String getVersion() {
        return "FIXME: version number";
    }
    
    public static void printUsage(PrintStream out) {
        out.println("Usage: horatio [OPTIONS]\n");
        out.println("Options:");
        out.println("-m, --model <path_or_url>       : Thrift IDL file (e.g. <modelname>.thrift)");
        out.println("-g, --genspec <path_or_url>     : Generation specification file (<name>.gen)");
        out.println("-o, --outdir <path>             : Base output directory");
        out.println("-i, --indir <path_or_url>       : Base template directory");
        out.println("-c, --charset <arg>             : Output charset (e.g. UTF8)");
        out.println("-t, --target <arg> [<arg> ...]  : Genspec target(s) to execute");
        out.println("-v, --version                   : Show Horatio version");
        out.println("-h, --help                      : Show this usage information");
    }

    public static void main(String[] args) {
        if (args.length == 1) {
            String opt = args[0];
            if ("-h".equals(opt) || "-help".equals(opt) || "--help".equals(opt)) {
                printUsage(System.out);
                System.exit(0);
            }
            if ("-v".equals(opt) || "--version".equals(opt)) {
                System.out.println("Horatio version "+getVersion());
                System.exit(0);
            }
        }
        Map<String, Object> options = new HashMap<String, Object>();
        for (int i = 0, argc = args.length; i < argc; i++) {
            String opt = args[i];
            if (("-m".equals(opt) || "--model".equals(opt)) && ++i < argc) {
                options.put(OPT_MODEL_FILE, args[i]);
            } else if (("-g".equals(opt) || "--genspec".equals(opt)) && ++i < argc) {
                options.put(OPT_GENSPEC_FILE, args[i]);
            } else if (("-o".equals(opt) || "--outdir".equals(opt)) && ++i < argc) {
                options.put(OPT_BASE_OUTPUT_DIR, args[i]);
            } else if (("-i".equals(opt) || "--indir".equals(opt)) && ++i < argc) {
                options.put(OPT_BASE_INPUT_DIR, args[i]);
            } else if (("-c".equals(opt) || "--charset".equals(opt)) && ++i < argc) {
                options.put(OPT_CHARSET, args[i]);
            } else if (("-t".equals(opt) || "--target".equals(opt)) && ++i < argc) {
                List<String> targets = new ArrayList<String>();
                while (true) {
                    targets.add(args[i]);
                    int nexti = i + 1;
                    if (nexti < argc && !args[nexti].startsWith("-")) {
                        i = nexti;
                    } else {
                        break;
                    }
                }
                options.put(OPT_TARGET, targets);
            } else {
                System.err.println("ERROR: Invalid or missing argument");
                printUsage(System.err);
                System.exit(-1);
            }
        }
        try {
            Horatio h = new Horatio(options);
            h.run();
        } catch (Exception e) {
            // FIXME: skip stack trace on input/config errors. need distinct exception type.
            e.printStackTrace();
            printError("Horatio terminated abnormally");
            System.exit(-1);
        }
    }
    
    
    

}

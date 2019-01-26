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

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GenspecUtil {
    private GenspecUtil(){};

    public static void printWarning(String warningMessage) {
        System.err.println("WARNING: " + warningMessage);
    }

    public static void printError(String errorMessage) {
        System.err.println("ERROR: " + errorMessage);
    }
    
    public static String getTargetGenId(String target, int generatorIndex) {
        return target + "[" + generatorIndex + "]";
    }

    public static String getString(String name, Map<String, ?> map, String deflt) {
        String value = (String)map.get(name);
        if (value != null) {
            value = value.trim();
            if (value.length() > 0) {
                return value;
            }
        }
        return deflt;
    }
    
    public static String getString(String name, Map<String, ?> container) {
        return getString(name, container, (String)null);
    }


    @SuppressWarnings("unchecked")
    public static HashMap<String, Object> getMap(String name, Map<String, ?> container) {
        HashMap<String, Object> map = (HashMap<String, Object>)container.get(name);
        return map == null ? new HashMap<String, Object>(2) : map;
    }
    
    @SuppressWarnings("unchecked")
    public static List<Object> getList(String name, Map<String, ?> container) {
        Object value = container.get(name);
        if (value == null) {
            return new ArrayList<Object>(1);
        }
        if (value instanceof List) {
            return (List<Object>)value; 
        }
        List<Object> list = new ArrayList<Object>(1);
        list.add(value);
        return list;
    }
    
    
    public static void checkFileReadable(File file, String displayName) {
        if (!file.exists()) {
            throw new GenspecException(displayName + " not found: " + file.getAbsolutePath());
        }
        if (!file.isFile()) {
            throw new GenspecException(displayName + " is not a file: " + file.getAbsolutePath());
        }
        if (!file.canRead()) {
            throw new GenspecException(displayName + " is not readable: " + file.getAbsolutePath());
        }
    }
    
    private static FileRef EMPTY_REF = new FileRef((File)null);
    
    public static FileRef getFileRef(String basedir, String subdir, String file) {
        file = trimToNull(file);
        if (file == null) {
            return EMPTY_REF;
        }
        if (isAbsolutePath(file)) {
            return getFileRef(file);
        } else if (isUrl(file)) {
            return getUrlRef(file);
        }
        basedir = trimToNull(basedir);
        subdir = trimToNull(subdir);
        if (basedir == null) {
            basedir = subdir;
        } else {
            if (subdir != null) {
                if (isAbsolutePath(subdir) || isUrl(subdir)) {
                    basedir = subdir;
                } else {
                    if (basedir.endsWith("/") || basedir.endsWith("\\")) {
                        basedir += subdir;
                    } else {
                        basedir = basedir + "/" + subdir;
                    }
                }
            }
        }
        String fullpath;
        if (basedir == null) {
            fullpath = file;
        } else {
            if (basedir.endsWith("/") || basedir.endsWith("\\")) {
                fullpath = basedir + file;
            } else {
                fullpath = basedir + "/" + file;
            }
        }
        return isUrl(fullpath) ? getUrlRef(fullpath) : getFileRef(fullpath);
    }
    
    
    public static boolean isUrl(String path) {
        return path.startsWith("http:") || path.startsWith("https:");
    }
    
    public static boolean isAbsolutePath(String path) {
        return path.charAt(0) == '/' || (path.length() > 1 && path.charAt(1) == ':');
    }
    
    private static FileRef getUrlRef(String urlstr) {
        try {
            return new FileRef(new URL(urlstr));
        } catch (Exception e) {
            throw new GenspecException(e);
        }
    }
    
    private static FileRef getFileRef(String filestr) {
        return new FileRef(new File(filestr));
    }
    
    
    
    private static String trimToNull(String s) {
        if (s != null) {
            if ((s = s.trim()).isEmpty()) {
                s = null;
            }
        }
        return s;
    }
    
    
    
}

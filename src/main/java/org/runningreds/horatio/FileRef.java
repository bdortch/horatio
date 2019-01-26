package org.runningreds.horatio;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;

public class FileRef {

    final File file;
    final URL url;

    public FileRef(File file) {
        this.file = file;
        this.url = null;
    }

    public FileRef(URL url) {
        this.url = url;
        this.file = null;
    }

    public boolean isFile() {
        return file != null;
    }

    public boolean isUrl() {
        return url != null;
    }
    
    public boolean isEmpty() {
        return file == null && url == null;
    }

    public File getFile() {
        return file;
    }

    public URL getUrl() {
        return url;
    }
    
    public InputStream getInputStream() {
        try {
            if (file != null) {
                return new FileInputStream(file);
            } else if (url != null) {
                return url.openStream();
            } else {
                throw new IllegalStateException();
            }
        } catch (Exception e) {
            throw new GenspecException(e);
        }
        
    }

    public FileRef getChildRef(String filename) {
        if (file != null) {
            return new FileRef(new File(file, filename));
        } else if (url != null) {
            try {
                return new FileRef(new URL(url.toString() + "/" + filename));
            } catch (Exception e) {
                throw new GenspecException(e);
            }
        } else {
            throw new IllegalStateException();
        }
    }
    
}

package de.heisluft.launcher.common;

import org.apache.logging.log4j.Level;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Util {
  public static void copyStream(InputStream is, OutputStream os, int bufSize) throws IOException {
    if(is == null || os == null) {
      if(os != null) os.close();
      if(is != null) is.close();
      return;
    }
    byte[] buf = new byte[bufSize];
    int lastRead;
    while((lastRead = is.read(buf)) != -1) {
      os.write(buf, 0, lastRead);
    }
    is.close();
    os.close();
  }
  public static Level jul2log4j(java.util.logging.Level level) {
    if(level == java.util.logging.Level.SEVERE)return Level.ERROR;
    if(level == java.util.logging.Level.WARNING) return Level.WARN;
    if(level == java.util.logging.Level.INFO) return Level.INFO;
    if(level == java.util.logging.Level.CONFIG) return Level.INFO;
    if(level == java.util.logging.Level.FINE) return Level.DEBUG;
    if(level == java.util.logging.Level.ALL) return Level.ALL;
    return Level.TRACE;
  }
}

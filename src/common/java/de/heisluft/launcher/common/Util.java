package de.heisluft.launcher.common;

import org.apache.logging.log4j.Level;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A shared utility class
 */
public class Util {

  /**
   * Util should not be instantiated
   */
  private Util() {throw new UnsupportedOperationException();}

  /**
   * Copies all bytes from is to os, using a buffer size of bufSize. It closes all streams
   * afterwards. If one stream is null, this method closes the other stream if nonnull, returning
   * without reading or writing
   *
   * @param is
   *     the inputStream to be read from
   * @param os
   *     the outputStream to be written to
   * @param bufSize
   *     the buffer size to be used
   *
   * @throws IOException
   *     if a reading, writing or closing operation fails
   */
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

  /**
   * Converts a JUL Level to its Corresponding Log4j level
   *
   * @param level
   *     the JUL Level
   *
   * @return the corresponding Log4j level
   */
  public static Level jul2log4j(java.util.logging.Level level) {
    if(level == java.util.logging.Level.SEVERE) return Level.ERROR;
    if(level == java.util.logging.Level.WARNING) return Level.WARN;
    if(level == java.util.logging.Level.INFO) return Level.INFO;
    if(level == java.util.logging.Level.CONFIG) return Level.INFO;
    if(level == java.util.logging.Level.FINE) return Level.DEBUG;
    if(level == java.util.logging.Level.ALL) return Level.ALL;
    return Level.TRACE;
  }
}

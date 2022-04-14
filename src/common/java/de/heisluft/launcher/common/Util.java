package de.heisluft.launcher.common;

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
}

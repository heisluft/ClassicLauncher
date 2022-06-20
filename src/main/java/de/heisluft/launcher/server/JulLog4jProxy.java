package de.heisluft.launcher.server;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;

import java.text.SimpleDateFormat;
import java.util.logging.LogRecord;

public class JulLog4jProxy {

  private static final SimpleDateFormat FMT = new SimpleDateFormat("HH:mm:ss");

  public static String format(LogRecord record) {
    // This is needed because 1.4+ registers the formatter also to write a log file.
    // We do not want to replace that, however, so we mimic the original behaviour if we are not
    // Invoked by a ConsoleHandler
    boolean invokedByConsoleHandler = false;
    for(StackTraceElement ste : Thread.currentThread().getStackTrace()) {
      if(ste.getClassName().contains("ConsoleHandler")) {
        invokedByConsoleHandler = true;
        break;
      }
    }
    if(invokedByConsoleHandler) {
      LogManager.getLogger(record.getLoggerName()).log(jul2log4j(record.getLevel()), record.getMessage());
      return "";
    }
    java.util.logging.Level var3 = record.getLevel();
    String var2 = "   ";
    if (var3 == java.util.logging.Level.WARNING) {
      var2 = "  !";
    }

    if (var3 == java.util.logging.Level.SEVERE) {
      var2 = "***";
    }

    return var2 + "  " + FMT.format(record.getMillis()) + "  " + record.getMessage() + "\n";
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

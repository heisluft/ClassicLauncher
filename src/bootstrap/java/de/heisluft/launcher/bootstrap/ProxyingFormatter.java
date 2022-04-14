package de.heisluft.launcher.bootstrap;

import de.heisluft.launcher.common.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * This is a Formatter redirecting all LogRecords to Log4j, returning an empty string so jul does
 * not print its log messages by itself
 */
public class ProxyingFormatter extends Formatter {

  @Override
  public String format(LogRecord record) {
    Logger l = LogManager.getLogger(record.getLoggerName());
    if(record.getThrown() != null)
      l.catching(Util.jul2log4j(record.getLevel()), record.getThrown());
    else l.log(Util.jul2log4j(record.getLevel()), record.getMessage());
    return "";
  }
}

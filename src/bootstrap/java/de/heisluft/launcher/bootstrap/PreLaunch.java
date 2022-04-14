package de.heisluft.launcher.bootstrap;

import de.heisluft.launcher.common.Util;
import net.minecraft.launchwrapper.Launch;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.LogManager;

public class PreLaunch {
  public static void main(String[] args) throws IOException {
    boolean hasTweakClass = false, hasAssetsDir = false, isServer = false; String gameDir = null;
    for(int i = 0; i < args.length; i++) {
      String arg = args[i];
      if(arg.equals("--tweakClass")) {
        hasTweakClass = true;
      }
      if(arg.equals("--gameDir")) {
        if(i == args.length - 1) break; // needed so we dont provoke IndexOutOfBounds. We just skip, Launch will fail anyway
        gameDir = args[++i];
      }
      if(arg.equals("--assetsDir")) {
        hasAssetsDir = true;
      }
      if(arg.equals("--version")) {
        if(i == args.length - 1) break; // needed so we dont provoke IndexOutOfBounds. We just skip, Launch will fail anyway
        isServer = args[++i].contains("server");
      }
    }
    boolean noNewAssetsDir = isServer || hasAssetsDir;
    int difference = (hasTweakClass ? 0 : 2) + (gameDir != null ? 0 : 2) + (noNewAssetsDir ? 0 : 2);
    String[] newArgs = difference == 0 ? args : new String[args.length + difference];
    if(difference != 0) {
      int i = 0;
      if(!hasTweakClass) {
        newArgs[i++] = "--tweakClass";
        newArgs[i++] = "de.heisluft.launcher.ClassicTweaker";
      }
      if(gameDir == null) {
        newArgs[i++] = "--gameDir";
        newArgs[i++] = gameDir = isServer ? "runServer/" : "run/";
      }
      if(!noNewAssetsDir) {
        newArgs[i++] = "--assetsDir";
        String unixStyleGameDir = gameDir.replace('\\', '/');
        newArgs[i] =
            unixStyleGameDir + (unixStyleGameDir.endsWith("/") ? "resources/" : "/resources/");
      }
      System.arraycopy(args, 0, newArgs, difference, args.length);
    }
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    Util.copyStream(PreLaunch.class.getResourceAsStream("/log4j-template.xml"), bos, 4096);
    byte[] transformed = bos.toString("utf-8").replace("${gameDir}", gameDir).getBytes("utf-8");
    Configurator.initialize(null, new ConfigurationSource(new ByteArrayInputStream(transformed)));
    LogManager.getLogManager().readConfiguration(PreLaunch.class.getResourceAsStream("/logging.properties"));

    Launch.main(newArgs);
  }
}
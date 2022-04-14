package de.heisluft.mcbootstrap;

import net.minecraft.launchwrapper.Launch;

public class PreLaunch {
  public static void main(String[] args) {
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

    if(gameDir != null && hasTweakClass && noNewAssetsDir) {
      Launch.main(args);
      return;
    }
    int difference = (hasTweakClass ? 0 : 2) + (gameDir != null ? 0 : 2) + (noNewAssetsDir ? 0 : 2);

    String[] newArgs = new String[args.length + difference];
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
      newArgs[i] = unixStyleGameDir + (unixStyleGameDir.endsWith("/") ? "resources/" : "/resources/");
    }
    System.arraycopy(args, 0, newArgs, difference, args.length);
    Launch.main(newArgs);
  }
}
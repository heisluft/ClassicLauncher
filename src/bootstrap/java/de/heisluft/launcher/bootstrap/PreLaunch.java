package de.heisluft.launcher.bootstrap;

import net.minecraft.launchwrapper.Launch;

/**
 * PreLaunch is a bootstrap class that can be used instead of calling Launch directly.
 * It calls Launch with all arguments, inferred where not typed manually. The aim is to be able
 * to call PreLaunch just with the --version arg and have gameDir, assetsDir and tweakClass inferred
 */
public class PreLaunch {
  public static void main(String[] args) {
    // First, we check which options actually are present
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
    // Servers dont need assetsDir set, they dont use it
    boolean noNewAssetsDir = isServer || hasAssetsDir;
    // Calculate how many args more we need
    int difference = (hasTweakClass ? 0 : 2) + (gameDir != null ? 0 : 2) + (noNewAssetsDir ? 0 : 2);
    // If all args are present, we can just let this be the original args, else create a bigger array
    String[] newArgs = difference == 0 ? args : new String[args.length + difference];
    if(difference != 0) {
      // We need to infer
      int i = 0;
      // This lib has been written for classic, other tweakers should not be needed.
      if(!hasTweakClass) {
        newArgs[i++] = "--tweakClass";
        newArgs[i++] = "de.heisluft.launcher.ClassicTweaker";
      }
      // If no gameDir is present, it defaults to './run/' on client and './runServer/' on the server
      if(gameDir == null) {
        newArgs[i++] = "--gameDir";
        newArgs[i++] = gameDir = isServer ? "runServer/" : "run/";
      }
      // If an assetDir should be inferred, it defaults to '$gameDir/resources/'
      if(!noNewAssetsDir) {
        newArgs[i++] = "--assetsDir";
        String unixStyleGameDir = gameDir.replace('\\', '/');
        newArgs[i] =
            unixStyleGameDir + (unixStyleGameDir.endsWith("/") ? "resources/" : "/resources/");
      }
      // finally, copy the original options over to newArgs
      System.arraycopy(args, 0, newArgs, difference, args.length);
    }
    // At last, call Launch
    Launch.main(newArgs);
  }
}
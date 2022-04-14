package de.heisluft.launcher;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ClassicTweaker implements ITweaker {

  private List<String> argList = new ArrayList<String>();
  private boolean isServer = false;
  public static File minecraftHome;
  public static File assetsDir;

  @Override
  public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
    isServer = profile != null && profile.contains("server");
    minecraftHome = gameDir == null ? new File(isServer ? "runServer/" : "run/") : gameDir;
    ClassicTweaker.assetsDir = assetsDir == null && !isServer ? new File(minecraftHome, "resources/") : assetsDir;
    if(isServer) minecraftHome.mkdirs();
    argList = args;
  }

  @Override
  public void injectIntoClassLoader(LaunchClassLoader launchClassLoader) {
    if(!isServer) {
      String libspath = new File("libs").getAbsolutePath();
      System.setProperty("org.lwjgl.librarypath", libspath);
      System.setProperty("net.java.games.input.librarypath", libspath);
      launchClassLoader.registerTransformer("de.heisluft.launcher.client.AssetReflux");
      launchClassLoader.registerTransformer("de.heisluft.launcher.client.URLTransformer");
      launchClassLoader.registerTransformer("de.heisluft.launcher.client.GameDirChanger");
      launchClassLoader.registerTransformer("de.heisluft.launcher.client.ComparatorFixer");
      return;
    }
    launchClassLoader.registerTransformer("de.heisluft.launcher.server.ServerClassTransformer");
    launchClassLoader.registerTransformer("de.heisluft.launcher.server.LogFormatTransformer");
    launchClassLoader.registerTransformer("de.heisluft.launcher.server.HeartbeatThreadTransformer");
  }

  @Override
  public String getLaunchTarget() {
    if(!isServer) return "de.heisluft.launcher.client.LaunchManipulator";
    return "com.mojang.minecraft.server.MinecraftServer";
  }

  @Override
  public String[] getLaunchArguments() {
    return argList.toArray(new String[argList.size()]);
  }
}


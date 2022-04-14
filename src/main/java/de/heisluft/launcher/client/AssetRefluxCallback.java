package de.heisluft.launcher.client;

import de.heisluft.launcher.ClassicTweaker;
import net.minecraft.launchwrapper.Launch;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Map;

/**
 * This class is part of AssetReflux. it has been split to ensure it does not accidentally load classes
 * or invoke forbidden methods
 */
public class AssetRefluxCallback {

  /**
   * the soundManager instance
   */
  private static Object soundManager;
  /**
   * Lookup of the SoundManager#addSound method
   */
  private static Method addSound;
  /**
   * Lookup of the SoundManager#addMusic method
   */
  private static Method addMusic;

  /**
   * initializes all reflection fields and runs the new asset download task afterwards.
   * DO NOT RENAME UNLESS YOU ALSO CHANGE THE CORE MOD STRING CONSTANTS!
   *
   * @param mc
   *       used to aquire the soundManager instance, the SoundManager class, and its 2 methods
   *       we are interested in
   * @throws Exception
   *       if either IO, Reflection or JSON parsing failed. As this method is only called
   *       from bytecode, we do not need a try catch anywhere
   */
  public static void callback(Object mc) throws Exception {
    AssetReflux.LOGGER.info("Callback invoked, setting up fields");
    Field smField = mc.getClass().getDeclaredField(AssetReflux.soundManagerFieldName);
    smField.setAccessible(true);
    soundManager = smField.get(mc);
    Class<?> smClass = soundManager.getClass();
    addSound = smClass.getMethod(AssetReflux.soundManagerAddSoundMethodName, File.class, String.class);
    addMusic = smClass.getMethod(AssetReflux.soundManagerAddMusicMethodName, String.class, File.class);

    AssetReflux.LOGGER.info("All good, now doing the actual stuff");
    run();
  }

  /**
   * Wrapper around the addSound Reflection call
   * @param file the sound file
   * @param s the resource name
   */
  private static void addSound(File file, String s) {
    try {
      addSound.invoke(soundManager, file, s);
    } catch(IllegalAccessException e) {
      e.printStackTrace();
    } catch(InvocationTargetException e) {
      e.printStackTrace();
    }
  }

  /**
   * Wrapper around the addMusic Reflection call
   * @param file the sound file
   * @param s the resource name
   */
  private static void addMusic(File file, String s) {
    try {
      addMusic.invoke(soundManager, s, file);
    } catch(IllegalAccessException e) {
      e.printStackTrace();
    } catch(InvocationTargetException e) {
      e.printStackTrace();
    }
  }

  /**
   * The substituted run method, downloading the assets from the new asset index
   * and registering them accordingly
   * @throws Exception if an IO Error occurred or JSON parsing failed
   */
  private static void run() throws Exception {
    URL assetsIndex = new URL("https://launchermeta.mojang.com/v1/packages/4759bad2824e419da9db32861fcdc3a274336532/pre-1.6.json");
    InputStream is = assetsIndex.openStream();
    JSONObject object = (JSONObject) new JSONParser().parse(new InputStreamReader(is));
    is.close();
    //http://resources.download.minecraft.net/<first 2 hex letters of hash>/<whole hash>
    JSONObject objects = object.getObject("objects");
    for(Map.Entry<String, ?> e : objects.entrySet()) {
      String resName = e.getKey();
      if(resName.contains("/")) new File(ClassicTweaker.assetsDir, resName.substring(0, resName.lastIndexOf('/'))).mkdirs();
      String hash = ((JSONObject) e.getValue()).getString("hash");
      File outFile = new File(ClassicTweaker.assetsDir, resName);
      byte[] buf = new byte[4096];
      InputStream is2 = new URL("http://resources.download.minecraft.net/" + hash.substring(0, 2) + "/" + hash).openStream();
      FileOutputStream fos = new FileOutputStream(outFile);
      int read;
      while((read = is2.read(buf)) != -1) fos.write(buf, 0, read);
      is2.close();
      fos.close();
      if(resName.startsWith("music/")) addMusic(outFile, resName.substring(resName.indexOf('/') + 1));
      if(resName.startsWith("sound/")) addSound(outFile, resName.substring(resName.indexOf('/') + 1));
    }
    AssetReflux.LOGGER.info("Successfully loaded in all assets!");
  }
}

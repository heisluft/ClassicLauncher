package de.heisluft.launcher.client;

import net.minecraft.launchwrapper.Launch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.swing.*;
import java.applet.Applet;
import java.applet.AppletStub;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

public class LaunchManipulator {

  private static final Logger LOGGER = LogManager.getLogger("LaunchManipulator");

  public static void main(String[] args) throws Exception {
    Class<?> clazz;

    try {
      clazz = Launch.classLoader.findClass("net.minecraft.client.MinecraftApplet");
    } catch (ClassNotFoundException ignored) {
      clazz = Launch.classLoader.findClass("com.mojang.minecraft.MinecraftApplet");
    }

    Constructor<?> constructor = clazz.getConstructor();
    Object object = constructor.newInstance();

    for (Field field : clazz.getDeclaredFields()) {
      String name = field.getType().getName();

      if (!name.contains("awt") && !name.contains("java") && !name.equals("long")) {
        LOGGER.info("Found likely Minecraft candidate: " + field);

        Field fileField = getWorkingDirField(name);
        if (fileField != null) {
          System.out.println("Found File, changing to " + Launch.minecraftHome);
          fileField.setAccessible(true);
          fileField.set(null, Launch.minecraftHome);
          break;
        }
      }
    }

    startMinecraft((Applet) object, args);
  }

  private static void fillServerParams(Map<String, String> params) throws Exception {
    if(!params.containsKey("port")) params.put("port", "25565");
    if(params.containsKey("mppass")) return;
    JSONObject object = (JSONObject) new JSONParser().parse(new InputStreamReader(new URL("http://localhost/mcspoof/servers.json").openStream()));
    String key = InetAddress.getByName(params.get("server")).getHostAddress() + ':' + params.get("port");
    if(!object.containsKey(key)) return;
    String salt = object.getObject(key).getString("salt");
    if("N/A".equals(salt)) return;
    params.put("mppass", new BigInteger(1, MessageDigest.getInstance("MD5").digest((salt + params.get("username")).getBytes())).toString(16));
  }

  private static void startMinecraft(final Applet applet, String[] args) throws Exception {
    if(args.length % 2 == 1)
      LOGGER.info("Ignoring argument '" + args[args.length - 1] + "'");

    final Map<String, String> params = new HashMap<String, String>();

    for(int i = 0; i < args.length / 2; i++) params.put(args[i * 2], args[i * 2 + 1]);
    String sessionId = String.valueOf(System.currentTimeMillis() % 1000);
    if(!params.containsKey("username")) params.put("username", "User" + sessionId);
    if(!params.containsKey("sessionid")) params.put("sessionid", sessionId);
    if(!params.containsKey("haspaid")) params.put("haspaid", "true");
    if(params.containsKey("server")) fillServerParams(params);

    final Frame launcherFrameFake = new Frame();
    launcherFrameFake.setTitle("Minecraft");
    launcherFrameFake.setBackground(Color.BLACK);

    final JPanel panel = new JPanel();
    launcherFrameFake.setLayout(new BorderLayout());
    panel.setPreferredSize(new Dimension(854, 480));
    launcherFrameFake.add(panel, BorderLayout.CENTER);
    launcherFrameFake.pack();

    launcherFrameFake.setLocationRelativeTo(null);
    launcherFrameFake.setVisible(true);

    launcherFrameFake.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        System.exit(1);
      }
    });


    class LauncherFake extends Applet implements AppletStub {
      private static final long serialVersionUID = 1L;

      public void appletResize(int width, int height) {
        // Actually empty as well
      }

      @Override
      public boolean isActive() {
        return true;
      }

      @Override
      public URL getDocumentBase() {
        try {
          return new URL("http://localhost/");
        } catch (MalformedURLException e) {
          e.printStackTrace();
        }
        return null;
      }

      @Override
      public URL getCodeBase() {
        try {
          return new URL("http://localhost/");
        } catch (MalformedURLException e) {
          e.printStackTrace();
        }
        return null;
      }

      @Override
      public String getParameter(String paramName) {
        LOGGER.debug("Client asked for parameter: " + paramName);
        if (params.containsKey(paramName)) {
          return params.get(paramName);
        } else LOGGER.warn("Client asked for non-existent parameter: " + paramName);
        return null;
      }
    }

    final LauncherFake fakeLauncher = new LauncherFake();
    applet.setStub(fakeLauncher);

    fakeLauncher.setLayout(new BorderLayout());
    fakeLauncher.add(applet, BorderLayout.CENTER);
    fakeLauncher.validate();

    launcherFrameFake.removeAll();
    launcherFrameFake.setLayout(new BorderLayout());
    launcherFrameFake.add(fakeLauncher, BorderLayout.CENTER);
    launcherFrameFake.validate();

    applet.init();
    applet.start();

    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        applet.stop();
      }
    });
  }

  private static Field getWorkingDirField(String className) throws ClassNotFoundException {
    Class<?> clazz = Launch.classLoader.findClass(className);

    for (Field field : clazz.getDeclaredFields()) {
      if (Modifier.isStatic(field.getModifiers()) && field.getType().getName().equals("java.io.File")) {
        return field;
      }
    }

    return null;
  }
}
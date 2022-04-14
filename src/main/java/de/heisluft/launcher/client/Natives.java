package de.heisluft.launcher.client;

import java.net.URL;

enum Natives {
  WIN("/jinput-dx8", "/jinput-raw", "/jinput-wintab", "/lwjgl", "/OpenAL"),
  LINUX("/libjinput-linux", "/liblwjgl", "/libopenal"),
  MAC("/libjinput-osx.jnilib", "/liblwjgl.dylib", "/libopenal.dylib");

  private final String[] baseNames;

  Natives(String... baseNames) {
    this.baseNames = baseNames;
  }

  public URL[] getURLs() {
    URL[] urls = new URL[baseNames.length];
    if(this == MAC) {
      for(int i = 0; i < baseNames.length; i++) urls[i] = Natives.class.getResource(baseNames[i]);
      return urls;
    }
    boolean is64Bit = System.getProperty("os.arch").contains("64");
    if(this == LINUX) {
      for(int i = 0; i < baseNames.length; i++) urls[i] = Natives.class.getResource(baseNames[i] + (is64Bit ? "64.so" : ".so"));
      return urls;
    }
    for(int i = 0; i < baseNames.length; i++) {
      String baseName = baseNames[i];
      // WINDOWS PLEASE...
      if(!"/jinput-wintab".equals(baseName)) baseName += is64Bit ? (baseName.startsWith("/j") ? "_64" : "64") : "";
      urls[i] = Natives.class.getResource(baseName + ".dll");
    }
    return urls;
  }
}
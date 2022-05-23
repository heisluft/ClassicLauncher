package de.heisluft.launcher.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;


public class URLTransformer implements IClassTransformer {

  private final Logger logger = LogManager.getLogger("URLTransformer");

  public byte[] transform(String s, String s1, byte[] bytes) {
    boolean dirty = false;
    ClassNode node = new ClassNode();
    ClassReader reader = new ClassReader(bytes);
    reader.accept(node, ClassReader.EXPAND_FRAMES);
    for(MethodNode mn : node.methods) {
      for(AbstractInsnNode ain : mn.instructions) {
        if(!(ain instanceof LdcInsnNode)) continue;
        LdcInsnNode lin = ((LdcInsnNode) ain);
        if(!(lin.cst instanceof String)) continue;
        if("minecraft.net".equals(lin.cst)) {
          if(node.name.endsWith("/MinecraftApplet") && mn.name.equals("init") || mn.desc.equals("(Lcom/mojang/minecraft/level/Level;)V")) {
            logger.info("Overriding documentHost check within " + node.name + "#" + mn.name + mn.desc);
            lin.cst = "localhost";
            dirty = true;
          }
        }
        if("/listmaps.jsp?user=".equals(lin.cst) || "/level/load.html?id=".equals(lin.cst) ||
            "/level/save.html".equals(lin.cst)) {
          logger.info("Inserting into " + node.name + "#" + mn.name + mn.desc);
          logger.debug("Matched String: " + lin.cst);
          lin.cst = "/mcspoof" + lin.cst;
          dirty = true;
        }
        if("http://www.minecraft.net/skin/".equals(lin.cst)) {
          logger.info("Proxying Skin URL in " + node.name + "#" + mn.name + mn.desc);
          lin.cst = "http://localhost/mcspoof/skin.php?skin=";
          dirty = true;
        }
      }
    }
    if(dirty) {
      ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
      node.accept(writer);
      return writer.toByteArray();
    }
    return bytes;
  }
}

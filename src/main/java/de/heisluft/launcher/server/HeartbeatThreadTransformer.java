package de.heisluft.launcher.server;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import static org.objectweb.asm.Opcodes.INVOKESPECIAL;

public class HeartbeatThreadTransformer implements IClassTransformer {
  private final Logger logger = LogManager.getLogger("HeartbeatThreadTransformer");

  public byte[] transform(String s, String s1, byte[] bytes) {
    ClassReader reader = new ClassReader(bytes);
    ClassNode node = new ClassNode();
    reader.accept(node, ClassReader.EXPAND_FRAMES);
    if(!"java/lang/Thread".equals(node.superName)) return bytes;

    boolean dirty = false;

    String rundir = Launch.minecraftHome.getAbsolutePath();
    if(!rundir.endsWith("/")) rundir += "/";

    for(MethodNode mn : node.methods) {
      if(!mn.name.equals("run") || !mn.desc.equals("()V")) continue;


      for(AbstractInsnNode abstractInsnNode : mn.instructions) {
        if(abstractInsnNode instanceof LdcInsnNode) {
          LdcInsnNode lin = (LdcInsnNode) abstractInsnNode;
          AbstractInsnNode next = lin.getNext();
          if(next.getOpcode() == INVOKESPECIAL) {
            MethodInsnNode mnext = (MethodInsnNode) next;
            if("java/net/URL".equals(mnext.owner)) {
              logger.info("Found the heartbeat target, changing to http://localhost/mcspoof/.");
              lin.cst = "http://localhost/mcspoof/heartbeat.php";
              dirty = true;
            } else if("java/io/File".equals(mnext.owner) ||
                "java/io/FileWriter".equals(mnext.owner) ||
                "java/io/FileReader".equals(mnext.owner) ||
                "java/io/FileOutputStream".equals(mnext.owner) ||
                "java/io/FileInputStream".equals(mnext.owner)) {
              logger.info("Prepending workdir before " + lin.cst);
              lin.cst = rundir + lin.cst;
              dirty = true;
            }
          }
        }
      }
    }
    if(!dirty) return bytes;
    ClassWriter writer = new ClassWriter(0);
    node.accept(writer);
    return writer.toByteArray();
  }
}

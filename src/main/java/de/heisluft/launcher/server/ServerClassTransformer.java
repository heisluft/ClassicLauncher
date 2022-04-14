package de.heisluft.launcher.server;

import de.heisluft.launcher.ClassicTweaker;
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

public class ServerClassTransformer implements IClassTransformer {

  private Logger logger = LogManager.getLogger("ServerClassTransformer");

  @Override
  public byte[] transform(String name, String transformedName, byte[] basicClass) {
    if(!name.equals("com.mojang.minecraft.server.MinecraftServer")) return basicClass;

    logger.info("Transforming server class");
    ClassReader reader = new ClassReader(basicClass);
    ClassNode node = new ClassNode();
    reader.accept(node, ClassReader.EXPAND_FRAMES);

    String rundir = Launch.minecraftHome.getAbsolutePath();
    if(!rundir.endsWith("/")) rundir += "/";

    for(MethodNode mn : node.methods) {
      for(AbstractInsnNode abstractInsnNode : mn.instructions) {
        if(abstractInsnNode instanceof LdcInsnNode) {
          LdcInsnNode lin = (LdcInsnNode) abstractInsnNode;
          AbstractInsnNode next = lin.getNext();
          if(next.getOpcode() == INVOKESPECIAL) {
            MethodInsnNode mnext = (MethodInsnNode) next;
            if("java/net/URL".equals(mnext.owner)) {
              logger.info("Found the heartbeat target, changing to http://localhost/mcspoof/.");
              lin.cst = "http://localhost/mcspoof/heartbeat.php";
            } else if("java/io/File".equals(mnext.owner) ||
                "java/io/FileWriter".equals(mnext.owner) ||
                "java/io/FileReader".equals(mnext.owner) ||
                "java/io/FileOutputStream".equals(mnext.owner) ||
                "java/io/FileInputStream".equals(mnext.owner)) {
              logger.info("Prepending workdir before " + lin.cst);
              lin.cst = rundir + lin.cst;
            }
          }
        }
      }
    }
    ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
    node.accept(writer);
    return writer.toByteArray();
  }
}

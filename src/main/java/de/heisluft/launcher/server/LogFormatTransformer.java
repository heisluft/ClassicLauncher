package de.heisluft.launcher.server;

import net.minecraft.launchwrapper.IClassTransformer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;


public class LogFormatTransformer implements IClassTransformer {

  private Logger logger = LogManager.getLogger("LogFormatTransformer");

  @Override
  public byte[] transform(String name, String transformedName, byte[] basicClass) {
    //Mappings may be unstable so we have to detect by inheritance
    ClassReader reader = new ClassReader(basicClass);
    ClassNode node = new ClassNode();
    reader.accept(node, ClassReader.EXPAND_FRAMES);
    if(!node.superName.equals("java/util/logging/Formatter")) return basicClass;
    for(MethodNode mn : node.methods) {
      if(!mn.name.equals("format")) continue;
      logger.info("Inserting LogFormatter proxy call");
      //insert pushes to index 0, so we have to insert in reversed order
      mn.instructions.insert(new InsnNode(Opcodes.ARETURN));
      mn.instructions.insert(new MethodInsnNode(Opcodes.INVOKESTATIC, "de/heisluft/launcher/server/JulLog4jProxy", mn.name, mn.desc));
      mn.instructions.insert(new VarInsnNode(Opcodes.ALOAD, 1));
    }
    ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
    node.accept(writer);
    return writer.toByteArray();
  }
}

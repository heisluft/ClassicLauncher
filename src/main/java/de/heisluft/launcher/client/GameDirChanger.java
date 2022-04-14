package de.heisluft.launcher.client;

import de.heisluft.launcher.ClassicTweaker;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public class GameDirChanger implements IClassTransformer {

  private final Logger logger = LogManager.getLogger("GameDirChanger");

  @Override
  public byte[] transform(final String name, final String transformedName, final byte[] bytes) {
    final ClassNode classNode = new ClassNode();
    final ClassReader classReader = new ClassReader(bytes);
    classReader.accept(classNode, ClassReader.EXPAND_FRAMES);

    if(!classNode.interfaces.contains("java/lang/Runnable")) return bytes;

    MethodNode runMethod = null;
    for(final MethodNode methodNode : classNode.methods) {
      if("run".equals(methodNode.name)) {
        runMethod = methodNode;
        break;
      }
    }
    if(runMethod == null) {
      // WTF? We got no run method in a runnable lol
      return bytes;
    }

    LabelNode lastJump = null;
    int var = -1;
    {
      boolean foundTSwitch = false;
      for(AbstractInsnNode instruction : runMethod.instructions) {
        if(instruction.getOpcode() == Opcodes.TABLESWITCH) {
          foundTSwitch = true;
        }
        if(foundTSwitch) {
          if(instruction.getOpcode() == Opcodes.GOTO) {
            lastJump = ((JumpInsnNode) instruction).label;
            AbstractInsnNode ain = lastJump.getPrevious();
            while(ain.getOpcode() != Opcodes.ASTORE) ain = ain.getPrevious();
            var = ((VarInsnNode) ain).var;
            break;
          }
        }
      }
    }
    if(lastJump == null) return bytes; // Was not the desired class

    logger.info("Found the workDir switch within class " + classNode.name);
    logger.debug("Last Label is " + lastJump.getLabel() + ", instruction number " + runMethod.instructions.indexOf(lastJump));
    logger.debug("File var is " + var);
    logger.info("Inserting call to ClassicTweaker.minecraftHome (currently: '" + ClassicTweaker.minecraftHome.getAbsolutePath() + "')");

    runMethod.instructions.insert(lastJump, new VarInsnNode(Opcodes.ASTORE, var));
    runMethod.instructions.insert(lastJump, new FieldInsnNode(Opcodes.GETSTATIC, "de/heisluft/launcher/ClassicTweaker", "minecraftHome", "Ljava/io/File;"));

    final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
    classNode.accept(writer);

    return writer.toByteArray();
  }
}

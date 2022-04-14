package de.heisluft.launcher.client;

import net.minecraft.launchwrapper.IClassTransformer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.HashMap;

public class ComparatorFixer implements IClassTransformer {

  private final Logger logger = LogManager.getLogger("ComparatorFixer");

  @Override
  public byte[] transform(String name, String transformedName, byte[] basicClass) {
    ClassReader reader = new ClassReader(basicClass);
    ClassNode node = new ClassNode();
    reader.accept(node, ClassReader.EXPAND_FRAMES);
    if(node.interfaces.size() != 1 || !node.interfaces.get(0).equals("java/util/Comparator"))
      return basicClass;

    MethodNode cmpNode = null;
    for(MethodNode method : node.methods) {
      if(method.name.equals("compare") && method.desc.equals("(Ljava/lang/Object;Ljava/lang/Object;)I")) {
        cmpNode = method;
        break;
      }
    }
    if(cmpNode == null) return basicClass;
    String classDesc = "";
    FieldInsnNode playerFieldGet = null;
    FieldInsnNode chunkVisibleGet = null;
    AbstractInsnNode invokeInsn = null;
    boolean iconstM1 = false;
    for(AbstractInsnNode ain : cmpNode.instructions) {
      if(ain.getOpcode() == Opcodes.CHECKCAST && classDesc.isEmpty()) {
        classDesc = ((TypeInsnNode) ain).desc;
      }
      if(ain.getOpcode() == Opcodes.GETFIELD) {
        FieldInsnNode fin = ((FieldInsnNode) ain);
        if(playerFieldGet == null && ain.getPrevious() instanceof VarInsnNode && ((VarInsnNode)ain.getPrevious()).var == 0) playerFieldGet = (FieldInsnNode) fin.clone(new HashMap<LabelNode, LabelNode>());
        else if(chunkVisibleGet == null && fin.owner.equals(classDesc)) chunkVisibleGet = (FieldInsnNode) fin.clone(new HashMap<LabelNode, LabelNode>());
      }
      if(ain.getOpcode() == Opcodes.INVOKEVIRTUAL && ((MethodInsnNode) ain).owner.equals(classDesc)) {
        invokeInsn = ain.clone(new HashMap<LabelNode, LabelNode>());
      }
      if(ain.getOpcode() == Opcodes.ICONST_M1) iconstM1 = true;
    }
    if(classDesc.isEmpty() || playerFieldGet == null|| invokeInsn == null) return basicClass;

    logger.info("Fixing compare method of " + node.name);

    cmpNode.instructions.clear();

    cmpNode.instructions.insert(new InsnNode(Opcodes.IRETURN));
    cmpNode.instructions.insert(new MethodInsnNode(Opcodes.INVOKESTATIC, "de/heisluft/launcher/client/ComparatorFixer", "compare", "(FFZZZ)I"));
    // since survival-test, the output is to be inverted - we detect that by checking if -1 or 1 are loaded
    // passing the result in as a boolean flag
    cmpNode.instructions.insert(new InsnNode(iconstM1 ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
    if(chunkVisibleGet == null) {
      cmpNode.instructions.insert(new InsnNode(Opcodes.ICONST_0));
      cmpNode.instructions.insert(new InsnNode(Opcodes.ICONST_0));
    } else {
      cmpNode.instructions.insert(chunkVisibleGet.clone(new HashMap<LabelNode, LabelNode>()));
      cmpNode.instructions.insert(new VarInsnNode(Opcodes.ALOAD, 4));
      cmpNode.instructions.insert(chunkVisibleGet.clone(new HashMap<LabelNode, LabelNode>()));
      cmpNode.instructions.insert(new VarInsnNode(Opcodes.ALOAD, 3));
    }
    cmpNode.instructions.insert(invokeInsn.clone(new HashMap<LabelNode, LabelNode>()));
    cmpNode.instructions.insert(playerFieldGet.clone(new HashMap<LabelNode, LabelNode>()));
    cmpNode.instructions.insert(new VarInsnNode(Opcodes.ALOAD, 0));
    cmpNode.instructions.insert(new VarInsnNode(Opcodes.ALOAD, 4));
    cmpNode.instructions.insert(invokeInsn.clone(new HashMap<LabelNode, LabelNode>()));
    cmpNode.instructions.insert(playerFieldGet.clone(new HashMap<LabelNode, LabelNode>()));
    cmpNode.instructions.insert(new VarInsnNode(Opcodes.ALOAD, 0));
    cmpNode.instructions.insert(new VarInsnNode(Opcodes.ALOAD, 3));
    cmpNode.instructions.insert(new VarInsnNode(Opcodes.ASTORE, 4));
    cmpNode.instructions.insert(new TypeInsnNode(Opcodes.CHECKCAST, classDesc));
    cmpNode.instructions.insert(new VarInsnNode(Opcodes.ALOAD, 2));
    cmpNode.instructions.insert(new VarInsnNode(Opcodes.ASTORE, 3));
    cmpNode.instructions.insert(new TypeInsnNode(Opcodes.CHECKCAST, classDesc));
    cmpNode.instructions.insert(new VarInsnNode(Opcodes.ALOAD, 1));

    ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
    node.accept(writer);
    return writer.toByteArray();
  }

  public static int compare(float dist1, float dist2, boolean v1, boolean v2, boolean invert) {
    if(invert) {
      if(v1 && !v2) return 1;
      if(v2 && !v1) return -1;
      return Float.compare(dist2, dist1);
    }
    if(v1 && !v2) return -1;
    if(v2 && !v1) return 1;
    return Float.compare(dist1, dist2);
  }
}

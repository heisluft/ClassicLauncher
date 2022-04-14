package de.heisluft.launcher.client;

import de.heisluft.launcher.ClassicTweaker;
import net.minecraft.launchwrapper.IClassTransformer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.ListIterator;

/**
 * A coremod to restore the sound functionality of the classic versions.
 */
public class AssetReflux implements IClassTransformer {

  /**
   * The name of the soundManager field within Minecraft.class
   */
  static String soundManagerFieldName;
  /**
   * The name of the SoundManager#addSound(File, String) method
   */
  static String soundManagerAddSoundMethodName;
  /**
   * The name of the SoundManager#addSound(String, File) method
   */
  static String soundManagerAddMusicMethodName;

  static final Logger LOGGER = LogManager.getLogger("AssetReflux");

  @Override
  public byte[] transform(String name, String transformedName, byte[] basicClass) {

    ClassReader reader = new ClassReader(basicClass);
    ClassNode node = new ClassNode();
    reader.accept(node, ClassReader.EXPAND_FRAMES);

    if(!node.superName.equals("java/lang/Thread")) return basicClass;

    // Check if we have exactly 3 fields, one being Boolean, one being File to increase hit chance
    if(node.fields.size() != 3) return basicClass;
    boolean foundBoolean = false, foundFile = false;
    for(FieldNode fn : node.fields) {
      if(fn.desc.equals("Ljava/io/File;")) {
        if(foundFile) return basicClass; // Exactly one File field
        foundFile = true;
      }
      if(fn.desc.equals("Z")) {
        if(foundBoolean) return basicClass; // Exactly one boolean field
        foundBoolean = true;
      }
    }
    if(!foundBoolean || !foundFile) return basicClass;
    LOGGER.info("Found the BackgroundDownload Thread");

    // We need those to push them onto the stack for our callback invocation
    String mcFieldName = "", mcFieldDesc = "";
    // This is simply a safeguard to validate our functions.
    String smClassName = "";

    //The remaining field is the minecraft field, but we neither know the Minecraft class nor the
    //mc field name, so we look them up directly
    for(FieldNode fn : node.fields) {
      if(!fn.desc.equals("Ljava/io/File;") && !fn.desc.equals("Z")) {
        mcFieldDesc = fn.desc;
        mcFieldName = fn.name;
      }
    }

    for(MethodNode mn : node.methods) {
      if(mn.name.equals("<init>")) {
        AbstractInsnNode start = null;
        for(AbstractInsnNode ain : mn.instructions) {
          if(ain.getOpcode() != Opcodes.INVOKESPECIAL) continue;
          MethodInsnNode min = (MethodInsnNode) ain;
          if(!min.owner.equals("java/io/File")) continue;
          if(!min.name.equals("<init>")) continue;
          if(!min.desc.equals("(Ljava/io/File;Ljava/lang/String;)V")) continue;
          AbstractInsnNode ldc = ain.getPrevious();
          if(ldc.getOpcode() != Opcodes.LDC) continue;
          if(!"resources/".equals(((LdcInsnNode)ldc).cst)) continue;
          AbstractInsnNode aload1 = ldc.getPrevious();
          if(aload1.getOpcode() != Opcodes.ALOAD) continue;
          if(((VarInsnNode)aload1).var != 1) continue;
          AbstractInsnNode dup = aload1.getPrevious();
          if(dup.getOpcode() != Opcodes.DUP) continue;
          AbstractInsnNode _new = dup.getPrevious();
          if(_new.getOpcode() != Opcodes.NEW) continue;
          if(!((TypeInsnNode)_new).desc.equals("java/io/File")) continue;
          start = _new;
          break;
        }
        if(start == null) continue;
        AbstractInsnNode next = start;
        LOGGER.info("Found the workdir Field set, changing to ClassicTweaker.assetsDir (currently '" + ClassicTweaker.assetsDir.getAbsolutePath() + "')");
        for(int i = 0; i < 5; i++) {
          start = next;
          next = start.getNext();
          mn.instructions.remove(start);
        }
        mn.instructions.insertBefore(next, new FieldInsnNode(Opcodes.GETSTATIC, "de/heisluft/launcher/ClassicTweaker", "assetsDir", "Ljava/io/File;"));
      }
      if(mn.name.equals("run")) {
        for(AbstractInsnNode ain : mn.instructions) {
          // There are two getfield references, one for the addMusic and one for the addSound call
          if(ain.getOpcode() == Opcodes.GETFIELD) {
            FieldInsnNode fin = (FieldInsnNode) ain;
            if(fin.owner.equals(mcFieldDesc.substring(1, mcFieldDesc.length() -1))) {
              // we only need to set this once. This is the soundManager field of the Minecraft class
              // we cache it so that we can obtain an instance to invoke the methods on
              if(soundManagerFieldName == null) {
                soundManagerFieldName = fin.name;
                LOGGER.info("Found the Sound manager field get.");
                LOGGER.info("MC field name: " + mcFieldName);
                LOGGER.info("MC field desc: " + mcFieldDesc);
                LOGGER.info("SoundManager field name: " + fin.name);
                LOGGER.info("SoundManager field desc: " + fin.desc);
                smClassName = fin.desc.substring(1, fin.desc.length() - 1);
              }
              // Skip all those other aloads
              AbstractInsnNode next = ain.getNext();
              while(!(next instanceof MethodInsnNode)) next = next.getNext();
              MethodInsnNode theCall = (MethodInsnNode) next;

              //sanity check that the called method belongs to SoundManager
              if(!smClassName.equals(theCall.owner)) continue;

              if("(Ljava/io/File;Ljava/lang/String;)V".equals(theCall.desc)) {
                LOGGER.info("Found the addSound method. (" + theCall.name + ")");
                soundManagerAddSoundMethodName = theCall.name;
              }
              else {
                LOGGER.info("Found the addMusic method. (" + theCall.name + ")");
                soundManagerAddMusicMethodName = theCall.name;
              }
            }
          }
        }
        // If our heuristics failed, its better not to modify the bytecode at all
        if(soundManagerFieldName != null && soundManagerAddSoundMethodName != null && soundManagerAddMusicMethodName != null) {
          LOGGER.info("CoreMod Setup successful! now inserting the proxy call");
          // AssetRefluxCallback.callback(this.mc); return;
          mn.instructions.clear();
          mn.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
          mn.instructions.add(new FieldInsnNode(Opcodes.GETFIELD, node.name, mcFieldName, mcFieldDesc));
          mn.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "de/heisluft/launcher/client/AssetRefluxCallback", "callback", "(Ljava/lang/Object;)V"));
          mn.instructions.add(new InsnNode(Opcodes.RETURN));
          mn.tryCatchBlocks.clear();
          mn.localVariables = null;
        }
      }
    }
    ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
    node.accept(writer);
    return writer.toByteArray();
  }
}

package com.badlogic.gdx.backends.lwjgl3;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.graphics.glutils.HdpiMode;

import java.io.PrintStream;

public class ReadableLwjgl3ApplicationConfiguration extends Lwjgl3ApplicationConfiguration {

  public boolean isDisableAudio() {
    return disableAudio;
  }

  public int getMaxNetThreads() {
    return maxNetThreads;
  }

  public int getAudioDeviceSimultaneousSources() {
    return audioDeviceSimultaneousSources;
  }

  public int getAudioDeviceBufferSize() {
    return audioDeviceBufferSize;
  }

  public int getAudioDeviceBufferCount() {
    return audioDeviceBufferCount;
  }

  public GLEmulation getGlEmulation() {
    return glEmulation;
  }

  public int getGles30ContextMajorVersion() {
    return gles30ContextMajorVersion;
  }

  public int getGles30ContextMinorVersion() {
    return gles30ContextMinorVersion;
  }

  public int getR() {
    return r;
  }

  public int getG() {
    return g;
  }

  public int getB() {
    return b;
  }

  public int getA() {
    return a;
  }

  public int getDepth() {
    return depth;
  }

  public int getStencil() {
    return stencil;
  }

  public int getSamples() {
    return samples;
  }

  public boolean isTransparentFramebuffer() {
    return transparentFramebuffer;
  }

  public int getIdleFPS() {
    return idleFPS;
  }

  public int getForegroundFPS() {
    return foregroundFPS;
  }

  public String getPreferencesDirectory() {
    return preferencesDirectory;
  }

  public Files.FileType getPreferencesFileType() {
    return preferencesFileType;
  }

  public HdpiMode getHdpiMode() {
    return hdpiMode;
  }

  public boolean isDebug() {
    return debug;
  }

  public PrintStream getDebugStream() {
    return debugStream;
  }

  public String getTitle() {
    return title;
  }

  public boolean isVSync() {
    return vSyncEnabled;
  }

  public void setVSync(boolean vsync) {
    super.useVsync(vsync);
  }
}
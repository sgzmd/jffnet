package com.sgzmd.jffnet;


import java.util.logging.Logger;

public class CommandLineProgress implements Progress {
  private static final Logger LOG = Logger.getLogger(CommandLineProgress.class.getName());

  private int progressMax = 0;
  private int progress = 0;

  @Override
  public int getProgressMax() {
    return progressMax;
  }

  @Override
  public void setProgressMax(int progressMax) {
    this.progressMax = progressMax;
  }

  @Override
  public void step() {
    LOG.info("Progress: " + progress + " of " + progressMax);
    ++progress;
  }

  @Override
  public int getProgress() {
    return progress;
  }
}

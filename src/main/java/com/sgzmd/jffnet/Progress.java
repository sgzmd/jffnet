package com.sgzmd.jffnet;

import com.google.inject.ImplementedBy;

@ImplementedBy(CommandLineProgress.class)
public interface Progress {
  int getProgressMax();
  void setProgressMax(int progressMax);
  void step();
  int getProgress();
}

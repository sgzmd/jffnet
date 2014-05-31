package com.sgzmd.jffnet;

public interface Progress {
  int getProgressMax();
  void setProgressMax(int progressMax);
  void step();
  int getProgress();
}

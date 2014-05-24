package com.sgzmd.jffnet;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class ChapterUrl {
  public static ChapterUrl create(String url, String chapter) {
    return new AutoValue_ChapterUrl(url, chapter);
  }

  public abstract String url();
  public abstract String chapter();
}

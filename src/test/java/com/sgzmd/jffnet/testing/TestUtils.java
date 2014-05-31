package com.sgzmd.jffnet.testing;

import com.google.common.base.Throwables;
import com.google.common.io.Resources;
import com.sgzmd.jffnet.Progress;
import com.sgzmd.jffnet.UrlContentFetcher;
import com.sgzmd.jffnet.ffnet.FFNetStoryExtractor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class TestUtils {
  public static final String URL1 = "https://www.fanfiction.net/s/3384712/1/The-Lie-I-ve-Lived";
  public static final String URL2 = "https://www.fanfiction.net/s/2680093/1/Circular-Reasoning";

  private static UrlContentFetcher URL_FETCHER = null;
  private static FFNetStoryExtractor FFNET_EXTRACTOR = null;
  static {
    try {
      URL_FETCHER = new FakeUrlContentFetcher(
          URL1, Resources.toString(Resources.getResource("test.html"), StandardCharsets.UTF_8),
          URL2, Resources.toString(Resources.getResource("test2.html"), StandardCharsets.UTF_8));
      FFNET_EXTRACTOR = new FFNetStoryExtractor(URL_FETCHER, new Progress() {
        @Override
        public int getProgressMax() {
          return 0;
        }

        @Override
        public void setProgressMax(int progressMax) {

        }

        @Override
        public void step() {

        }

        @Override
        public int getProgress() {
          return 0;
        }
      });

    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  public static UrlContentFetcher fetcher() {
    return URL_FETCHER;
  }

  public static FFNetStoryExtractor getFfNetExtractor() throws IOException {
    return FFNET_EXTRACTOR;
  }
}

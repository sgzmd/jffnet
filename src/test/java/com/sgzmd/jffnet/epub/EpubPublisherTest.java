package com.sgzmd.jffnet.epub;

import com.sgzmd.jffnet.ffnet.FFNetStoryExtractor;
import com.sgzmd.jffnet.proto.Jffnet;
import com.sgzmd.jffnet.testing.TestUtils;
import org.junit.Test;

import static org.truth0.Truth.ASSERT;

public class EpubPublisherTest {
  @Test
  public void testRenderTitlePage() throws Exception {
    FFNetStoryExtractor extractor = TestUtils.getFfNetExtractor();
    Jffnet.StoryInfo story = extractor.extractStoryMetadata(TestUtils.URL1);
    String titlePage = new EpubPublisher().renderTitlePage(Jffnet.Story.newBuilder().setInfo(story).buildPartial());
    ASSERT.that(titlePage).contains(story.getTitle());
    ASSERT.that(titlePage).contains(story.getAuthor(0));
    System.out.println(titlePage);
  }
}
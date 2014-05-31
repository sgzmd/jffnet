package com.sgzmd.jffnet.ffnet;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.io.Resources;
import com.sgzmd.jffnet.ChapterUrl;
import com.sgzmd.jffnet.proto.Jffnet;
import com.sgzmd.jffnet.testing.FakeUrlContentFetcher;
import com.sgzmd.jffnet.testing.TestUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;

import static org.truth0.Truth.ASSERT;

public class FFNetStoryExtractorTest {

  public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern("YYYY/MM/dd");
  private FakeUrlContentFetcher urlFetcher;
  private FFNetStoryExtractor storyExtractor;

  private final String[] EXPECTED_CHAPTERS = {
      "1. A House of Cards",
      "2. Flawed Assumptions",
      "3. The Summer of Change",
      "4. Of Innuendo and Ice Cream",
      "5. Closets and Secrets",
      "6. Plans are Subject to Change",
      "7. I Belong to Nowhere",
      "8. Censure and Sensibility",
      "9. Bartering the Truth",
      "10. Every Rose has a Thorn",
      "11. Appearances Can Be Revealing",
      "12. What's the Price of Your Fame?",
      "13. Caged Fear",
      "14. Sometimes, It's Good to be Me",
      "15. Code of the Marauders",
      "16. Dancing in the Whirlwind",
      "17. The Dykstra Shuffle",
      "18. A Prayer for Forgiveness",
      "19. Out of Africa",
      "20. The Geist and the Grotto",
      "21. Mind Your Manners",
      "22. Head Games",
      "23. Humiliation and Other Diversionary",
      "24. Cry Havoc"
  };

  @Before public void setUp() throws IOException {
    this.urlFetcher = new FakeUrlContentFetcher(
        TestUtils.URL1, Resources.toString(Resources.getResource("test.html"), StandardCharsets.UTF_8),
        TestUtils.URL2, Resources.toString(Resources.getResource("test2.html"), StandardCharsets.UTF_8));

    this.storyExtractor = TestUtils.getFfNetExtractor();
  }

  @Test public void fetchUrlSmokeTest() throws IOException {
    ASSERT.that(urlFetcher.fetchUrl(TestUtils.URL1)).isNotNull();
  }

  @Test public void testSomething() throws IOException {
    Document doc = Jsoup.parse(urlFetcher.fetchUrl(TestUtils.URL1));
    Elements els = doc.select("div[id*=profile_top] > a[1]");
    for (Element el : els) {
      System.err.println(el.toString());
    }
  }

  @Test public void testExtractStoryInfo() throws IOException {
    Jffnet.StoryInfo storyInfo = storyExtractor.extractStoryMetadata(TestUtils.URL1);
    ASSERT.that(storyInfo.getAuthorList()).has().exactly("jbern");
    ASSERT.that(storyInfo.getDescription()).is("Not all of James died that night. Not all of Harry lived. " +
        "The Triwizard Tournament as it should have been and a hero discovering who he really wants to be.");
    ASSERT.that(storyInfo.getTitle()).is("The Lie I've Lived");
  }

  @Test public void testExtractAnotherStoryInfo() throws IOException {
    Jffnet.StoryInfo storyInfo = storyExtractor.extractStoryMetadata(TestUtils.URL2);
    ASSERT.that(storyInfo.getTitle()).is("Circular Reasoning");
    ASSERT.that(storyInfo.getAuthorList()).has().exactly("Swimdraconian");
    ASSERT.that(storyInfo.hasUpdatedDate()).isTrue();
    ASSERT.that(storyInfo.getStatus()).is(Jffnet.Status.WIP);
  }

  @Test public void testInfoParsed() {
    String info = "Rated: Fiction M - English - " +
        "Adventure/Romance - Harry P., Fleur D. - " +
        "Chapters: 24 - Words: 234,571 - Reviews: 4,029 - " +
        "Favs: 7,015 - Follows: 3,257 - " +
        "Updated: 5/28/2009 - Published: 2/9/2007 - " +
        "Status: Complete - id: 3384712";

    Jffnet.StoryInfo.Builder builder = Jffnet.StoryInfo.newBuilder();
    storyExtractor.parseStoryMeta(info, builder);
    ASSERT.that(builder.getGenreList()).has().allOf("Adventure", "Romance");
    ASSERT.that(builder.getCharacterList()).has().allOf("Harry P.", "Fleur D.");
    ASSERT.that(builder.getFavs()).is(7015);
    ASSERT.that(builder.getReviews()).is(4029);
    ASSERT.that(builder.getStatus()).is(Jffnet.Status.COMPLETED);

    ASSERT
        .that(DATE_TIME_FORMATTER.print(new DateTime(builder.getUpdatedDate(), DateTimeZone.UTC)))
        .is("2009/05/28");

    ASSERT
        .that(DATE_TIME_FORMATTER.print(new DateTime(builder.getPublishedDate(), DateTimeZone.UTC)))
        .is("2007/02/09");
  }

  @Test public void testExtractChapters() throws IOException {
    Document doc = Jsoup.parse(urlFetcher.fetchUrl(TestUtils.URL1));
    Iterable<ChapterUrl> chapters = storyExtractor.extractChapters(doc);

    ASSERT.that(Iterables.transform(chapters, new Function<ChapterUrl, String>() {
      @Override
      public String apply(ChapterUrl input) {
        return input.chapter();
      }
    })).iteratesAs(EXPECTED_CHAPTERS);

    ASSERT
        .that(Iterables.getFirst(chapters, null).url())
        .is("https://www.fanfiction.net/s/3384712/1/The-Lie-I-ve-Lived");
  }

  @Test public void testGetDate() throws ParseException {
    ASSERT.that(new LocalDate(2011, 5, 15)).isEqualTo(FFNetStoryExtractor.parseDate("05/15/2011"));
  }
}

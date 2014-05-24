package com.sgzmd.jffnet;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.google.inject.internal.util.ImmutableMap;
import com.sgzmd.jffnet.proto.Jffnet;
import org.apache.commons.lang.StringEscapeUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FFNetStoryExtractor {
  private static final Logger LOG = Logger.getLogger(FFNetStoryExtractor.class.getName());
  private static final String BASE_URL = "https://www.fanfiction.net";

  private static final String AUTHOR_XPATH = "div[id*=profile_top]>a[href^=/u]";
  private static final String STORY_TITLE_XPATH = "div[id*=profile_top]>b";
  private static final String STORY_DESCRIPTION_XPATH = "div[id*=profile_top]>div";
  private static final String CHAPTER_SELECTOR_XPATH = "select[id*=chap_select]";

  private static final ImmutableMap<String, Jffnet.Rating> RATINGS_MAP = ImmutableMap.of(
      "Fiction K",
      Jffnet.Rating.K,
      "Fiction K+",
      Jffnet.Rating.K_PLUS,
      "Fiction T",
      Jffnet.Rating.T,
      "Fiction M",
      Jffnet.Rating.M
  );

  static final Function<String, String> STRING_TRIM_FUNCTION = new Function<String, String>() {
    @Override
    public String apply(String input) {
      return input.trim();
    }
  };

  private static final int INDEX_RATING = 0;
  private static final int INDEX_LANGUAGE = 1;
  private static final int INDEX_GENRE = 2;
  private static final int INDEX_CHARACTER = 3;
  private static final int INDEX_REVIEWS = 6;
  private static final int INDEX_FAVS = 7;

  private static final Pattern ONCHANGE_PATTERN = Pattern.compile("self.location = '(/s/[0-9]+/)'.+\\+ '(/.+)';");

  @VisibleForTesting static final DateTimeFormatter DATE_PARSER = DateTimeFormat.forPattern("MM/dd/YYYY");

  private final UrlContentFetcher urlFetcher;

  @Inject public FFNetStoryExtractor(UrlContentFetcher urlFetcher) {
    this.urlFetcher = urlFetcher;
  }

  public Jffnet.Story getStory(String storyUrl) throws IOException {
    LOG.info("getStory -> " + storyUrl);
    Document doc = Jsoup.parse(urlFetcher.fetchUrl(storyUrl));

    Jffnet.Story.Builder storyBuilder = Jffnet.Story.newBuilder();

    storyBuilder.setInfo(this.extractStoryMetadata(doc));
    Iterable<ChapterUrl> chapterUrls = extractChapters(doc);

    for (ChapterUrl url : chapterUrls) {
      LOG.info("Reading chapter '" + url.chapter() + "' (" + url.url() + ")" );
      String chapterHtml = urlFetcher.fetchUrl(url.url());
      Element div = Jsoup.parse(chapterHtml).select("div.storytext").first();
      storyBuilder.addChapterBuilder()
          .setTitle(url.chapter())
          .setUrl(url.url())
          .setText(StringEscapeUtils.unescapeHtml(div.html()));
    }

    return storyBuilder.build();
  }

  @VisibleForTesting void parseStoryMeta(String meta, Jffnet.StoryInfo.Builder builder) {
    String[] values = meta.split("-");

    for (Map.Entry<String, Jffnet.Rating> entry : RATINGS_MAP.entrySet()) {
      if (values[INDEX_RATING].contains(entry.getKey())) {
        builder.setRating(entry.getValue());
      }
    }

    builder.setLanguage(values[INDEX_LANGUAGE].trim());

    builder.addAllGenre(Iterables.transform(
        Splitter.on('/').split(values[INDEX_GENRE]),
        STRING_TRIM_FUNCTION
    ));

    builder.addAllCharacter(Iterables.transform(
        Splitter.on(',').split(values[INDEX_CHARACTER]),
        STRING_TRIM_FUNCTION
    ));

    builder.setReviews(extractNumericValue(values, INDEX_REVIEWS));
    builder.setFavs(extractNumericValue(values, INDEX_FAVS));

    builder.setStatus(Jffnet.Status.WIP);

    // parsing remaining values, which may not be all present
    for (int i = INDEX_FAVS + 1; i < values.length; ++i) {
      String value = values[i].toLowerCase().trim();
      if (value.startsWith("updated")) {
        try {
          DateTime dateTime = getDateTime(value);
          if (dateTime.getYear() > 1991) {
            builder.setUpdatedDate(dateTime.getMillis());
          }
        } catch (Throwable e) {
          LOG.log(Level.WARNING, "Problem parsing updated date <" + value + ">", e);
        }
      } else if (value.startsWith("published")) {
        try {
          DateTime dateTime = getDateTime(value);
          if (dateTime.getYear() > 1991) {
            builder.setPublishedDate(dateTime.getMillis());
          }
        } catch (Throwable e) {
          LOG.log(Level.WARNING, "Problem parsing published date <" + value + ">", e);
        }
      } else if (value.toLowerCase().contains("complete")) {
        builder.setStatus(Jffnet.Status.COMPLETED);
      }
    }
  }

  @VisibleForTesting DateTime getDateTime(String value) {
    return DATE_PARSER.parseLocalDate(value.split(":")[1].trim()).toDateTimeAtStartOfDay(DateTimeZone.UTC);
  }

  @VisibleForTesting int extractNumericValue(String[] values, int idx) {
    return Integer.parseInt(values[idx].split(":")[1].replace(",", "").trim());
  }

  @VisibleForTesting Jffnet.StoryInfo extractStoryMetadata(String story) throws IOException {
    Document doc = Jsoup.parse(urlFetcher.fetchUrl(story));
    return extractStoryMetadata(doc);
  }

  @VisibleForTesting Jffnet.StoryInfo extractStoryMetadata(Document doc) {
    Jffnet.StoryInfo.Builder builder = Jffnet.StoryInfo.newBuilder();

    String author = doc.select(AUTHOR_XPATH).first().text();

    Element descrElement = doc.select(STORY_DESCRIPTION_XPATH).first();
    Element titleElement = doc.select(STORY_TITLE_XPATH).first();
    Element infoElement = descrElement.nextElementSibling();

    parseStoryMeta(infoElement.text(), builder);
    builder.addAuthor(author);
    builder.setDescription(descrElement.text());
    builder.setTitle(titleElement.text());

    return builder.build();
  }

  @VisibleForTesting Iterable<ChapterUrl> extractChapters(Document doc) {
    Element select = doc.select(CHAPTER_SELECTOR_XPATH).first();
    Elements options = select.getElementsByTag("option");

    String onchange = select.attr("onchange");

    LOG.info(onchange);

    Matcher matcher = ONCHANGE_PATTERN.matcher(onchange);

    if (matcher.matches()) {

      final String locFirstPart = matcher.group(1);
      final String locTitlePart = matcher.group(2);

      return Iterables.transform(options, new Function<Element, ChapterUrl>() {
        @Override
        public ChapterUrl apply(Element input) {
          return ChapterUrl.create(
              BASE_URL + locFirstPart + input.attr("value") + locTitlePart,
              input.text());
        }
      });
    } else {
      throw new RuntimeException("Couldn't parse onchange attr " + onchange);
    }
  }
}

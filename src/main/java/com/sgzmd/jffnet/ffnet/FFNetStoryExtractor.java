package com.sgzmd.jffnet.ffnet;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.*;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.joestelmach.natty.DateGroup;
import com.joestelmach.natty.Parser;
import com.sgzmd.jffnet.ChapterUrl;
import com.sgzmd.jffnet.Progress;
import com.sgzmd.jffnet.UrlContentFetcher;
import com.sgzmd.jffnet.proto.Jffnet;
import org.apache.commons.lang.StringEscapeUtils;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.List;
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

  private static final Pattern ONCHANGE_PATTERN = Pattern.compile("self.location = '(/s/[0-9]+/)'.+\\+ '(/.+)';");
  private static final DateTimeFormatter DATE_PARSER = DateTimeFormat.forPattern("MM/dd/YYYY");

  private static final int INDEX_RATING = 0;
  private static final int INDEX_LANGUAGE = 1;
  private static final int INDEX_GENRE = 2;
  private static final int INDEX_CHARACTER = 3;
  private static final int INDEX_REVIEWS = 6;
  private static final int INDEX_FAVS = 7;



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

  private static class MetaParser {
    public static final Predicate<String> KVP_PREDICATE = new Predicate<String>() {
      @Override
      public boolean apply(String input) {
        return isKvpLine(input);
      }
    };

    private final Iterable<String> kvpLines;
    private final ImmutableList<String> nonKvpLines;

    MetaParser(String[] values) {
      ImmutableList<String> list = ImmutableList.copyOf(values);

      // Partitioning meta into KVP and non-KVP lines
      this.kvpLines = Iterables.filter(list, KVP_PREDICATE);
      this.nonKvpLines = ImmutableList.copyOf(Iterables.filter(list, Predicates.not(KVP_PREDICATE)));
    }

    private static boolean isKvpLine(String line) {
      return line.indexOf(':') > 0;
    }

    private Optional<String> findStringByPrefix(final String prefix) {
      return Iterables.tryFind(kvpLines, new Predicate<String>() {
        @Override
        public boolean apply(String input) {
          return input.trim().toLowerCase().startsWith(prefix.toLowerCase());
        }
      });
    }

    private Optional<String> getValue(Optional<String> metaLineOptional) {
      if (!metaLineOptional.isPresent()) {
        return Optional.absent();
      }

      String metaLine = metaLineOptional.get();
      if (metaLine.contains(":")) {
        return Optional.of(metaLine.split(":")[1].trim());
      } else {
        LOG.info(String.format("Couldn't getValue from line: %s", metaLine));
        return Optional.absent();
      }
    }

    Optional<Integer> fromString(Optional<String> string) {
      if (string.isPresent()) {
        try {
          return Optional.of(Integer.parseInt(string.get().replaceAll(",", "")));
        } catch (NumberFormatException e) {
          LOG.warning(String.format("Misformatted number: %s", string));
        }
      }

      return Optional.absent();
    }

    Optional<Jffnet.Rating> getRating() {
      Optional<String> ratingString = getValue(findStringByPrefix("Rated"));

      if (!ratingString.isPresent()) {
        return Optional.absent();
      }

      return Optional.fromNullable(RATINGS_MAP.get(ratingString));
    }

    Optional<Integer> getNumChapters() {
      return getIntegerOptionalForPrefix("Chapters");
    }

    Optional<Integer> getNumWords() {
      return getIntegerOptionalForPrefix("Words");
    }

    Optional<Integer> getNumReviews() {
      return getIntegerOptionalForPrefix("Reviews");
    }

    Optional<Integer> getNumFavs() {
      return getIntegerOptionalForPrefix("Favs");
    }

    Optional<Integer> getNumFollows() {
      return getIntegerOptionalForPrefix("Follows");
    }

    Optional<String> getUpdated() {
      return getValue(findStringByPrefix("Updated"));
    }

    Optional<String> getPublished() {
      return getValue(findStringByPrefix("Published"));
    }

    private Optional<Integer> getIntegerOptionalForPrefix(String chapters) {
      return fromString(getValue(findStringByPrefix(chapters)));
    }

    Optional<String> getLanguage() {
      if (nonKvpLines.size() > 0) {
        return Optional.of(nonKvpLines.get(0));
      } else {
        return Optional.absent();
      }
    }

    Optional<String> getGenre() {
      // Here goes the assumption that if Genre OR pairing is omitted,
      // Genre is more likely to be present. I know, it sucks.

      if (nonKvpLines.size() > 1) {
        return Optional.of(nonKvpLines.get(1));
      } else {
        return Optional.absent();
      }
    }

    Optional<String> getPairing() {
      if (nonKvpLines.size() > 2) {
        return Optional.of(nonKvpLines.get(2));
      } else {
        return Optional.absent();
      }
    }
  }

   /*


Rated: Fiction M
 English
 Chapters: 31
 Words: 246,320
 Reviews: 5,293
 Favs: 9,410
 Follows: 2,804
 Updated: 4/7/2008
 Published: 2/18/2007
 Status: Complete
 id: 3401052

   */

  static final Function<String, String> STRING_TRIM_FUNCTION = new Function<String, String>() {
    @Override
    public String apply(String input) {
      return input.trim();
    }
  };

  private final UrlContentFetcher urlFetcher;
  private final Progress progress;

  @Inject public FFNetStoryExtractor(UrlContentFetcher urlFetcher, Progress progress) {
    this.urlFetcher = urlFetcher;
    this.progress = progress;
  }

  public Jffnet.Story getStory(String storyUrl) throws IOException {
    LOG.info("getStory -> " + storyUrl);
    Document doc = Jsoup.parse(urlFetcher.fetchUrl(storyUrl));

    Jffnet.Story.Builder storyBuilder = Jffnet.Story.newBuilder();

    storyBuilder.setInfo(this.extractStoryMetadata(doc));
    Iterable<ChapterUrl> chapterUrls = extractChapters(doc);

    progress.setProgressMax(Iterables.size(chapterUrls));

    for (ChapterUrl url : chapterUrls) {
      LOG.info("Reading chapter '" + url.chapter() + "' (" + url.url() + ")" );
      String chapterHtml = urlFetcher.fetchUrl(url.url());
      Element div = Jsoup.parse(chapterHtml).select("div.storytext").first();
      storyBuilder.addChapterBuilder()
          .setTitle(url.chapter())
          .setUrl(url.url())
          .setText(StringEscapeUtils.unescapeHtml(div.html()));

      progress.step();
    }

    return storyBuilder.build();
  }

  @VisibleForTesting void parseStoryMeta(String meta, Jffnet.StoryInfo.Builder builder) {
    String[] values = meta.split("-");

    MetaParser metaParser = new MetaParser(values);

    if (metaParser.getRating().isPresent()) {
      builder.setRating(metaParser.getRating().get());
    } else {
      LOG.info("No rating info found for this story: " + meta);
    }

    if (metaParser.getLanguage().isPresent()) {
      builder.setLanguage(metaParser.getLanguage().get());
    } else {
      LOG.info("No Language found for this story: " + meta);
    }

    if (metaParser.getGenre().isPresent()) {
      builder.addAllGenre(Splitter.on('/').split(metaParser.getGenre().get()));
    } else {
      LOG.info("No Genre found for this story: " + meta);
    }

//    builder.addAllGenre(Iterables.transform(
//        Splitter.on('/').split(values[INDEX_GENRE]),
//        STRING_TRIM_FUNCTION
//    ));
//
//    builder.addAllCharacter(Iterables.transform(
//        Splitter.on(',').split(values[INDEX_CHARACTER]),
//        STRING_TRIM_FUNCTION
//    ));
//
//    builder.setReviews(extractNumericValue(values, INDEX_REVIEWS));
//    builder.setFavs(extractNumericValue(values, INDEX_FAVS));
//
//    builder.setStatus(Jffnet.Status.WIP);

    // parsing remaining values, which may not be all present
//    for (int i = INDEX_FAVS + 1; i < values.length; ++i) {
//      String value = values[i].toLowerCase().trim();
//      if (value.startsWith("updated")) {
//        try {
//          LocalDate dateTime = getDateTime(value);
//          if (dateTime.getYear() > 1991) {
//            builder.setUpdatedDate(dateTime.toDateTimeAtStartOfDay(DateTimeZone.UTC).getMillis());
//          }
//        } catch (Throwable e) {
//          LOG.log(Level.WARNING, "Problem parsing updated date <" + value + ">", e);
//        }
//      } else if (value.startsWith("published")) {
//        try {
//          LocalDate dateTime = getDateTime(value);
//          if (dateTime.getYear() > 1991) {
//            builder.setPublishedDate(dateTime.toDateTimeAtStartOfDay(DateTimeZone.UTC).getMillis());
//          }
//        } catch (Throwable e) {
//          LOG.log(Level.WARNING, "Problem parsing published date <" + value + ">", e);
//        }
//      } else if (value.toLowerCase().contains("complete")) {
//        builder.setStatus(Jffnet.Status.COMPLETED);
//      }
//    }
  }

  @VisibleForTesting static LocalDate parseDate(String str) {
    Parser parser = new Parser();
    List<DateGroup> dateGroups = parser.parse(str);
    for (DateGroup dg : dateGroups) {
      return new LocalDate(Iterables.getFirst(dg.getDates(), null).getTime());
    }

    return null;
  }

  @VisibleForTesting LocalDate getDateTime(String value) {
    String text = value.split(":")[1].trim();
    return parseDate(text);
  }

  @VisibleForTesting int extractNumericValue(String[] values, int idx) {
    return Integer.parseInt(values[idx].trim().split(":")[1].replace(",", "").trim());
  }

  @VisibleForTesting public Jffnet.StoryInfo extractStoryMetadata(String story) throws IOException {
    Document doc = Jsoup.parse(urlFetcher.fetchUrl(story));
    return extractStoryMetadata(doc);
  }

  @VisibleForTesting public Jffnet.StoryInfo extractStoryMetadata(Document doc) {
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

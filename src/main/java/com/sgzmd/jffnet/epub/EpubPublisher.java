package com.sgzmd.jffnet.epub;

import com.floreysoft.jmte.Engine;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import com.sgzmd.jffnet.proto.Jffnet;
import nl.siegmann.epublib.domain.*;
import nl.siegmann.epublib.epub.EpubWriter;
import nl.siegmann.epublib.service.MediatypeService;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

public class EpubPublisher {
  private static final Logger LOG = Logger.getLogger(EpubPublisher.class.getName());
  private static final ThreadLocal<Engine> ENGINE = new ThreadLocal<Engine>() {
    @Override
    protected Engine initialValue() {
      return new Engine();
    }
  };
  private final String titlePageTemplate;
  private final String pageTemplate;

  @Inject public EpubPublisher() throws IOException {
    this.pageTemplate = Resources.toString(Resources.getResource("page.html"), StandardCharsets.UTF_8);
    this.titlePageTemplate = Resources.toString(Resources.getResource("title_page.html"), StandardCharsets.UTF_8);
  }

  public void publish(Jffnet.Story story, OutputStream output) throws IOException {
    LOG.info("Publishing " + story.getInfo().getTitle());

    Book book = new Book();
    DateTimeFormatter isoDateTimeFormat = ISODateTimeFormat.date();
    Metadata meta = new Metadata();
    meta.addTitle(story.getInfo().getTitle());

    for (String author : story.getInfo().getAuthorList()) {
      meta.addAuthor(new Author(author));
    }

    meta.addDescription(story.getInfo().getDescription());
    meta.setLanguage(story.getInfo().getLanguage());

    if (story.getInfo().hasPublishedDate()) {
      meta.addDate(new Date(
          isoDateTimeFormat.print(story.getInfo().getPublishedDate()),
          Date.Event.PUBLICATION));
    }

    if (story.getInfo().hasUpdatedDate()) {
      meta.addDate(new Date(
          isoDateTimeFormat.print(story.getInfo().getUpdatedDate()),
          Date.Event.MODIFICATION));
    }

    book.setMetadata(meta);
    book.setCoverPage(new Resource(renderTitlePage(story).getBytes(), MediatypeService.XHTML));

    // TODO(sgzmd): add reviews, status, favs et al as extra fields

    int counter = 0;
    for (Jffnet.Chapter chapter : story.getChapterList()) {
      LOG.info(chapter.getTitle());
      book.addSection(
          chapter.getTitle(),
          new Resource(ENGINE.get().transform(
              pageTemplate,
              ImmutableMap.<String, Object>of("chapter", chapter)).getBytes(), MediatypeService.XHTML));
    }

    new EpubWriter().write(book, output);
  }

  @VisibleForTesting String renderTitlePage(Jffnet.Story story) throws IOException {
    return ENGINE.get().transform(titlePageTemplate, ImmutableMap.<String, Object>of("info", story.getInfo()));
  }
}

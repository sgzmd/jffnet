package com.sgzmd.jffnet;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.html.HtmlEscapers;
import com.google.common.util.concurrent.Futures;
import com.google.inject.Guice;
import com.google.protobuf.TextFormat;
import com.sgzmd.jffnet.proto.Jffnet;
import de.jetwick.snacktory.HtmlFetcher;
import de.jetwick.snacktory.JResult;
import nl.siegmann.epublib.domain.*;
import nl.siegmann.epublib.epub.EpubWriter;
import nl.siegmann.epublib.service.MediatypeService;
import org.apache.commons.lang.StringEscapeUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.logging.Logger;

public class CommandLineApp {
  private static final Logger LOG = Logger.getLogger(CommandLineApp.class.getName());
  private static final String URL = "https://www.fanfiction.net/s/3401052/1/A-Black-Comedy";
  private static final String BASE_URL = "https://www.fanfiction.net";
  public static final String HTML_PREFIX = "<html xmlns=\"http://www.w3.org/1999/xhtml\" xmlns:epub=\"http://www.idpf.org/2007/ops\"><head><title>Sample</title></head><body>";

//  public static void main_(String[] args) throws Exception {
//    Jffnet.StoryInfo storyInfo = Jffnet.StoryInfo.newBuilder()
//        .setTitle("abc")
//        .addAuthor("123")
//        .build();
//
//    Document soup = Jsoup.connect(URL).get();
//    Element chapterSelect = soup.getElementById("chap_select");
//
//    String onChange = chapterSelect.attr("onChange");
//    final String[] comps = onChange.replaceAll("'", "").split("=")[1].split("\\+");
//
//    LOG.info(comps[0] + comps[2]);
//
//    Elements options = chapterSelect.select("option");
//
//    Iterable<String> urls = Iterables.transform(Iterables.transform(
//        options, new Function<Element, String>() {
//          @Override
//          public String apply(Element element) {
//            return element.attr("value");
//          }
//        }
//    ), new Function<String, String>() {
//      @Override
//      public String apply(String s) {
//        return (BASE_URL + comps[0].trim() + s + comps[2].trim()).replaceAll(";", "");
//      }
//    });
//
//
//    Book book = new Book();
//    Metadata metadata = new Metadata();
//    metadata.addTitle("My Sample Epub");
//    metadata.addAuthor(new Author("Me, myself and I"));
//
//    book.setMetadata(metadata);
//
//
//    HtmlFetcher fetcher = new HtmlFetcher();
//    int resId = 0;
//    for (String s : urls) {
//      LOG.info(s);
//      Elements select = Jsoup.connect(s).get().select("div.storytext");
//
//      Element el = select.first();
//      while ((el = el.nextElementSibling()) != null) {
//        for (Attribute attr : el.attributes().asList()) {
//          el.attributes().remove(attr.getKey());
//        }
//      }
//
//      String html = HTML_PREFIX + select.outerHtml() + "</body></html>";
//
//      book.addSection(s, new Resource(StringEscapeUtils.unescapeHtml(html).getBytes(), MediatypeService.XHTML));
//
//      LOG.info(html);
//
//      if (++resId > 3)
//        break;
//    }
//
//    EpubWriter epubWriter = new EpubWriter();
//    epubWriter.write(book, new FileOutputStream(new File("output.epub")));
//
//
//    LOG.info(TextFormat.shortDebugString(storyInfo));
//  }

  public static void main(String[] args) throws IOException {
    FFNetStoryExtractor extractor = Guice.createInjector().getInstance(FFNetStoryExtractor.class);
    LOG.info(extractor.getStory(args[0]).toString());
  }
}

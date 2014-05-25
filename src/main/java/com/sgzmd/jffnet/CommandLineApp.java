package com.sgzmd.jffnet;

import com.google.inject.Guice;
import com.sgzmd.jffnet.epub.EpubPublisher;
import com.sgzmd.jffnet.ffnet.FFNetStoryExtractor;
import com.sgzmd.jffnet.proto.Jffnet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Logger;

public class CommandLineApp {
  private static final Logger LOG = Logger.getLogger(CommandLineApp.class.getName());
  private static final String URL = "https://www.fanfiction.net/s/3401052/1/A-Black-Comedy";

  public static void main(String[] args) throws IOException {
    FFNetStoryExtractor extractor = Guice.createInjector().getInstance(FFNetStoryExtractor.class);
    Jffnet.Story story = extractor.getStory(args[0]);
    new EpubPublisher().publish(story, Files.newOutputStream(Paths.get(args[1])));
  }
}

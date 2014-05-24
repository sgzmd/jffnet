package com.sgzmd.jffnet;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

@Singleton
public class SimpleUrlContentFetcher implements UrlContentFetcher {
  @Override
  public String fetchUrl(String url) throws IOException {
    return IOUtils.toString(new URL(url).openStream());
  }
}

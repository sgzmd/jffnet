package com.sgzmd.jffnet;

import com.google.inject.Singleton;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

@Singleton
public class SimpleUrlContentFetcher implements UrlContentFetcher {
  @Override
  public String fetchUrl(String url)
      throws IOException {
    URL u = new URL(url);
    BufferedReader in = new BufferedReader(
        new InputStreamReader(u.openStream()));
    StringBuilder sb = new StringBuilder();
    String inputLine;
    while ((inputLine = in.readLine()) != null)
      sb.append(inputLine);
    in.close();

    return sb.toString();
  }
}

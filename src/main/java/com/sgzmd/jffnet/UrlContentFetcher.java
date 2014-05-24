package com.sgzmd.jffnet;

import com.google.inject.ImplementedBy;

import java.io.BufferedReader;
import java.io.IOException;

@ImplementedBy(SimpleUrlContentFetcher.class)
public interface UrlContentFetcher {
  String fetchUrl(String url) throws IOException;
}

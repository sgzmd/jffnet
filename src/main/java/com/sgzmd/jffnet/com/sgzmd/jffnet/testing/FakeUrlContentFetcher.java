package com.sgzmd.jffnet.com.sgzmd.jffnet.testing;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.sgzmd.jffnet.UrlContentFetcher;

import java.io.IOException;

public class FakeUrlContentFetcher implements UrlContentFetcher {
  private final ImmutableMap<String, String> map;

  public FakeUrlContentFetcher(String... args) {
    Preconditions.checkArgument(args.length % 2 == 0);
    ImmutableMap.Builder<String,String> builder = ImmutableMap.builder();
    for (int i = 0; i < args.length; i+=2) {
      builder.put(args[i], args[i+1]);
    }
    this.map = builder.build();
  }

  public String fetchUrl(String url) throws IOException {
    return map.get(url);
  }
}

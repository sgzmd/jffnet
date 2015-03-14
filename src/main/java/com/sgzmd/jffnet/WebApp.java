package com.sgzmd.jffnet;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import sun.net.httpserver.HttpServerImpl;

import java.io.IOException;
import java.net.InetSocketAddress;

public class WebApp {
  public static void main(String[] args) throws IOException {
    HttpServer httpServer = HttpServer.create(new InetSocketAddress(8000), 0);
    httpServer.createContext("/download", new HttpHandler() {
      @Override
      public void handle(HttpExchange httpExchange) throws IOException {

        String url = (String) httpExchange.getAttribute("url");
        httpExchange.sendResponseHeaders(200, url.length());
        httpExchange.getResponseBody().write(url.getBytes());
        httpExchange.getResponseBody().close();
      }
    });

    httpServer.start();
  }
}

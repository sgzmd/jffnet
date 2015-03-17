package com.sgzmd.jffnet.web;

import com.google.common.base.Preconditions;
import com.sgzmd.jffnet.Progress;
import com.sgzmd.jffnet.SimpleUrlContentFetcher;
import com.sgzmd.jffnet.epub.EpubPublisher;
import com.sgzmd.jffnet.ffnet.FFNetStoryExtractor;
import com.sgzmd.jffnet.proto.Jffnet;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class DownloaderServlet extends HttpServlet {
  FFNetStoryExtractor extractor = new FFNetStoryExtractor(
      new SimpleUrlContentFetcher(),
      new Progress() {
        @Override
        public int getProgressMax() {
          return 0;
        }

        @Override
        public void setProgressMax(int progressMax) {

        }

        @Override
        public void step() {

        }

        @Override
        public int getProgress() {
          return 0;
        }
      });
  
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String url = request.getParameter("url");

    Jffnet.Story story = extractor.getStory(url);
    EpubPublisher publisher = new EpubPublisher();

    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    publisher.publish(story, bos);
    
    
    response.setContentType("application/epub+zip");
    response.setContentLength(bos.size());
    response.setHeader("Content-Disposition", "attachment; filename=\"" + story.getInfo().getTitle() + ".epub\"");
    response.getOutputStream().write(bos.toByteArray());
    

//    response.setContentType("text/html");
    response.setStatus(HttpServletResponse.SC_OK);
//    response.getWriter().println("<h1>Hello Servlet</h1>");
//    response.getWriter().println("session=" + request.getSession(true).getId());


  }
}

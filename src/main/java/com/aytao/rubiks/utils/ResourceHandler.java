package com.aytao.rubiks.utils;

import java.io.File;
import java.net.URL;

public class ResourceHandler {
  public static File getFile(String fileName) throws Exception {
    ClassLoader classLoader = ResourceHandler.class.getClassLoader();
    URL resource = classLoader.getResource(fileName);
    if (resource == null)
      throw new Exception("Error retrieving resource " + fileName);
    String filePath = resource.toURI().getPath();
    File file = new File(filePath);
    return file;
  }
}

package com.xml.processing.xml.processing.service;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FileProcessingServiceImpl implements FileProcessingService {

  private static final Logger LOGGER = LoggerFactory.getLogger(FileProcessingServiceImpl.class);

  @Override
  public ResponseEntity processFilesData(
      String inputFilePath, String sourceFilesPath, String targetFilePath) throws IOException {

    LOGGER.info("Entering into processFilesData " + inputFilePath);
    String fileSuffix = new SimpleDateFormat("yyyyMMdd").format(new Date());
    File directory = new File(targetFilePath + "nhiAuth_" + fileSuffix + ".txt");
    FileUtils.deleteQuietly(directory);
    directory.getParentFile().mkdirs();
    directory.createNewFile();

    List<String> allFileLines =
        Stream.of(
                Objects.requireNonNull(
                    new File(sourceFilesPath).listFiles((FileFilter) FileFileFilter.FILE)))
            .map(
                file -> {
                  try {
                    return Files.lines(Paths.get(file.getPath()));
                  } catch (IOException e) {
                    throw new RuntimeException(e);
                  }
                })
            .flatMap(list -> list)
            .collect(Collectors.toList());

    String totalString =
        Files.lines(Paths.get(inputFilePath))
            .parallel()
            .map(StringUtils::splitPreserveAllTokens)
            .filter(ArrayUtils::isNotEmpty)
            .map(
                words -> {
                  String searchableString = words[0];
                  System.out.println(searchableString);
                  return CompletableFuture.supplyAsync(
                      () -> searchLineInAllFilesByWord(searchableString, allFileLines),
                      ForkJoinPool.commonPool());
                })
            .map(CompletableFuture::join)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.joining("\n"));

    writeMatchedLineToAFile(directory, totalString);

    return new ResponseEntity<Map.Entry<String, String>>(
        new AbstractMap.SimpleEntry<>(
            "message", "successfully  written matched data to target file"),
        HttpStatus.OK);
  }

  private void writeMatchedLineToAFile(File directory, String totalString) {
    try (FileWriter fw = new FileWriter(directory.getPath(), true);
        BufferedWriter bw = new BufferedWriter(fw);
        PrintWriter out = new PrintWriter(bw)) {
        out.println(totalString);
    } catch (IOException e) {
      // exception handling left as an exercise for the reader
      e.printStackTrace();
    }
  }

  private Optional<String> searchLineInAllFilesByWord(String word, List<String> lines) {

    return lines
        .parallelStream()
        .filter(line -> StringUtils.containsIgnoreCase(line, word))
        .findFirst();
  }
}

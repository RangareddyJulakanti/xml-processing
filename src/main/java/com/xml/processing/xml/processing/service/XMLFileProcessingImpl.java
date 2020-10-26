package com.xml.processing.xml.processing.service;

import com.ximpleware.*;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class XMLFileProcessingImpl {

  public static void main(String[] args) throws IOException {

    String sourceFilesPath = "E:\\SWetha\\SearchableFiles";
    String inputFilePath = "E:\\SWetha\\InputFile";
    List<AutoPilot> collectAllAutopilot =
        Stream.of(
                Objects.requireNonNull(
                    new File(sourceFilesPath).listFiles((FileFilter) FileFileFilter.FILE)))
            .map(
                file -> {
                  try {
                    byte[] fileDataInBytes = Files.readAllBytes(Paths.get(file.getPath()));
                    return autoPilot(fileDataInBytes);
                  } catch (IOException e) {
                    throw new RuntimeException(e);
                  }
                })
            .collect(Collectors.toList());

    AutoPilot inputFileAutopilot =
        Stream.of(
                Objects.requireNonNull(
                    new File(inputFilePath).listFiles((FileFilter) FileFileFilter.FILE)))
            .map(
                file -> {
                  try {
                    byte[] fileDataInBytes = Files.readAllBytes(Paths.get(file.getPath()));
                    return autoPilot(fileDataInBytes);
                  } catch (IOException e) {
                    throw new RuntimeException(e);
                  }
                })
            .findFirst()
            .get();
    List<String> authorizationIds = collectAllAuthorizationIds(inputFileAutopilot);
    String result = collectMatchedNodes(authorizationIds, collectAllAutopilot);
    System.out.println(result);
  }

  private static String collectMatchedNodes(
      List<String> authorizationIds, List<AutoPilot> collectAllAutopilot) {
    String outPutString =
        authorizationIds.stream()
            .map(authorizationId -> selectMatchedString(authorizationId, collectAllAutopilot))
            .filter(Objects::nonNull)
            .collect(Collectors.joining("\\n"));
    String result =
        new StringBuilder()
            .append(
                "<tns:authorizations xmlns:tns=\"http://highmark.com/hmp/batch/components/NaviHealthAuthImport/common\">")
            .append(outPutString)
            .append("</tns:authorizations>")
            .toString();
    return result;
  }

  private static String selectMatchedString(String authorizationId, List<AutoPilot> inputFileAutopilots) {
    StringBuilder sb = new StringBuilder();
    for (AutoPilot inputFileAutopilot : inputFileAutopilots) {
      if (sb.length() != 0) {
        break;
      }
      int lineNumber = 1;
      String currentAuthorizationPath = "//authorization[" + lineNumber + "]";
      try {
        inputFileAutopilot.selectXPath(currentAuthorizationPath);
        while ((inputFileAutopilot.evalXPath()) != -1) {
          Optional<String> value =
              getValue(
                  inputFileAutopilot,
                  currentAuthorizationPath + "/authorizationId/text()",
                  String.class);
          if (value.isPresent() && value.get().equalsIgnoreCase(authorizationId)) {
            long index = inputFileAutopilot.getNav().getContentFragment();
            sb.append(
                "<tns:authorization>"
                    + inputFileAutopilot.getNav().toString((int) index, (int) (index >> 32))
                    + "</tns:authorization>");
            break;
          }
          lineNumber++;
          currentAuthorizationPath = "//authorization[" + lineNumber + "]";
          inputFileAutopilot.selectXPath(currentAuthorizationPath);
        }

      } catch (XPathParseException | NavException | XPathEvalException e) {
        throw new RuntimeException(e);
      }
    }
    return sb.toString().length() < 1 ? null : sb.toString();
  }

  private static List<String> collectAllAuthorizationIds(AutoPilot inputFileAutopilot) {
    List<String> authorizationIds = new ArrayList<>();
    int lineNumber = 1;
    String currentAuthorizationPath = "//authorization[" + lineNumber + "]";
    try {
      inputFileAutopilot.selectXPath(currentAuthorizationPath);
      while ((inputFileAutopilot.evalXPath()) != -1) {
        Optional<String> authorizationId =
            getValue(
                inputFileAutopilot,
                currentAuthorizationPath + "/authorizationId/text()",
                String.class);
        authorizationId.ifPresent(authorizationIds::add);
        lineNumber++;
        currentAuthorizationPath = "//authorization[" + lineNumber + "]";
        inputFileAutopilot.selectXPath(currentAuthorizationPath);
      }
      return authorizationIds;
    } catch (XPathParseException | NavException | XPathEvalException e) {
      throw new RuntimeException(e);
    }
  }

  private static <T> Optional<T> getValue(AutoPilot autoPilot, String xPath, Class<T> responseType) {
    try {
      autoPilot.selectXPath(xPath);
      if ("String".equals(responseType.getSimpleName())) {
        return Optional.ofNullable(!StringUtils.isBlank(autoPilot.evalXPathToString()) ? (T) autoPilot.evalXPathToString() : null);
      }
      return Optional.empty();
    } catch (XPathParseException e) {
      throw new RuntimeException(e);
    }
  }

  private static AutoPilot autoPilot(byte[] fileDataInBytes) {
    VTDGen generator = new VTDGen();
    generator.setDoc(fileDataInBytes);
    try {
      generator.parse(true);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    VTDNav navigator = generator.getNav();
    return new AutoPilot(navigator);
  }
}

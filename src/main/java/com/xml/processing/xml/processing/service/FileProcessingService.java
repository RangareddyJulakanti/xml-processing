package com.xml.processing.xml.processing.service;

import org.springframework.http.ResponseEntity;

import java.io.IOException;

public interface FileProcessingService {

    ResponseEntity processFilesData(String inputFilePath, String sourceFilesPath, String targetFilePath) throws IOException;

}

package com.xml.processing.xml.processing.controller;

import com.xml.processing.xml.processing.service.FileProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

import static org.springframework.http.HttpStatus.OK;

@RestController
@RequestMapping("/highmark/services")
public class FileProcessingController {

    @Autowired
    private FileProcessingService fileProcessingService;

    private static final Logger LOGGER = LoggerFactory.getLogger(FileProcessingController.class);

    @ResponseStatus(OK)
    @RequestMapping(value = "/process", method = RequestMethod.GET)
    public ResponseEntity processFilesData(@RequestParam String inputFilePath, @RequestParam String sourceFilesPath,
                                           @RequestParam String targetFilePath) throws Exception, IOException {
        LOGGER.info("Entering into processFilesData  ", inputFilePath);
        // return ResponseEntity.status(OK).body(fileProcessingService.processFilesData(inputFilePath));
        return fileProcessingService.processFilesData(inputFilePath, sourceFilesPath, targetFilePath);
    }


}

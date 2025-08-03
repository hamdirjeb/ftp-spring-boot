package com.ftp.controller;

import com.ftp.service.FtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/ftp")
public class FtpController {

    private final FtpService ftpService;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file,
                                             @RequestParam("sourcepath") String sourcepath) {
        try {
            ftpService.uploadFile(file.getInputStream(), sourcepath);
            return ResponseEntity.status(HttpStatus.OK).body("File uploaded successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload file: " + e.getMessage());
        }
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> download(@RequestParam String filename, @RequestParam String sourcepath) {
        try {
            Resource fileResource = ftpService.downloadFileToInputStream(filename, sourcepath);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(fileResource);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/downloadbyte")
    public ResponseEntity<byte[]> downloadFileToByte(
            @RequestParam String directory,
            @RequestParam String fileName
    ) throws IOException {

        byte[] fileData = ftpService.downloadFileToByte(directory, fileName);
        String contentType = Files.probeContentType(Paths.get(fileName));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentDisposition(ContentDisposition.inline().filename(fileName).build());
        return new ResponseEntity<>(fileData, headers, HttpStatus.OK);
    }

    @GetMapping("/listDirectories")
    public List<String> getCurrentWorkingDir() throws IOException {
        return ftpService.getDirectories();
    }

    @GetMapping("/newDir")
    public String createNewDirectory(@RequestParam String dirName) throws IOException {
        ftpService.createNewDirectory(dirName);
        return "New Working Directory: " + dirName;
    }

    @GetMapping("/changeDir")
    public String changeDirectory(@RequestParam String dirName) throws IOException {
        ftpService.changeDirectory(dirName);
        return "Using new working directory for operations.";
    }

    @GetMapping("/files")
    public List<String> getFiles(@RequestParam String dirName) throws IOException {
        return ftpService.listNamesFiles(dirName);
    }

    @DeleteMapping("/")
    public ResponseEntity<String> deleteFile(@RequestParam String directory, @RequestParam String fileName) throws IOException {
        boolean deleted = ftpService.deleteFile(directory, fileName);
        if (deleted) {
            return ResponseEntity.ok("File deleted successfully.");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found or could not be deleted.");
        }
    }

    @GetMapping("/isExist")
    public Boolean checkIfDirectoryExist(@RequestParam String directory) throws IOException {
        return ftpService.checkIfDirectoryExist(directory);
    }

}

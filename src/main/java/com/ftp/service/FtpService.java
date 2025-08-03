package com.ftp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Service
public class FtpService {

    @Value("${ftp.host}")
    private String ftpHost;

    @Value("${ftp.port}")
    private int ftpPort;

    @Value("${ftp.username}")
    private String ftpUsername;

    @Value("${ftp.password}")
    private String ftpPassword;

    // configures and returns a connected and logged-in FTPClient.
    private FTPClient configureFtpClient() throws IOException {

        FTPClient ftpClient = new FTPClient();
        ftpClient.connect(ftpHost, ftpPort);
        ftpClient.login(ftpUsername, ftpPassword);
        ftpClient.enterLocalPassiveMode();
        ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
        return ftpClient;
    }

    public void uploadFile(InputStream inputStream, String uploadPath) throws IOException {
        FTPClient ftpClient = configureFtpClient();
        try {
            boolean result = ftpClient.storeFile(uploadPath, inputStream);
            if (!result) {
                throw new IOException("Could not upload the file to the FTP server.");
            }
        } finally {
            // ensure FTPClient is disconnected
            if (ftpClient.isConnected()) {
                ftpClient.logout();
                ftpClient.disconnect();
            }
        }

    }

    public Resource downloadFileToInputStream(String filename, String dir) throws IOException {

        FTPClient ftpClient = configureFtpClient();
        boolean changed = ftpClient.changeWorkingDirectory(dir);
        if (!changed) {
            throw new RuntimeException("Could not change directory to /images/");
        }
        InputStream inputStream = ftpClient.retrieveFileStream(filename);
        if (inputStream == null) {
            throw new RuntimeException("File not found or could not be downloaded.");
        }

        return new InputStreamResource(inputStream);
    }

    public byte[] downloadFileToByte(String directory, String fileName) throws IOException {
        FTPClient ftpClient = configureFtpClient();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        ftpClient.enterLocalPassiveMode();
        String filePath = directory + "/" + fileName;
        boolean retrieved = ftpClient.retrieveFile(filePath, outputStream);
        if (!retrieved) {
            throw new IOException("Failed to retrieve file: " + filePath);
        }
        return outputStream.toByteArray();
    }

    public void createNewDirectory(String dirName) throws IOException {
        FTPClient ftpClient = configureFtpClient();
        ftpClient.makeDirectory(dirName);
    }

    public void changeDirectory(String dirName) throws IOException {
        FTPClient ftpClient = configureFtpClient();
        ftpClient.changeWorkingDirectory(dirName);
    }

    public List<String> getDirectories() throws IOException {
        List<String> directories = new ArrayList<>();
        FTPClient ftpClient = configureFtpClient();
        FTPFile[] dirs = ftpClient.listDirectories();
        Arrays.stream(dirs).forEach(dir -> {
            directories.add(dir.getName());
        });
        return directories;
    }

    public List<String> listNamesFiles(String directory) throws IOException {
        List<String> fileNames = new ArrayList<>();
        FTPClient ftpClient = configureFtpClient();
        ftpClient.enterLocalPassiveMode(); // passive mode for firewall safety

        FTPFile[] files = ftpClient.listFiles(directory);
        for (FTPFile file : files) {
            if (file.isFile()) {
                fileNames.add(file.getName());
            }
        }
        return fileNames;
    }

    public boolean deleteFile(String directory, String fileName) throws IOException {
        boolean deleted;
        FTPClient ftpClient = configureFtpClient();
        ftpClient.enterLocalPassiveMode();
        String filePath = directory + "/" + fileName;
        deleted = ftpClient.deleteFile(filePath);
        return deleted;
    }

    public Boolean checkIfDirectoryExist(String directory) throws IOException {
        FTPClient ftpClient = configureFtpClient();
        String[] directories = directory.split("/");
        String currentPath = "";

        for (String dir : directories) {
            if (dir.isEmpty()) continue; // skip empty strings from leading slashes
            currentPath += "/" + dir;
            boolean changed = ftpClient.changeWorkingDirectory(currentPath);
            if (!changed) {
                return false; // Directory or subdirectory does not exist
            }
        }
        return true;
    }
}


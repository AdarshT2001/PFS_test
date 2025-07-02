package com.example.sftp.service;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.ChannelSftp;

import java.util.Properties;

public class SftpConnectionValidator {

    public static boolean validateSftpConnection(String sftpHost, int sftpPort, String username, String password) {
        boolean isValid = false;
        ChannelSftp sftpChannel = null;
        Session session = null;

        try {
            // Create a JSch instance
            JSch jsch = new JSch();

            // Set session properties
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");  // Disable strict host key checking for demo purposes

            // Create a session to the SFTP server
            session = jsch.getSession(username, sftpHost, sftpPort);
            session.setPassword(password);
            session.setConfig(config);

            // Connect to the server
            session.connect();

            // Open an SFTP channel
            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();

            // Try listing files in the root directory of the SFTP server
            sftpChannel.ls("/");

            // If no exception occurred, the connection is valid
            isValid = true;
            System.out.println("SFTP connection successful!");

        } catch (Exception e) {
            // Catch any exceptions (e.g., connection issues, authentication failure, etc.)
            System.err.println("SFTP connection failed: " + e.getMessage());
        } finally {
            // Close the SFTP channel and session
            if (sftpChannel != null && sftpChannel.isConnected()) {
                sftpChannel.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }

        return isValid;
    }
}

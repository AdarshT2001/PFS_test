# AWS Transfer Family SFTP & S3 Connector (Java)

This project demonstrates how to:
- Connect to AWS Transfer Family (SFTP) using Java 21
- List, download, and upload files via SFTP
- Connect to an S3 bucket and upload files using streaming

## Tech Stack
- Java 21
- Maven
- AWS SDK for Java v2.31.58
- JSch for SFTP

## Setup

1. Ensure you have Java 21 and Maven installed.
2. Configure your AWS credentials (environment variables, ~/.aws/credentials, or IAM roles).
3. Build the project:
   ```bash
   mvn clean install
   ```

## Usage
- See `Main.java` for example usage.

## Files
- `SFTPConnector.java`: SFTP connection and file operations via JSch
- `S3Uploader.java`: S3 streaming upload using AWS SDK
- `Main.java`: Example usage

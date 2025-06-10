package com.example.sftp.dto.response;

import lombok.Builder;
import lombok.Data;
import software.amazon.awssdk.services.transfer.model.TransferTableStatus;

@Data
@Builder
public class DirectoryListingResponse {
    private String ListingId;
    private String outputFileName;

}

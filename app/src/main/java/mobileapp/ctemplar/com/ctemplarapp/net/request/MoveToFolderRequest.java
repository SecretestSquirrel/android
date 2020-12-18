package mobileapp.ctemplar.com.ctemplarapp.net.request;

import com.google.gson.annotations.SerializedName;

public class MoveToFolderRequest {
    @SerializedName("folder")
    private String folder;

    public MoveToFolderRequest(String folder) {
        this.folder = folder;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }
}

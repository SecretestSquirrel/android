package mobileapp.ctemplar.com.ctemplarapp.net.response;

import com.google.gson.annotations.SerializedName;

public class SignUpResponse {
    @SerializedName("token")
    String token;

    public String getToken() {
        return token;
    }
}

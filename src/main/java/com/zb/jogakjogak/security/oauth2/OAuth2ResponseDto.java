package com.zb.jogakjogak.security.oauth2;

public interface OAuth2ResponseDto {

    String getProvider();

    String getProviderId();

    String getEmail();

    String getName();

    String getPhoneNumber();

    String getNickname();
}

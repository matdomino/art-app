package pl.mdomino.artapp.api.controller;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ApiResponse {
    private String response;

    public ApiResponse() {}

    public ApiResponse(String response) {
        this.response = response;
    }
}

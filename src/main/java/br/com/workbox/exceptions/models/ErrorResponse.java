package br.com.workbox.exceptions.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse implements Serializable {

    @Setter
    private Instant timestamp;

    @Setter
    private int status;

    @Setter
    private String statusName;

    @Setter
    private String exception;

    @Setter
    private String message;

    @Setter
    private String path;

    private List<FieldError> fieldErrors;

}

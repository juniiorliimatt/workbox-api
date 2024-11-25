package br.com.workbox.exceptions.models;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class FieldError implements Serializable {

    private String field;
    private String errorCode;

}

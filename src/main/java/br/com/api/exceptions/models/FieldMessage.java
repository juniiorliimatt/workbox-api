package br.com.api.exceptions.models;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class FieldMessage implements Serializable {

    private String fieldName;
    private String message;

}

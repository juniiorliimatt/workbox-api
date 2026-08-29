package br.com.workbox.exceptions.models;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author CLAUDE-CODE
 * @author Junior Lima - oojuniin@outlook.com
 * @since 29-08-2026
 */

@Getter
@Setter
public class FieldMessage implements Serializable {

    private String fieldName;
    private String message;

}

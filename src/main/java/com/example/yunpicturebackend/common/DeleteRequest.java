package com.example.yunpicturebackend.common;

import lombok.Data;

import java.io.Serializable;

@Data
public class DeleteRequest implements Serializable {

    private static final long serialVersionUID = 7267765699596756185L;
    private Long id;

    
}

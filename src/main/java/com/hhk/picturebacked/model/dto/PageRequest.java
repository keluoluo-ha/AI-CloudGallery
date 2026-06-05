package com.hhk.picturebacked.model.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class PageRequest implements Serializable {

    public int pageNum;

    public int pageSize;
}

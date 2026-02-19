package com.wyy.yunpicturebackend.model.dto.space;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SpaceLevel {
    private final String text;

    private final int value;

    private final long maxCount;

    private final long maxSize;
}

package com.vowser.backend.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DisabilityType {
    VISUAL("시각 장애"),
    HAND_MOTOR("손/운동 장애"),
    NONE("해당 없음");

    private final String description;

    @Override
    public String toString() {
        return description;
    }
}

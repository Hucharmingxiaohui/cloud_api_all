package com.dji.sample.control.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum TestEnum {
    AAA("A",1,"aaa"),
    BBB("B",2,"bbb"),
    ;
    String method;
    int number;
    private String description;
    TestEnum(String a, int i, String aaa) {
        this.method = a;
        this.number = i;
        this.description = aaa;
    }

    @JsonCreator
    public static TestEnum find(String method) {
        return Arrays.stream(values())
                .filter(methodEnum -> methodEnum.method.equals(method)
                )
                .findAny()
                .orElseThrow();
    }
}

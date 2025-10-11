package com.df.server.dto.uniPoint;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CheckParams {

    private List<String> subCodeList;
    /*private List<String> pointTypeValues = new ArrayList<>();*/
    private List<String> deviceTypeValues = new ArrayList<>();
    private List<String> meterTypeValues = new ArrayList<>();
    private List<String> appearanceTypeValues = new ArrayList<>();
    private List<String> saveTypeListValues = new ArrayList<>();
    private List<String> recognitionTypeListValues = new ArrayList<>();
    private List<String> phaseValues = new ArrayList<>();
    private List<String> taskTypeValues = new ArrayList<>();
    private List<String> taskSubTypeValues2 = new ArrayList<>();
    private List<String> taskSubTypeValues3 = new ArrayList<>();
    private List<String> taskSubTypeValues5 = new ArrayList<>();
    private List<String> levelValues = new ArrayList<>();
    private List<String> pointAnalyseCategoryValues = new ArrayList<>();
    private List<String> pointAnalyseTypeValues = new ArrayList<>();
    private List<String> pointAnalyseTypePbValues = new ArrayList<>();
    private List<String> qxsbTypeValues = new ArrayList<>();
    private List<String> deviceMainTypeValues = new ArrayList<>();

}

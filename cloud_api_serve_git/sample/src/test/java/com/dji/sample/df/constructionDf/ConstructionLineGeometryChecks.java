package com.dji.sample.df.constructionDf;

import java.util.Arrays;

public final class ConstructionLineGeometryChecks {
    public static void main(String[] args) {
        ConstructionAreaGeometry.Point a = new ConstructionAreaGeometry.Point(120, 30);
        ConstructionAreaGeometry.Point b = new ConstructionAreaGeometry.Point(120.001, 30);
        ConstructionAreaGeometry.Point c = new ConstructionAreaGeometry.Point(120.001, 30.001);
        if (!ConstructionLineGeometry.validate(Arrays.asList(a, b, c)).isEmpty()) throw new AssertionError("valid line rejected");
        if (ConstructionLineGeometry.lengthMeters(Arrays.asList(a, b)) <= 0) throw new AssertionError("length missing");
        if (ConstructionLineGeometry.validate(Arrays.asList(a, a)).isEmpty()) throw new AssertionError("duplicate neighbours accepted");
        if (ConstructionLineGeometry.validate(Arrays.asList(new ConstructionAreaGeometry.Point(181, 30), b)).isEmpty()) throw new AssertionError("invalid longitude accepted");
        System.out.println("Construction line geometry checks passed.");
    }
}

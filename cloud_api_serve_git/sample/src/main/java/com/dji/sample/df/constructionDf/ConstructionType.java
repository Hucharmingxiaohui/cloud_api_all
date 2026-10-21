package com.dji.sample.df.constructionDf;

/** Business plan type. 5 is reserved for EUA; this is unrelated to task_type. */
public final class ConstructionType {
    public static final int PLAN_TYPE = 6;
    private ConstructionType() { }
    public static boolean matches(Integer value) { return Integer.valueOf(PLAN_TYPE).equals(value); }
}

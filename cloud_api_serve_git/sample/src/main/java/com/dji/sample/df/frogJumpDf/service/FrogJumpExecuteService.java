package com.dji.sample.df.frogJumpDf.service;

import com.dji.sample.df.frogJumpDf.model.dto.FrogJumpExecuteParam;
import com.dji.sdk.common.HttpResultResponse;

public interface FrogJumpExecuteService {

    HttpResultResponse execute(FrogJumpExecuteParam param);
}

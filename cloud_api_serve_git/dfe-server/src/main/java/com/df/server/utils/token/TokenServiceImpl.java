package com.df.server.utils.token;


import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.df.framework.constant.ParamConstants;
import com.df.framework.constant.PlatformApiConstants;
import com.df.framework.exception.FastException;
import com.df.framework.utils.HttpUtils;
import com.df.framework.vo.Result;
import com.df.server.service.sys.SysConfigService;
import com.df.server.vo.SysUser.SysUserAppVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service("tokenService")
public class TokenServiceImpl implements TokenService {

    @Autowired
    private HttpUtils httpUtils;
    @Autowired
    private SysConfigService sysConfigService;


    @Override
    public SysUserAppVO checkToken(String token) {
        if (token.startsWith("Bearer ")) {
            token = token.replace("Bearer ", "").trim();
        }
        Map<String, Object> params = new HashMap<>();
        params.put("token", token);
        String value = sysConfigService.getValueByKey(ParamConstants.PLATFORM_API);
        String url = value + PlatformApiConstants.CHECK_TOKEN;
        String msg = httpUtils.sendPostJson(url, JSONObject.toJSONString(params));

        Result<SysUserAppVO> resUlt = JSONObject.parseObject(msg, new TypeReference<Result<SysUserAppVO>>() {
        });
        if (resUlt == null || 0 != resUlt.getCode()) {
            throw new FastException("【token验证失败】" + msg);
        }
        return resUlt.getData();
    }

}

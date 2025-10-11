package com.df.framework.utils;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.df.framework.dto.PageDTO;
import com.df.framework.vo.PageVO;

import java.util.List;

/**
 * 分页工具类
 * <p>
 * Created by lyc on 2020/2/5
 */
public class PageUtils {

    /**
     * 按MyBatis的固定规则，转换分页参数
     *
     * @param pageDTO
     * @param <T>
     * @return
     */
    /*public static <T> Page<T> pageParamsFixed(PageDTO pageDTO) {
        Page<T> page = new Page();
        page.setCurrent(pageDTO.getPage());
        page.setSize(pageDTO.getSize());
        return page;
    }*/

    /**
     * 按自定义可扩展的规则，转换分页参数
     *
     * @param pageDTO 参数 page-查询页数 和 size-每次查询多少个
     * @return params 返回 start-起始位置 和 end 结束位置
     */
    public static PageDTO pageParamsExtend(PageDTO pageDTO) {
        ParamsUtils.isNull(pageDTO, "page", "size");
        int page = pageDTO.getPage();
        int size = pageDTO.getSize();
        int start = (page - 1) * size;
        pageDTO.setStart(start);
        return pageDTO;
    }

    /**
     * 按MyBatis的规则，转换分页查询结果
     *
     * @param iPage
     * @param list
     * @param <T>
     * @return
     */
    public static <T> PageVO<T> returnPageVOFixed(IPage iPage, List<T> list) {
        PageVO<T> pageVO = new PageVO<>();
        pageVO.setList(list);
        pageVO.setTotal((int) iPage.getTotal());
        pageVO.setPages((int) iPage.getPages());
        pageVO.setPage((int) iPage.getCurrent());
        pageVO.setSize((int) iPage.getSize());
        return pageVO;
    }

    /**
     * 按自定义可扩展的规则，转换分页查询结果
     *
     * @param pageDTO
     * @param list
     * @param total
     * @param <T>
     * @return
     */
    public static <T> PageVO<T> returnPageVOExtend(PageDTO pageDTO, List<T> list, Integer total) {
        int page = pageDTO.getPage();
        int size = pageDTO.getSize();
        int pages = (int) Math.ceil((double) total / size);

        PageVO<T> dto = new PageVO<>();
        dto.setList(list);
        dto.setTotal(total);
        dto.setPage(page);
        dto.setSize(size);
        dto.setPages(pages);
        return dto;
    }
}

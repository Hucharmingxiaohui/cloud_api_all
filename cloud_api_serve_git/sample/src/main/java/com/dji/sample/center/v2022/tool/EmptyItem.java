package com.dji.sample.center.v2022.tool;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * 空bean，用于<Items></Items>为空时
 * creator: 姜学云 2021-7-6 15:49
 */
@XmlRootElement(name = "Item")
public class EmptyItem extends BaseItem {
}

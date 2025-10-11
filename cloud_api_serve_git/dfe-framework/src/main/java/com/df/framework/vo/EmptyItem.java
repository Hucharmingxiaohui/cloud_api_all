package com.df.framework.vo;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * 空bean，用于<Items></Items>为空时
 */
@XmlRootElement(name = "Item")
public class EmptyItem extends BaseItem {
}

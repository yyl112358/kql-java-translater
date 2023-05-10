package cn.wkiki.kql.tree.TreeNodeImpl.FieldSearchTreeNode;

import cn.wkiki.kql.tree.TreeNode;
import lombok.Getter;
import lombok.Setter;

/**
 * 精确指定属性查询类型的树节点基类<br/>
 * 属性查询可以存在在内部查询中，
 * 通过给 nestedProp {@link FieldSearchTreeNode#nestedProp} 与
 * nestedPropName {@link FieldSearchTreeNode#nestedPropName} 赋值通知翻译过程使用加工后的内部属性名
 * 而非使用从 source中解析到的fieldNameTreeNode
 */
public abstract class FieldSearchTreeNode extends TreeNode {

    FieldNameTreeNode fieldNameTreeNode;

    /**
     * 标记当前属性查询树是否在一个内部属性查询语句中
     */
    @Getter
    @Setter
    private boolean nestedProp;
    /**
     * 若当前属性在一个内部属性查询语句中，则此字段为包含外部属性名的内部属性名
     * 翻译过程可直接使用此属性进行目标结构的翻译过程，加工此属性名的过程在构建语法树的过程
     */
    @Getter
    @Setter
    private String nestedPropName;

    public FieldNameTreeNode getFieldNameTreeNode() {
        return fieldNameTreeNode;
    }

    public void setFieldNameTreeNode(FieldNameTreeNode fieldNameTreeNode) {
        this.fieldNameTreeNode = fieldNameTreeNode;
    }
}

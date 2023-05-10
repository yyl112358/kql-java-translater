package cn.wkiki.kql.tree.TreeNodeImpl.FieldSearchTreeNode;

import cn.wkiki.kql.Token;
import cn.wkiki.kql.tree.TreeNode;
import lombok.Getter;
import lombok.Setter;

/**
 * 字段名节点
 */
@Getter
@Setter
public class FieldNameTreeNode extends TreeNode {

    public FieldNameTreeNode() {
    }

    public FieldNameTreeNode(Token fieldNameToken) {
        this.fieldNameToken = fieldNameToken;
    }

    /**
     * 字段名
     */
    Token fieldNameToken;

    @Override
    public String toSourceStr() {
        return fieldNameToken.getValue();
    }
}

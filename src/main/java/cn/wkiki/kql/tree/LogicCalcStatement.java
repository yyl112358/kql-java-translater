package cn.wkiki.kql.tree;


import cn.wkiki.kql.Token;

/**
 * 逻辑运算表达式
 */
public interface LogicCalcStatement {

    void setLeftSubNode(TreeNode.LogicCalcTreeNode.SubTreeNode leftSubNode);

    void setLogicCalcToken(Token logicCalcToken);

    void setRightSubNode(TreeNode.LogicCalcTreeNode.SubTreeNode rightSubNode);
}

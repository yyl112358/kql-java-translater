package cn.wkiki.kql.tree;


import cn.wkiki.kql.Token;
import cn.wkiki.kql.tree.TreeNodeImpl.LogicCalcTreeNode;

/**
 * 逻辑运算表达式
 */
public interface LogicCalcStatement {

    void setLeftSubNode(LogicCalcTreeNode.SubTreeNode leftSubNode);

    void setLogicCalcToken(Token logicCalcToken);

    void setRightSubNode(LogicCalcTreeNode.SubTreeNode rightSubNode);
}

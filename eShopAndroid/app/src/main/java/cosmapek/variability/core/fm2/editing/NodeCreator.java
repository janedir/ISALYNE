/* FeatureIDE - A Framework for Feature-Oriented Software Development
 * Copyright (C) 2005-2013  FeatureIDE team, University of Magdeburg, Germany
 *
 * This file is part of FeatureIDE.
 * 
 * FeatureIDE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * FeatureIDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with FeatureIDE.  If not, see <http://www.gnu.org/licenses/>.
 *
 * See http://www.fosd.de/featureide/ for further information.
 */
package cosmapek.variability.core.fm2.editing;

import cosmapek.variability.core.fm2.IFeature;
import cosmapek.variability.core.fm2.IFeatureModel;
import cosmapek.variability.core.prop4j.*;

import java.util.*;
import java.util.Map.Entry;

/**
 * Takes a feature model as input and returns a propositional formula
 * representing the valid feature combinations.
 *
 * @author Thomas Thuem
 */
public class NodeCreator {

    private static final HashMap<Object, Node> EMPTY_MAP = new HashMap<Object, Node>();
    //collections of Nodes prop4j

    public static Node createNodes(IFeatureModel featureModel) {
        return createNodes(featureModel, true);
    }

    public static Node createNodes(IFeatureModel featureModel, boolean ignoreAbstractFeatures) {
        return createNodes(featureModel,
            ignoreAbstractFeatures ? EMPTY_MAP : calculateReplacingMap(featureModel));
    }

    public static Node createNodes(IFeatureModel featureModel, Set<String> removeFeatures) {
        return createNodes(featureModel, calculateReplacingMap(featureModel, removeFeatures),
            removeFeatures);
    }

    //important
    public static Node createNodes(IFeatureModel featureModel, Map<Object, Node> replacingMap) {
        IFeature root = featureModel.getRoot();
        LinkedList<Node> nodes = new LinkedList<Node>();
        if (root != null) {
            //  nodes.add(new Literal(getVariable(root.getName(), featureModel)));// has error
            nodes.add(new Literal(root.getName()));
            // convert grammar rules into propositional formulas
            createNodes(nodes, root, featureModel, true, replacingMap);
            // add extra constraints
            for (Node node : new ArrayList<Node>(featureModel.getPropositionalNodes()))
                nodes.add(node.clone());//error
        }
        And and = new And(nodes);
        and = (And) replaceAbstractVariables(and, replacingMap, false);
        and = eliminateAbstractVariables(and, replacingMap, featureModel);
        return replaceNames(and, featureModel);
    }

    public static Node createNodes(IFeatureModel featureModel, Map<Object, Node> replacingMap,
        Set<String> removeFeatures) {
        IFeature root = featureModel.getRoot();
        LinkedList<Node> nodes = new LinkedList<Node>();
        if (root != null) {
            nodes.add(new Literal(root.getName()));
            // nodes.add(new Literal(getVariable(root.getName(), featureModel)));
            // convert grammar rules into propositional formulas
            createNodes(nodes, root, featureModel, true, replacingMap);
            // add extra constraints
            for (Node node : new ArrayList<Node>(featureModel.getPropositionalNodes()))
                nodes.add(node.clone());
        }
        And and = new And(nodes);
        and = (And) replaceAbstractVariables(and, replacingMap, false);
        and = eliminateAbstractVariables(and, replacingMap, featureModel, removeFeatures);
        return replaceNames(and, featureModel);
    }

    /**
     * @param //and
     * @param //featureModel
     * @return
     */
    private static Node replaceNames(Node node, IFeatureModel featureModel) {
        if (node == null)
            return null;
        if (node instanceof Literal) {
            Literal literal = (Literal) node;
            //literal.var = featureModel.getRenamingsManager().getOldName(literal.var.toString());
            if (literal.var instanceof String) {
                literal.var = featureModel.getRenamingsManager().getOldName((String) literal.var);
            }
        } else {
            Node[] children = node.getChildren();
            for (int i = 0; i < children.length; i++) {
                children[i] = replaceNames(children[i], featureModel);
                if (children[i] == null)
                    return null;
            }
        }

        return node;
    }

    public static Node replaceAbstractVariables(Node node, Map<Object, Node> map,
        boolean replaceNull) {
        if (node == null)
            return null;
        if (node instanceof Literal) {
            Literal literal = (Literal) node;
            if (map.containsKey(literal.var)) {
                Node replacing = map.get(literal.var);
                if (replacing == null)
                    return replaceNull ? null : node;
                replacing = replacing.clone();
                node = literal.positive ? replacing : new Not(replacing);
            }
        } else {
            Node[] children = node.getChildren();
            for (int i = 0; i < children.length; i++) {
                children[i] = replaceAbstractVariables(children[i], map, replaceNull);
                if (replaceNull && children[i] == null)
                    return null;
            }
        }
        return node;
    }

    // Using objects for true and false instead of strings ensures, that the
    // user cannot choose the same name by accident. Overriding the toString
    // method is just for convenient printing of formulas.
    public final static Object varTrue = new Object() {
        public String toString() {
            return "True";
        }

        ;
    };
    public final static Object varFalse = new Object() {
        public String toString() {
            return "False";
        }

        ;
    };

    public static And eliminateAbstractVariables(And and, Map<Object, Node> map,
        IFeatureModel featureModel) {
        for (Entry<Object, Node> entry : map.entrySet())
            if (entry.getValue() == null) {
                String name = entry.getKey().toString();
                List<Node> nochange = new LinkedList<Node>();
                List<Node> change = new LinkedList<Node>();
                calculateNodesToReplace(and.getChildren(), name, nochange, change);
                if (!change.isEmpty()) {
                    Node toChange = new And(change);
                    Node trueNode = replaceFeature(toChange.clone(), name, varTrue);
                    Node falseNode = replaceFeature(toChange.clone(), name, varFalse);
                    Node newPart = simplify(new Or(trueNode, falseNode));
                    newPart = simplify(newPart.toCNF());
                    if (!(newPart instanceof And))
                        newPart = new And(newPart);
                    Node[] children = new Node[nochange.size() + newPart.getChildren().length];
                    int i = 0;
                    for (Node child : nochange)
                        children[i++] = child;
                    for (Node child : newPart.getChildren())
                        children[i++] = child;
                    and = new And(children);
                }
            }
        Node[] concreteFeatures = new Node[featureModel.getAnalyser().countConcreteFeatures() + 1];
        int i = 0;
        for (IFeature feature : featureModel.getFeatures())
            if (feature.isConcrete())
                //concreteFeatures[i++] = new Literal(getVariable(feature.getName(), featureModel));
                concreteFeatures[i++] = new Literal(feature.getName());
        concreteFeatures[i] = new Literal(varTrue);
        return new And(and, varTrue, new Not(varFalse), new Or(concreteFeatures));
    }

    public static And eliminateAbstractVariables(And and, Map<Object, Node> map,
        IFeatureModel featureModel, Set<String> removeFeatures) {
        for (Entry<Object, Node> entry : map.entrySet())
            if (entry.getValue() == null) {
                String name = entry.getKey().toString();
                List<Node> nochange = new LinkedList<Node>();
                List<Node> change = new LinkedList<Node>();
                calculateNodesToReplace(and.getChildren(), name, nochange, change);
                if (!change.isEmpty()) {
                    Node toChange = new And(change);
                    Node trueNode = replaceFeature(toChange.clone(), name, varTrue);
                    Node falseNode = replaceFeature(toChange.clone(), name, varFalse);
                    Node newPart = simplify(new Or(trueNode, falseNode));
                    newPart = simplify(newPart.toCNF());
                    if (!(newPart instanceof And))
                        newPart = new And(newPart);
                    Node[] children = new Node[nochange.size() + newPart.getChildren().length];
                    int i = 0;
                    for (Node child : nochange)
                        children[i++] = child;
                    for (Node child : newPart.getChildren())
                        children[i++] = child;
                    and = new And(children);
                }
            }

        List<Node> featureList =
            new ArrayList<Node>(featureModel.getFeatures().size() - removeFeatures.size());
        for (IFeature feature : featureModel.getFeatures()) {
            if (!removeFeatures.contains(feature.getName())) {
                // featureList.add(new Literal(getVariable(feature.getName(), featureModel)));
                featureList.add(new Literal(feature.getName()));
            }
        }
        Node[] concreteFeatures = new Node[featureList.size() + 1];
        featureList.toArray(concreteFeatures);

        concreteFeatures[concreteFeatures.length - 1] = new Literal(varTrue);
        return new And(and, varTrue, new Not(varFalse), new Or(concreteFeatures));
    }

    private static void calculateNodesToReplace(Node[] children, String abstractFeature,
        List<Node> nochange, List<Node> change) {
        for (Node node : children)
            if (nodeContains(node, abstractFeature))
                change.add(node);
            else
                nochange.add(node);
    }

    private static boolean nodeContains(Node node, String abstractFeature) {
        if (node instanceof Literal) {
            Literal lit = (Literal) node;
            return lit.var.equals(abstractFeature);
        }
        for (Node child : node.getChildren())
            if (nodeContains(child, abstractFeature))
                return true;
        return false;
    }

    private static Node simplify(Node node) {
        if (node instanceof Literal) {
            Literal lit = (Literal) node;
            if (lit.var.equals(varFalse) && !lit.positive)
                return new Literal(varTrue);
            if (lit.var.equals(varTrue) && !lit.positive)
                return new Literal(varFalse);
            return lit;
        }
        Node[] children = node.getChildren();
        int removeChildren = 0;
        for (int i = 0; i < children.length; i++) {
            Node child = simplify(children[i]);
            if (child instanceof Literal) {
                Literal lit = (Literal) child;
                // we assume that litTrue and litFalse can only occur positive
                if (lit.var.equals(varTrue)) {
                    if (node instanceof Not)
                        return new Literal(varFalse);
                    if (node instanceof And) {
                        removeChildren++;
                        child = null;
                    }
                    if (node instanceof Or)
                        return lit;
                    if (node instanceof Implies) {
                        if (i == 0)
                            return children[1];
                        else
                            return lit;
                    }
                    if (node instanceof Equals) {
                        if (i == 0)
                            return children[1];
                        else
                            return children[0];
                    }
                    if (node instanceof AtMost) {
                        AtMost atmost = (AtMost) node;
                        if (atmost.max < 1)
                            return new Literal(varFalse);
                        Node[] newChildren = new Node[children.length - 1];
                        for (int j = 0; j < i; j++)
                            newChildren[j] = children[j];
                        for (int j = i + 1; j < children.length; j++)
                            newChildren[j - 1] = children[j];
                        if (atmost.max > 1)
                            return simplify(new AtMost(atmost.max - 1, newChildren));
                        for (int j = 0; j < newChildren.length; j++) {
                            Node newChild = newChildren[j];
                            if (newChild instanceof Literal)
                                ((Literal) newChild).positive = !((Literal) newChild).positive;
                            else
                                newChildren[j] = new Not(newChild);
                        }
                        return simplify(new And(newChildren));
                    }
                } else if (lit.var.equals(varFalse)) {
                    if (node instanceof Not)
                        return new Literal(varTrue);
                    if (node instanceof And)
                        return lit;
                    if (node instanceof Or) {
                        removeChildren++;
                        child = null;
                    }
                    if (node instanceof Implies) {
                        if (i == 0)
                            return new Literal(varTrue);
                        else
                            return simplify(new Not(children[0]));
                    }
                    if (node instanceof Equals) {
                        if (i == 0)
                            return simplify(new Not(children[1]));
                        else
                            return simplify(new Not(children[0]));
                    }
                }
            }
            children[i] = child;
        }
        /*
        if (removeChildren == 0)
            return node;
        if (children.length - removeChildren == 0) {
            if (node instanceof And)
                return new Literal(varTrue);
            if (node instanceof Or)
                return new Literal(varFalse);
        }
        Node[] newChildren = new Node[children.length - removeChildren];
        int i = 0;
        for (Node child : children)
            if (child != null)
                newChildren[i++] = child;
        node.setChildren(newChildren);
        return node;*/
        final int newSize = children.length - removeChildren;
        switch (newSize) {
            case 0:
                if (node instanceof And) {
                    return new Literal(varTrue);
                }
                if (node instanceof Or) {
                    return new Literal(varFalse);
                }
                break;
            case 1:
                if (node instanceof And || node instanceof Or) {
                    for (Node child : children) {
                        if (child != null) {
                            return child;
                        }
                    }
                }
                break;
            default:
                break;
        }
        if (removeChildren == 0) {
            return node;
        }

        final Node[] newChildren = new Node[newSize];
        int i = 0;
        for (Node child : children) {
            if (child != null) {
                newChildren[i++] = child;
            }
        }
        node.setChildren(newChildren);

        return node;
    }

    private static Node replaceFeature(Node node, Object abstractFeature, Object replacement) {
        if (node instanceof Literal) {
            Literal lit = (Literal) node;
            if (lit.var.equals(abstractFeature))
                return new Literal(replacement, lit.positive);
            else
                return node;
        }
        Node[] children = node.getChildren();
        for (int i = 0; i < children.length; i++)
            children[i] = replaceFeature(children[i], abstractFeature, replacement);
        return node;
    }

    @SuppressWarnings("unchecked")
    private static void createNodes(Collection<Node> nodes, IFeature rootFeature,
        IFeatureModel featureModel, boolean recursive, Map<Object, Node> replacings) {
        if (rootFeature == null || !rootFeature.hasChildren())
            return;
        //String s = getVariable(rootFeature.getName(), featureModel);
        String s = rootFeature.getName();

        Node[] children = new Node[rootFeature.getChildrenCount()];
        int i = 0;
        for (IFeature rootChild : rootFeature.getChildren()) {
            //String var = getVariable(rootChild.getName(), featureModel);
            String var = rootChild.getName();
            children[i++] = new Literal(var);
        }
        Node definition = children.length == 1 ? children[0] : new Or(children);

        if (rootFeature.isAnd()) {// &&
            // (!replacings.containsKey(featureModel.getOldName(rootFeature.getName()))
            // || !rootFeature.isPossibleEmpty())) {
            LinkedList<Node> manChildren = new LinkedList<Node>();
            for (IFeature feature : rootFeature.getChildren())
                if (feature.isMandatory()) {
                    //String var = getVariable(feature.getName(), featureModel);
                    String var = feature.getName();
                    manChildren.add(new Literal(var));
                }

            // add constraints for all mandatory children S => (A & B)
            if (manChildren.size() == 1)
                nodes.add(new Implies(new Literal(s), manChildren.getFirst()));
            else if (manChildren.size() > 1)
                nodes.add(new Implies(new Literal(s), new And(manChildren)));

            // add contraint (A | B | C) => S
            nodes.add(new Implies(definition, new Literal(s)));
        } else {
            // add constraint S <=> (A | B | C)
            if (replacings.get(featureModel.getRenamingsManager().getOldName(rootFeature.getName()))
                == null)
                nodes.add(new Equals(new Literal(s), definition));

            if (rootFeature.isAlternative()) {
                // add constraint atmost1(A, B, C)
                if (children.length > 1)
                    nodes.add(new AtMost(1, Node.clone(children)));
            }
        }

        if (recursive)
            for (IFeature feature : (LinkedList<IFeature>) rootFeature.getChildren().clone())
                createNodes(nodes, feature, featureModel, true, replacings);
    }

    public static HashMap<Object, Node> calculateReplacingMap(IFeatureModel featureModel) {
        HashMap<Object, Node> map = new HashMap<Object, Node>();
        for (IFeature feature : featureModel.getFeatures()) {
            if (feature.isAbstract()) {
                // String var = getVariable(feature.getName(), featureModel);
                String var = feature.getName();
                Node replacing = calculateReplacing(var, featureModel);
                replacing = NodeCreator.replaceAbstractVariables(replacing, map, true);
                updateMap(map, var, replacing);
            }
        }
        return map;
    }

    public static HashMap<Object, Node> calculateReplacingMap(IFeatureModel featureModel,
        Set<String> featureNames) {
        HashMap<Object, Node> map = new HashMap<Object, Node>();
        for (String featureName : featureNames) {
            // String var = getVariable(featureName, featureModel);
            String var = featureName;
            IFeature feature = getFeature(var, featureModel);
            Node replacing = calculateReplacing(featureModel, feature, featureNames);
            replacing = NodeCreator.replaceAbstractVariables(replacing, map, true);
            updateMap(map, var, replacing);
        }
        return map;
    }

    /**
     * Replaces all occurrences of the given variable in values of the map.
     */
  /*  private static void updateMap(HashMap<Object, Node> map, Object var, Node replacing) {
        for (Object key : map.keySet()) {
            Node value = map.get(key);
            HashMap<Object, Node> tempMap = new HashMap<Object, Node>();
            tempMap.put(var, replacing);
            value = NodeCreator.replaceAbstractVariables(value, tempMap, true);
            map.put(key, value);
        }
        map.put(var, replacing);
    }*/
    private static void updateMap(HashMap<Object, Node> map, Object var,
        Node replacing) {
        for (Entry<Object, Node> entry : map.entrySet()) {
            HashMap<Object, Node> tempMap = new HashMap<Object, Node>();
            tempMap.put(var, replacing);
            entry.setValue(NodeCreator.replaceAbstractVariables(entry.getValue(), tempMap, true));
        }
        map.put(var, replacing);
    }

    private static Node calculateReplacing(Object var, IFeatureModel featureModel) {
        IFeature feature = getFeature(var, featureModel);
        return calculateReplacing(featureModel, feature);
    }

    private static Node calculateReplacing(IFeatureModel featureModel, IFeature feature) {
        if (!feature.hasChildren()) {
            IFeature parent = feature.getParent();
            if (parent == null || parent.isAbstract())
                return null;
            if ((parent.isAnd() && feature.isMandatorySet()) || (!parent.isAnd()
                && parent.getChildrenCount() == 1))
                return new Literal(featureModel.getRenamingsManager().getOldName(parent.getName()));
            return null;
        }
        if (feature.isAnd()) {
            for (IFeature child : feature.getChildren())
                if (child.isMandatorySet() && child.isConcrete())
                    return new Literal(
                        featureModel.getRenamingsManager().getOldName(child.getName()));
            for (IFeature child : feature.getChildren())
                if (child.isMandatorySet())
                    return new Literal(
                        featureModel.getRenamingsManager().getOldName(child.getName()));
            return null;
        }
        LinkedList<Node> children = new LinkedList<Node>();
        for (IFeature child : feature.getChildren()) {
            String var2 = featureModel.getRenamingsManager().getOldName(child.getName());
            children.add(new Literal(var2));
        }
        if (children.size() == 1)
            return children.getFirst();
        return new Or(children);
    }

    private static Node calculateReplacing(IFeatureModel featureModel, IFeature feature,
        Set<String> featureNames) {
        if (!feature.hasChildren()) {
            IFeature parent = feature.getParent();
            if (parent == null || featureNames.contains(parent.getName()))// if (parent == null || featureNames.contains(parent))
                return null;
            if ((parent.isAnd() && feature.isMandatorySet()) || (!parent.isAnd()
                && parent.getChildrenCount() == 1))
                return new Literal(featureModel.getRenamingsManager().getOldName(parent.getName()));
            return null;
        }
        if (feature.isAnd()) {
            for (IFeature child : feature.getChildren())
                if (child.isMandatorySet() && !featureNames.contains(child))
                    return new Literal(
                        featureModel.getRenamingsManager().getOldName(child.getName()));
            for (IFeature child : feature.getChildren())
                if (child.isMandatorySet())
                    return new Literal(
                        featureModel.getRenamingsManager().getOldName(child.getName()));
            return null;
        }
        LinkedList<Node> children = new LinkedList<Node>();
        for (IFeature child : feature.getChildren()) {
            String var2 = featureModel.getRenamingsManager().getOldName(child.getName());
            children.add(new Literal(var2));
        }
        if (children.size() == 1)
            return children.getFirst();
        return new Or(children);
    }

    private static IFeature getFeature(Object var, IFeatureModel featureModel) {
        String currentName = featureModel.getRenamingsManager().getNewName((String) var);
        return featureModel.getFeature(currentName);
    }
/*
    public static String getVariable(String featureName, IFeatureModel featureModel) {
        return featureModel.getRenamingsManager().getOldName(featureName);
    }

    public static String getVariable(IFeature feature, IFeatureModel featureModel) {
        return featureModel.getRenamingsManager().getOldName(feature.getName());
    }
*/
}

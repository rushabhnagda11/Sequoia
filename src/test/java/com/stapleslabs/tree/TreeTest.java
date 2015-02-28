package com.stapleslabs.tree;

import com.stapleslabs.features.IFeature;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class TreeTest {

    private Tree<Feature, Map<Feature, Integer>> tree;

    @Before
    public void setUp() {
        int[] childChildOffsets = {-1, -1};

        int[] nodeZeroChildOffsets = {1, 2};
        Node<Feature, Map<Feature, Integer>> nodeZero = new Node<>(Feature.COST, -1.0, false, nodeZeroChildOffsets,
                new Numeric(23));

        int[] nodeOneChildOffsets = {5, 3};
        Map<Integer, Integer> dayOfWeekMappings = new HashMap<>();
        dayOfWeekMappings.put(0, 0);
        dayOfWeekMappings.put(1, 0);
        dayOfWeekMappings.put(2, 1);
        Node<Feature, Map<Feature, Integer>> nodeOne = new Node<>(Feature.DAY_OF_WEEK, -1.0, false,
                nodeOneChildOffsets, new Categorical(dayOfWeekMappings));

        int[] nodeTwoChildOffsets = {3, 4};
        Map<Integer, Integer> monthMappings = new HashMap<>();
        monthMappings.put(0, 1);
        monthMappings.put(1, 1);
        monthMappings.put(2, 0);
        monthMappings.put(3, 1);
        Node<Feature, Map<Feature, Integer>> nodeTwo = new Node<>(Feature.MONTH, -1.0, false, nodeTwoChildOffsets, new
                Categorical(monthMappings));

        Node<Feature, Map<Feature, Integer>> nodeThree = new Node<>(null, 5.0, true, childChildOffsets, null);
        Node<Feature, Map<Feature, Integer>> nodeFour = new Node<>(null, 10.0, true, childChildOffsets, null);
        Node<Feature, Map<Feature, Integer>> nodeFive = new Node<>(null, 15.0, true, childChildOffsets, null);

        tree = new Tree<>(Arrays.asList(nodeZero, nodeOne, nodeTwo, nodeThree, nodeFour, nodeFive));
    }

    @Test
    public void testReduceToValue() {
        Map<Feature, Integer> features = new HashMap<>();
        assertEquals(15.0, tree.reduceToValue(features), 0.0);

        features.put(Feature.COST, 24);
        assertEquals(5.0, tree.reduceToValue(features), 0.0);

        features.put(Feature.MONTH, 2);
        assertEquals(5.0, tree.reduceToValue(features), 0.0);

        features.put(Feature.MONTH, 1);
        assertEquals(10.0, tree.reduceToValue(features), 0.0);

        features.put(Feature.COST, 22);
        features.put(Feature.DAY_OF_WEEK, 1);
        assertEquals(15.0, tree.reduceToValue(features), 0.0);

        features.put(Feature.DAY_OF_WEEK, 2);
        assertEquals(5.0, tree.reduceToValue(features), 0.0);
    }

    @Test
    public void testReduceToSubTree() {
        Set<Feature> missingFeatures = new HashSet<>();
        missingFeatures.add(Feature.DAY_OF_WEEK);
        Map<Feature, Integer> features = new HashMap<>();
        features.put(Feature.COST, 22);
        features.put(Feature.DAY_OF_WEEK, 1);

        Tree<Feature, Map<Feature, Integer>> subTree = tree.reduceToTree(features, missingFeatures);

        assertEquals(3, subTree.getNodes().length);
        assertEquals(15.0, subTree.reduceToValue(features), 0.0);

        features.put(Feature.DAY_OF_WEEK, 2);
        assertEquals(5.0, subTree.reduceToValue(features), 0.0);
    }

    @Test
    public void testReduceToSubTreeProducesSingleNodeIfAppropriate() {
        Set<Feature> missingFeatures = new HashSet<>();
        missingFeatures.add(Feature.DAY_OF_WEEK);
        Map<Feature, Integer> features = new HashMap<>();
        features.put(Feature.COST, 24);
        features.put(Feature.MONTH, 2);

        Tree<Feature, Map<Feature, Integer>> singleNodeThree = tree.reduceToTree(features, missingFeatures);

        assertEquals(1, singleNodeThree.getNodes().length);
        assertEquals(5.0, singleNodeThree.reduceToValue(features), 0.0);

        features.put(Feature.MONTH, 1);

        Tree<Feature, Map<Feature, Integer>> singleNodeFour = tree.reduceToTree(features, missingFeatures);

        assertEquals(1, singleNodeFour.getNodes().length);
        assertEquals(10.0, singleNodeFour.reduceToValue(features), 0.0);
    }

    @Test
    public void testOptimizedReduceToValueWithNoFeaturesDiffering() {
        Set<Feature> differingFeatures = new HashSet<>();

        Map<Feature, Integer> features1 = new HashMap<>();
        features1.put(Feature.COST, 24);
        features1.put(Feature.MONTH, 2);
        features1.put(Feature.DAY_OF_WEEK, 1);

        Map<Feature, Integer> features2 = new HashMap<>(features1);

        Map<Feature, Integer> features3 = new HashMap<>(features2);


        List<Map<Feature, Integer>> featureList = Arrays.asList(features1, features2, features3);

        double[] expected = {5.0, 5.0, 5.0};
        assertArrayEquals(expected, tree.optimizedReduceToValue(featureList, differingFeatures), 0.0);
    }

    @Test
    public void testOptimizedReduceToValueWithFeatureDiffering() {
        Set<Feature> differingFeatures = new HashSet<>();
        differingFeatures.add(Feature.DAY_OF_WEEK);
        Map<Feature, Integer> features1 = new HashMap<>();
        features1.put(Feature.COST, 22);
        features1.put(Feature.DAY_OF_WEEK, 1);

        Map<Feature, Integer> features2 = new HashMap<>(features1);
        features2.put(Feature.DAY_OF_WEEK, 2);

        List<Map<Feature, Integer>> featureList = Arrays.asList(features1, features2);


        double[] expected = {15.0, 5.0};
        assertArrayEquals(expected, tree.optimizedReduceToValue(featureList, differingFeatures), 0.0);
    }

    private enum Feature {
        COST,
        MONTH,
        DAY_OF_WEEK
    }

    private static class Categorical implements ICondition<Feature, Map<Feature, Integer>> {

        private final Map<Integer, Integer> conditions;

        public Categorical(Map<Integer, Integer> conditions) {
            this.conditions = conditions;
        }

        @Override
        public int nextOffsetIndex(final Feature feature, final Map<Feature, Integer> features) {
            Integer featureValue = features.get(feature);
            if (featureValue == null) {
                return 0;
            }
            return conditions.get(featureValue);
        }
    }

    private static class Numeric implements ICondition<Feature, Map<Feature, Integer>> {

        private final int cutPoint;

        public Numeric(int cutPoint) {
            this.cutPoint = cutPoint;
        }

        @Override
        public int nextOffsetIndex(final Feature feature, final Map<Feature, Integer> features) {
            Integer value = features.get(feature);
            if (value == null) {
                return 0;
            } else if (value > cutPoint) {
                return 1;
            } else {
                return 0;
            }
        }
    }

}
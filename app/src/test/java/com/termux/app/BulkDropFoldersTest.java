package com.termux.app;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/** BulkDropFolders ke pure hisse: naam parse, unique naam, fetch plan. */
public class BulkDropFoldersTest {

    private static Set<Integer> known(int... ns) {
        Set<Integer> s = new HashSet<>();
        for (int n : ns) s.add(n);
        return s;
    }

    // ---------------------------------------------------------- parse

    @Test
    public void parseExactName() {
        assertEquals(1, BulkDropFolders.parseDropFolderName("Project 1", known(1, 10)));
        assertEquals(10, BulkDropFolders.parseDropFolderName("Project 10", known(1, 10)));
    }

    @Test
    public void parseCaseInsensitiveAndSpaces() {
        assertEquals(10, BulkDropFolders.parseDropFolderName("project 10", known(10)));
        assertEquals(3, BulkDropFolders.parseDropFolderName("PROJECT 3", known(3)));
        assertEquals(7, BulkDropFolders.parseDropFolderName("Project  7", known(7)));
        assertEquals(7, BulkDropFolders.parseDropFolderName("  Project 7  ", known(7)));
    }

    @Test
    public void parseUnknownNumberRejected() {
        assertEquals(-1, BulkDropFolders.parseDropFolderName("Project 99", known(1, 2)));
        assertEquals(-1, BulkDropFolders.parseDropFolderName("Project 0", known(0, 1)));
    }

    @Test
    public void parseJunkRejected() {
        Set<Integer> k = known(1, 2);
        assertEquals(-1, BulkDropFolders.parseDropFolderName("Project", k));
        assertEquals(-1, BulkDropFolders.parseDropFolderName("Project X", k));
        assertEquals(-1, BulkDropFolders.parseDropFolderName("Project 1 backup", k));
        assertEquals(-1, BulkDropFolders.parseDropFolderName("Projects 1", k));
        assertEquals(-1, BulkDropFolders.parseDropFolderName("My Project 1", k));
        assertEquals(-1, BulkDropFolders.parseDropFolderName("", k));
        assertEquals(-1, BulkDropFolders.parseDropFolderName(null, k));
        assertEquals(-1, BulkDropFolders.parseDropFolderName("Project 1", null));
    }

    @Test
    public void dropFolderNameShape() {
        assertEquals("Project 5", BulkDropFolders.dropFolderName(5));
        assertEquals("Project 14", BulkDropFolders.dropFolderName(14));
    }

    // ---------------------------------------------------------- unique name

    @Test
    public void uniqueNameFreeStays() {
        assertEquals("a.jpg", BulkDropFolders.uniqueDestName("a.jpg", knownNames("b.jpg")));
    }

    @Test
    public void uniqueNameCollisionSuffix() {
        assertEquals("a_1.jpg", BulkDropFolders.uniqueDestName("a.jpg", knownNames("a.jpg")));
        assertEquals("a_2.jpg", BulkDropFolders.uniqueDestName("a.jpg",
            knownNames("a.jpg", "a_1.jpg")));
    }

    @Test
    public void uniqueNameNoExtension() {
        assertEquals("img_1", BulkDropFolders.uniqueDestName("img", knownNames("img")));
    }

    private static Set<String> knownNames(String... names) {
        return new HashSet<>(Arrays.asList(names));
    }

    // ---------------------------------------------------------- fetch plan

    @Test
    public void planMovesInNameOrder() {
        List<BulkDropFolders.FetchAction> plan = BulkDropFolders.planFetch(
            Arrays.asList("a.jpg", "b.jpg"), new HashMap<>(), knownNames(), new HashSet<>());
        assertEquals(2, plan.size());
        assertEquals(BulkDropFolders.FetchAction.MOVE, plan.get(0).kind);
        assertEquals("a.jpg", plan.get(0).destName);
        assertEquals("b.jpg", plan.get(1).destName);
    }

    @Test
    public void planDivertsDuplicateHash() {
        Map<String, String> hashes = new HashMap<>();
        hashes.put("a.jpg", "H1");
        hashes.put("b.jpg", "H2");
        List<BulkDropFolders.FetchAction> plan = BulkDropFolders.planFetch(
            Arrays.asList("a.jpg", "b.jpg"), hashes, knownNames("x.jpg"),
            new HashSet<>(Arrays.asList("H1")));
        assertEquals(BulkDropFolders.FetchAction.DUPLICATE, plan.get(0).kind);
        assertNull(plan.get(0).destName);
        assertEquals(BulkDropFolders.FetchAction.MOVE, plan.get(1).kind);
    }

    @Test
    public void planRenamesOnDestCollision() {
        List<BulkDropFolders.FetchAction> plan = BulkDropFolders.planFetch(
            Arrays.asList("a.jpg"), new HashMap<>(), knownNames("a.jpg"), new HashSet<>());
        assertEquals("a_1.jpg", plan.get(0).destName);
    }

    @Test
    public void planRenamesSecondSameNameInSameBatch() {
        // Do sources ek hi naam ke (mukhtalif hash) — dusra rename hoga
        Map<String, String> hashes = new HashMap<>();
        hashes.put("a.jpg", "H9");
        List<BulkDropFolders.FetchAction> plan = BulkDropFolders.planFetch(
            Arrays.asList("a.jpg", "a_1.jpg"), hashes, knownNames("a.jpg"),
            new HashSet<>());
        assertEquals("a_1.jpg", plan.get(0).destName);
        assertEquals("a_1_1.jpg", plan.get(1).destName);
    }

    @Test
    public void planEmptyHashNeverDuplicate() {
        Map<String, String> hashes = new HashMap<>();
        hashes.put("a.jpg", "");
        List<BulkDropFolders.FetchAction> plan = BulkDropFolders.planFetch(
            Arrays.asList("a.jpg"), hashes, knownNames(), new HashSet<>(Arrays.asList("")));
        assertEquals(BulkDropFolders.FetchAction.MOVE, plan.get(0).kind);
    }

    @Test
    public void planDoesNotMutateInputs() {
        Set<String> destNames = knownNames("a.jpg");
        Set<String> destHashes = new HashSet<>(Arrays.asList("H1"));
        BulkDropFolders.planFetch(Arrays.asList("b.jpg"), new HashMap<>(),
            destNames, destHashes);
        assertEquals(1, destNames.size());
        assertEquals(1, destHashes.size());
    }

    @Test
    public void planEmptySourcesEmptyPlan() {
        assertEquals(0, BulkDropFolders.planFetch(
            Arrays.asList(), new HashMap<>(), knownNames(), new HashSet<>()).size());
        assertEquals(0, BulkDropFolders.planFetch(
            null, null, null, null).size());
    }
}

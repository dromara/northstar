package org.dromara.northstar.rl.experience;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExperienceStoreTest {

    @TempDir
    Path roundsDir;

    @Test
    void shardsAreNamedPerRoundAndContract() throws Exception {
        ExperienceStore store = new ExperienceStore(roundsDir, 3);
        store.append(0, 0, new Transition(new float[] {1, 2, 3}, 1, 0.5f, new float[] {4, 5, 6}, 7, false));
        store.append(0, 1, new Transition(new float[] {7, 8, 9}, 2, -0.5f, new float[] {10, 11, 12}, 3, true));
        store.flush();

        assertTrue(Files.exists(roundsDir.resolve("replay-r0-c0.csv")));
        assertTrue(Files.exists(roundsDir.resolve("replay-r0-c1.csv")));
        assertFalse(Files.exists(roundsDir.resolve("replay-r0-c2.csv")));

        List<String> lines = Files.readAllLines(roundsDir.resolve("replay-r0-c0.csv"));
        assertEquals("s0,s1,s2,action,reward,ns0,ns1,ns2,dt,terminated", lines.get(0));
        assertEquals("1.0,2.0,3.0,1,0.5,4.0,5.0,6.0,7,0", lines.get(1));

        List<String> lines2 = Files.readAllLines(roundsDir.resolve("replay-r0-c1.csv"));
        assertEquals(2, lines2.size());
        assertEquals("7.0,8.0,9.0,2,-0.5,10.0,11.0,12.0,3,1", lines2.get(1));
    }

    @Test
    void appendAppendsAcrossRoundsWithoutDuplicateHeader() throws Exception {
        ExperienceStore store = new ExperienceStore(roundsDir, 2);
        Transition t = new Transition(new float[] {0, 0}, 0, 0f, new float[] {0, 0}, 1, false);
        store.append(0, 0, t);
        store.append(1, 0, t);
        store.append(1, 0, t);
        store.flush();

        List<String> lines = Files.readAllLines(roundsDir.resolve("replay-r1-c0.csv"));
        assertEquals(3, lines.size());
        assertEquals(1, lines.stream().filter(l -> l.startsWith("s0,")).count(), "同一分片只有一个表头");
    }

    @Test
    void rejectsDimensionMismatch() {
        ExperienceStore store = new ExperienceStore(roundsDir, 2);
        assertThrows(IllegalArgumentException.class,
                () -> store.append(0, 0, new Transition(new float[] {1}, 0, 0f, new float[] {1, 2}, 1, false)));
    }
}

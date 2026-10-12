package drs.common;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import drs.common.model.TaskStatus;

/** Enhanced Feature 2 - field task state machine. */
class TaskStatusTest {

    @ParameterizedTest(name = "{0} -> {1} allowed={2}")
    @CsvSource({
            "DISPATCHED,ACKNOWLEDGED,true", "DISPATCHED,DECLINED,true", "DISPATCHED,ON_SCENE,false",
            "ACKNOWLEDGED,EN_ROUTE,true", "ACKNOWLEDGED,COMPLETED,false",
            "EN_ROUTE,ON_SCENE,true", "EN_ROUTE,DECLINED,false",
            "ON_SCENE,COMPLETED,true", "ON_SCENE,DISPATCHED,false",
            "COMPLETED,ON_SCENE,false", "DECLINED,ACKNOWLEDGED,false"})
    @DisplayName("Only forward lifecycle transitions are legal")
    void transitions(TaskStatus from, TaskStatus to, boolean allowed) {
        assertEquals(allowed, from.canTransitionTo(to));
    }

    @Test
    void terminalStates() {
        assertTrue(TaskStatus.COMPLETED.isTerminal());
        assertTrue(TaskStatus.DECLINED.isTerminal());
        assertFalse(TaskStatus.ON_SCENE.isTerminal());
        assertTrue(TaskStatus.COMPLETED.nextStatuses().isEmpty());
    }

    @Test
    void nullIsNeverAValidTarget() {
        assertFalse(TaskStatus.DISPATCHED.canTransitionTo(null));
    }

    @Test
    void nextStatusesIsUnmodifiable() {
        assertThrows(UnsupportedOperationException.class,
                () -> TaskStatus.DISPATCHED.nextStatuses().add(TaskStatus.COMPLETED));
    }
}

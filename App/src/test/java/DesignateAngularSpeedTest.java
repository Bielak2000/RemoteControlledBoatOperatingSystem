import com.example.systemobslugilodzizdalniesterowanej.boatmodel.autonomiccontrol.AutonomicController;
import com.example.systemobslugilodzizdalniesterowanej.maps.OSMMap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DesignateAngularSpeedTest {

    @Mock
    private static OSMMap osmMap;

    private static AutonomicController autonomicController;

    @BeforeAll
    public static void beforeAll() {
        autonomicController = new AutonomicController(osmMap);
    }

    @Test
    public void returnAngularSpeedEqualsZero() {
        // given
        double expectedCourse = 30;
        double currentCourse = 30;
        // when
        double angularSpeed = autonomicController.getAngularSpeed(expectedCourse, currentCourse);
        // then
        assertEquals(angularSpeed, 0.0);
    }

    @Test
    public void returnAngularSpeedLessThanZeroAndCurrentIsGreaterThanExpected() {
        // given
        double expectedCourse = 30;
        double currentCourse1 = 40;
        double currentCourse2 = 60;
        double currentCourse3 = 90;
        double currentCourse4 = 210;
        // when
        double angularSpeed1 = autonomicController.getAngularSpeed(expectedCourse, currentCourse1);
        double angularSpeed2 = autonomicController.getAngularSpeed(expectedCourse, currentCourse2);
        double angularSpeed3 = autonomicController.getAngularSpeed(expectedCourse, currentCourse3);
        double angularSpeed4 = autonomicController.getAngularSpeed(expectedCourse, currentCourse4);
        // then
        assertTrue(angularSpeed1 < 0);
        assertTrue(angularSpeed2 < 0);
        assertTrue(angularSpeed3 < 0);
        assertTrue(angularSpeed4 < 0);
    }

    @Test
    public void returnAngularSpeedLessThanZeroAndCurrentIsLessThanExpected() {
        // given
        double expectedCourse = 350;
        double currentCourse1 = 40;
        double currentCourse2 = 60;
        double currentCourse3 = 90;
        double currentCourse4 = 170;
        // when
        double angularSpeed1 = autonomicController.getAngularSpeed(expectedCourse, currentCourse1);
        double angularSpeed2 = autonomicController.getAngularSpeed(expectedCourse, currentCourse2);
        double angularSpeed3 = autonomicController.getAngularSpeed(expectedCourse, currentCourse3);
        double angularSpeed4 = autonomicController.getAngularSpeed(expectedCourse, currentCourse4);
        // then
        assertTrue(angularSpeed1 < 0);
        assertTrue(angularSpeed2 < 0);
        assertTrue(angularSpeed3 < 0);
        assertTrue(angularSpeed4 < 0);
    }

    @Test
    public void returnAngularSpeedGreaterThanZeroAndCurrentIsGreaterThanExpected() {
        // given
        double expectedCourse = 30;
        double currentCourse1 = 211;
        double currentCourse2 = 250;
        double currentCourse3 = 330;
        double currentCourse4 = 359;
        // when
        double angularSpeed1 = autonomicController.getAngularSpeed(expectedCourse, currentCourse1);
        double angularSpeed2 = autonomicController.getAngularSpeed(expectedCourse, currentCourse2);
        double angularSpeed3 = autonomicController.getAngularSpeed(expectedCourse, currentCourse3);
        double angularSpeed4 = autonomicController.getAngularSpeed(expectedCourse, currentCourse4);
        // then
        assertTrue(angularSpeed1 > 0);
        assertTrue(angularSpeed2 > 0);
        assertTrue(angularSpeed3 > 0);
        assertTrue(angularSpeed4 > 0);
    }

    @Test
    public void returnAngularSpeedGreaterThanZeroAndCurrentIsLessThanExpected() {
        // given
        double expectedCourse = 350;
        double currentCourse1 = 171;
        double currentCourse2 = 201;
        double currentCourse3 = 250;
        double currentCourse4 = 320;
        // when
        double angularSpeed1 = autonomicController.getAngularSpeed(expectedCourse, currentCourse1);
        double angularSpeed2 = autonomicController.getAngularSpeed(expectedCourse, currentCourse2);
        double angularSpeed3 = autonomicController.getAngularSpeed(expectedCourse, currentCourse3);
        double angularSpeed4 = autonomicController.getAngularSpeed(expectedCourse, currentCourse4);
        // then
        assertTrue(angularSpeed1 > 0);
        assertTrue(angularSpeed2 > 0);
        assertTrue(angularSpeed3 > 0);
        assertTrue(angularSpeed4 > 0);
    }

}

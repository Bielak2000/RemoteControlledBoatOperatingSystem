import com.example.systemobslugilodzizdalniesterowanej.boatmodel.autonomiccontrol.AutonomicController;
import com.example.systemobslugilodzizdalniesterowanej.common.Utils;
import com.example.systemobslugilodzizdalniesterowanej.maps.OSMMap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DesignateLinearSpeedTest {

    @Mock
    private static OSMMap osmMap;

    private static AutonomicController autonomicController;

    @BeforeAll
    public static void beforeAll() {
        autonomicController = new AutonomicController(osmMap);
    }

    @Test
    public void returnLinearSpeedEqualsMin() {
        // given
        double distanceBetweenCurrentAndNextPositions = 0;
        // when
        double linearSpeed = autonomicController.getLinearSpeed(distanceBetweenCurrentAndNextPositions);
        // then
        assertEquals(linearSpeed, Utils.MIN_LINEAR_SPEED_PERCENTAGE);
    }

    @Test
    public void returnLinearSpeedEqualsMax() {
        // given
        double distanceBetweenCurrentAndNextPositions = 25.1;
        // when
        double linearSpeed = autonomicController.getLinearSpeed(distanceBetweenCurrentAndNextPositions);
        // then
        assertEquals(linearSpeed, Utils.MAX_LINEAR_SPEED_PERCENTAGE);
    }

    @Test
    public void returnLinearSpeedBetweenMinAndMax() {
        // given
        double distanceBetweenCurrentAndNextPositions = 10;
        // when
        double linearSpeed = autonomicController.getLinearSpeed(distanceBetweenCurrentAndNextPositions);
        // then
        assertTrue(linearSpeed < Utils.MAX_LINEAR_SPEED_PERCENTAGE);
        assertTrue(linearSpeed > Utils.MIN_LINEAR_SPEED_PERCENTAGE);
    }

}

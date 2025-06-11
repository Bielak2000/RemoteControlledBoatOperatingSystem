import com.example.systemobslugilodzizdalniesterowanej.boatmodel.autonomiccontrol.LinearAndAngularSpeed;
import com.example.systemobslugilodzizdalniesterowanej.boatmodel.components.Engines;
import com.example.systemobslugilodzizdalniesterowanej.common.Utils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DesignateEnginesPowerTest {

    private static Engines engines;

    @BeforeAll
    public static void beforeAll() {
        engines = new Engines();
    }

    @Test
    public void returnEnginesPowerEqualsZero() {
        // given
        LinearAndAngularSpeed linearAndAngularSpeed = new LinearAndAngularSpeed(0.0, 0.0, Utils.ANGULAR_FACTORY_NORMAL);
        // when
        engines.setEnginesPowerByAngularAndLinearSpeed(linearAndAngularSpeed);
        // then
        assertEquals(engines.getMotorLeft(), 0.0);
        assertEquals(engines.getMotorRight(), 0.0);
    }

    @Test
    public void returnEnginesPowerTurnRightWithLinearSpeedEqualsZero() {
        // given
        LinearAndAngularSpeed linearAndAngularSpeed = new LinearAndAngularSpeed(10.0, 0.0, Utils.ANGULAR_FACTORY_NORMAL);
        // when
        engines.setEnginesPowerByAngularAndLinearSpeed(linearAndAngularSpeed);
        // then
        assertTrue(engines.getMotorLeft() > 0.0);
        assertTrue(engines.getMotorRight() > 0.0);
    }

    @Test
    public void returnEnginesPowerTurnLeftWithLinearSpeedEqualsZero() {
        // given
        LinearAndAngularSpeed linearAndAngularSpeed = new LinearAndAngularSpeed(-10.0, 0.0, Utils.ANGULAR_FACTORY_NORMAL);
        // when
        engines.setEnginesPowerByAngularAndLinearSpeed(linearAndAngularSpeed);
        // then
        assertTrue(engines.getMotorLeft() < 0.0);
        assertTrue(engines.getMotorRight() < 0.0);
    }

    @Test
    public void returnEnginesPowerTurnLeftAndGoAhead() {
        // given
        LinearAndAngularSpeed linearAndAngularSpeed = new LinearAndAngularSpeed(-10.0, 40.0, Utils.ANGULAR_FACTORY_MAX);
        // when
        engines.setEnginesPowerByAngularAndLinearSpeed(linearAndAngularSpeed);
        // then
        assertTrue(engines.getMotorLeft() > 0.0);
        assertTrue(engines.getMotorRight() < 0.0);
        assertTrue((engines.getMotorLeft() / mapLeftEngineCoefficient()) < Math.abs(engines.getMotorRight()));
    }

    @Test
    public void returnEnginesPowerTurnRightAndGoAhead() {
        // given
        LinearAndAngularSpeed linearAndAngularSpeed = new LinearAndAngularSpeed(10.0, 40.0, Utils.ANGULAR_FACTORY_NORMAL);
        // when
        engines.setEnginesPowerByAngularAndLinearSpeed(linearAndAngularSpeed);
        // then
        assertTrue(engines.getMotorLeft() > 0.0);
        assertTrue(engines.getMotorRight() < 0.0);
        assertTrue((engines.getMotorLeft() / mapLeftEngineCoefficient()) > Math.abs(engines.getMotorRight()));
    }

    @Test
    public void returnEnginesPowerGoAhead() {
        // given
        LinearAndAngularSpeed linearAndAngularSpeed = new LinearAndAngularSpeed(0.0, 50.0, Utils.ANGULAR_FACTORY_NORMAL);
        // when
        engines.setEnginesPowerByAngularAndLinearSpeed(linearAndAngularSpeed);
        // then
        assertTrue(engines.getMotorLeft() > 0.0);
        assertTrue(engines.getMotorRight() < 0.0);
        assertTrue(Math.abs((engines.getMotorLeft() / mapLeftEngineCoefficient()) - Math.abs(engines.getMotorRight())) < 2);
    }

    @Test
    public void returnEnginesPowerGoBack() {
        // given
        LinearAndAngularSpeed linearAndAngularSpeed = new LinearAndAngularSpeed(0.0, -50.0, Utils.ANGULAR_FACTORY_NORMAL);
        // when
        engines.setEnginesPowerByAngularAndLinearSpeed(linearAndAngularSpeed);
        // then
        assertTrue(engines.getMotorLeft() < 0.0);
        assertTrue(engines.getMotorRight() > 0.0);
        assertTrue(Math.abs(Math.abs(engines.getMotorLeft() / mapLeftEngineCoefficient()) - Math.abs(engines.getMotorRight())) < 3);
    }

    @Test
    public void returnEnginesPowerGoRight() {
        // given
        LinearAndAngularSpeed linearAndAngularSpeed = new LinearAndAngularSpeed(50.0, 0.0, Utils.ANGULAR_FACTORY_NORMAL);
        // when
        engines.setEnginesPowerByAngularAndLinearSpeed(linearAndAngularSpeed);
        // then
        assertTrue(engines.getMotorLeft() > 0.0);
        assertTrue(engines.getMotorRight() > 0.0);
        assertTrue(Math.abs(Math.abs(engines.getMotorLeft() / mapLeftEngineCoefficient()) - Math.abs(engines.getMotorRight())) < 3);
    }

    @Test
    public void returnEnginesPowerGoLeft() {
        // given
        LinearAndAngularSpeed linearAndAngularSpeed = new LinearAndAngularSpeed(-80.0, 0.0, Utils.ANGULAR_FACTORY_NORMAL);
        // when
        engines.setEnginesPowerByAngularAndLinearSpeed(linearAndAngularSpeed);
        // then
        assertTrue(engines.getMotorLeft() < 0.0);
        assertTrue(engines.getMotorRight() < 0.0);
        assertTrue(Math.abs(Math.abs(engines.getMotorLeft() / mapLeftEngineCoefficient()) - Math.abs(engines.getMotorRight())) < 3);
    }

    private double mapLeftEngineCoefficient() {
        return 50.0 / Utils.MAX_LINEAR_SPEED_PERCENTAGE;
    }

}

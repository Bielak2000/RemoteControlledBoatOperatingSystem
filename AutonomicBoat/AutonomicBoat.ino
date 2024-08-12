#include <SoftwareSerial.h>
#include <TinyGPS++.h>
#include <Adafruit_BNO08x.h>
#include <math.h>
// ********************************************************************************
// *********************IMPLEMENTACJA TYLKO DO TESTOW******************************
#include <LiquidCrystal.h>
// ********************************************************************************

#define EARTH_RADIUS 6371.0
#define MINIMAL_DIFFERENCE_LOCALIZATION 100
#define INTERVAL_SEND_DATA 300

// ********************************************************************************
// *********************IMPLEMENTACJA TYLKO DO TESTOW******************************

#define GPS_COURSE_ACCURACY 3
#define COMPASS_COURSE_ACCURACY 3

// ********************************************************************************

// ZMIENNE DO OBSLUGI GPS - lokalizacja
TinyGPSPlus gps;
double newLat = 400;
double newLng = 400;
double oldLat = 400;
double oldLng = 400;
bool newLocalization = false;

// ZMIENNE DO OBSLUG GPS - kurs
double gpsCourse = 400;

// ZMIENNE DO OBSLUGI BNO08X - kompas
uint32_t COMPASS_REPORT_INTERVAL = 1000000;  // 100000 µs = 100 ms = 10 Hz
Adafruit_BNO08x bno08x(-1);
sh2_SensorValue_t compassValue;
double previousCompassCourse = 400;
double compassCourse = 400;
bool newCompassCourse = false;
struct euler_t {
  float yaw;
  float pitch;
  float roll;
} compassData;

// ZMIENNE DO WYSYŁANIA DANYCH
String dataBuffer = "";
unsigned long currentMillis;
unsigned long previousMillis = 0;
// OZNACZENIA
const String LOCALIZATION_ASSIGN = "1";
const String GPS_COURSE_ASSIGN = "5";
const String COMPASS_COURSE_ASSIGN = "6";

// ********************************************************************************
// *********************IMPLEMENTACJA TYLKO DO TESTOW******************************

double previousGpsCourse = 400;
bool newGpsCourse = false;
LiquidCrystal lcd(12, 11, 5, 4, 3, 6);

// ********************************************************************************

void compassInterrupt() {
  newCompassCourse = true;
}

void setup() {
    Serial2.begin(9600); // gps
    Serial3.begin(57600); // radionadanjnik
    setCompassSensor();
    setCompassReports();
    attachInterrupt(digitalPinToInterrupt(2), compassInterrupt, FALLING);
    delay(500);

// ********************************************************************************
// *********************IMPLEMENTACJA TYLKO DO TESTOW******************************
    Serial.begin(4800);
    lcd.begin(16,2);
// ********************************************************************************    
}

void serialEvent2() {
  while (Serial2.available()) {
    if (gps.encode(Serial2.read())) {
      if (gps.location.isValid()) {
        newLat = gps.location.lat();
        newLng = gps.location.lng();
        newLocalization = true;
      }
      if(gps.course.isValid()) {
        gpsCourse = gps.course.deg();
        newGpsCourse = true;
      }
    }
  }
}

void loop() {
  // OBSLUGA DANYCH LOKALIZACYJNYCH JESLI SIE POJAWILY
  if(newLocalization) {
    newLocalization = false;
    if(newLocalizationHandler()) {
      appendData(LOCALIZATION_ASSIGN + "_" + String(gps.location.lat(), 5) + "," + String(gps.location.lng(), 5) + "_");
      // ********************************************************************************
      // *********************IMPLEMENTACJA TYLKO DO TESTOW******************************
      // lcd.setCursor(0,0);
      // lcd.print(LOCALIZATION_ASSIGN + "_" + String(gps.location.lat(), 5) + "," + String(gps.location.lng(), 5) + "_");
      // ********************************************************************************   
    }
  }

  // OBSLUGA DANYCH KURSU Z GPS JESLI SIE POJAWILY
  if(newGpsCourse) {
    newGpsCourse = false;
    if(newGpsCourseHandler()) {
      appendData(GPS_COURSE_ASSIGN + "_" + String(gpsCourse) + "_");
      // ********************************************************************************
      // *********************IMPLEMENTACJA TYLKO DO TESTOW******************************
      // lcd.setCursor(0,1);
      // lcd.print(GPS_COURSE_ASSIGN + "_" + String(gpsCourse) + "_");
      // ********************************************************************************   
    }
  }

  // OBSLUGA DANYCH KURSU Z KOMPASU JESLI SIE POJAWILY
  if (newCompassCourse) {
    compassRead();
    if(newCompassCourseHandler()) {
      appendData(COMPASS_COURSE_ASSIGN + "_" + String(compassCourse) + "_");
    }
  }

  // WYSYLANIE DANYCH
  if(dataBuffer.length() > 0) {
    sendDataIfNecessary();
  }
}

void sendDataIfNecessary() {
  currentMillis = millis();
  if (currentMillis - previousMillis >= INTERVAL_SEND_DATA || previousMillis == 0) {
    int delimiterIndex = dataBuffer.indexOf(';');
    String dataToSend;
    if (delimiterIndex != -1) {
      dataToSend = dataBuffer.substring(0, delimiterIndex);
      dataBuffer = dataBuffer.substring(delimiterIndex + 1);
    } else {
      dataToSend = dataBuffer;
      dataBuffer = "";
    }
    Serial3.print(dataToSend);
    previousMillis = millis();
  }
}

void appendData(String data) {
  if (dataBuffer.length() > 0) {
    dataBuffer += ";";
  }
  dataBuffer += data;
}

bool newLocalizationHandler() {
  double dis = calculateCmDistance();
  if(dis > MINIMAL_DIFFERENCE_LOCALIZATION || oldLat == 0) {
      oldLat = newLat;
      oldLng = newLng;
      lcd.setCursor(0,1);
      lcd.print(dis);
      return true;
  }
  return false;
}

double calculateCmDistance() {
  return TinyGPSPlus::distanceBetween(newLat, newLng, oldLat, oldLng) * 100;
    // double lat1Rad = newLat * M_PI / 180.0;
    // double lon1Rad = newLng * M_PI / 180.0;
    // double lat2Rad = oldLat * M_PI / 180.0;
    // double lon2Rad = oldLng * M_PI / 180.0;
    // double dLat = lat2Rad - lat1Rad;
    // double dLon = lon2Rad - lon1Rad;
    // double a = sin(dLat / 2) * sin(dLat / 2) +
    //            cos(lat1Rad) * cos(lat2Rad) *
    //            sin(dLon / 2) * sin(dLon / 2);
    // double c = 2 * atan2(sqrt(a), sqrt(1 - a));
    // return EARTH_RADIUS * c * 100000.0;
}

// DODAĆ ODPOWIEDNIA OBSLUGE BLEDOW
void setCompassSensor() {
  if (!bno08x.begin_I2C()) {
    Serial.write("Failed to find BNO08x chip\n");
    while (1) {
      delay(10);
    }
    bno08x.hardwareReset();
  }
}

void setCompassReports() {
  if (!bno08x.enableReport(SH2_ROTATION_VECTOR, COMPASS_REPORT_INTERVAL)) {
    Serial.write("Could not enable rotation vector\n");
  }
}

void quaternionToEuler(float qr, float qi, float qj, float qk, euler_t* ypr, bool degrees) {
    float sqr = sq(qr);
    float sqi = sq(qi);
    float sqj = sq(qj);
    float sqk = sq(qk);

    ypr->yaw = atan2(2.0 * (qi * qj + qk * qr), (sqi - sqj - sqk + sqr));
    ypr->pitch = asin(-2.0 * (qi * qk - qj * qr) / (sqi + sqj + sqk + sqr));
    ypr->roll = atan2(2.0 * (qj * qk + qi * qr), (-sqi - sqj + sqk + sqr));

    if (degrees) {
      ypr->yaw *= RAD_TO_DEG;
      ypr->yaw = -ypr->yaw;
      if(ypr->yaw<0) {
        ypr->yaw = 360.0+ypr->yaw;
      }
    }
}

void quaternionToEulerRV(sh2_RotationVectorWAcc_t* rotational_vector, euler_t* ypr, bool degrees) {
    quaternionToEuler(rotational_vector->real, rotational_vector->i, rotational_vector->j, rotational_vector->k, ypr, degrees);
}

void compassRead() {
  newCompassCourse = false;
  if (bno08x.wasReset()) {
    Serial.write("sensor was reset\n");
    setCompassReports();
  }    
  if (bno08x.getSensorEvent(&compassValue)) {
    switch (compassValue.sensorId) {
      case SH2_ROTATION_VECTOR:
        quaternionToEulerRV(&compassValue.un.rotationVector, &compassData, true);
        compassCourse = compassData.yaw;
        lcd.clear();
        lcd.setCursor(0,0);
        lcd.print(compassCourse);
        break;
    }
  }
}

bool newCompassCourseHandler() {
  if(abs(compassCourse - previousCompassCourse) > COMPASS_COURSE_ACCURACY) {
      previousCompassCourse = compassCourse;
      return true;
  }
  return false;
}

// ********************************************************************************
// *********************IMPLEMENTACJA TYLKO DO TESTOW******************************

bool newGpsCourseHandler() {
  if(abs(gpsCourse - previousGpsCourse) > GPS_COURSE_ACCURACY) {
      previousGpsCourse = gpsCourse;
      return true;
  }
  return false;
}

// ********************************************************************************
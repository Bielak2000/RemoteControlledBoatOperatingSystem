#include <SoftwareSerial.h>
#include <TinyGPS++.h>
#include <Adafruit_BNO08x.h>
#include <math.h>
#include <Servo.h>
// ********************************************************************************
// *********************IMPLEMENTACJA TYLKO DO TESTOW******************************
#include <LiquidCrystal.h>
// ********************************************************************************

#define EARTH_RADIUS 6371.0
// sprawdzic to czy 1cm czy 10cm
#define MINIMAL_DIFFERENCE_LOCALIZATION 1
#define INTERVAL_SEND_DATA 300
#define INTERVAL_ENGINE_POWER 50
#define INTERVAL_LIGHTING_POWER 500
#define BOAT_MODE_KEYBOARD 0
#define BOAT_MODE_KEYBOARD_INIT 4
#define BOAT_MODE_TO_AUTONOMIC 1
#define BOAT_MODE_AUTONOMIC_CONTROL 2
#define FINISH_AUTONOMIC_CONTROL 3

#define ONLY_GPS_ALOGRITHM 0
#define SENSOR_AND_GPS_ALOGIRTHM 1
#define BASIC_ALGORITHM 2
#define KALMAN_ALGORITHM 3


// USTAWIĆ ODPOWIEDNIE PINY PO PODŁACZENIU
#define LEFT_ENGINE 8
#define RIGHT_ENGINE 9
#define LEFT_FLAP 8
#define RIGHT_FLAP 9
#define LIGHTING 8

// sprawdzic to czy 1 czy 0.5
double GPS_COURSE_ACCURACY = 0.5;
double COMPASS_COURSE_ACCURACY = 0.5;

double linearAccelarationAccuracy = 0.09;
double angularSpeedAccuarcy = 0.09;

// MODEL STEROWANIA ŁODZIĄ
int boatMode = BOAT_MODE_KEYBOARD;
int selectedPositionAlgorithm = -1;

// SILNIKI
Servo leftEngine;
Servo rightEngine;
int enginesSpeed = 1500;
unsigned long setServoPowerCurrentMillis;
unsigned long setEnginePowerPreviousMillis = 0;
int newEnginesSpeed[2] = {0,0};
int currentEngineSpeeds[2] = {0,0};

// ZAPADKI
Servo leftFlap;
Servo rightFlap;
int leftFlapState = 0;
int rightFlapState = 0;
int leftFlapPower = 1400;
int rightFlapPower = 1700;

// SWIATLO
Servo lighting;
int newLight = 0;
int currentLight = 0;
bool sentLigthingValue = false;
unsigned long setLightingPowerPreviousMillis = 0;

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
uint32_t COMPASS_REPORT_INTERVAL = 1000000 / 2;  // 1S / 2 = 2HZ, dwa razy na sekunde
Adafruit_BNO08x bno08x(-1);
sh2_SensorValue_t compassValue;
double previousCompassCourse = 400;
double compassCourse = 400;
double linearAccelarationX = 0.0;
double linearAccelarationY = 0.0;
double speedAngular = 0.0;
double previousLinearAccelarationX = 0.0;
double previousLinearAccelarationY = 0.0;
double previousSpeedAngular = 0.0;
bool newSensorValue = false;
struct euler_t {
  float yaw;
  float pitch;
  float roll;
} compassData;

// ZMIENNE DO WYSYŁANIA DANYCH
String dataBuffer = "";
unsigned long currentMillis;
unsigned long previousMillis = 0;

// ZMIENNE DO ODBIORU DANYCH - keyboard handler
const uint8_t buffLength = 9;
char buff[buffLength];
uint8_t buffIndex = 0;
uint8_t arrayIndex = 0;
int newDataForKeyboardHandler[5] = {0,0,0,0,0};
bool receivedFirstData = true;

// OZNACZENIA DANYCH WYSYLANYCH DO APLIKACJI
const String LIGTHING_ASSIGN = "0";
const String LOCALIZATION_ASSIGN = "1";
const String FINISH_SWIMMING_BY_WAYPOINTS = "2";
const String GPS_COURSE_ASSIGN = "5";
const String COMPASS_COURSE_ASSIGN = "6";
const String LINEAR_ACCELARATION_ANGULAR_SPEED_ASSIGN = "7";

double previousGpsCourse = 400;
bool newGpsCourse = false;
LiquidCrystal lcd(12, 11, 5, 4, 3, 6);

void compassInterrupt() {
  newSensorValue = true;
}

void enginesInit() {
  leftEngine.attach(LEFT_ENGINE);
  leftEngine.writeMicroseconds(enginesSpeed);
  rightEngine.attach(RIGHT_ENGINE);
  rightEngine.writeMicroseconds(enginesSpeed);
}

void flapsInit() {
  leftFlap.attach(LEFT_FLAP);
  leftFlap.writeMicroseconds(leftFlapPower);
  rightFlap.attach(RIGHT_FLAP);
  rightFlap.writeMicroseconds(rightFlapPower);
}

void lightingInit() {
  lighting.attach(LIGHTING);
  lighting.writeMicroseconds(1200);
}

void setup() {
  Serial2.begin(9600); // gps
  Serial3.begin(57600); // radionadanjnik

  // USTAWIĆ ODPOWIEDNIE PINY PO PODŁĄCZENIU - NAJLEPIEJ ZA POMOCĄ DEFINE I ODKOMENTOWAĆ FUNKCJE
  // enginesInit();
  // flapsInit();
  lightingInit();

  delay(500);

// ********************************************************************************
// *********************IMPLEMENTACJA TYLKO DO TESTOW******************************
  Serial.begin(4800);
  lcd.begin(16,2);
// ********************************************************************************    
}

// GPS
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

// RADIONADAJNIK
void serialEvent3() {
  while (Serial3.available()) {
    char newChar = Serial3.read();
    if(receivedFirstData) {
      int boatModeTemp = newChar - '0';
      if(boatMode != boatModeTemp) {
        boatMode = boatModeTemp;
      }
    }
    receivedFirstData = false;

    if(boatMode == BOAT_MODE_KEYBOARD) {
      if (buffIndex < buffLength-1) { 
        readDataFromAppForKeyboardMode(newChar);      
      }
    } else if(boatMode == BOAT_MODE_TO_AUTONOMIC) {
      receivedFirstData = true;
    } else if(boatMode == BOAT_MODE_AUTONOMIC_CONTROL) {
      readDataFromAppForAutonomicMode(newChar);
    } else if (boatMode == FINISH_AUTONOMIC_CONTROL) {
      setStopEnginePower();
      setEnginePowerAutonomicMode();
      clearData();
      delay(1000);
      appendData(FINISH_SWIMMING_BY_WAYPOINTS + "_");
      boatMode = BOAT_MODE_KEYBOARD;
    } else if (boatMode == BOAT_MODE_KEYBOARD_INIT) {
      readDataFromAppForInitializeConnection(newChar);
    }
  }
}

void loop() {
  // OBGLUGA DANYCH Z APLIKACJI - RECZNE STEROWANIE
  if(boatMode == BOAT_MODE_KEYBOARD && checkNewDataFromAppInKeyboardMode()) {
    keyboardHandler();
  }

  // OBGLUGA DANYCH Z APLIKACJI - AUTONOMICZNE STEROWANIE
  if(boatMode == BOAT_MODE_AUTONOMIC_CONTROL && checkNewDataFromAppInAutonomicMode()) {
    autonomicHandler();
  }

  // OBSLUGA DANYCH LOKALIZACYJNYCH JESLI SIE POJAWILY
  if(newLocalization) {
    newLocalization = false;
    if(newLocalizationHandler()) {
      replaceOrAppendStringStartingWith(LOCALIZATION_ASSIGN + "_", LOCALIZATION_ASSIGN + "_" + String(gps.location.lat(), 7) + "," + String(gps.location.lng(), 7) + "_");
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
      replaceOrAppendStringStartingWith(GPS_COURSE_ASSIGN + "_", GPS_COURSE_ASSIGN + "_" + String(gpsCourse) + "_");
      // ********************************************************************************
      // *********************IMPLEMENTACJA TYLKO DO TESTOW******************************
      // lcd.setCursor(0,1);
      // lcd.print(GPS_COURSE_ASSIGN + "_" + String(gpsCourse) + "_");
      // ********************************************************************************   
    }
  }

  // OBSLUGA DANYCH KURSU Z KOMPASU JESLI SIE POJAWILY
  if (selectedPositionAlgorithm != ONLY_GPS_ALOGRITHM && newSensorValue) {
    sensorRead();
    if(newCompassCourseHandler()) {
      replaceOrAppendStringStartingWith(COMPASS_COURSE_ASSIGN + "_", COMPASS_COURSE_ASSIGN + "_" + String(compassCourse) + "_");
    }
    if(selectedPositionAlgorithm == KALMAN_ALGORITHM && newAccelarationOrSpeedHandler()) {
      replaceOrAppendStringStartingWith(LINEAR_ACCELARATION_ANGULAR_SPEED_ASSIGN + "_", 
          LINEAR_ACCELARATION_ANGULAR_SPEED_ASSIGN + "_" + String(linearAccelarationX, 2) + "," + String(linearAccelarationY, 2) + "," + String(speedAngular) + "_");
    }
  }

  if(boatMode == BOAT_MODE_KEYBOARD) {
    if (sentLigthingValue && newDataForKeyboardHandler[2] != 1) {
      appendData(LIGTHING_ASSIGN + "_" + String(currentLight) + "_");
      sentLigthingValue = false;
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

void replaceOrAppendStringStartingWith(const String &prefix, const String &newValue) {
    int startIndex = 0;
    int prefixLength = prefix.length();
    bool found = false;
    while ((startIndex = dataBuffer.indexOf(prefix, startIndex)) != -1) {
        found = true;
        int endIndex = dataBuffer.indexOf(';', startIndex);
        if (endIndex == -1) {
            endIndex = dataBuffer.length();
        }
        if(endIndex - startIndex > 2) {
            dataBuffer.remove(startIndex, endIndex - startIndex + (endIndex < dataBuffer.length() ? 1 : 0));
            if (startIndex + newValue.length() < dataBuffer.length()) {
              newValue += ";";
            }
            dataBuffer = dataBuffer.substring(0, startIndex) + newValue + dataBuffer.substring(startIndex);
        }
        startIndex += newValue.length();
    }
    if (!found) {
        appendData(newValue);
    }
}

void clearData() {
  dataBuffer = "";
}

bool newLocalizationHandler() {
  double dis = calculateCmDistance();
  if(dis > MINIMAL_DIFFERENCE_LOCALIZATION || oldLat == 0) {
      oldLat = newLat;
      oldLng = newLng;
      return true;
  }
  return false;
}

double calculateCmDistance() {
  return TinyGPSPlus::distanceBetween(newLat, newLng, oldLat, oldLng) * 100;
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
  if(selectedPositionAlgorithm == SENSOR_AND_GPS_ALOGIRTHM || selectedPositionAlgorithm == BASIC_ALGORITHM) {
    if(!bno08x.enableReport(SH2_ROTATION_VECTOR, COMPASS_REPORT_INTERVAL)) {
      Serial.println("Could not enable SH2_ROTATION_VECTOR");
    }

  } else if(selectedPositionAlgorithm == KALMAN_ALGORITHM) {
    if(!bno08x.enableReport(SH2_ROTATION_VECTOR, COMPASS_REPORT_INTERVAL)) {
      Serial.println("Could not enable SH2_ROTATION_VECTOR");
    }
    delay(10);
    if(!bno08x.enableReport(SH2_LINEAR_ACCELERATION, COMPASS_REPORT_INTERVAL)) {
      Serial.println("Could not enable SH2_LINEAR_ACCELERATION");
    }
    delay(10);
    if(!bno08x.enableReport(SH2_GYROSCOPE_CALIBRATED, COMPASS_REPORT_INTERVAL)) {
      Serial.println("Could not enable SH2_GYROSCOPE_CALIBRATED");
    }
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

void sensorRead() {
  newSensorValue = false;
  if (bno08x.wasReset()) {
    Serial.write("sensor was reset\n");
    setCompassReports();
  }    
  if (bno08x.getSensorEvent(&compassValue)) {
    switch (compassValue.sensorId) {
      case SH2_ROTATION_VECTOR:
        quaternionToEulerRV(&compassValue.un.rotationVector, &compassData, true);
        compassCourse = compassData.yaw;
        // lcd.clear();
        // lcd.setCursor(0,0);
        // lcd.print(compassCourse);
        break;
      case SH2_LINEAR_ACCELERATION:
        linearAccelarationX = compassValue.un.accelerometer.x;
        linearAccelarationY = compassValue.un.accelerometer.y;
        break;
      case SH2_GYROSCOPE_CALIBRATED:
        speedAngular = compassValue.un.gyroscope.z;
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

bool newAccelarationOrSpeedHandler() {
  if((abs(linearAccelarationX - previousLinearAccelarationX) > linearAccelarationAccuracy || abs(linearAccelarationY - previousLinearAccelarationY) > linearAccelarationAccuracy ||
        abs(speedAngular - previousSpeedAngular) > angularSpeedAccuarcy)) {
      if(abs(linearAccelarationX - 0.0) < 0.1) linearAccelarationX = 0.0;
      if(abs(linearAccelarationY - 0.0) < 0.1) linearAccelarationY = 0.0;
      if(abs(speedAngular - 0.0) < 0.1) speedAngular = 0.0;
      previousLinearAccelarationX = linearAccelarationX;
      previousLinearAccelarationY = linearAccelarationY;
      previousSpeedAngular = speedAngular;
      return true;
  }
  return false;
}

void readDataFromAppForKeyboardMode(char newChar) {
   if (newChar == '_') { 
      buff[buffIndex++] = 0;
      buffIndex = 0;
      if(arrayIndex==5){
        rightFlapState = atoi(buff);
      }
      else if(arrayIndex==4){
        leftFlapState = atoi(buff);
      }
      else if(arrayIndex==3){
        newLight = atoi(buff);
      }
      else if(arrayIndex==2){
        newEnginesSpeed[1] = atoi(buff);
      }
      else if(arrayIndex==1) {
        newEnginesSpeed[0] = atoi(buff);
      }
      arrayIndex++;
      if (arrayIndex == 6) 
      { 
        newDataForKeyboardHandler[0]=1;
        newDataForKeyboardHandler[1]=1;
        if(newLight != currentLight){
          newDataForKeyboardHandler[2]=1;
        }
        if(rightFlapState != 0){
           newDataForKeyboardHandler[3]=1;
        }
        if(leftFlapState != 0){
           newDataForKeyboardHandler[4]=1;
        }
        arrayIndex = 0;
        receivedFirstData = true;
      }
   }
   else if (newChar == '-' || ('0' <= newChar && newChar <= '9'))
   {
       buff[buffIndex++] = newChar;
   }
}

void readDataFromAppForAutonomicMode(char newChar) {
   if (newChar == '_') { 
      buff[buffIndex++] = 0;
      buffIndex = 0;
      if(arrayIndex==2){
        newEnginesSpeed[1] = atoi(buff);
                    lcd.setCursor(8, 1);         
    lcd.print(newEnginesSpeed[1]);
      }
      else if(arrayIndex==1) {
        newEnginesSpeed[0] = atoi(buff);
        lcd.setCursor(0, 1);         
        lcd.print(newEnginesSpeed[0]);
      }
      arrayIndex++;
      if (arrayIndex == 3) 
      { 
        newDataForKeyboardHandler[0]=1;
        newDataForKeyboardHandler[1]=1;
        arrayIndex = 0;
        receivedFirstData = true;
      }
   }
   else if (newChar == '-' || ('0' <= newChar && newChar <= '9'))
   {
       buff[buffIndex++] = newChar;
   }
}

void readDataFromAppForInitializeConnection(char newChar) {
   if (newChar == '_') { 
      buff[buffIndex++] = 0;
      buffIndex = 0;
      if(arrayIndex==1){
        selectedPositionAlgorithm = atoi(buff);
      }
      arrayIndex++;
      if (arrayIndex == 2) 
      { 
        arrayIndex = 0;
        receivedFirstData = true;
        if(selectedPositionAlgorithm != ONLY_GPS_ALOGRITHM) {
          setCompassSensor();
          attachInterrupt(digitalPinToInterrupt(2), compassInterrupt, FALLING);
          setCompassReports();
        }
      }
   }
   else if (newChar == '-' || ('0' <= newChar && newChar <= '9'))
   {
       buff[buffIndex++] = newChar;
   }
}

void keyboardHandler() {
  enginesNewPowerHandler(INTERVAL_ENGINE_POWER, false);

  // jesli sa informacje to wlacz zapadke (jesli przeslano 1)
  if(newDataForKeyboardHandler[3]==1 || newDataForKeyboardHandler[4]==1){
    openOrCloseFlaps();
    rightFlapState = 0;
    leftFlapState = 0;
  }
}

void autonomicHandler() {
  enginesNewPowerHandler(INTERVAL_ENGINE_POWER, true);
}

void enginesNewPowerHandler(int intervalEnginePower, bool auonomicMode) {
    // szybczkosc zwiekszania mocy na silniki / oswietlenie / zapadki
  setServoPowerCurrentMillis = millis();
  if (setServoPowerCurrentMillis - setEnginePowerPreviousMillis >= intervalEnginePower || setEnginePowerPreviousMillis == 0) {
    //jesli dostano dane i jeszcze nie zostaly ustawione na maksa to wykonaj
    if((newDataForKeyboardHandler[0]==1 || newDataForKeyboardHandler[1]==1)){
          if(auonomicMode) {
            setEnginePowerAutonomicMode();
          } else {
            setEnginePowerKeyboardMode();
          }
      setEnginePowerPreviousMillis = millis();
    }
  }
  
  if (setServoPowerCurrentMillis - setLightingPowerPreviousMillis >= INTERVAL_LIGHTING_POWER || setLightingPowerPreviousMillis == 0) {
    if(newDataForKeyboardHandler[2]==1){
      setLight();
      sentLigthingValue = true;
      setLightingPowerPreviousMillis = millis();
    }
  }
}

bool checkNewDataFromAppInKeyboardMode() {
  return newDataForKeyboardHandler[0]==1 || newDataForKeyboardHandler[1]==1 || newDataForKeyboardHandler[2]==1 || newDataForKeyboardHandler[3]==1 || newDataForKeyboardHandler[4]==1;
}

bool checkNewDataFromAppInAutonomicMode() {
  return newDataForKeyboardHandler[0]==1 || newDataForKeyboardHandler[1]==1;
}

void setStopEnginePower() {
  newEnginesSpeed[0] = 0;
  newEnginesSpeed[1] = 0;
}

void setEnginePowerKeyboardMode(){
    for(int i=0; i<2; i++)
    {
      if(newEnginesSpeed[i]<0 && currentEngineSpeeds[i]>newEnginesSpeed[i])
      {
        currentEngineSpeeds[i]=currentEngineSpeeds[i]-1;
      }
      else if(newEnginesSpeed[i]>0 && currentEngineSpeeds[i]<newEnginesSpeed[i])
      {
        currentEngineSpeeds[i]=currentEngineSpeeds[i]+1;
      }
      else if(newEnginesSpeed[i]==0)
      {
        currentEngineSpeeds[i]=0;
        newDataForKeyboardHandler[i]=0;
      }
      else
      {
        newDataForKeyboardHandler[i]=0;
      }
    }

    enginesSpeed = map(currentEngineSpeeds[0], -100, 100, 1000, 2000);
    leftEngine.writeMicroseconds(enginesSpeed); 
    lcd.setCursor(0, 0);         
    lcd.print(enginesSpeed);

    enginesSpeed = map(currentEngineSpeeds[1], -100, 100, 1000, 2000);
    rightEngine.writeMicroseconds(enginesSpeed);
    lcd.setCursor(8, 0);         
    lcd.print(enginesSpeed);
}

void setEnginePowerAutonomicMode(){
    for(int i=0; i<2; i++)
    {
      if(newEnginesSpeed[i]==0)
      {
        currentEngineSpeeds[i]=0;
        newDataForKeyboardHandler[i]=0;
      } else if (newEnginesSpeed[i] - currentEngineSpeeds[i] > 0) {
        currentEngineSpeeds[i]=currentEngineSpeeds[i]+1;
      } else if(newEnginesSpeed[i] - currentEngineSpeeds[i] < 0) {
        currentEngineSpeeds[i]=currentEngineSpeeds[i]-1;
      } else {
        newDataForKeyboardHandler[i]=0;
      }
    }

    enginesSpeed = map(currentEngineSpeeds[0], -100, 100, 1000, 2000);
    leftEngine.writeMicroseconds(enginesSpeed); 
    lcd.setCursor(0, 0);         
    lcd.print(enginesSpeed);

    enginesSpeed = map(currentEngineSpeeds[1], -100, 100, 1000, 2000);
    rightEngine.writeMicroseconds(enginesSpeed);
    lcd.setCursor(8, 0);         
    lcd.print(enginesSpeed);
}

void openOrCloseFlaps() {
  if(rightFlapState == 1) {
    if(rightFlapPower == 1700) {
      rightFlapPower = 1250;
    } else {
      rightFlapPower = 1700;
    }
    rightFlap.writeMicroseconds(rightFlapPower);
    newDataForKeyboardHandler[3] = 0;
  }
  if(leftFlapState == 1) {
    if(leftFlapPower == 1400) {
      leftFlapPower = 1850;
    } else {
      leftFlapPower=1400;
    }
    leftFlap.writeMicroseconds(leftFlapPower);
    newDataForKeyboardHandler[4] = 0;
  }
}

void setLight() {
  if(newLight == -1) {
    newDataForKeyboardHandler[2]=0;
  } else if(currentLight < newLight) {
    currentLight = currentLight + 5;
  }
  else if(currentLight > newLight) {
    currentLight = currentLight - 5;
  } else {
    newDataForKeyboardHandler[2] = 0;
  }
  lighting.writeMicroseconds(map(currentLight, 0, 100, 1210, 2000));
}

bool newGpsCourseHandler() {
  if(abs(gpsCourse - previousGpsCourse) > GPS_COURSE_ACCURACY) {
      previousGpsCourse = gpsCourse;
      return true;
  }
  return false;
}

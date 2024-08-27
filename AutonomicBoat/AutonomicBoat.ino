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
#define MINIMAL_DIFFERENCE_LOCALIZATION 100
#define INTERVAL_SEND_DATA 300
#define INTERVAL_ENGINE_POWER 50
#define INTERVAL_LIGHTING_POWER 500
#define BOAT_MODE_KEYBOARD 0
#define BOAT_MODE_TO_AUTONOMIC 1
#define BOAT_MODE_RECEIVIING_WAYPOINTS 2
#define BOAT_MODE_STOP_RECEIVING_WAYPOINTS 3

// ********************************************************************************
// *********************IMPLEMENTACJA TYLKO DO TESTOW******************************

#define GPS_COURSE_ACCURACY 3
#define COMPASS_COURSE_ACCURACY 3

// ********************************************************************************

// MODEL STEROWANIA ŁODZIĄ
int boatMode = BOAT_MODE_KEYBOARD;

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

// ZMIENNE DO ODBIORU DANYCH - keyboard handler
const uint8_t buffLength = 9;
char buff[buffLength];
uint8_t buffIndex = 0;
uint8_t arrayIndex = 0;
int newDataForKeyboardHandler[5] = {0,0,0,0,0};
bool receivedFirstData = true;

// AUTONOMICZNOSC
struct Waypoint {
  double lat;
  double lon;
};
int currentWaypointsIndex = 0;
struct Waypoint waypoints[5] = {
  {0.0, 0.0},
  {0.0, 0.0},
  {0.0, 0.0},
  {0.0, 0.0},
  {0.0, 0.0}
};

// OZNACZENIA
const String LIGTHING_ASSIGN = "0";
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

void enginesInit() {
  leftEngine.attach(8);
  leftEngine.writeMicroseconds(enginesSpeed);
  rightEngine.attach(9);
  rightEngine.writeMicroseconds(enginesSpeed);
}

void flapsInit() {
  leftFlap.attach(9);
  leftFlap.writeMicroseconds(leftFlapPower);
  rightFlap.attach(8);
  rightFlap.writeMicroseconds(rightFlapPower);
}

void lightingInit() {
  lighting.attach(8);
  lighting.writeMicroseconds(1200);
}

void setup() {
  Serial2.begin(9600); // gps
  Serial3.begin(57600); // radionadanjnik

  setCompassSensor();
  setCompassReports();
  attachInterrupt(digitalPinToInterrupt(2), compassInterrupt, FALLING);

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
    bool newMode = false;
    char newChar = Serial3.read();
    if(receivedFirstData) {
      int boatModeTemp = newChar - '0';
      if(boatMode != boatModeTemp) {
        boatMode = boatModeTemp;
        newMode = true;
      }
    }
    receivedFirstData = false;

    lcd.setCursor(1, 10);
    lcd.print(boatMode);
    if(boatMode == BOAT_MODE_KEYBOARD) {
      if (buffIndex < buffLength-1) { 
        if(newMode) {
         // TODO: czyszczenie po zakonczeniu autonomicznosci
         clearWaypointsData();
        }
        readDataFromAppForKeyboardMode(newChar);
      }
    } else if(boatMode == BOAT_MODE_TO_AUTONOMIC) {
      receivedFirstData = true;
    } else if(boatMode == BOAT_MODE_RECEIVIING_WAYPOINTS) {
      readWaypointsFromApp(newChar);
    } else if(boatMode == BOAT_MODE_STOP_RECEIVING_WAYPOINTS) {
      // TODO: odpowiednia konwersja na double
      // lcd.setCursor(12, 1);
      // lcd.print("9");
      // TODO: obsluga odebranych danych
            // lcd.setCursor(0, 0);
      // lcd.print(String(waypoints[0].lat));
                  // lcd.setCursor(0,1);
      // lcd.print(String(waypoints[0].lon));

    }
  }
}

void loop() {
  // OBGLUGA DANYCH Z APLIKACJI - RECZNE STEROWANIE
  if(boatMode == BOAT_MODE_KEYBOARD && checkNewDataFromAppInKeyboardMode()) {
    keyboardHandler();
  }

  if(boatMode == BOAT_MODE_KEYBOARD) {
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

bool newLocalizationHandler() {
  double dis = calculateCmDistance();
  if(dis > MINIMAL_DIFFERENCE_LOCALIZATION || oldLat == 0) {
      oldLat = newLat;
      oldLng = newLng;
      // lcd.setCursor(0,1);
      // lcd.print(dis);
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
        // lcd.clear();
        // lcd.setCursor(0,0);
        // lcd.print(compassCourse);
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

void keyboardHandler() {
  // szybczkosc zwiekszania mocy na silniki / oswietlenie / zapadki
  setServoPowerCurrentMillis = millis();
  if (setServoPowerCurrentMillis - setEnginePowerPreviousMillis >= INTERVAL_ENGINE_POWER || setEnginePowerPreviousMillis == 0) {
    //jesli dostano dane i jeszcze nie zostaly ustawione na maksa to wykonaj
    if((newDataForKeyboardHandler[0]==1 || newDataForKeyboardHandler[1]==1)){
      setEnginePowerForKeyboardMode();
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

  // jesli sa informacje to wlacz zapadke (jesli przeslano 1)
  if(newDataForKeyboardHandler[3]==1 || newDataForKeyboardHandler[4]==1){
    openOrCloseFlaps();
    rightFlapState = 0;
    leftFlapState = 0;
  }
}

bool checkNewDataFromAppInKeyboardMode() {
  return newDataForKeyboardHandler[0]==1 || newDataForKeyboardHandler[1]==1 || newDataForKeyboardHandler[2]==1 || newDataForKeyboardHandler[3]==1 || newDataForKeyboardHandler[4]==1;
}

void setEnginePowerForKeyboardMode(){
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
  lcd.setCursor(0, 1);
  lcd.print(currentLight);
  lighting.writeMicroseconds(map(currentLight, 0, 100, 1210, 2000));
}

void readWaypointsFromApp(char newChar) {
   if (newChar == '_') { 
      buff[buffIndex++] = 0;
      buffIndex = 0;
            // TODO: odpowiednia konwersja na double
      if(arrayIndex==2){
        lcd.setCursor(0,1);
        lcd.print(buff);
        waypoints[currentWaypointsIndex].lon = strtod(buff, NULL);
      } else if(arrayIndex==1){
                lcd.setCursor(0,0);
        lcd.print(buff);
        waypoints[currentWaypointsIndex].lat = strtod(buff, NULL);
      }
      arrayIndex++;
      if (arrayIndex == 3) 
      { 
        currentWaypointsIndex++;
        arrayIndex = 0;
        receivedFirstData = true;
      }
   }
   else if (newChar == '.' || ('0' <= newChar && newChar <= '9'))
   {
       buff[buffIndex++] = newChar;
   }
}

void clearWaypointsData() {
  currentWaypointsIndex = 0;
  waypoints[0] = {0,0};
  waypoints[1] = {0,0};
  waypoints[2] = {0,0};
  waypoints[3] = {0,0};
  waypoints[4] = {0,0};
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
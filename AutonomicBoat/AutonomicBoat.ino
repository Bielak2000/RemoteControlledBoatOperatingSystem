#include <SoftwareSerial.h>
#include <TinyGPS++.h>
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

// ZMIENNE DO WYSYŁANIA DANYCH
String dataBuffer = "";
unsigned long currentMillis;
unsigned long previousMillis = 0;
// OZNACZENIA
const String LOCALIZATION_ASSIGN = "1";
const String GPS_COURSE_ASSIGN = "6";

// ********************************************************************************
// *********************IMPLEMENTACJA TYLKO DO TESTOW******************************

double previousGpsCourse = 400;
bool newGpsCourse = false;
LiquidCrystal lcd(12, 11, 5, 4, 3, 2);

// ********************************************************************************

void setup() {
    Serial2.begin(9600); // gps
    Serial3.begin(57600); // radionadanjnik

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
      Serial.write("send\n");
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
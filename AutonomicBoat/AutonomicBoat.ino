#include <SoftwareSerial.h>
#include <TinyGPS++.h>
#include <math.h>

#define EARTH_RADIUS 6371.0
#define MINIMAL_DIFFERENCE_LOCALIZATION 30
#define LOCALIZATION_ASSIGN 1
#define INTERVAL_SEND_DATA 200

// ********************************************************************************
// *********************IMPLEMENTACJA TYLKO DO TESTOW******************************

#define GPS_COURSE_ASSIGN 1
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

// ********************************************************************************
// *********************IMPLEMENTACJA TYLKO DO TESTOW******************************

double previousGpsCourse = 400;
bool newGpsCourse = false;

// ********************************************************************************

void setup() {
    Serial2.begin(9600); // gps
    Serial3.begin(57600); // radionadanjnik
}

void serialEvent1() {
  while (Serial1.available()) {
    if (gps.encode(Serial2.read())) {
      if (gps.location.isValid()) {
        // localization = String(gps.location.lat(), 5)+","+String(gps.location.lng(), 5);
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
    }
  }

  // OBSLUGA DANYCH KURSU Z GPS JESLI SIE POJAWILY
  if(newGpsCourse) {
    newGpsCourse = false;
    if(newGpsCourseHandler()) {
      appendData(GPS_COURSE_ASSIGN + "_" + String(gpsCourse) + "_");
    }
  }

  // WYSYLANIE DANYCH
  if(dataBuffer.length() > 0) {
    sendDataIfNecessary();
  }
}

void sendDataIfNecessary() {
  currentMillis = millis();
  if (currentMillis - previousMillis >= INTERVAL_SEND_DATA) {
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
  }
}

void appendData(String data) {
  if (dataBuffer.length() > 0) {
    dataBuffer += ";";
  }
  dataBuffer += data;
}

bool newLocalizationHandler() {
  if(calculateCmDistance() > MINIMAL_DIFFERENCE_LOCALIZATION) {
      oldLat = newLat;
      oldLng = newLng;
      return true;
  }
  return false;
}

double calculateCmDistance() {
    double lat1Rad = newLat * M_PI / 180.0;
    double lon1Rad = newLng * M_PI / 180.0;
    double lat2Rad = oldLat * M_PI / 180.0;
    double lon2Rad = oldLng * M_PI / 180.0;
    double dLat = lat2Rad - lat1Rad;
    double dLon = lon2Rad - lon1Rad;
    double a = sin(dLat / 2) * sin(dLat / 2) +
               cos(lat1Rad) * cos(lat2Rad) *
               sin(dLon / 2) * sin(dLon / 2);
    double c = 2 * atan2(sqrt(a), sqrt(1 - a));
    return EARTH_RADIUS * c * 100000.0;
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







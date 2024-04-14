#include <SoftwareSerial.h>
#include <LiquidCrystal.h>
#include <TinyGPS++.h>
#include <Adafruit_BNO08x.h>
#include <math.h>

//-------------wyswietlacz
LiquidCrystal lcd(12, 11, 5, 4, 3, 2);
//-------------

// gps
TinyGPSPlus gps;

// magnometr
Adafruit_BNO08x bno08x(-1);
sh2_SensorValue_t sensorValue;

double SENSOR_COURSE_ACCURACY = 3;

struct euler_t {
  float yaw;
  float pitch;
  float roll;
} ypr;

void setup() {
  Serial3.begin(57600); // radionadanjnik
  Serial2.begin(9600); // gps
  Serial.begin(115200);

  // lcd
  lcd.begin(16,2);
  setSensor();
  setReports();
  delay(1000);
}

// dane do wyslania
String sent="";
String localization="0";
String previousLocalization="0";
double sensorCourse=-1;
double previousCourse=-1;
double gpsCourse=-1;
double previousGpsCourse=-1;

long counter=0;//szybkosc odczytu danych nawigacyjnych (GPS)
long counter2=0;//szybkosc odczytu danych z czujnika do kursu
int temp1=0;

void loop() {
//     readLocalization();
//              lcd.setCursor(0,0);
//         lcd.print(localization);
  
//  // odczyt danych i sprawdzenie czy cos sie zmienilo
  if(counter>50000)
  {

     readLocalization();
//         Serial.println(localization);
     if(strcmp(localization.c_str(), "INVALID")!=0 && strcmp(previousLocalization.c_str(), localization.c_str())!=0)
     {
         lcd.setCursor(0,0);
         lcd.print(localization);
         temp1=1;
         previousLocalization=localization;
     }
     if(gpsCourse!=-1 && gpsCourse!=previousGpsCourse && abs(gpsCourse - previousGpsCourse)>SENSOR_COURSE_ACCURACY)
     {
         lcd.setCursor(8,1);
         lcd.print(gpsCourse);
         temp1=1;
         previousGpsCourse=gpsCourse;
     }
          
     counter=0;
  }

  if(counter2>200000) {
     sensorRead();             
     if(sensorCourse!=-1 && sensorCourse!=previousCourse && abs(sensorCourse - previousCourse)>SENSOR_COURSE_ACCURACY) 
     {
         lcd.setCursor(0,1);
         lcd.print(sensorCourse);
         temp1=1;
         previousCourse=sensorCourse;
     }
     counter2=0;
  }

  // wyslanie danych
  if(temp1==1){
    sent = String(localization)+"_"+String(sensorCourse)+"_"+String(gpsCourse);
    Serial3.print(sent);
    temp1=0;

    Serial.println("Wsylano: " + sent);
    
    sent="";
  }

  counter++;
  counter2++;
}

void readLocalization(){
    while (Serial2.available() > 0)
      if (gps.encode(Serial2.read()))
      {
        if (gps.location.isValid())
        {
//          Serial.println("location valid");
          localization=String(gps.location.lat(), 5)+","+String(gps.location.lng(), 5);
//                    lcd.setCursor(0,0);
//                    lcd. clear();
//          lcd.print(localization);
        }
        else
        {
//          Serial.println("location invalid");
          localization="INVALID";
        }
        if(gps.course.isValid()) 
        {
          gpsCourse=gps.course.deg();
        } 
        else 
        {
          gpsCourse=-1;
        }
      }
}

void setSensor() {
  if (!bno08x.begin_I2C()) {
    Serial.println("Failed to find BNO08x chip");
    while (1) {
      delay(10);
    }
    bno08x.hardwareReset();
  }
}


void setReports(void) {
  if (!bno08x.enableReport(SH2_ROTATION_VECTOR)) {
    Serial.println("Could not enable rotation vector");
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
//  delay(100);

  if (bno08x.wasReset()) {
    Serial.print("sensor was reset ");
    setReports();
  }
//
//  if (!bno08x.getSensorEvent(&sensorValue)) {
//    return;
//  }

  if (bno08x.getSensorEvent(&sensorValue)) {
    switch (sensorValue.sensorId) {
      case SH2_ROTATION_VECTOR:
        quaternionToEulerRV(&sensorValue.un.rotationVector, &ypr, true);
        sensorCourse=ypr.yaw;
        break;
    }
  }
}

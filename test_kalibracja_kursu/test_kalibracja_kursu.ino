#include <SoftwareSerial.h>
#include <LiquidCrystal.h>
#include <TinyGPS++.h>
#include <Adafruit_BNO08x.h>

//-------------wyswietlacz
LiquidCrystal lcd(12, 11, 5, 4, 3, 2);
//-------------

// gps
TinyGPSPlus gps;

// magnometr
Adafruit_BNO08x bno08x(-1);
sh2_SensorValue_t sensorValue;

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
String localization="";
String previousLocalization="";
String sensorCourse="";
String previousCourse="";
String gpsCourse="";
String previousGpsCourse="";

long counter=0;//szybkosc wysylania danych
int temp1=0;

void loop() {
  // odczyt danych i sprawdzenie czy cos sie zmienilo
  if(counter>100000)
  {
     readLocalization();
     if(strcmp(localization.c_str(), "INVALID")!=0 && strcmp(previousLocalization.c_str(), localization.c_str())!=0)
     {
         lcd.setCursor(0,0);
         lcd.print(localization);
         temp1=1;
         previousLocalization=localization;
     }
     if(strcmp(gpsCourse.c_str(), "INVALID")!=0 && strcmp(previousGpsCourse.c_str(), gpsCourse.c_str())!=0)
     {
         lcd.setCursor(8,1);
         lcd.print(gpsCourse);
         temp1=1;
         previousGpsCourse=gpsCourse;
     }

     sensorRead();             
     if(strcmp(sensorCourse.c_str(), "")!=0 && strcmp(sensorCourse.c_str(), previousCourse.c_str())!=0) 
     {
         lcd.setCursor(0,1);
         lcd.print(sensorCourse);
         temp1=1;
         previousCourse=sensorCourse;
     }
          
     counter=0;
  }

  // wyslanie danych
  if(temp1==1){
    sent = String(localization)+"_"+String(sensorCourse)+"_"+String(gpsCourse);
    Serial3.print(sent);
    temp1=0;
    sent="";
  }

  counter++;
}

void readLocalization(){
    while (Serial2.available() > 0)
      if (gps.encode(Serial2.read()))
      {
        if (gps.location.isValid())
        {
          localization=String(gps.location.lat(), 4)+","+String(gps.location.lng(), 4);
        }
        else
        {
          localization="INVALID";
        }
        if(gps.course.isValid()) 
        {
          gpsCourse=String(gps.course.deg(), 4);
        } 
        else 
        {
          gpsCourse="INVALID";
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
      ypr->pitch *= RAD_TO_DEG;
      ypr->roll *= RAD_TO_DEG;
    }
}

void quaternionToEulerRV(sh2_RotationVectorWAcc_t* rotational_vector, euler_t* ypr, bool degrees) {
    quaternionToEuler(rotational_vector->real, rotational_vector->i, rotational_vector->j, rotational_vector->k, ypr, degrees);
}

void sensorRead() {
  delay(100);

  if (bno08x.wasReset()) {
    Serial.print("sensor was reset ");
    setReports();
  }

  if (!bno08x.getSensorEvent(&sensorValue)) {
    return;
  }

  switch (sensorValue.sensorId) {
    case SH2_ROTATION_VECTOR:
      quaternionToEulerRV(&sensorValue.un.rotationVector, &ypr, true);
      sensorCourse=String(ypr.yaw, 2);
      break;
  }
}

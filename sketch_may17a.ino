#include <SoftwareSerial.h>
#include <Servo.h>
#include <LiquidCrystal.h>
#include <TinyGPS++.h>

//-------------wyswietlacz
LiquidCrystal lcd(12, 13, 3, 2, 7, 4);
//-------------

// do czujnikow odleglosci
//unsigned char data[4]={};
//unsigned char data2[4]={};
//int distance2;
//int distance4;

// gps
TinyGPSPlus gps;

//int Trig = 9;  //pin 2 Arduino połączony z pinem Trigger czujnika
//int Echo = 8;  //pin 3 Arduino połączony z pinem Echo czujnika

// silniki
int engines[2] = {0,0};
int speeds[2] = {0,0};
int speed = 1500;

// swiatlo
int light=0;;
int realLight=0;

//int CM=0;

// silniki
Servo s1;
Servo s2;

// zapadki
Servo flap1;
Servo flap2;
int servo1=0;
int servo2=0;
int f1=1700;
int f2=1400;

// swiatlo
Servo lighting;

void setup() {
  Serial3.begin(57600); // radionadanjnik
  //Serial1.begin(9600); // czujnik odl 1
  Serial2.begin(9600); // gps
  //Serial3.begin(9600); // czujnik odl 2

  // silniki
  s1.attach(5);
  s1.writeMicroseconds(speed);
  s2.attach(6);
  s2.writeMicroseconds(speed);

  // zapadki
  flap1.attach(10);
  flap1.writeMicroseconds(f1);
  flap2.attach(11);
  flap2.writeMicroseconds(f2);

  //swiatlo
  lighting.attach(8);
  lighting.writeMicroseconds(1200);

  // lcd
  lcd.begin(16,2);
  delay(1000);
}

// do odczytu danych
const uint8_t buff_len = 7; // buffer size
char buff[buff_len]; // buffer
uint8_t buff_i = 0; // buffer index
uint8_t arr_i = 0; // number array index


int counter=0;// szybkosc zwiekszania mocy na silnikach
int y[5]={0,0,0,0,0};// do odbioru dancyh
int checker[2]={0,0};//czy zczytywac z czujnika - porusza sie lodz
int temp=0;//czy zmienilo sie oswietlenie i nie zostalo jeszcze wyslane?
int temp1=0;//czy trzeba wyslac dane z czujnikow?

// dane do wyslania
String sent="";
String localization="";
String previousLocalization="";

long counter2=0;//szybkosc wysylania danych
long counterLight=0;//szybkosc zmiany ostwietlenia

// czy zmienily sie dane pomiarowe
//int ostatnioWyslane1=0;
//int ostatnioWyslane2=0;

void loop() {
  
  if(Serial3.available()>0)
  {
    while (Serial3.available() > 0) 
    {
        char c = Serial3.read();
        if (buff_i < buff_len-1) 
        { 
          readData(c);
        }
    }
  }

  // szybczkosc zwiekszania mocy na silniki
  if(counter>10000)
  {
    //jesli dostano dane i jeszcze nie zostaly ustawione na maksa to wykonaj
    if((y[0]==1 || y[1]==1)){
      checker[0]=1;
      checker[1]=1;
      ustawPredkosc();
    }

    //nadaj oswietlenie jesli realLight!=light
    if(counterLight>30000){
      if(y[2]==1){
        ustawOswietlenie();
        temp=1;
        counterLight=0;
      }
    }
   

    //jesli sa informacje to wlacz zapadke (jesli przeslano 1)
    if(y[3]==1 || y[4]==1){
      runFlaps();
      servo1=0;
      servo2=0;
    }
    
    //prawdziwe gdy predkosc sie zmienia lub gdy juz jest maks zawsze
    if(checker[0]==1 || checker[1]==1){
       //czujnikOdleglosci();
       //czujnikOdleglosci2();
       readLokalization();
       // pomiar_odleglosci();

       // czy ma byc wyslane?
       if(strcmp(localization.c_str(), "INVALID")!=0 && strcmp(previousLocalization.c_str(), localization.c_str())!=0)
       {
           lcd.setCursor(0,1);
           lcd.print(localization);
           temp1=1;
       }
        
     }
     
     counter=0;
  }

  // wyslanie danych zwiazanych z oswietleniem
  if(temp==1 && y[2]!=1){
    sent = String(realLight)+"_"+String(localization);
    Serial3.print(sent);
    temp=0;
    sent="";
  }
  
  //wyslanie danych gdy lodz sie porusza i lokalizacja sie zmienila
  if(temp1==1 && counter2>80000){
     sent = String(realLight)+"_"+String(localization);
     previousLocalization=localization;
     Serial3.print(sent);
     sent="";
     temp1=0;
     counter2=0;
   }

  counterLight++; 
  counter2++;
  counter++;
}

void readData(char c) //funkcja w ktorej są zadania rozpoznane z polecenia buff
{
   if (c == '_') 
   { 
      buff[buff_i++] = 0;
      buff_i = 0;
      if(arr_i==4){
        servo2=atoi(buff);
      }
      else if(arr_i==3){
        servo1=atoi(buff);
      }
      else if(arr_i==2){
        light=atoi(buff);
      }
      else{
        engines[arr_i] = atoi(buff);
      }
      arr_i++;
      if (arr_i == 5) 
      { 
        y[0]=1;
        y[1]=1;
        if(light!=realLight){
          y[2]=1;
        }
        if(servo1!=0){
           y[3]=1;
        }
        if(servo2!=0){
           y[4]=1;
        }
        arr_i = 0;
      }
   }
   else if (c == '-' || ('0' <= c && c <= '9'))
   {
       buff[buff_i++] = c;
   }
}

void ustawPredkosc(){
    for(int i=0; i<2; i++)
    {
      if(engines[i]<0 && speeds[i]>engines[i])
      {
        speeds[i]=speeds[i]-1;
      }
      else if(engines[i]>0 && speeds[i]<engines[i])
      {
        speeds[i]=speeds[i]+1;
      }
      else if(engines[i]==0)
      {
        speeds[i]=0;
        y[i]=0;
        checker[i]=0;//gdy predkosc zero to nie zczytuj z czujnika danych
      }
      else
      {
        y[i]=0;
      }
    }

    speed=map(speeds[0], -100, 100, 1000, 2000);
    s1.writeMicroseconds(speed);
       
    lcd.setCursor(0, 0);         
    lcd.print(speed);

    speed=map(speeds[1], -100, 100, 1000, 2000);
    s2.writeMicroseconds(speed);
    
    lcd.setCursor(8, 0);         
    lcd.print(speed);
}

void ustawOswietlenie(){
  if(light==-1){
    y[2]=0;
  }
  else if(realLight<light){
    realLight=realLight+5;
  }
  else if(realLight>light){
    realLight=realLight-5;
  }
  else{
    y[2]=0;
  }
  //lcd.setCursor(0, 1);
  //lcd.print(realLight);
  lighting.writeMicroseconds(map(realLight, 0, 100, 1210, 2000));
}

void runFlaps(){
  if(servo1==1){
    if(f1==1700){
      f1=1250;
    }
    else{
      f1=1700;
    }
    flap1.writeMicroseconds(f1);
    //delay(500);
    //flap1.writeMicroseconds(1200);
    //delay(500);
    //flap1.writeMicroseconds(1500);
    //lcd.setCursor(3, 1);
    //lcd.print(servo1);
    y[3]=0;
  }
  if(servo2==1){
    if(f2==1400){
      f2=1850;
    }
    else{
      f2=1400;
    }
    flap2.writeMicroseconds(f2);
    //delay(500);
    //flap2.writeMicroseconds(1800);
    //delay(500);
    //flap2.writeMicroseconds(1500);
    //lcd.setCursor(3, 1);
    //lcd.print(servo2);
    y[4]=0;
  }
}

//void czujnikOdleglosci(){
//  float distance;
//  do{
//     for(int i=0;i<4;i++)
//     {
//       data[i]=Serial1.read();
//     }
//  }while(Serial1.read()==0xff);
//
//  Serial1.flush();
//
//  if(data[0]==0xff)
//    {
//      int sum;
//      sum=(data[0]+data[1]+data[2])&0x00FF;
//      if(sum==data[3])
//      {
//        distance=(data[1]<<8)+data[2];
//        if(distance>280)
//        {
//          distance2=(distance)/10;
//        }
//      }
//     }
//}
//
//void czujnikOdleglosci2(){
//  float distance3;
//  do{
//     for(int i=0;i<4;i++)
//     {
//       data2[i]=Serial3.read();
//     }
//  }while(Serial3.read()==0xff);
//
//  Serial3.flush();
//
//  if(data2[0]==0xff)
//    {
//      int sum2;
//      sum2=(data2[0]+data2[1]+data2[2])&0x00FF;
//      if(sum2==data2[3])
//      {
//        distance3=(data2[1]<<8)+data2[2];
//        if(distance3>280)
//        {
//          distance4=(distance3)/10;
//        }
//      }
//     }
//}

void readLokalization(){
    lcd.setCursor(3, 1);
    lcd.print("1");
    while (Serial2.available() > 0)
      if (gps.encode(Serial2.read()))
      {
        if (gps.location.isValid())
        {
          localization=String(gps.location.lat(), 4)+","+String(gps.location.lng(), 4);
        }
        else
        {
          //lcd.setCursor(0, 1);
    //lcd.print("1");
          localization="INVALID";
        }
      }
}

//void pomiar_odleglosci()
//{
//  digitalWrite(Trig, LOW);        //ustawienie stanu wysokiego na 2 uS - impuls inicjalizujacy - patrz dokumentacja
//  delayMicroseconds(2);
//  digitalWrite(Trig, HIGH);       //ustawienie stanu wysokiego na 10 uS - impuls inicjalizujacy - patrz dokumentacja
//  delayMicroseconds(10);
//  digitalWrite(Trig, LOW);
//  digitalWrite(Echo, HIGH); 
//  long CZAS = pulseIn(Echo, HIGH);
//  CM = CZAS / 58;                //szerokość odbitego impulsu w uS podzielone przez 58 to odleglosc w cm - patrz dokumentacja
//  if(CM<20){
//    CM=0;
//  }
//}

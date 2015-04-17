#include <SoftwareSerial.h>
 
SoftwareSerial mySerial(0, 1);//rx, tx
int dataFromBT;
const int analogInPin = A0;
int sensorValue = 0;
int outputValue = 0;
 
void setup() {
  Serial.begin(9600);
  Serial.println("LEDOnOff Starting...");
  mySerial.begin(9600);
  pinMode(13, OUTPUT);  
}
 
void loop() {
  if (mySerial.available())
    dataFromBT = mySerial.read();
 
  if (dataFromBT == '0') {
    digitalWrite(13, LOW);
  } else if (dataFromBT == '1') {
    digitalWrite(13, HIGH);
  } else if (dataFromBT == '2') {
    sensorValue = analogRead(analogInPin);
    outputValue = map(sensorValue, 0, 1023, 0, 255);
    Serial.println(outputValue);
    delay(1000);
  }else if (dataFromBT == '3') {
  }
}

/*
  Arduino sample code of UnlimitedHand 
  for Unity, UnrealEngine and Processing, 
  ver 0043 a
  
  This code is used with:
  Arduino IDE ver 1.6.9 - 1.6.11 to upload this code to UnlimitedHand,
  or an Unity Code "UH.cs" in UH_Unity0041*.zip 
  or an Processing Code in PUnlimitedHand0027.zip ~ PUnlimitedHand003*.zip
  or an UnrealEngine Project in UH4UE00**.zip
  Please visit http://dev.UnlimitedHand.com
*/
#include <Kalman.h>
#include <UH.h>
UH uh;

/* Objects for EMS(Electric Mucsle Stimulation) */
int currentVol;                 //Voltage for the EMS
int stimulationTime = 60;       //Stimulation Time for the EMS
bool isHighSpeedMode = false;   //Flag for High Speed Mode

/* Objects for the Acceleration and the  Gyro */
int accelRaw[3];
int gyroRaw[3];
double temp;

/* Objects for Quaternion */
float quaternionData[4];

///////////////////////////////////////////////////////////////////////////////
//   SET UP
///////////////////////////////////////////////////////////////////////////////
void setup() { 
  delay(500);                     // time to start serial monitor
  Serial.begin(115200);           // Serial.println("UH Start");
  
  //according to docs this holds until serial is open, this does not appear to work
  while (!Serial);  

  uh.setupVibrationMotor();        // set up the Vibration Motor in UnlimitedHand 
  uh.setupAcceleGyro();            // start the I2C connection for the Acceleration and the Gyro
  
  uh.readRawAccelValues(accelRaw); // read the raw accelaration values for the Kalman setup
  uh.readRawGyroValues(gyroRaw);   // read the raw Gyro values for the Kalman setup
  uh.initAccelGyroKalman(micros(), accelRaw); // setup the Kalman
  
  uh.initEMS();                    // setup the EMS(Electric Muscle Stimulation) to output the haptic feeling 
  currentVol = 12;                  // define the voltage of the EMS
  
  uh.initPR();                     // setup the Photo-reflectors to detect hand movements.
  
  //printHelp();                     // Please delete it, if you don't need the serial connection help
}


///////////////////////////////////////////////////////////////////////////////
// LOOP
///////////////////////////////////////////////////////////////////////////////
void loop() {
  uh.updateEMS(); // you can choose also "updateEMS_POWERFUL();"
  
  if(isHighSpeedMode){uh.checkXYZPR(); }
}


///////////////////////////////////////////////////////////////////////////////
// Original Function: To Print the Help text
///////////////////////////////////////////////////////////////////////////////
void printHelp(){
  Serial.println("[HOW TO USE THIS ARDUINO CODE ON THE SERIAL MONITOR]");
  Serial.println("a: display the raw values of the acceleration and Gyro sensors");
  Serial.println("A: display the forearm angles");
  Serial.println("b: move the vibration motor for 0.3 sec");
  Serial.println("B: move the vibration motor for 1 sec");
  Serial.println("c: check the Photo-reflectors values");
  Serial.println("d: turn on the VibrationMotor");
  Serial.println("e: turn off the VibrationMotor");
  Serial.println("s: High Speed Mode");
  Serial.println("t: increase the bootup level for High Pluse");
  Serial.println("u: decrease the bootup level for High Pluse");
  Serial.println(" ");
  Serial.println("0~7:give the EMS(Electric Muscle Stimulation)");
  Serial.println("h: increase the voltage of the EMS.,  l: decrease the voltage of the EMS");
  Serial.println("m: increase the stimulation time of the EMS.,  n: decrease the stimulation time of the EMS");
  Serial.println(" ");
//  Serial.println("T: change the TEST mode");
  Serial.println("------------------------------------------------------------");
  Serial.println("Please input any charactor:");

}




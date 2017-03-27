//Test for Vibration Motor
void test1() {
  Serial.println("Entering TEST MODE 1-3");
  uh.moveVibrationMotor(300);
  delay(100);
       
  while (tCount < 2 ) {
    uh.checkXYZPR_test();
    delay(1000);

    serialEvent();
    if(1<tCount){
      break;
    }
  }
  
}

//Test for EMS
void test2() {
  Serial.println("Entering TEST MODE 4");
  while (tCount < 3) {
    for (int i = 0; i < 8; i++) {
      stimulation(i);
      delay(500);
      serialEvent();
    }
    serialEvent();
    if(2<tCount){
      break;
    }
  }
}

void stimulation(int EMSCh) {
  if (EMSCh == 0) {
  // EMSTimeCount = stimuTimeCount;
         uh.setStimulationChannel(0);
         uh.setStimulationTime();
         uh.setStimulationVoltage(currentVol);
         for(int i=0;i<20;i++){uh.keepVoltage(currentVol);}
         Serial.print("Vol:"); Serial.print(currentVol);
         Serial.print(", EMS num: "); Serial.println(uh.currentEMSChannel);  
  } else if (EMSCh == 1) {
   // EMSTimeCount =stimuTimeCount;
        uh.setStimulationChannel(1);
         uh.setStimulationTime();
         uh.setStimulationVoltage(currentVol);
         for(int i=0;i<20;i++){uh.keepVoltage(currentVol);}
         Serial.print("Vol:"); Serial.print(currentVol);
         Serial.print(", EMS num: "); Serial.println(uh.currentEMSChannel);  
  } else if (EMSCh == 2) {
   // EMSTimeCount =stimuTimeCount;
         uh.setStimulationChannel(2);
         uh.setStimulationTime();
         uh.setStimulationVoltage(currentVol);
         for(int i=0;i<20;i++){uh.keepVoltage(currentVol);}
         Serial.print("Vol:"); Serial.print(currentVol);
         Serial.print(", EMS num: "); Serial.println(uh.currentEMSChannel);  
  } else if (EMSCh == 3) {
  //  EMSTimeCount =stimuTimeCount;
         uh.setStimulationChannel(3);
         uh.setStimulationTime();
         uh.setStimulationVoltage(currentVol);
         for(int i=0;i<20;i++){uh.keepVoltage(currentVol);}
         Serial.print("Vol:"); Serial.print(currentVol);
         Serial.print(", EMS num: "); Serial.println(uh.currentEMSChannel);  
  } else if (EMSCh == 4) {
   // EMSTimeCount =stimuTimeCount;
         uh.setStimulationChannel(4);
         uh.setStimulationTime();
         uh.setStimulationVoltage(currentVol);
         for(int i=0;i<20;i++){uh.keepVoltage(currentVol);}
         Serial.print("Vol:"); Serial.print(currentVol);
         Serial.print(", EMS num: "); Serial.println(uh.currentEMSChannel);   
  } else if (EMSCh == 5) {
   // EMSTimeCount =stimuTimeCount;
         uh.setStimulationChannel(5);
         uh.setStimulationTime();
         uh.setStimulationVoltage(currentVol);
         for(int i=0;i<20;i++){uh.keepVoltage(currentVol);}
         Serial.print("Vol:"); Serial.print(currentVol);
         Serial.print(", EMS num: "); Serial.println(uh.currentEMSChannel);     
   
  } else if (EMSCh == 6) {
   // EMSTimeCount =stimuTimeCount;
         uh.currentEMSChannel = 6;
         for(int i=0;i<20;i++){uh.keepVoltage(currentVol);}
         Serial.print("Vol:"); Serial.print(currentVol);
         Serial.print(", EMS num: "); Serial.println(uh.currentEMSChannel); 
           
  } else if (EMSCh == 7) {
  //  EMSTimeCount =stimuTimeCount;
        uh.setStimulationChannel(6);
         uh.setStimulationTime();
         uh.setStimulationVoltage(currentVol);
         for(int i=0;i<20;i++){uh.keepVoltage(currentVol);}
         Serial.print("Vol:"); Serial.print(currentVol);
         Serial.print(", EMS num: "); Serial.println(uh.currentEMSChannel);  
  }
  for(int i=0;i<40;i++){
    //stimulateD();
    uh.updateEMS();
    
  }
  delay(3000);
}

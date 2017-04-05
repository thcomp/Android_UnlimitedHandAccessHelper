/* Objects for Serial Connection   */
int bootupLevel = 15;
int inByte = 0;
int tCount = 0;                 //Object for the Test Mode Connt 

void serialEvent() {
  if(Serial.available()>0){
    inByte = Serial.read();

    if(inByte <= 0x1F){
      bool readPR = inByte & 1;
      bool readAngle = inByte & 2;
      bool readAccel = inByte & 4;
      bool readGyro = inByte & 8;
      bool readQuaternion = inByte & 16;

      if(readPR){
        Serial.print("_PR:");
        for(int i=0; i<8; i++){
          if(i != 0){
            Serial.print(",");
          }
          Serial.print(uh.readPR(i), DEC);
        }
      }
      if(readAngle || readAccel || readGyro){
        int angXYZ[3];

        if(readAngle || readAccel){
          uh.readRawAccelValues(accelRaw);
          if(readAccel){
            Serial.print("_ACCEL:");
            for(int i=0; i<(sizeof(accelRaw) / sizeof(int)); i++){
              if(i != 0){
                Serial.print(",");
              }
              Serial.print(accelRaw[i], DEC);
            }
          }
        }
        if(readAngle || readGyro){
          uh.readRawGyroValues(gyroRaw);
          if(readGyro){            
            Serial.print("_GYRO:");
            // gyroはこれまでaccelとtemperatureの分のデータ（空）を返す（整合性のため）
            for(int i=0; i<(sizeof(accelRaw) / sizeof(int)); i++){
              if(i != 0){
                Serial.print(",");
              }
              Serial.print(0, DEC);
            }
            Serial.print(",");
            Serial.print(0, DEC);
            for(int i=0; i<(sizeof(gyroRaw) / sizeof(int)); i++){
              Serial.print(",");
              Serial.print(gyroRaw[i], DEC);
            }
          }
        }
        if(readAngle){
          uh.readAccelGyro_XYZ_Kalman(accelRaw, gyroRaw, angXYZ);
          Serial.print("_ANGLE:");
          for(int i=0; i<(sizeof(angXYZ) / sizeof(int)); i++){
            if(i != 0){
              Serial.print(",");
            }
            Serial.print(angXYZ[i], DEC);
          }
        }
      }
      if(readQuaternion){
        uh.updateQuaternion(quaternionData);
        Serial.print("_QUAT:");
        for(int i=0; i<sizeof(quaternionData); i++){
          if(i != 0){
            Serial.print(",");
          }
          Serial.print(quaternionData[i], DEC);
        }
      }

      if(!readPR && !readAngle && !readAccel && !readGyro && !readQuaternion){
        Serial.println("_");
      }else{
        Serial.println("");
      }
    }else{
      switch (inByte) {
        case 48:                    //EMS 0
           uh.setStimulationChannel(0);
           uh.setStimulationTime(stimulationTime);
           uh.setStimulationVoltage(currentVol);
           for(int i=0;i<bootupLevel;i++){uh.keepVoltage(currentVol);}
           //Serial.print("Vol:"); Serial.print(currentVol);
           //Serial.print(", EMS num: "); Serial.println(uh.currentEMSChannel);  
           break;
        case 49:                    //EMS 1
           uh.setStimulationChannel(1);
           uh.setStimulationTime(stimulationTime);
           uh.setStimulationVoltage(currentVol);
           for(int i=0;i<bootupLevel;i++){uh.keepVoltage(currentVol);}
           //Serial.print("Vol:"); Serial.print(currentVol);
           //Serial.print(", EMS num: "); Serial.println(uh.currentEMSChannel);          
           break;
        case 50:                    //EMS 2
           uh.setStimulationTime(stimulationTime);
           uh.setStimulationChannel(2);
           uh.setStimulationVoltage(currentVol);
           for(int i=0;i<bootupLevel;i++){uh.keepVoltage(currentVol);}
           //Serial.print("Vol:"); Serial.print(currentVol);
           //Serial.print(", EMS num: "); Serial.println(uh.currentEMSChannel);         
           break;
        case 51:                    //EMS 3
           uh.setStimulationTime(stimulationTime);
           uh.setStimulationChannel(3);
           uh.setStimulationVoltage(currentVol);
           for(int i=0;i<bootupLevel;i++){uh.keepVoltage(currentVol);}
           //Serial.print("Vol:"); Serial.print(currentVol);
           //Serial.print(", EMS num: "); Serial.println(uh.currentEMSChannel);            
           break;
        case 52:                    //EMS 4
           uh.setStimulationTime(stimulationTime);
           uh.setStimulationChannel(4);
           uh.setStimulationVoltage(currentVol);
           for(int i=0;i<bootupLevel;i++){uh.keepVoltage(currentVol);}
           //Serial.print("Vol:"); Serial.print(currentVol);
           //Serial.print(", EMS num: "); Serial.println(uh.currentEMSChannel);            
           break;
        case 53:                    //EMS 5
           uh.setStimulationTime(stimulationTime);
           uh.setStimulationChannel(5);
           uh.setStimulationVoltage(currentVol);
           for(int i=0;i<bootupLevel;i++){uh.keepVoltage(currentVol);}
           //Serial.print("Vol:"); Serial.print(currentVol);
           //Serial.print(", EMS num: "); Serial.println(uh.currentEMSChannel);            
           break;
        case 54:                     //EMS 6
           uh.setStimulationTime(stimulationTime);
           uh.setStimulationChannel(6);
           uh.setStimulationVoltage(currentVol);
           for(int i=0;i<bootupLevel;i++){uh.keepVoltage(currentVol);}
           //Serial.print("Vol:"); Serial.print(currentVol);
           //Serial.print(", EMS num: "); Serial.println(uh.currentEMSChannel);            
           break;
        case 55:                    //EMS 7
           uh.setStimulationTime(stimulationTime);
           uh.setStimulationChannel(7);
           uh.setStimulationVoltage(currentVol);
           for(int i=0;i<bootupLevel;i++){uh.keepVoltage(currentVol);}
           //Serial.print("Vol:"); Serial.print(currentVol);
           //Serial.print(", EMS num: "); Serial.println(uh.currentEMSChannel);            
           break;
        case 97:                                    //a(Acceleration and Gyro)
            uh.readRawAccelValues(accelRaw);
            uh.readRawGyroValues(gyroRaw);
            temp = uh.readTemperature();
            Serial.print(accelRaw[0], DEC);//AcX = 
            Serial.print("+"); Serial.print(accelRaw[1], DEC);// | AcY =
            Serial.print("+"); Serial.print(accelRaw[2], DEC);// | AcZ = 
            Serial.print("+"); Serial.print(temp, 3);  //temperature
            Serial.print("+"); Serial.print(gyroRaw[0], DEC);
            Serial.print("+"); Serial.print(gyroRaw[1], DEC);
            Serial.print("+"); Serial.println(gyroRaw[2], DEC);
            delay(1);
           break;
        case 65:                                    //A(Acceleration and Gyro) with kalman
           uh.checkAccelGyro_XYZ_Kalman();
           break;
        case 98:                                    //b(move the vibration motor)
           uh.moveVibrationMotor(300);
           Serial.print("move the Vibration Motor. Time(sec):");
           Serial.println(300);
           break;
        case 66:                                    //B(move the vibration motor)
           uh.moveVibrationMotor(1000);
           Serial.print("move the Vibration Motor. Time(sec):");
           Serial.println(1000);
           break;
       case 67:                                    //C: (Acceleration and Gyro) with kalman and PRs
           uh.checkKalmanXYZPR();
           break;
        case 99:                                    //c(check the Photo-reflectors)
          uh.checkPR();
          break;
        case 100:                                   //d
          uh.onVibrationMotor();
          break;
        case 101:                                    //e
          uh.offVibrationMotor();
          break;
        case 102:                                    //f
          for(int p=0;p<20;p++){
            uh.checkRawAccelValues();
            uh.checkTemperature();
            uh.checkRawGyroValues();
            Serial.print(" ,PR, ");
            uh.checkPR();
            Serial.println(" ");
          }
          break;
        case 103:                                    //g
          //checkPR();
          break;
        case 104:                                    //ｈ
           if(currentVol < 12){
             currentVol = currentVol + 1;
             Serial.print("Vol:");
             Serial.println(currentVol);
           }else if(12 <= currentVol){
             Serial.println("Vol:12, it is maximum of the EMS Voltage");
           }
           break;
        case 108:                                    //l
           if(0 < currentVol){
             currentVol = currentVol - 1;
             Serial.print("Vol:");
             Serial.println(currentVol);
           }else if(currentVol <= 0){
             Serial.println("Vol:0, it is mimimum of the EMS Voltage");
           }
           break;
  
        ////stimuTimeCount///////////////////////////////////////   
        case 109:                                    //m
           if(stimulationTime<=200){
             stimulationTime = stimulationTime + 20;
             Serial.print("stimulation Time:");
             Serial.println(stimulationTime);
           }else{
            Serial.println("Current stimulation time is MAX: 2000 mSec(2 sec)");
           }
           
           break;
        case 110:                                    //n
           if(20<stimulationTime){
             stimulationTime = stimulationTime - 20;
             Serial.print("stimulationTime:");
             Serial.println(stimulationTime);
           }else{
            Serial.println("Current stimulation time is MINIMAM: 200 mSec(0.2sec)");
           }
           
           break;  
        case 111:          //o
          if(uh.stimuHighWid<250){
            uh.stimuHighWid += 50;
            Serial.print("stimuHighWid:");
             Serial.println(uh.stimuHighWid);
          }
          break;
        case 112:          //p
          if(100<uh.stimuHighWid){
            uh.stimuHighWid -= 50;
            Serial.print("stimuHighWid:");
             Serial.println(uh.stimuHighWid);
          }
          break;
        case 113:          //q
          uh.updateQuaternion(quaternionData);
          Serial.print(quaternionData[0]);Serial.print("+");
          Serial.print(quaternionData[1]);Serial.print("+");
          Serial.print(quaternionData[2]);Serial.print("+");
          Serial.println(quaternionData[3]);
          break;
        case 81:          //Q : print quaternion data and also the photo-reflectors' data
          uh.updateQuaternion(quaternionData);
          Serial.print(quaternionData[0]);Serial.print("+");
          Serial.print(quaternionData[1]);Serial.print("+");
          Serial.print(quaternionData[2]);Serial.print("+");
          Serial.print(quaternionData[3]);Serial.print("+");
          for(int i=0;i<PR_CH_NUM-1;i++){
            Serial.print(uh.readPR(i));Serial.print("+");
          }
           Serial.println(uh.readPR(PR_CH_NUM-1));
          break;
        case 114:         //r
          uh.resetQuaternion(quaternionData);
          Serial.println("reset Quaternion");
          Serial.print(quaternionData[0]);Serial.print("+");
          Serial.print(quaternionData[1]);Serial.print("+");
          Serial.print(quaternionData[2]);Serial.print("+");
          Serial.println(quaternionData[3]);
          break;
        
        case 83:            //S:print quaternion data, the photo-reflectors' data  and also Accel Gyro
          //quaternion data
          uh.updateQuaternion(quaternionData);
          Serial.print(quaternionData[0]);Serial.print("+");
          Serial.print(quaternionData[1]);Serial.print("+");
          Serial.print(quaternionData[2]);Serial.print("+");
          Serial.print(quaternionData[3]);Serial.print("+");
          
          // photo-reflectors' data
          for(int i=0;i<PR_CH_NUM;i++){
            Serial.print(uh.readPR(i));Serial.print("+");
          }//Serial.println(uh.readPR(PR_CH_NUM-1));
          
          //Accel Gyro data
          uh.readRawAccelValues(accelRaw);
          uh.readRawGyroValues(gyroRaw);
          temp = uh.readTemperature();
          Serial.print(accelRaw[0], DEC);Serial.print("+"); //   AcX = 
          Serial.print(accelRaw[1], DEC);Serial.print("+"); // | AcY =
          Serial.print(accelRaw[2], DEC);Serial.print("+"); // | AcZ = 
          Serial.print(temp, 3);Serial.print("+");          //temperature
          Serial.print(gyroRaw[0], DEC);Serial.print("+"); 
          Serial.print(gyroRaw[1], DEC);Serial.print("+"); 
          Serial.println(gyroRaw[2], DEC);
          delay(1);
          break;
        case 115:          //s High Speed Mode
          isHighSpeedMode = !isHighSpeedMode;
          break;
        case 116:           //t
          if(bootupLevel<=20){
            bootupLevel+=5;
            Serial.print("Current Bootup Level for High Pluse");Serial.println(bootupLevel);
          }
          break;
        case 117:           //u
          if(5<=bootupLevel){
            bootupLevel-=5;
            Serial.print("Current Bootup Level for High Pluse");Serial.println(bootupLevel);
          }  
          break;
        case 118: //v version
          {
            char sketch_name[] = "Serial_Unity_Quaternion__0043";
            Serial.print("Version:");Serial.println(sketch_name);// retrun the version
            break;
          }
  
  
          
       case 84:                    //TEST MODE <T>
           tCount++;
           if(tCount==1){
              test1(); // vibration, muscle motion sensor, accelerometer and gyro.
           }else if(tCount==2){
              test2(); // EMS
           }else{
              Serial.println("Exit TEST MODE...");
           }         
           break;
      
                   
        //default:
      }
     /*Serial.print("I received: ");
     Serial.println(inByte, DEC);*/
    }
  }
}


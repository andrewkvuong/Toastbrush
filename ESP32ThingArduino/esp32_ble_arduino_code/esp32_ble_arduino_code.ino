#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <queue>
#include <sstream>
#include <string>

BLECharacteristic *pCharacteristic;
BLEServer *pServer;
BLEService *pService;
bool deviceConnected = false;
bool ready_flag = true;
bool canceled_recieved = false;
bool data_processed = false;
float txValue = 0;

const int DIR1 = 5;
const int PUL1 = 25;
const int SLP1 = 22;
const int DRV1 = 19;

const int DIR2 = 15;
const int PUL2 = 26;
const int SLP2 = 23;
const int DRV2 = 18;

const int X_CAL = 35;
const int Y_CAL = 34;

const int RELAY = 27;

std::queue<std::string>* instructions;

int x;
int x_steps = 0;
int y_steps = 0;
int x_dir = -1;
int y_dir = -1;
int x_pos = 0;
int y_pos = 0;
unsigned int spd = 2000;
int packet_num = 0;
bool instruction_in_progress = false;
bool processing_request = false;
bool processing_instruction = false;
bool first_inst = false;
bool calibrating = false;

#define SERVICE_UUID           "6E400001-B5A3-F393-E0A9-E50E24DCCA9E" 
#define CHARACTERISTIC_UUID_RX "6E400002-B5A3-F393-E0A9-E50E24DCCA9E"
#define CHARACTERISTIC_UUID_TX "6E400003-B5A3-F393-E0A9-E50E24DCCA9E"

void send_str(const char* s)
{
    char txString[128];
    snprintf(txString, 128, s);

    pCharacteristic->setValue(txString);
    
    pCharacteristic->notify(); // Send the value to the app!
    Serial.print("*** Sent Value: ");
    Serial.print(txString);
    Serial.println(" ***");
}

class MyCallbacks: public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic *pCharacteristic) {
      std::string rxValue = pCharacteristic->getValue();

      if (rxValue.length() > 0) {
          ready_flag = false;
          ++packet_num;
        processing_request = true;
        digitalWrite(RELAY, HIGH);
        Serial.println("*********");
        Serial.print("Received Value: ");
        debug_number(rxValue.length());
        for (int i = 0; i < rxValue.length(); i++) {
          Serial.print(rxValue[i]);
        }

        if(rxValue == "CANCEL")
        {
          Serial.println("RECEIVED CANCEL");
          canceled_recieved = true;
          packet_num = 0;
          instruction_in_progress = false;
          processing_request = false;
          processing_instruction = false;
          digitalWrite(RELAY, LOW); 
          delete instructions;
          instructions = new std::queue<std::string>();
          calibrate();
          ready_flag = true;
          return;
        }
      
        Serial.println();

        std::stringstream sstr(rxValue);
        std::string code;
        calibrating = true;
        Serial.println("Adding instructions to queue");
        while(std::getline(sstr, code,'\n')){
          instructions->push(code);
          if(code[0] != '1' && code[0] != '0')
          {
            data_processed = true;
            processing_request = false;
            packet_num = 0;
          }
        }
        Serial.println("Instructions received");
          ready_flag = true;
      }
    }
};

class MyServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
      deviceConnected = true;
      Serial.println("Connected");
    };

    void onDisconnect(BLEServer* pServer) {
      deviceConnected = false;
      // Start the service
      Serial.println("Disconnected");
      pServer->getAdvertising()->start();
    }
};

void setup() {
  Serial.begin(115200);

  pinMode(DIR1, OUTPUT);
  pinMode(PUL1, OUTPUT);
  pinMode(DIR2, OUTPUT);
  pinMode(PUL2, OUTPUT);
  pinMode(X_CAL, INPUT);
  pinMode(Y_CAL, INPUT);
  pinMode(RELAY, OUTPUT);
  pinMode(SLP1, OUTPUT);
  pinMode(DRV1, OUTPUT);
  pinMode(SLP2, OUTPUT);
  pinMode(DRV2, OUTPUT);
  digitalWrite(RELAY, LOW); 
  digitalWrite(SLP1, LOW);
  digitalWrite(DRV1, LOW);
  digitalWrite(SLP2, LOW);
  digitalWrite(DRV2, LOW);


  instructions = new std::queue<std::string>();

  // Create the BLE Device
  BLEDevice::init("ESP32 UART Test"); // Give it a name
  BLEDevice::setMTU(4096);

  // Create the BLE Server
  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());

  // Create the BLE Service
  pService = pServer->createService(SERVICE_UUID);

  // Create a BLE Characteristic
  pCharacteristic = pService->createCharacteristic(
                      CHARACTERISTIC_UUID_TX,
                      BLECharacteristic::PROPERTY_NOTIFY
                    );
                      
  pCharacteristic->addDescriptor(new BLE2902());

  BLECharacteristic *pCharacteristic = pService->createCharacteristic(
                                         CHARACTERISTIC_UUID_RX,
                                         BLECharacteristic::PROPERTY_WRITE
                                       );

  pCharacteristic->setCallbacks(new MyCallbacks());

  // Start the service
  pService->start();

  // Start advertising
  pServer->getAdvertising()->start();
  Serial.println("Waiting a client connection to notify...");
  calibrate();
}

void moveSteps()
{
	spd = 500/4; // Set to rapid speed
	digitalWrite(DIR2, HIGH);
	for(int i = 0; i < 920*40; i++)
	{
		digitalWrite(PUL2,HIGH); //Trigger one step forward
		delayMicroseconds(1);
		digitalWrite(PUL2,LOW); //Pull step pin low so it can be triggered again
		delayMicroseconds(spd);
	}
	
  digitalWrite(SLP1, LOW);
  digitalWrite(DRV1, LOW);
  digitalWrite(SLP2, LOW);
  digitalWrite(DRV2, LOW);
  
  Serial.println("Calibrating Complete. Beginning instructions...");
}

void loop() {
  if (!processing_request) {
    if(!instructions->empty() && !instruction_in_progress)
    {
      digitalWrite(RELAY, HIGH);
      digitalWrite(SLP1, HIGH);
      digitalWrite(DRV1, HIGH);
      digitalWrite(SLP2, HIGH);
      digitalWrite(DRV2, HIGH);
      ready_flag = false;
      instruction_in_progress = true;
      if(first_inst)
      {
        moveSteps();
        first_inst = false;
      }
      processInstruction();
      if(x_steps > y_steps)
        StepForwardDefaultX();
      else
        StepForwardDefaultY();
      instruction_in_progress = false;
    }
  }
  if(ready_flag)
  {
    if(!processing_request)
    {
        digitalWrite(RELAY,LOW);
        digitalWrite(SLP1, LOW);
        digitalWrite(DRV1, LOW);
        digitalWrite(SLP2, LOW);
        digitalWrite(DRV2, LOW);
    }
    if(deviceConnected)
    {
      std::stringstream strs;
      strs << packet_num;
      std::string temp_str = strs.str();
      send_str(temp_str.c_str());
      delay(1000);
    }
  }
  if(canceled_recieved)
  {
    send_str("cancelled");
    canceled_recieved = false;
  }
  if(data_processed)
  {
    send_str("start");
    data_processed = false;
  }
}

void calibrate()
{
  Serial.println("Beginning Calibration");
  digitalWrite(DIR1, LOW);
  digitalWrite(DIR2, LOW);

  digitalWrite(RELAY, LOW);
  digitalWrite(SLP1, HIGH);
  digitalWrite(DRV1, HIGH);
  digitalWrite(SLP2, HIGH);
  digitalWrite(DRV2, HIGH);
  spd = 500/4;
  while(digitalRead(X_CAL) || digitalRead(Y_CAL))
  {
    if(digitalRead(X_CAL))
    {
      digitalWrite(PUL1,HIGH); //Trigger one step forward
    }
    if(digitalRead(Y_CAL))
    {
      digitalWrite(PUL2,HIGH); //Trigger one step forward
    }
    delayMicroseconds(1);
    if(digitalRead(X_CAL))
    {
      digitalWrite(PUL1,LOW); //Pull step pin low so it can be triggered again
    }
    if(digitalRead(Y_CAL))
    {
      digitalWrite(PUL2,LOW); //Trigger one step forward
    }
    delayMicroseconds(spd);
  }
  calibrating = false;
  x_pos = 0;
  y_pos = 0;
  x_steps = 0;
  y_steps = 0;
  first_inst = true;
}

void processInstruction(){
  std::string inst = instructions->front();
  instructions->pop();

  spd = 400/4; // Set to rapid speed
  if(inst[0] != '0' && inst[0] != '1')
  {
    ready_flag = true;
    calibrate();
    send_str("end");
    digitalWrite(DIR1, HIGH);
    delete instructions;
    instructions = new std::queue<std::string>();
    return;
  }

  //case "G00":
  // Rapid move
  spd = 350/4;
  int desired_x = inst[1]*140;
  int desired_y = inst[2]*140;
  std::stringstream stream;
  stream << "x=" << int(inst[1]) << "\ty=" << int(inst[2]);
  Serial.println(stream.str().c_str());
  int x_to_move = desired_x - x_pos;
  int y_to_move = desired_y - y_pos;
  x_dir = x_to_move<0 ? -1 : 1;
  y_dir = y_to_move<0 ? -1 : 1;
  digitalWrite(DIR1, x_to_move<0 ? LOW : HIGH);
  digitalWrite(DIR2, y_to_move<0 ? LOW : HIGH);
  x_steps = abs(x_to_move);
  y_steps = abs(y_to_move);
  if(inst[0] == '1')
  {
    spd = 3000/4; // Set speed to F
  }    
}

void debug_number(int x)
{
  std::stringstream strs;
  strs << x;
  std::string temp_str = strs.str();
  Serial.println(temp_str.c_str());
}

void StepForwardDefaultX()
{
  if(x_steps != 0)
  {
    double delta_err = y_steps/(double)x_steps;
    double err = 0;
    for(; x_steps > 0; x_steps--)
    {
      err += delta_err;
      if(err > 0.5)
      {
        // Move Y and X
        digitalWrite(PUL1,HIGH); //Trigger one step forward
        digitalWrite(PUL2,HIGH); //Trigger one step forward
        delayMicroseconds(1);
        digitalWrite(PUL1,LOW); //Pull step pin low so it can be triggered again
        digitalWrite(PUL2,LOW); //Pull step pin low so it can be triggered again
        delayMicroseconds(spd);
        y_pos += y_dir;
        y_steps--;
        err -= 1;
      }
      else // Move only X
      {
        digitalWrite(PUL1,HIGH); //Trigger one step forward
        delayMicroseconds(1);
        digitalWrite(PUL1,LOW); //Pull step pin low so it can be triggered again
        delayMicroseconds(spd);
      }
      x_pos += x_dir;
    }
  }
  // Clean up any remaining y_steps
  for(; y_steps > 0; y_steps--)
  {
      digitalWrite(PUL2,HIGH); //Trigger one step forward
      delayMicroseconds(1);
      digitalWrite(PUL2,LOW); //Pull step pin low so it can be triggered again
      delayMicroseconds(spd);
      y_pos += y_dir;
  }
  instruction_in_progress = false;
}


void StepForwardDefaultY()
{
  if(y_steps != 0)
  {
    double delta_err = x_steps/(double)y_steps;
    double err = 0.;
    for(; y_steps > 0; y_steps--)
    {
      err += delta_err;
      if(err > 0.5)
      {
        // Move Y and X
        digitalWrite(PUL1,HIGH); //Trigger one step forward
        digitalWrite(PUL2,HIGH); //Trigger one step forward
        delayMicroseconds(1);
        digitalWrite(PUL1,LOW); //Pull step pin low so it can be triggered again
        digitalWrite(PUL2,LOW); //Pull step pin low so it can be triggered again
        delayMicroseconds(spd);
        x_pos += x_dir;
        x_steps--;
        err -= 1;
      }
      else // Move only Y
      {
        digitalWrite(PUL2,HIGH); //Trigger one step forward
        delayMicroseconds(1);
        digitalWrite(PUL2,LOW); //Pull step pin low so it can be triggered again
        delayMicroseconds(spd);
      }
      y_pos += y_dir;
    }
  }
  // Clean up any remaining x_steps
  for(; x_steps > 0; x_steps--)
  {
      digitalWrite(PUL1,HIGH); //Trigger one step forward
      delayMicroseconds(1);
      digitalWrite(PUL1,LOW); //Pull step pin low so it can be triggered again
      delayMicroseconds(spd);
      x_pos += x_dir;
  }
  instruction_in_progress = false;
}

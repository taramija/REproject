import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import g4p_controls.*; 
import processing.serial.*; 
import java.awt.Font; 
import java.util.concurrent.CopyOnWriteArrayList; 
import processing.video.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class ArmLink extends PApplet {

/***********************************************************************************
 *  }--\     InterbotiX     /--{
 *      |    Arm Link      |
 *   __/                    \__
 *  |__|                    |__|
 *
 *  The following software will allow you to control InterbotiX Robot Arms.
 *  Arm Link will send serial packets to the ArbotiX Robocontroller that
 *  specify coordinates for that arm to move to. TheArbotiX robocontroller
 *  will then do the Inverse Kinematic calculations and send commands to the
 *  DYNAMIXEL servos to move in such a way that the end effector ends up at
 *  the specified coordinate.
 *
 *  Robot Arm Compatibilty:
 *    The Arm Link Software is desiged to work with InterbotiX Robot Arms
 *    running the Arm Link firmware. Currently supported arms are:
 *      1)PhantomX Pincher Robot Arm
 *      2)PhantomX Reactor Robot Arm
 *      3)WidowX Robot Arm
 *      4)RobotGeek Snapper
 *
 *  Computer Compatibility:
 *    Arm Link can be used on any system that supports
 *      1)Java
 *      2)Processing 2.0
 *      3)Java serial library (Included for Mac/Windows/Linux with processing 2.0)
 *    Arm Link has been tested on the following systems
 *      1)Windows XP, Vista, 7, 8
 *      2)Mac 10.6+
 *      3)Linux?
 *     Binaries for these systems are available
 *
 *  Building:
 *    Once Java and Processing 2.0 have been installed, you will also need to download/install
 *    the G4p GUI library for processing.
 *    http://sourceforge.net/projects/g4p/files/?source=navbar
 *    
 *    (More information on G4P can be found at http://www.lagers.org.uk/g4p/download.html )
 *    
 *     Notice for Mac users:
 *       To get the Serial library to work properly you will need to issue the following commands
 *        sudo mkdir -p /var/lock
 *        sudo chmod 777 /var/lock
 *
 *
 *  External Resources
 *  Arm Link Setup & Documentation
 *    http://learn.trossenrobotics.com/arbotix/arbotix-communication-controllers/31-arm-control
 *
 *  PhantomX Pincher Robot Arm
 *    http://learn.trossenrobotics.com/interbotix/robot-arms/pincher-arm
 *  PhantomX Reactor Robot Arm
 *    http://learn.trossenrobotics.com/interbotix/robot-arms/reactor-arm
 *  WidowX Robot Arm
 *    http://learn.trossenrobotics.com/interbotix/robot-arms/widowx-arm
 *
 ***********************************************************************************/

      //import g4p library for GUI elements
 //import serial library to communicate with the ArbotiX
       //import font



Serial sPort;               //serial port object, used to connect to a serial port and send data to the ArbotiX

PrintWriter debugOutput;        //output object to write to a file

int numSerialPorts = Serial.list().length;                 //Number of serial ports available at startup
String[] serialPortString = new String[numSerialPorts+1];  //string array to the name of each serial port - used for populating the drop down menu
int selectedSerialPort;                                    //currently selected port from serialList drop down

boolean debugConsole = true;      //change to 'false' to disable debuging messages to the console, 'true' to enable 
boolean debugFile = false;        //change to 'false' to disable debuging messages to a file, 'true' to enable

boolean debugGuiEvent = true;     //change to 'false' to disable GUI debuging messages, 'true' to enable
boolean debugSerialEvent = false;     //change to 'false' to disable GUI debuging messages, 'true' to enable
//int lf = 10;    // Linefeed in ASCII

boolean debugFileCreated  = false;  //flag to see if the debug file has been created yet or not

boolean enableAnalog = false; //flag to enable reading analog inputs from the Arbotix

boolean updateFlag = false;     //trip flag, true when the program needs to send a serial packet at the next interval, used by both 'update' and 'autoUpdate' controls
int updatePeriod = 33;          //minimum period between packet in Milliseconds , 33ms = 30Hz which is the standard for the commander/arm link protocol

long prevCommandTime = 0;       //timestamp for the last time that the program sent a serial packet
long heartbeatTime = 0;         //timestamp for the last time that the program received a serial packet from the Arm
long currentTime = 0;           //timestamp for currrent time

int packetRepsonseTimeout = 5000;      //time to wait for a response from the ArbotiX Robocontroller / Arm Link Protocol

int currentArm = 0;          //ID of current arm. 1 = pincher, 2 = reactor, 3 = widowX, 5 = snapper
int currentMode = 0;         //Current IK mode, 1=Cartesian, 2 = cylindrical, 3= backhoe
int currentOrientation = 0;  //Current wrist oritnation 1 = straight/normal, 2=90 degrees

String helpLink = "http://learn.trossenrobotics.com";  //link for error panel to display


PImage logoImg;
PImage footerImg;

//booleans for key tracking
boolean xkey = false;
boolean ykey = false;
boolean zkey = false;
boolean wangkey = false;
boolean wrotkey = false;
boolean gkey = false;
boolean dkey = false;

int startupWaitTime = 10000;    //time in ms for the program to wait for a response from the ArbotiX
Serial[] sPorts = new Serial[numSerialPorts];  //array of serial ports, one for each avaialable serial port.

int armPortIndex = -1; //the index of the serial port that an arm is currently connected to(relative to the list of avaialble serial ports). -1 = no arm connected


int analogSampleTime = 33;//time between analog samples
long lastAnalogSample = millis();//
int nextAnalog = 0;
int[]analogValues = new int[8];


/********DRAG AND DROP VARS*/
int numPanels =0;
int currentTopPanel = 0;
int dragFlag = -1;
int panelsX = 100;  //x coordinate for all panels
int panelsYStart = 25;//distance between top of parent and first panel
int panelYOffset = 25;//distance between panels
int panelHeight = 15;//height of the panel
int lastDraggedOverId = -1;
int lastDraggedOverColor = -1;
int numberPanelsDisplay = 14;
int draggingPosition = -1;
float draggingY = 0;

GPanel tempPanel0;
GPanel tempPanel1;

GPanel tempPanel;

int currentPose = 0;  //current pose that has been selected. 


CopyOnWriteArrayList<int[]> poseData;
int[] blankPose = new int[9]; //blank pose : x, y, z, wristangle, wristRotate, Gripper, Delta, digitals, pause


int[] defaultPose = {
  0, 200, 200, 0, 0, 256, 125, 0, 1000
}; //blank pose : x, y, z, wristangle, wristRotate, Gripper, Delta, digitals





boolean playSequence = false;
//boolean waitForResponse = false;
int lastTime;
int lastPose;



int connectFlag = 0;
int disconnectFlag = 0;
int autoConnectFlag = 0;
int cameraFlag = 2;


Capture cam;


int pauseTime = 1000;



  //holds the data from the last packet sent
  int lastX;
  int lastY;
  int lastZ;
  int lastWristangle;
  int lastWristRotate;
  int lastGripper;
  int lastButton;
  int lastExtended;
  int lastDelta;
   
  
  
  
/***********/

public void setup() {
    //draw initial screen //475
  poseData = new CopyOnWriteArrayList<int[]>();

  createGUI();   //draw GUI components defined in gui.pde

  //Build Serial Port List
  serialPortString[0] = "Serial Port";   //first item in the list will be "Serial Port" to act as a label
  //iterate through each avaialable serial port  
  for (int i=0; i<numSerialPorts; i++) 
  {
    serialPortString[i+1] = Serial.list()[i];  //add the current serial port to the list, add one to the index to account for the first item/label "Serial Port"
  }
  serialList.setItems(serialPortString, 0);  //add contents of srialPortString[] to the serialList GUI    

  prepareExitHandler();//exit handler for clearing/stopping file handler
}

//Main Loop
public void draw()
{

  background(128);//draw background color
  image(logoImg, 5, 0, 280, 50);  //draw logo image
  image(footerImg, 15, 770);      //draw footer image

  currentTime = millis();  //get current timestamp

  if (connectFlag ==1)
  {


    //check to make sure serialPortSelected is not -1, -1 means no serial port was selected. Valid port indexes are 0+
    if (selectedSerialPort > -1)
    {    

      //try to connect to the port at 38400bps, otherwise show an error message
      try
      {
        sPorts[selectedSerialPort] =  new Serial(this, Serial.list()[selectedSerialPort], 38400);
      }
      catch(Exception e)
      {
        printlnDebug("Error Opening Serial Port"+serialList.getSelectedText());
        sPorts[selectedSerialPort] = null;
        displayError("Unable to open selected serial port" + serialList.getSelectedText() +". See link for possible solutions.", "http://learn.trossenrobotics.com/arbotix/8-advanced-used-of-the-tr-dynamixel-servo-tool");
      }
    }
      if(selectedSerialPort != -1)
      {
        //check to see if the serial port connection has been made
        if (sPorts[selectedSerialPort] != null)
        {
    
          //try to communicate with arm
          if (checkArmStartup() == true)
          {       
            //disable connect button and serial list
            connectButton.setEnabled(false);
            connectButton.setAlpha(128);
            serialList.setEnabled(false);
            serialList.setAlpha(128);
            autoConnectButton.setEnabled(false);
            autoConnectButton.setAlpha(128);
            //enable disconnect button
            disconnectButton.setEnabled(true);
            disconnectButton.setAlpha(255);
    
            //enable & set visible control and mode panel
            modePanel.setVisible(true);
            modePanel.setEnabled(true);
            controlPanel.setVisible(true);
            controlPanel.setEnabled(true);
            sequencePanel.setVisible(true);
            sequencePanel.setEnabled(true);
            ioPanel.setVisible(true);
            ioPanel.setEnabled(true);
            delayMs(100);//short delay 
            setCartesian();
            statusLabel.setText("Connected");
          }
    
          //if arm is not found return an error
          else  
          {
            sPorts[selectedSerialPort].stop();
            //      sPorts.get(selectedSerialPort) = null;
            sPorts[selectedSerialPort] = null;
            printlnDebug("No Arm Found on port "+serialList.getSelectedText()) ;
    
            //displayError("No Arm found on serial port" + serialList.getSelectedText() +". Make sure power is on and the arm is connected to the computer.", "http://learn.trossenrobotics.com/arbotix/8-advanced-used-of-the-tr-dynamixel-servo-tool");
             genericMessageDialog("Arm Error", "No Arm found on serial port" + serialList.getSelectedText() +". <br /> Make sure power is on and the arm is connected to the computer.", G4P.WARNING);
    
            
            
            statusLabel.setText("Not Connected");
          }
      }
    }

    connectFlag = 0;
  }


  if (disconnectFlag == 1)
  {
    
    autoUpdateCheckbox.setSelected(false);
    putArmToSleep();
    //TODO: call response & check

    ///stop/disconnect the serial port and set sPort to null for future checks
    sPorts[armPortIndex].stop();   
    sPorts[armPortIndex] = null;

    //enable connect button and serial port 
    connectButton.setEnabled(true);
    connectButton.setAlpha(255);
    serialList.setEnabled(true);
    serialList.setAlpha(255); 
    autoConnectButton.setEnabled(true);
    autoConnectButton.setAlpha(255);

    //disable disconnect button
    disconnectButton.setEnabled(false);
    disconnectButton.setAlpha(128);
    //disable & set invisible control and mode panel
    controlPanel.setVisible(false);
    controlPanel.setEnabled(false); 
    sequencePanel.setVisible(false);
    sequencePanel.setEnabled(false);    
    modePanel.setVisible(false);
    modePanel.setEnabled(false);
    ioPanel.setVisible(false);
    ioPanel.setEnabled(false);
    wristPanel.setVisible(false);
    wristPanel.setEnabled(false);

    //uncheck all checkboxes to reset
    autoUpdateCheckbox.setSelected(false);
    //digitalCheckbox0.setSelected(false);
    digitalCheckbox1.setSelected(false);
    digitalCheckbox2.setSelected(false);
    digitalCheckbox3.setSelected(false);
    digitalCheckbox4.setSelected(false);
    digitalCheckbox5.setSelected(false);
    digitalCheckbox6.setSelected(false);
    digitalCheckbox7.setSelected(false);

    analogCheckbox.setSelected(false);

    //set arm/mode/orientation to default
    currentMode = 0;
    currentArm = 0;
    currentOrientation = 0;

    //reset button color mode
    cartesianModeButton.setLocalColorScheme(GCScheme.CYAN_SCHEME);
    cylindricalModeButton.setLocalColorScheme(GCScheme.CYAN_SCHEME);
    backhoeModeButton.setLocalColorScheme(GCScheme.CYAN_SCHEME);

    //reset alpha trapsnparency on orientation buttons
    //DEPRECATED armStraightButton.setAlpha(128);
    //DEPRECATEDarm90Button.setAlpha(128);


    disconnectFlag = 0;
    statusLabel.setText("Not Connected");
  }



  if (autoConnectFlag == 1)
  {


    //disable connect button and serial list
    connectButton.setEnabled(false);
    connectButton.setAlpha(128);
    serialList.setEnabled(false);
    serialList.setAlpha(128);
    autoConnectButton.setEnabled(false);
    autoConnectButton.setAlpha(128);
    //enable disconnect button
    disconnectButton.setEnabled(true);
    disconnectButton.setAlpha(255);

    //for (int i=0;i<Serial.list().length;i++) //scan from bottom to top
    //scan from the top of the list to the bottom, for most users the ArbotiX will be the most recently added ftdi device
    for (int i=Serial.list ().length-1; i>=0; i--) 
    {
      println("port"+i);
      //try to connect to the port at 38400bps, otherwise show an error message
      try
      {
        sPorts[i] = new Serial(this, Serial.list()[i], 38400);
      }
      catch(Exception e)
      {
        printlnDebug("Error Opening Serial Port "+Serial.list()[i] + " for auto search");
        sPorts[i] = null;
      }
    }//end interating through serial list

    //try to communicate with arm
    if (checkArmStartup() == true)
    {
      printlnDebug("Arm Found from auto search on port "+Serial.list()[armPortIndex]) ;

      //enable & set visible control and mode panel, enable disconnect button
      modePanel.setVisible(true);
      modePanel.setEnabled(true);
      controlPanel.setVisible(true);
      controlPanel.setEnabled(true);
      sequencePanel.setVisible(true);
      sequencePanel.setEnabled(true);
      ioPanel.setVisible(true);
      ioPanel.setEnabled(true);
      disconnectButton.setEnabled(true);
      delayMs(200);//shot delay 
      setCartesian();

      statusLabel.setText("Connected");

      //break;
    }

    //if arm is not found return an error
    else  
    {
      //enable connect button and serial port 
      connectButton.setEnabled(true);
      connectButton.setAlpha(255);
      serialList.setEnabled(true);
      serialList.setAlpha(255); 
      autoConnectButton.setEnabled(true);
      autoConnectButton.setAlpha(255);

      //disable disconnect button
      disconnectButton.setEnabled(false);
      disconnectButton.setAlpha(128);
      //disable & set invisible control and mode panel

      //displayError("No Arm found using auto seach. Please check power and connections", "");
      
      genericMessageDialog("Arm Not Found", "No Arm found using auto seach. <br />Please check power and connections.", G4P.WARNING);
        
      
      statusLabel.setText("Not Connected");
    }
    //stop all serial ports without an arm connected 
    for (int i=0; i<numSerialPorts; i++) 
    {      
      //if the index being scanned is not the index of an port with an arm connected, stop/null the port
      //if the port is already null, then it was never opened
      if (armPortIndex != i & sPorts[i] != null)
      {
        printlnDebug("Stopping port "+Serial.list()[i]) ;
        sPorts[i].stop();
        sPorts[i] = null;
      }
    }


    autoConnectFlag = 0;
  }




  //check if
  //  -update flag is true, and a packet needs to be sent
  //  --it has been more than 'updatePeriod' ms since the last packet was sent
  if (currentTime - prevCommandTime > updatePeriod )
  {



    //check if
    //--analog retrieval is enabled
    //it has been long enough since the last sample
    if (currentTime - lastAnalogSample > analogSampleTime && (true == enableAnalog))
    {
      if ( currentArm != 0)
      {
        println("analog");

        analogValues[nextAnalog] = analogRead(nextAnalog);
        analogLabel[nextAnalog].setText(
        Integer.toString(nextAnalog) + ":" + Integer.toString(analogValues[nextAnalog]));

        nextAnalog = nextAnalog+1;
        if (nextAnalog > 7)
        {
          nextAnalog = 0;
          lastAnalogSample = millis();
        }
      }
    }






    //check if
    //  -update flag is true, and a packet needs to be sent
    else if (updateFlag == true)
    {
      updateOffsetCoordinates();     //prepare the currentOffset coordinates for the program to send
      updateButtonByte();  //conver the current 'digital button' checkboxes into a value to be sent to the arbotix/arm
      prevCommandTime = currentTime; //update the prevCommandTime timestamp , used to calulcate the time the program can next send a command


      //check that the serial port is active - if the 'armPortIndex' variable is not -1, then a port has been connected and has an arm attached
      if (armPortIndex > -1)
      {
        

        //if a sequence is playing, wait for the arm response before moving on
        //        if(playSequence ==true)
        //        {
        //          //use this code to enable return packet checking for positional commands
        //          byte[] responseBytes = new byte[5];    //byte array to hold response data
        //          //responseBytes = readFromArm(5);//read raw data from arm, complete with wait time
        //          responseBytes = readFromArmFast(5);
        //        }
        //        if(verifyPacket(responseBytes) == true)
        //        {
        //          printlnDebug("Moved!"); 
        //        }
        //        else
        //        {
        //          printlnDebug("No Arm Found"); 
        //        }
      }

      //in normal update mode, pressing the update button signals the program to send a packet. In this
      //case the program must set the update flag to false in order to stop new packets from being sent
      //until the update button is pressed again. 
      //However in autoUpdate mode, the program should not change this flag (only unchecking the auto update flag should set the flag to false)
      if (autoUpdateCheckbox.isSelected() == false)
      {
        
        updateButtonByte();
        sendCommanderPacket(xCurrentOffset, yCurrentOffset, zCurrentOffset, wristAngleCurrentOffset, wristRotateCurrentOffset, gripperCurrentOffset, deltaCurrentOffset, digitalButtonByte, extendedByte);  
        updateFlag = false;//only set the updateFlag to false if the autoUpdate flag is false
        //send normally
        
      }
      else
      {
        //autoupdate,so send with check
           //send commander packet with the current global currentOffset coordinates, don't send the same data twice in a row
           
        updateButtonByte();
        sendCommanderPacketWithCheck(xCurrentOffset, yCurrentOffset, zCurrentOffset, wristAngleCurrentOffset, wristRotateCurrentOffset, gripperCurrentOffset, deltaCurrentOffset, digitalButtonByte, extendedByte);  
 
        //use this oppurtunity to set the extended byte to 0 if autoupdate is enabled - this way the extended packet only gets sent once

        if (extendedByte != 0)
        {
          extendedByte = 0;
          extendedTextField.setText("0");
        }
        
        
      }
 
    }//end command code
  }



  //DRAG AND DROP CODE
  //check if the 'dragFlag' is set
  if (dragFlag > -1)
  {

    int dragPanelNumber = dragFlag - currentTopPanel;  //dragPanelNumber now has the panel # (of the panel that was just dragged) relative to the panels that are currently being displayed.

    float dragPanelY = poses.get(dragFlag).getY();  //the final y coordinate of the panel that was last dragged

    int newPanelPlacement = floor((dragPanelY - panelsYStart)/25);//determine the panel #(relative to panels being shown) that the dragged panel should displace

    //set bounds for dragging panels too high/low
    newPanelPlacement = max(0, newPanelPlacement);//for negative numbers (i.e. dragged above first panel) set new panel to '0'
    newPanelPlacement = min(min(numberPanelsDisplay-1, poses.size())-1, newPanelPlacement);//for numbers that are too high (i.e. dragged below the last panel) set to the # of panels to display, or the size of the array list, whichever is smaller
    println(newPanelPlacement);


    if (lastDraggedOverId == -1)
    { 

      lastDraggedOverId = newPanelPlacement + currentTopPanel;
      lastDraggedOverColor =   poses.get(newPanelPlacement + currentTopPanel).getLocalColorScheme();
      poses.get(newPanelPlacement + currentTopPanel).setLocalColorScheme(15);

      println("First");
    } else if ((lastDraggedOverId != (newPanelPlacement + currentTopPanel)))
    {

      poses.get(lastDraggedOverId).setLocalColorScheme(lastDraggedOverColor);
      println("change! " +" " + lastDraggedOverColor+ " " + currentTopPanel);

      lastDraggedOverId = newPanelPlacement + currentTopPanel;
      lastDraggedOverColor =   poses.get(newPanelPlacement + currentTopPanel).getLocalColorScheme();
      poses.get(newPanelPlacement + currentTopPanel).setLocalColorScheme(15);
    } else
    {

      //lastDraggedOverId = newPanelPlacement + currentTopPanel;
      //poses.get(newPanelPlacement + currentTopPanel).setLocalColorScheme(0);
    }



    //check is the panel that set the 'dragFlag' has stopped being dragged.
    if (poses.get(dragFlag).isDragging() == false)
    {

      poses.get(lastDraggedOverId).setLocalColorScheme(lastDraggedOverColor);//set color for the displaced panel
      lastDraggedOverId = -1;//reset lastDragged vars for next iteration

      //dragFlag now contains a value corresponding to the the panel that was just being dragged
      //


      int lowestPanel = min(dragPanelNumber, newPanelPlacement); //figure out which panel number is lower 



      println("you dragged panel #" + dragPanelNumber+ "to position "+ dragPanelY  +" Which puts it at panel #"+newPanelPlacement);


      //array list management
      tempPanel0 = poses.get(dragPanelNumber);//copy the panel that was being dragged to a temporary object
      poses.remove(dragPanelNumber);//remove the panel from the array list
      poses.add(newPanelPlacement, tempPanel0);//add the panel into the array list at the position of the displaced panel

      int[] tempPoseData0 = poseData.get(dragPanelNumber);//copy the panel that was being dragged to a temporary object
      poseData.remove(dragPanelNumber);
      poseData.add(newPanelPlacement, tempPoseData0);//add the panel into the array list at the position of the displaced panel


      //rebuild all of the list placement based on its correct array placement
      for (int i = lowestPanel; i < poses.size ()-currentTopPanel; i++)
      {
        println("i " + i);
        poses.get(currentTopPanel+i).moveTo(panelsX, panelsYStart + (panelYOffset*i));//move the panel that was being dragged to its new position
        poses.get(currentTopPanel+i).setText(Integer.toString(currentTopPanel+i));//set the text displayed to the same as the new placement
        //whenever the program displaces a panel down, one panel will need to go from being visible to not being visible this will always be the 'numberPanelsDisplay'th panel
        if (i == numberPanelsDisplay)
        {
          poses.get(currentTopPanel+i).setVisible(false);//set the panel that has 'dropped off' the visual plane to not visible
        }
      }       




      tempPanel0 = null;
      dragFlag = -1;   
      println("reset Flag");
    }//end dragging check.
  }//end dragFlag check






  if (playSequence == true)
  {
    pauseTime = PApplet.parseInt(pauseTextField.getText());

    if (millis() - lastTime > (20 * deltaCurrent + pauseTime))
    {
      //println("50 millis");
      for (int i = 0; i < poses.size (); i++)
      {
        if (i == lastPose)
        {
          poses.get(i).setCollapsed(false);
          poses.get(i).forceBufferUpdate();
          poseToWorkspaceInternal(lastPose);
          updateFlag = true;//set update flag to signal sending an update on the next cycle
          updateOffsetCoordinates();//update the coordinates to offset based on the current mode



          println("play"+i);
        } else
        {
          poses.get(i).setCollapsed(true);
          poses.get(i).forceBufferUpdate();
        }
      }
      lastPose = lastPose + 1;

      lastTime = millis();

      if (lastPose  >= poses.size())
      {
        //playSequence = false;
        lastPose = 0;
        println("play ending");
      }
    }
  }



  if (cameraFlag == 1)
  {


    try {  
      cam = new Capture(this, 320, 240, 30);
      cam.start();
      frame.setResizable(true);
    }
    catch(Exception e)
    {
    }
    frame.setSize(850, 750);
    cameraFlag = 2;
  }

  if (cameraFlag == 0)
  {

    frame.setSize(475, 750);
    cameraFlag = 2;
  }

  try {
    if (cam.available()) 
    {
      cam.read();
    }
    image(cam, 500, 0);
  }
  catch(Exception e)
  {
  }
}//end draw()


/*****************************************************
 *  stop()
 *
 *  Tasks to perform on end of program
 ******************************************************/
public void stop()
{

  debugOutput.flush(); // Writes the remaining data to the file
  debugOutput.close(); // Finishes the file
}


/******************************************************
 *  prepareExitHandler()
 *
 *  Tasks to perform on end of program
 * https://forum.processing.org/topic/run-code-on-exit
 ******************************************************/
private void prepareExitHandler () {
  Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
    public void run () 
    {
      if (debugFileCreated == true)
      {
        debugOutput.flush(); // Writes the remaining data to the file
        debugOutput.close(); // Finishes the file
      }
    }
  }
  ));
}

/******************************************************
 *  printlnDebug()
 *
 *  function used to easily enable/disable degbugging
 *  enables/disables debugging to the console
 *  prints a line to the output
 *
 *  Parameters:
 *    String message
 *      string to be sent to the debugging method
 *    int type
 *        Type of event
 *         type 0 = normal program message
 *         type 1 = GUI event
 *         type 2 = serial packet 
 *  Globals Used:
 *      boolean debugGuiEvent
 *      boolean debugConsole
 *      boolean debugFile
 *      PrintWriter debugOutput
 *      boolean debugFileCreated
 *  Returns: 
 *    void
 ******************************************************/
public void printlnDebug(String message, int type)
{
  if (debugConsole == true)
  {
    if ((type == 1 & debugGuiEvent == true) || type == 0 || type == 2)
    {
      println(message);
    }
  }

  if (debugFile == true)
  {

    if ((type == 1 & debugGuiEvent == true) || type == 0 || type == 2)
    {

      if (debugFileCreated == false)
      {
        debugOutput = createWriter("debugArmLink.txt");
        debugOutput.println("Started at "+ day() +"-"+ month() +"-"+ year() +" "+ hour() +":"+ minute() +"-"+ second() +"-"); 
        debugFileCreated = true;
      }


      debugOutput.println(message);
    }
  }
}

//wrapper for printlnDebug(String, int)
//assume normal behavior, message type = 0
public void printlnDebug(String message)
{
  printlnDebug(message, 0);
}

/******************************************************
 *  printlnDebug()
 *
 *  function used to easily enable/disable degbugging
 *  enables/disables debugging to the console
 *  prints normally to the output
 *
 *  Parameters:
 *    String message
 *      string to be sent to the debugging method
 *    int type
 *        Type of event
 *         type 0 = normal program message
 *         type 1 = GUI event
 *         type 2 = serial packet 
 *  Globals Used:
 *      boolean debugGuiEvent
 *      boolean debugConsole
 *      boolean debugFile
 *      PrintWriter debugOutput
 *      boolean debugFileCreated
 *  Returns: 
 *    void
 ******************************************************/
public void printDebug(String message, int type)
{
  if (debugConsole == true)
  {
    if ((type == 1 & debugGuiEvent == true)  || type == 2)
    {
      print(message);
    }
  }

  if (debugFile == true)
  {

    if ((type == 1 & debugGuiEvent == true) || type == 0 || type == 2)
    {

      if (debugFileCreated == false)
      {
        debugOutput = createWriter("debugArmLink.txt");

        debugOutput.println("Started at "+ day() +"-"+ month() +"-"+ year() +" "+ hour() +":"+ minute() +"-"+ second() ); 

        debugFileCreated = true;
      }


      debugOutput.print(message);
    }
  }
}

//wrapper for printlnDebug(String, int)
//assume normal behavior, message type = 0
public void printDebug(String message)
{
  printDebug(message, 0);
}

/******************************************************
 * Key combinations
 * holding a number ket and pressing up/down will
 * increment/decrement the corresponding field
 *
 * The program will use keyPressed() to log whenever a
 * number key is being held. If later 'Up' or 'Down'
 * is also logged, they value will be changed
 *  keyReleased() will be used to un-log the number values
 *  once they are released.
 *
 * 1-x/base
 * 2-y/shoulder
 * 3-z/elbow
 * 4-wrist angle
 * 5-wrist rotate
 * 6-gripper
 ******************************************************/
public void keyPressed()
{

  if (currentArm != 0)
  {
    //change 'updageFlag' variable if 'enter' is pressed
    if (key ==ENTER)
    {
      updateFlag = true;
      updateOffsetCoordinates();
    }

    //if any of the numbers 1-6 are currently being pressed, change the state of the variable
    if (key =='1')
    {
      xkey=true;
    }
    if (key =='2')
    {
      ykey=true;
    }
    if (key =='3')
    {
      zkey=true;
    }
    if (key =='5')
    {
      wangkey=true;
    }
    if (key =='6')
    {
      wrotkey=true;
    }
    if (key =='4')
    {
      gkey=true;
    }
    if (key =='7')
    {
      dkey=true;
    }




    if (key == ' ')
    {

      sendCommanderPacket(0, 0, 0, 0, 0, 0, 0, 0, 17);    //send a commander style packet - the first 8 bytes are inconsequntial, only the last byte matters. '17' is the extended byte that will stop the arm
      updateFlag = false;
      autoUpdateCheckbox.setSelected(false);
    }  

    if (key == 'p')
    {

      playSequence = !playSequence;
    }





    if (key == ',')
    {

      poseToWorkspaceInternal(currentPose);
    }
    if (key == '.')
    {
      workspaceToPoseInternal();
    }


    //check for up/down keys
    if (key == CODED)
    {
      
      //if up AND a number 1-6 are being pressed, increment the appropriate field
      if (keyCode == UP)
      {
        if (xkey == true)
        {
          println(xCurrent);
          xCurrent = xCurrent + 1;
        }
        if (ykey == true)
        {
          yCurrent = yCurrent + 1;
        }
        if (zkey == true)
        {
          zCurrent = zCurrent + 1;
        }
        if (wangkey == true)
        {
          wristAngleCurrent = wristAngleCurrent + 1;
        }
        if (wrotkey == true)
        {
          wristRotateCurrent = wristRotateCurrent + 1;
        }
        if (gkey == true)
        {
          gripperCurrent = gripperCurrent + 1;
        }
        if (dkey == true)
        {
          deltaCurrent = deltaCurrent + 1;
        }
        
        

        xTextField.setText(Integer.toString(xCurrent));
        yTextField.setText(Integer.toString(yCurrent));
        zTextField.setText(Integer.toString(zCurrent));
        wristAngleTextField.setText(Integer.toString(wristAngleCurrent));
        wristRotateTextField.setText(Integer.toString(wristRotateCurrent));
        gripperTextField.setText(Integer.toString(gripperCurrent));
  
        if (currentMode == 1)
        {  
          xSlider.setValue(xCurrent);
          ySlider.setValue(yCurrent);
          zSlider.setValue(zCurrent);
          wristRotateKnob.setValue(wristRotateCurrent);
          wristAngleKnob.setValue(wristAngleCurrent);
          gripperLeftSlider.setValue(gripperCurrent);
          gripperRightSlider.setValue(gripperCurrent);
          deltaSlider.setValue(deltaCurrent);
        } else if (currentMode == 2 )
        {
          baseKnob.setValue(xCurrent);
          ySlider.setValue(yCurrent);
          zSlider.setValue(zCurrent);
          wristRotateKnob.setValue(wristRotateCurrent);
          wristAngleKnob.setValue(wristAngleCurrent);
          gripperLeftSlider.setValue(gripperCurrent);
          gripperRightSlider.setValue(gripperCurrent);
          deltaSlider.setValue(deltaCurrent);
        } else if (currentMode == 3)
        {
          baseKnob.setValue(xCurrent);
          shoulderKnob.setValue(yCurrent);
          elbowKnob.setValue(zCurrent);
          wristRotateKnob.setValue(wristRotateCurrent);
          wristAngleKnob.setValue(wristAngleCurrent);
          gripperLeftSlider.setValue(gripperCurrent);
          gripperRightSlider.setValue(gripperCurrent);
          deltaSlider.setValue(deltaCurrent);
        }
      
      }

      //if down AND a number 1-6 are being pressed, increment the appropriate field
      if (keyCode == DOWN)
      {
        if (xkey == true)
        {
          xCurrent = xCurrent - 1;
        }
        if (ykey == true)
        {
          yCurrent = yCurrent - 1;
        }
        if (zkey == true)
        {
          zCurrent = zCurrent - 1;
        }
        if (wangkey == true)
        {
          wristAngleCurrent = wristAngleCurrent - 1;
        }
        if (wrotkey == true)
        {
          wristRotateCurrent = wristRotateCurrent - 1;
        }
        if (gkey == true)
        {
          gripperCurrent = gripperCurrent - 1;
        }
        if (dkey == true)
        {
          deltaCurrent = deltaCurrent - 1;
        }
        
  
        xTextField.setText(Integer.toString(xCurrent));
        yTextField.setText(Integer.toString(yCurrent));
        zTextField.setText(Integer.toString(zCurrent));
        wristAngleTextField.setText(Integer.toString(wristAngleCurrent));
        wristRotateTextField.setText(Integer.toString(wristRotateCurrent));
        gripperTextField.setText(Integer.toString(gripperCurrent));
  
        if (currentMode == 1)
        {  
          xSlider.setValue(xCurrent);
          ySlider.setValue(yCurrent);
          zSlider.setValue(zCurrent);
          wristRotateKnob.setValue(wristRotateCurrent);
          wristAngleKnob.setValue(wristAngleCurrent);
          gripperLeftSlider.setValue(gripperCurrent);
          gripperRightSlider.setValue(gripperCurrent);
          deltaSlider.setValue(deltaCurrent);
        } else if (currentMode == 2 )
        {
          baseKnob.setValue(xCurrent);
          ySlider.setValue(yCurrent);
          zSlider.setValue(zCurrent);
          wristRotateKnob.setValue(wristRotateCurrent);
          wristAngleKnob.setValue(wristAngleCurrent);
          gripperLeftSlider.setValue(gripperCurrent);
          gripperRightSlider.setValue(gripperCurrent);
          deltaSlider.setValue(deltaCurrent);
        } else if (currentMode == 3)
        {
          baseKnob.setValue(xCurrent);
          shoulderKnob.setValue(yCurrent);
          elbowKnob.setValue(zCurrent);
          wristRotateKnob.setValue(wristRotateCurrent);
          wristAngleKnob.setValue(wristAngleCurrent);
          gripperLeftSlider.setValue(gripperCurrent);
          gripperRightSlider.setValue(gripperCurrent);
          deltaSlider.setValue(deltaCurrent);
        }
        
      }


    }
  }
}
public void keyReleased()
{

  //change variable state when number1-6 is released

    if (key =='1')
  {
    xkey=false;
  }
  if (key =='2')
  {
    ykey=false;
  }
  if (key =='3')
  {
    zkey=false;
  }
  if (key =='5')
  {
    wangkey=false;
  }
  if (key =='6')
  {
    wrotkey=false;
  }
  if (key =='4')
  {
    gkey=false;
  }
  if (key =='7')
  {
    dkey = false;
  }
}
/***********************************************************************************
 *  }--\     InterbotiX     /--{
 *      |    Arm Link      |
 *   __/                    \__
 *  |__|                    |__|
 *
 *  arbotix.pde
 *	
 *	This file has several functions for interfacing with the ArbotiX robocontroller
 *	using the ArmLink protocol. 
 *	See 'ArmLnk.pde' for building this application.
 *
 ***********************************************************************************/


/******************************************************
 *  readFromArm(int, boolean)
 *
 *  reads data back from the ArbotiX/Arm
 *
 *  Normally this is called from readFromArm(int) - 
 *  this will block the program and make it wait 
 * 'packetRepsonseTimeout' ms. Most of the time the program
 *  will need to wait, as the arm is moving to a position
 *  and will not send a response packet until it has 
 *  finished moving to that position.
 *  
 *  However this will add a lot of time to the 'autoSearch' 
 *  functionality. When the arm starts up it will immediatley send a
 *  ID packet to identify itself so a non-waiting version is   
 *  avaialble -  readFromArmFast(int) which is equivalent to
 *  readFromArm(int, false)
 *
 *  Parameters:
 *    int bytesExpected
 *      # of bytes expected in the response packet
 *    boolean wait
 *        Whether or not to wait 'packetRepsonseTimeout' ms for a response
 *         true = wait
 *         false = do not wait
 *  Globals Used:
 *      Serial sPort
 *      long packetRepsonseTimeout
 *
 *  Returns: 
 *    byte[]  responseBytes
 *      byte array with response data from ArbotiX/Arm
 ******************************************************/ 
public byte[] readFromArm(int bytesExpected, boolean wait)
{
  byte[] responseBytes = new byte[bytesExpected];    //byte array to hold response data
  delayMs(100);//wait a minimum 100ms to ensure that the controller has responded - this applies to both wait==true and wait==false conditions
  
  byte bufferByte = 0;  //current byte that is being read
  long startReadingTime = millis();//time that the program started looking for data
  
  printDebug("Incoming Raw Packet from readFromArm():",2); //debug
  
  //if the 'wait' flag is TRUE this loop will wait until the serial port has data OR it has waited more than packetRepsonseTimeout milliseconds.
  //packetRepsonseTimeout is a global variable
  
  while(wait == true & sPorts[armPortIndex].available() < bytesExpected  & millis()-startReadingTime < packetRepsonseTimeout)
  {
     //do nothing, just waiting for a response or timeout
  }
  
  for(int i =0; i < bytesExpected;i++)    
  {
    // If data is available in the serial port, continute
    if(sPorts[armPortIndex].available() > 0)
    {
      bufferByte = PApplet.parseByte(sPorts[armPortIndex].readChar());
      responseBytes[i] = bufferByte;
      printDebug(hex(bufferByte) + "-",2); //debug 
    }
    else
    {
      printDebug("NO BYTE-");//debug
    }
  }//end looking for bytes from packet
  printlnDebug(" ",2); //debug  finish line
  
  sPorts[armPortIndex].clear();  //clear serial port for the next read
  
  return(responseBytes);  //return serial data
}


//wrapper for readFromArm(int, boolean)
//assume normal behavior, wait = true
public byte[] readFromArm(int bytesExpected)
{
  return(readFromArm(bytesExpected,true));
}


//wrapper for readFromArm(int, boolean)
//wait = false. Used for autosearch/startup
public byte[] readFromArmFast(int bytesExpected)
{
  return(readFromArm(bytesExpected,false));
}




/******************************************************
 *  verifyPacket(int, boolean)
 *
 *  verifies a packet received from the ArbotiX/Arm
 *
 *  This function will do the following to verify a packet
 *  -calculate a local checksum and compare it to the
 *    transmitted checksum 
 *  -check the error byte for any data 
 *  -check that the armID is supported by this program
 *
 *  Parameters:
 *    byte[]  returnPacket
 *      byte array with response data from ArbotiX/Arm
 *
 *
 *  Returns: 
 *    boolean verifyPacket
 *      true = packet is OK
 *      false = problem with the packet
 *
 *  TODO: -Modify to return specific error messages
 *        -Make the arm ID check modular to facilitate 
 *         adding new arms.
 ******************************************************/ 
public boolean verifyPacket(byte[] returnPacket)
{
  int packetLength = returnPacket.length;  //length of the packet
  int tempChecksum = 0; //int for temporary checksum calculation
  byte localChecksum; //local checksum calculated by processing
  
  printDebug("Begin Packet Verification of :");
  for(int i = 0; i < packetLength;i++)
  {
    printDebug(returnPacket[i]+":");
  }
  //check header, which should always be 255/0xff
  if(returnPacket[0] == PApplet.parseByte(255))
  {  
      //iterate through bytes # 1 through packetLength-1 (do not include header(0) or checksum(packetLength)
      for(int i = 1; i<packetLength-1;i++)
      {
        tempChecksum = PApplet.parseInt(returnPacket[i]) + tempChecksum;//add byte value to checksum
      }
  
      localChecksum = PApplet.parseByte(~(tempChecksum % 256)); //calculate checksum locally - modulus 256 to islotate bottom byte, then invert(~)
      
      //check if calculated checksum matches the one in the packet
      if(localChecksum == returnPacket[packetLength-1])
      {
        //check is the error packet is 0, which indicates no error
        if(returnPacket[3] == 0)
        {
          //check that the arm id packet is a valid arm
          if(returnPacket[1] == 1 || returnPacket[1] == 2 || returnPacket[1] == 3 || returnPacket[1] == 5)
          {
            printlnDebug("verifyPacket Success!");
            return(true);
          }
          else {printlnDebug("verifyPacket Error: Invalid Arm Detected! Arm ID:"+returnPacket[1]);}
        }
        else {printlnDebug("verifyPacket Error: Error Packet Reports:"+returnPacket[3]);}
      }
      else {printlnDebug("verifyPacket Error: Checksum does not match: Returned:"+ returnPacket[packetLength-1] +" Calculated:"+localChecksum );}
  }
  else {printlnDebug("verifyPacket Error: No Header!");}

  return(false);

}

/******************************************************
 *  checkArmStartup()
 *
 *  function used to check for the presense of a 
 *  ArbotiX/Arm on a serial port. 
 
 *  This function also sets the initial Global 'currentArm'
 *
 *  Parameters:
 *    None
 *
 *  Globals used:
 *    int currentArm
 *
 *  Returns: 
 *    boolean 
 *      true = arm has been detected on current serial port
 *      false = no arm detected on current serial port
 *
 ******************************************************/ 
public boolean checkArmStartup()
{
  byte[] returnPacket = new byte[5];  //byte array to hold return packet, which is 5 bytes long
  long startTime = millis();
  long currentTime = startTime;
  printlnDebug("Checking for arm on startup "); 
  while(currentTime - startTime < startupWaitTime )
  {
    delayMs(100);  //The ArbotiX has a delay of 50ms between starting the serial continueing the program, include an extra 10ms for other ArbotiX startup tasks
    for(int i = 0; i< sPorts.length;i++)
    {  
      if(sPorts[i] != null)
      {
        armPortIndex = i;
        
        printlnDebug("Checking for arm on startup - index# " + i); 
        sendCommanderPacket(0, 200, 200, 0, 512, 256, 128, 0, 112);    //send a commander style packet - the first 8 bytes are inconsequntial, only the last byte matters. '112' is the extended byte that will request an ID packet
        returnPacket = readFromArmFast(5);//read raw data from arm, complete with wait time
        
        if(verifyPacket(returnPacket) == true)
        {
          currentArm = returnPacket[1]; //set the current arm based on the return packet
          printlnDebug("Startup Arm #" +currentArm+ " Found"); 
          setPositionParameters();      //set the GUI default/min/maxes and field lables
          
          return(true) ;                //Return a true signal to signal that an arm has been found
        }
      }
    }

    currentTime = millis();
  }  
  armPortIndex = -1;
  return(false);
 

}


/******************************************************
 *  isArmConnected()
 *
 *  generic function to check for the presence of an arm
 *  during normal operation.
 *
 *  Parameters:
 *    None
 *
 *  Globals used:
 *    int currentArm
 *
 *  Returns: 
 *    boolean 
 *      true = arm has been detected on current serial port
 *      false = no arm detected on current serial port
 *
 ******************************************************/ 
public boolean isArmConnected()
{  
  byte[] returnPacket = new byte[5];//return id packet is 5 bytes long
 
  printlnDebug("Checking for arm -  sending packet"); 
  sendCommanderPacket(0, 200, 200, 0, 512, 256, 128, 0, 112);    //send a commander style packet - the first 8 bytes are inconsequntial, only the last byte matters. '112' is the extended byte that will request an ID packet
  
  returnPacket = readFromArm(5);//read raw data from arm, complete with wait time

  if(verifyPacket(returnPacket) == true)
  {
    printlnDebug("Arm Found"); 
    return(true) ;
  }
  else
  {
    printlnDebug("No Arm Found"); 
    return(false); 
  }
}

/******************************************************
 *  putArmToSleep()
 *
 *  function to put the arm to sleep. This will move 
 *  the arm to a 'rest' position and then turn the 
 * torque off for the servos
 *
 *  Parameters:
 *    None
 *
 *
 *  Returns: 
 *    boolean 
 *      true = arm has been put to sleep
 *      false = no return packet was detected from the arm.
 *
 ******************************************************/ 
public boolean putArmToSleep()
{
  printDebug("Attempting to put arm in sleep mode - "); 
  sendCommanderPacket(0,0,0,0,0,0,0,0,96);//only the last/extended byte matters - 96 signals the arm to go to sleep
  
  byte[] returnPacket = new byte[5];//return id packet is 5 bytes long
  returnPacket = readFromArm(5);//read raw data from arm
  if(verifyPacket(returnPacket) == true)
  {
    printlnDebug("Sleep mode success!"); 
    return(true) ;
  }
  else
  {
    printlnDebug("Sleep mode-No return packet detected"); 
    //displayError("There was a problem putting the arm in sleep mode","");
    
    genericMessageDialog("Arm Error", "There was a problem putting the arm in sleep mode.", G4P.WARNING);
    
    return(false); 
  }
}


/******************************************************
 *  changeArmMode()
 *
 *  sends a packet to set the arms mode and orientation
 *  based on the global mode and orientation values
 *  This function will send a packet with the extended 
 *  byte coresponding to the correct IK mode and wrist 
 *  orientation. The arm will move from its current 
 *  position to the 'home' position for the current 
 *  mode.
 *  Backhoe mode does not have different straight/
 *  90 degree modes.
 *  
 *  Extended byte - Mode 
 *  32 - cartesian, straight mode
 *  40 - cartesian, 90 degree mode
 *  48 - cylindrical, straight mode
 *  56 - cylindrical, 90 degree mode
 *  64 - backhoe
 *  
 *  Parameters:
 *    None
 *
 *  Globals used:
 *    currentMode
 *    currentOrientation
 *
 *  Returns: 
 *    boolean 
 *      true = arm has been put in the mode correctly
 *      false = no return packet was detected from the arm.
 *
 ******************************************************/ 
public boolean changeArmMode()
{
  
  byte[] returnPacket = new byte[5];//return id packet is 5 bytes long
  
 //switch based on the current mode
 switch(currentMode)
  {
    //cartesian mode case
    case 1:
      //switch based on the current orientation
      switch(currentOrientation)
      {
        case 1:
          sendCommanderPacket(0,0,0,0,0,0,0,0,32);//only the last/extended byte matters, 32 = cartesian, straight mode
          printDebug("Setting Arm to Cartesian IK mode, Gripper Angle Straight - "); 
          break;
        case 2:
          sendCommanderPacket(0,0,0,0,0,0,0,0,40);//only the last/extended byte matters, 40 = cartesian, 90 degree mode
          printDebug("Setting Arm to Cartesian IK mode, Gripper Angle 90 degree - "); 
          break;
      }//end orientation switch
      break;//end cartesian mode case
      
    //cylindrical mode case
    case 2:
      //switch based on the current orientation
      switch(currentOrientation)
      {
        case 1:
          sendCommanderPacket(0,0,0,0,0,0,0,0,48);//only the last/extended byte matters, 48 = cylindrical, straight mode
          printDebug("Setting Arm to Cylindrical IK mode, Gripper Angle Straight - "); 
          break;
        case 2:
          sendCommanderPacket(0,0,0,0,0,0,0,0,56);//only the last/extended byte matters, 56 = cylindrical, 90 degree mode
          printDebug("Setting Arm to Cylindrical IK mode, Gripper Angle 90 degree - "); 
          break;
      }//end orientation switch
      break;//end cylindrical mode case

    //backhoe mode case
    case 3:
      sendCommanderPacket(0,0,0,0,0,0,0,0,64);//only the last/extended byte matters, 64 = backhoe
          printDebug("Setting Arm to Backhoe IK mode - "); 
      break;//end backhoe mode case
  } 
  
  returnPacket = readFromArm(5);//read raw data from arm
  if(verifyPacket(returnPacket) == true)
  {
    printlnDebug("Response succesful! Arm mode changed"); 
    return(true) ;
  }
  else
  {
    printlnDebug("No Response - Failure?"); 
    
    //displayError("There was a problem setting the arm mode","");
    
    genericMessageDialog("Arm Error", "There was a problem setting the arm mode.", G4P.WARNING);
    
    return(false); 
  }
  
}

/******************************************************
 *  delayMs(int)
 *
 *  function waits/blocks the program for 'ms' milliseconds
 *  Used for very short delays where the program only needs
 *  to wait and does not need to execute code
 *  
 *  Parameters:
 *    int ms
 *      time, in milliseconds to wait
 *  Returns: 
 *    void
 ******************************************************/ 
public void delayMs(int ms)
{
  
  int time = millis();  //time that the program starts the loop
  while(millis()-time < ms)
  {
     //loop/do nothing until the different between the current time and 'time'
  }
}


/******************************************************
 *  sendCommanderPacket(int, int, int, int, int, int, int, int, int)
 *
 *  This function will send a commander style packet 
 *  the ArbotiX/Arm. This packet has 9 bytes and includes
 *  positional data, button data, and extended instructions.
 *  This function is often used with the function
 *  readFromArm()    
 *  to verify the packet was received correctly
 *   
 *  Parameters:
 *    int x
 *      offset X value (cartesian mode), or base value(Cylindrical and backhoe mode) - will be converted into 2 bytes
 *    int y
 *        Y Value (cartesian and cylindrical mode) or shoulder value(backhoe mode) - will be converted into 2 bytes
 *    int z
 *        Z Value (cartesian and cylindrical mode) or elbow value(backhoe mode) - will be converted into 2 bytes
 *    int wristAngle
 *      offset wristAngle value(cartesian and cylindrical mode) or wristAngle value (backhoe mode) - will be converted into 2 bytes
 *    int wristRotate
 *      offset wristRotate value(cartesian and cylindrical mode) or wristRotate value (backhoe mode) - will be converted into 2 bytes
 *    int gripper
 *      Gripper Value(All modes) - will be converted into 2 bytes
 *    int delta
 *      delta(speed) value (All modes) - will be converted into 1 byte
 *    int button
 *      digital button values (All modes) - will be converted into 1 byte
 *    int extended
 *       value for extended instruction / special instruction - will be converted into 1 byte
 *
 *  Global used: sPort
 *
 *  Return: 
 *    Void
 *
 ******************************************************/ 
public void sendCommanderPacket(int x, int y, int z, int wristAngle, int wristRotate, int gripper, int delta, int button, int extended)
{
  
  
      try
    {
       sPorts[armPortIndex].clear();//clear the serial port for the next round of communications  
    }
    //catch an exception in case of serial port problems
    catch(Exception e)
    {
       printlnDebug("Error: serial port problem");
       return;
    }   
    
  //convert each positional integer into 2 bytes using intToBytes()
  byte[] xValBytes = intToBytes(x);
  byte[] yValBytes = intToBytes(y);
  byte[] zValBytes =  intToBytes(z);
  byte[] wristRotValBytes = intToBytes(wristRotate);
  byte[] wristAngleValBytes = intToBytes(wristAngle);
  byte[] gripperValBytes = intToBytes(gripper);
  //cast int to bytes
  byte buttonByte = PApplet.parseByte(button);
  byte extValByte = PApplet.parseByte(extended);
  byte deltaValByte = PApplet.parseByte(delta);
  boolean flag = true;

 
  //calculate checksum - add all values, take lower byte (%256) and invert result (~). you can also invert results by (255-sum)
  byte checksum = (byte)(~(xValBytes[1]+xValBytes[0]+yValBytes[1]+yValBytes[0]+zValBytes[1]+zValBytes[0]+wristAngleValBytes[1]+wristAngleValBytes[0]+wristRotValBytes[1]+wristRotValBytes[0]+gripperValBytes[1]+gripperValBytes[0]+deltaValByte + buttonByte+extValByte)%256);

  //send commander style packet. Following labels are for cartesian mode, see function comments for clyindrical/backhoe mode
    //try to write the first header byte
    try
    {
      sPorts[armPortIndex].write(0xff);//header        
    }
    //catch an exception in case of serial port problems
    catch(Exception e)
    {
       printlnDebug("Error: packet not sent: " + e + ": 0xFF 0x" +hex(xValBytes[1]) +" 0x" +hex(xValBytes[0]) +" 0x" +hex(yValBytes[1]) +" 0x" +hex(yValBytes[0])+" 0x" +hex(zValBytes[1])+" 0x" +hex(zValBytes[0]) +" 0x" +hex(wristAngleValBytes[1]) +" 0x" +hex(wristAngleValBytes[0]) +" 0x" + hex(wristRotValBytes[1])+" 0x" +hex(wristRotValBytes[0]) +" 0x" + hex(gripperValBytes[1])+" 0x" + hex(gripperValBytes[0])+" 0x" + hex(deltaValByte)+" 0x" +hex(buttonByte) +" 0x" +hex(extValByte) +" 0x"+hex(checksum) +"",2); 
       flag = false;
    }   
    if(flag == true)
    {
      sPorts[armPortIndex].write(xValBytes[1]); //X Coord High Byte
      sPorts[armPortIndex].write(xValBytes[0]); //X Coord Low Byte
      sPorts[armPortIndex].write(yValBytes[1]); //Y Coord High Byte
      sPorts[armPortIndex].write(yValBytes[0]); //Y Coord Low Byte
      sPorts[armPortIndex].write(zValBytes[1]); //Z Coord High Byte
      sPorts[armPortIndex].write(zValBytes[0]); //Z Coord Low Byte
      sPorts[armPortIndex].write(wristAngleValBytes[1]); //Wrist Angle High Byte
      sPorts[armPortIndex].write(wristAngleValBytes[0]); //Wrist Angle Low Byte
      sPorts[armPortIndex].write(wristRotValBytes[1]); //Wrist Rotate High Byte
      sPorts[armPortIndex].write(wristRotValBytes[0]); //Wrist Rotate Low Byte
      sPorts[armPortIndex].write(gripperValBytes[1]); //Gripper High Byte
      sPorts[armPortIndex].write(gripperValBytes[0]); //Gripper Low Byte
      sPorts[armPortIndex].write(deltaValByte); //Delta Low Byte  
      sPorts[armPortIndex].write(buttonByte); //Button byte  
      sPorts[armPortIndex].write(extValByte); //Extended instruction  
      sPorts[armPortIndex].write(checksum);  //checksum
      printlnDebug("Packet Sent: 0xFF 0x" +hex(xValBytes[1]) +" 0x" +hex(xValBytes[0]) +" 0x" +hex(yValBytes[1]) +" 0x" +hex(yValBytes[0])+" 0x" +hex(zValBytes[1])+" 0x" +hex(zValBytes[0]) +" 0x" +hex(wristAngleValBytes[1]) +" 0x" +hex(wristAngleValBytes[0]) +" 0x" + hex(wristRotValBytes[1])+" 0x" +hex(wristRotValBytes[0]) +" 0x" + hex(gripperValBytes[1])+" 0x" + hex(gripperValBytes[0])+" 0x" + hex(deltaValByte)+" 0x" +hex(buttonByte) +" 0x" +hex(extValByte) +" 0x"+hex(checksum) +"",2); 
    }
  
    
  
  
  
  
         
}


//sends the commander packet if an only if the packet is different from the last one sent with this function. This stops duplicate packets from being sent multile times in a row
public void sendCommanderPacketWithCheck(int x, int y, int z, int wristAngle, int wristRotate, int gripper, int delta, int button, int extended)
{



   
   
    //check for changes - if there are no changes then don't send the packet to avoid sending multiple identical packets unnesscarsily

  if(  lastX != x || lastY != y || lastZ != z || lastWristangle != wristAngle || lastWristRotate != wristRotate || lastGripper != gripper || lastButton != button || lastExtended != extended || lastDelta != delta)
  {
    sendCommanderPacket(x,y, z, wristAngle,  wristRotate,  gripper,  delta,  button,  extended);
  }
    
    
 
     //holds the data from the last packet sent
  lastX = x;
  lastY = y;
  lastZ = z;
  lastWristangle = wristAngle;
  lastWristRotate = wristRotate;
  lastGripper = gripper;
  lastButton = button;
  lastExtended = extended;
  lastDelta = delta;
   
  

}

/******************************************************
 *  intToBytes(int)
 *
 *  This function will take an interger and convert it
 *  into two bytes. These bytes can then be easily 
 *  transmitted to the ArbotiX/Arm. Byte[0] is the low byte
 *  and Byte[1] is the high byte
 *   
 *  Parameters:
 *    int convertInt
 *      integer to be converted to bytes
 *  Return: 
 *    byte[]
 *      byte array with two bytes Byte[0] is the low byte and Byte[1] 
 *      is the high byte
 ******************************************************/ 
public byte[] intToBytes(int convertInt)
{
  byte[] returnBytes = new byte[2]; // array that holds the two bytes to return
  byte mask = PApplet.parseByte(255);          //mask for the low byte (255/0xff)
  returnBytes[0] =PApplet.parseByte(convertInt & mask);//low byte - perform an '&' operation with the byte mask to remove the high byte
  returnBytes[1] =PApplet.parseByte((convertInt>>8) & mask);//high byte - shift the byte to the right 8 bits. perform an '&' operation with the byte mask to remove any additional data
  return(returnBytes);  //return byte array
  
}

/******************************************************
 *  bytesToInt(byte[])
 *
 *  Take two bytes and convert them into an integer
 *   
 *  Parameters:
 *    byte[] convertBytes
 *      bytes to be converted to integer
 *  Return: 
 *    int
 *      integer value from 2 butes
 ******************************************************/ 
public int bytesToInt(byte[] convertBytes)
{
  return((PApplet.parseInt(convertBytes[1]<<8))+PApplet.parseInt(convertBytes[0]));//shift high byte up 8 bytes, and add it to the low byte. cast to int to ensure proper signed/unsigned behavior
}

/****************
 *  updateOffsetCoordinates()
 *
 *  modifies the current global coordinate
 *  with an appropriate offset
 *
 *  As the Arm Link software communicates in
 *  unsigned bytes, any value that has negative
 *  values in the GUI must be offset. This function
 *  will add the approprate offsets based on the 
 *  current mode of operation( global variable 'currentMode')
 *
 *  Parameters:
 *    None:
 *  Globals used:
 *    'Current' position vars
 *    'CurrentOffset' position vars
 *  Return: 
 *    void
 ***************/

public void  updateOffsetCoordinates()
{
  //offsets are applied based on current mode
  switch(currentMode)
    {
       case 1:        
         //x, wrist angle, and wrist rotate must be offset, all others are normal
         xCurrentOffset = xCurrent + 512;
         yCurrentOffset = yCurrent;
         zCurrentOffset = zCurrent;
         wristAngleCurrentOffset =  wristAngleCurrent + 90;
         //wristRotateCurrentOffset = wristRotateCurrent + 512;
         wristRotateCurrentOffset = wristRotateCurrent;
         gripperCurrentOffset = gripperCurrent;
         deltaCurrentOffset = deltaCurrent;
         break;
        
       case 2:
       
         //wrist angle, and wrist rotate must be offset, all others are normal
         xCurrentOffset = xCurrent;
         yCurrentOffset = yCurrent;
         zCurrentOffset = zCurrent;
         wristAngleCurrentOffset =  wristAngleCurrent + 90;
         //wristRotateCurrentOffset = wristRotateCurrent + 512;
         wristRotateCurrentOffset = wristRotateCurrent;
         gripperCurrentOffset = gripperCurrent;
         deltaCurrentOffset = deltaCurrent;
         break;
        
       case 3:
       
         //no offsets needed
         xCurrentOffset = xCurrent;
         yCurrentOffset = yCurrent;
         zCurrentOffset = zCurrent;
         wristAngleCurrentOffset =  wristAngleCurrent;
         wristRotateCurrentOffset = wristRotateCurrent;
         gripperCurrentOffset = gripperCurrent;
         deltaCurrentOffset = deltaCurrent;
        break; 
    }  
}

/****************
 *  updateButtonByte()
 *
 *  
 *
 *  Parameters:
 *    None:
 *  Globals used:
 *    int[] digitalButtons
 *    int digitalButtonByte
 *  Return: 
 *    void
 ***************/

public void updateButtonByte()
{
  digitalButtonByte = 0;
   for(int i=0;i<8;i++)
  {
    if(digitalButtons[i] == true)
    {
      digitalButtonByte += pow(2,i);
    }
  }
}


//TODO//
public boolean getArmInfo()
{
  return(true);
  
}




public int analogRead(int analogPort)
{
  byte[] returnPacket = new byte[5];  //byte array to hold return packet, which is 5 bytes long
  int analog = 0;
  printlnDebug("sending request for anlaog 1"); 
  int analogExtentded = 200 + analogPort;
  sendCommanderPacket(xCurrentOffset, yCurrentOffset, zCurrentOffset, wristAngleCurrentOffset, wristRotateCurrentOffset, gripperCurrentOffset, deltaCurrentOffset, digitalButtonByte, analogExtentded);    //send a commander style packet - the first 8 bytes are inconsequntial, only the last byte matters. '112' is the extended byte that will request an ID packet
  returnPacket = readFromArmFast(5);//read raw data from arm, complete with wait time
  byte[] analogBytes = {returnPacket[3],returnPacket[2]};
  analog = bytesToInt(analogBytes);
  
  printlnDebug("Return Packet" + PApplet.parseInt(returnPacket[0]) + "-" +  PApplet.parseInt(returnPacket[1]) + "-"  + PApplet.parseInt(returnPacket[2]) + "-"  + PApplet.parseInt(returnPacket[3]) + "-"  + PApplet.parseInt(returnPacket[4]));
  printlnDebug("analog value: " + analog);
  
  return(analog);
        
}
public int registerRead()
{

 byte[] returnPacket = new byte[5];  //byte array to hold return packet, which is 5 bytes long
  int registerVal = 0;
  printlnDebug("sending request for anlaog 1"); 
  int getRegExtentded = 0x81;
  sendCommanderPacket (regIdCurrent,regNumCurrent,regLengthCurrent, 0, 0, 0, 0, 0, getRegExtentded);
  returnPacket = readFromArmFast(5);//read raw data from arm, complete with wait time
  byte[] registerBytes = {returnPacket[3],returnPacket[2]};
  registerVal   = bytesToInt(registerBytes);
  
  printlnDebug("Return Packet" + PApplet.parseInt(returnPacket[0]) + "-" +  PApplet.parseInt(returnPacket[1]) + "-"  + PApplet.parseInt(returnPacket[2]) + "-"  + PApplet.parseInt(returnPacket[3]) + "-"  + PApplet.parseInt(returnPacket[4]));
  printlnDebug("analog value: " + registerVal);
  
  return(registerVal);
  

}
/***********************************************************************************
 *  }--\     InterbotiX     /--{
 *      |    Arm Link      |
 *   __/                    \__
 *  |__|                    |__|
 *
 *  global.pde
 *  
 *  This file has several global variables relating to the positional data for the arms.
 *  See 'ArmControl.pde' for building this application.
 *
 *
 * The following variables are named for Cartesian mode -
 * however the data that will be held/sent will vary based on the current IK mode
 ****************************************************************************
 * Variable name | Cartesian Mode | Cylindrcal Mode | Backhoe Mode          |
 *_______________|________________|_________________|_______________________|
 *   x           |   x            |   base          |   base joint          |
 *   y           |   y            |   y             |   shoulder joint      |
 *   z           |   z            |   z             |   elbow joint         |
 *   wristAngle  |  wristAngle    |  wristAngle     |   wrist angle joint   |
 *   wristRotate |  wristeRotate  |  wristeRotate   |   wrist rotate jount  |
 *   gripper     |  gripper       |  gripper        |   gripper joint       |
 *   delta       |  delta         |  delta          |   n/a                 |
********************************************************************************/

float programVersion = 1.601f;

int regIdCurrent = 0;
int regNumCurrent = 0;
int regLengthCurrent = 0;
int regValCurrent = 0;

//WORKING POSITION VARIABLES

//default values and min/max , {default, min, max}
//initially set to values for pincher in normal mode which should be safe for most arms (this shouldn't matter, as these values will get changed when an arm is connected)
//these parameters will be loaded based on the 1)Arm type 2)IK mode 3)Wrist Angle Orientation
int[] xParameters = {0,-200,200};//
int[] yParameters = {200,50,240};
int[] zParameters = {200,20,250};
int[] wristAngleParameters = {0,-90,90};
int[] wristRotateParameters = {0,-512,511};
int[] gripperParameters = {256,0,512};
int[] deltaParameters = {125,0,256};
int[] pauseParameters = {1000,0,10000};

//values for the current value directly from the GUI element. These are updated by the slider/text boxes
int xCurrent = xParameters[0]; //current x value in text field/slider
int yCurrent = yParameters[0]; //current y value in text field/slider
int zCurrent = zParameters[0]; //current z value in text field/slider
int wristAngleCurrent = wristAngleParameters[0]; //current Wrist Angle value in text field/slider
int wristRotateCurrent = wristRotateParameters[0]; //current  Wrist Rotate value in text field/slider
int gripperCurrent = gripperParameters[0]; //current Gripper value in text field/slider
int deltaCurrent = deltaParameters[0]; //current delta value in text field/slider};
int pauseCurrent = 1000;

//offset values to be send to the ArbotiX/Arm. whether or not these values get offsets depends on the current mode
//it will be possible for the 'Current' value to be the same as the 'currentOffset' value.
// see updateOffsetCoordinates()
int xCurrentOffset = xParameters[0]; //current x value to be send to ArbotiX/Arm
int yCurrentOffset = yParameters[0]; //current y value to be send to ArbotiX/Arm
int zCurrentOffset = zParameters[0]; //current z value to be send to ArbotiX/Arm
int wristAngleCurrentOffset = wristAngleParameters[0]; //current Wrist Angle value to be send to ArbotiX/Arm
int wristRotateCurrentOffset = wristRotateParameters[0]; //current  Wrist Rotate value to be send to ArbotiX/Arm
int gripperCurrentOffset = gripperParameters[0]; //current Gripper value to be send to ArbotiX/Arm
int deltaCurrentOffset = deltaParameters[0]; //current delta value to be send to ArbotiX/Arm

boolean[] digitalButtons = {false,false,false,false,false,false,false,false};  //array of 8 boolean to hold the current states of the checkboxes that correspond to the digital i/o
int digitalButtonByte;//int will hold the button byte (will be cast to byte later)

int extendedByte = 0;  //extended byte for special instructions


//END WORKING POSITION VARIABLES

//DEFAULT ARM PARAMETERS 

int numberOfArms = 5;

 //XYZ 
int[][] armParam0X = new int[numberOfArms][3];
int[][] armParam0Y = new int[numberOfArms][3];
int[][] armParam0Z = new int[numberOfArms][3];
int[][] armParam0WristAngle = new int[numberOfArms][3];
int[][] armParam0WristRotate = new int[numberOfArms][3];

int[][] armParam90X = new int[numberOfArms][3];
int[][] armParam90Y = new int[numberOfArms][3];
int[][] armParam90Z = new int[numberOfArms][3];
int[][] armParam90WristAngle = new int[numberOfArms][3];
int[][] armParam90WristRotate = new int[numberOfArms][3];



int[][] armParamBase = new int[numberOfArms][3];
int[][] armParamBHShoulder = new int[numberOfArms][3];
int[][] armParamBHElbow = new int[numberOfArms][3];
int[][] armParamBHWristAngle = new int[numberOfArms][3];
int[][] armParamBHWristRot = new int[numberOfArms][3];


int[][] armParamGripper = new int[numberOfArms][3];


int[][] armParamWristAngle0Knob = new int[numberOfArms][2];
int[][] armParamWristAngle90Knob = new int[numberOfArms][2];
int[][] armParamWristAngleBHKnob = new int[numberOfArms][2];
int[][] armParamWristRotKnob= new int[numberOfArms][2];

int[][] armParamBaseKnob = new int[numberOfArms][2];
int[][] armParamElbowKnob = new int[numberOfArms][2];
int[][] armParamShoulderKnob = new int[numberOfArms][2];
float[] armParamElbowKnobRotation = new float[numberOfArms];


//default values for the phantomX pincher. These will be loaded into the working position variables 
//when the pincher is connected, and when modes are changed.
int[] pincherNormalX = {0,-200,200};
int[] pincherNormalY = {170,50,240};
int[] pincherNormalZ = {210,20,250};
int[] pincherNormalWristAngle = {0,-30,30};
int[] pincherWristRotate = {0,0,0};//not implemented in hardware
int[] pincherGripper = {256,0,512};
int[] pincher90X = {0,-200,200};
int[] pincher90Y = {140,20,150};
int[] pincher90Z = {30,10,150};
int[] pincher90WristAngle = {-90,-90,-45};
int[] pincherBase = {512,1023,0};
int[] pincherBHShoulder = {512,815,205};
int[] pincherBHElbow = {512,1023,205};
int[] pincherBHWristAngle = {512,815,205};
int[] pincherBHWristRot = {512,0,1023};

int[] pincherBHWristAngleNormalKnob = {150,210};//angle data for knob limits
int[] pincherBHWristAngle90Knob = {90,45};//angle data for knob limits

int[] pincherWristAngleBHKnob = {90,270};//angle data for knob limits
int[] pincherWristRotKnob = {120,60};

int[] pincherBaseKnob = {120,60};
int[] pincherShoulderKnob = {180,0};
int[] pincherElbowKnob = {180,60};
float pincherElbowKnobRotation = -PI*1/3;



//default values for the phantomX reactor. These will be loaded into the working position variables 
//when the reactor is connected, and when modes are changed.
int[] reactorNormalX = {0,-300,300};
int[] reactorNormalY = {235,50,350};
int[] reactorNormalZ = {210,20,250};
int[] reactorNormalWristAngle = {0,-30,30};
//int[] reactorWristRotate = {0,511,-512};
int[] reactorWristRotate = {512,0,1023};
int[] reactorGripper = {256,0,512};
int[] reactor90X = {0,-300,300};
int[] reactor90Y = {140,20,150};
int[] reactor90Z = {30,10,150};
int[] reactor90WristAngle = {-90,-90,-45};
int[] reactorBase = {512,1023,0};
int[] reactorBHShoulder = {512,810,205};
int[] reactorBHElbow = {512,210,900};
int[] reactorBHWristAngle = {512,200,830};
int[] reactorBHWristRot = {512,1023,0};

int[] reactorWristAngleNormalKnob = {150,210};//angle data for knob limits
int[] reactorWristAngle90Knob = {90,135};//angle data for knob limits
int[] reactorWristAngleBHKnob = {90,270};//angle data for knob limits
int[] reactorWristRotKnob = {120,60};
int[] reactorBaseKnob = {120,60};
int[] reactorShoulderKnob = {180,0};
int[] reactorElbowKnob = {180,30};
float reactorElbowKnobRotation = 0;



//default values for the widowx. These will be loaded into the working position variables 
//when the widowx is connected, and when modes are changed.
int[] widowNormalX = {0,-300,300};
int[] widowNormalY = {250,50,400};
int[] widowNormalZ = {225,20,350};
int[] widowNormalWristAngle = {0,-30,30};
int[] widowWristRotate = {512,0,1023};
int[] widowGripper = {256,0,512};
int[] widow90X = {0,-300,300};
int[] widow90Y = {150,20,250};
int[] widow90Z = {30,10,200};
int[] widow90WristAngle = {-90,-90,-45};
int[] widowBase = {2048,4095,0};
int[] widowBHShoulder = {2048,3072,1024};
int[] widowBHElbow = {2048,1024,3072};
int[] widowBHWristAngle = {2048,1024,3072};
int[] widowBHWristRot = {512,1023,0};

int[] widowBHWristAngleNormalKnob = {150,210};//angle data for knob limits
int[] widowBHWristAngle90Knob = {90,135};//angle data for knob limits

int[] widowWristAngleBHKnob = {90,270};//angle data for knob limits
int[] widowWristRotKnob = {120,60};

int[] widowBaseKnob = {90,90};
int[] widowShoulderKnob = {180,0};
int[] widowElbowKnob =  {180,0};
float widowElbowKnobRotation = 0;//-PI*1/3;



//default values for the RobotGeek Snapper. These will be loaded into the working position variables 
//when the snapper is connected, and when modes are changed.
int[] snapperNormalX = {0,-150,150};
int[] snapperNormalY = {150,55,200}; //the limits are really 50-200, but 50-54 cause a problem when height Z is maximum
int[] snapperNormalZ = {150,20,225};
int[] snapperNormalWristAngle = {0,-30,30};


int[] snapperWristRotate = {0,0,0};  //Not Implemented in Snapper hardware
int[] snapperGripper = {1500,750,2400};
int[] snapper90X = {0,-200,200};          //Not Implemented in Snapper firmware
int[] snapper90Y = {140,20,150};          //Not Implemented in Snapper firmware
int[] snapper90Z = {30,10,150};          //Not Implemented in Snapper firmware
int[] snapper90WristAngle = {-90,-90,-45};          //Not Implemented in Snapper firmware
int[] snapperBase = {1500,2400,600};          //Not Implemented in Snapper firmware
int[] snapperBHShoulder = {1500,600,2400};          //Not Implemented in Snapper firmware
int[] snapperBHElbow = {1500,2400,600};          //Not Implemented in Snapper firmware
int[] snapperBHWristAngle = {1500,2400,600};          //Not Implemented in Snapper firmware
int[] snapperBHWristRot = {1500,600,2400};          //Not Implemented in Snapper firmware

int[] snapperBHWristAngleNormalKnob = {150,210};//angle data for knob limits
int[] snapperBHWristAngle90Knob = {90,45};//angle data for knob limits

int[] snapperWristAngleBHKnob = {270,90};//angle data for knob limits
int[] snapperWristRotKnob = {120,60};

int[] snapperBaseKnob = {180,360};
int[] snapperShoulderKnob = {180,360};
int[] snapperElbowKnob = {180,360};
float snapperElbowKnobRotation = 0;//-PI*1/3;




//END DEFAULT ARM PARAMETERS 
/***********************************************************************************
 *  }--\     InterbotiX     /--{
 *      |    Arm Link      |
 *   __/                    \__
 *  |__|                    |__|
 *
 *  gui.pde
 *  
 *  This file has all of the variables and functions regarding the gui.
 *  See 'ArmControl.pde' for building this application.
 *
 *
 * The following variables are named for Cartesian mode -
 * however the data that will be held/sent will vary based on the current IK mode
 ****************************************************************************
 * Variable name | Cartesian Mode | Cylindrcal Mode | Backhoe Mode          |
 *_______________|________________|_________________|_______________________|
 *   x           |   x            |   base          |   base joint          |
 *   y           |   y            |   y             |   shoulder joint      |
 *   z           |   z            |   z             |   elbow joint         |
 *   wristAngle  |  wristAngle    |  wristAngle     |   wrist angle joint   |
 *   wristRotate |  wristeRotate  |  wristeRotate   |   wrist rotate jount  |
 *   gripper     |  gripper       |  gripper        |   gripper joint       |
 *   delta       |  delta         |  delta          |   n/a                 |
********************************************************************************/


GButton helpButton; 

//setup panel
GPanel setupPanel; //panel to hold setup data
GPanel ioPanel; //panel to hold I/O data
GPanel sequencePanel;
GDropList serialList; //drop down to hold list of serial ports
GButton connectButton,disconnectButton,autoConnectButton; //buttons for connecting/disconnecting and auto seeach

//mode panel
GPanel modePanel; //panel to hold current mode buttons
//wrist panel
GPanel wristPanel; //panel to hold current wrist buttons

GButton cartesianModeButton, cylindricalModeButton, backhoeModeButton,orient90Button, orientStraightButton; //buttons to chage IK mode
GButton setRegisterButton, getRegisterButton;

//DEPRACATED GImageButton armStraightButton,arm90Button;//image buttons to hold wrist angle orientation mode

//control panel
GPanel controlPanel; 
//text fields for positional data/delta/extended
GTextField xTextField, yTextField, zTextField, wristAngleTextField, wristRotateTextField, gripperTextField, deltaTextField, extendedTextField;
GTextField pauseTextField;
GTextField regId, regLength, regNum, regVal;
GLabel regIdLabel, regLengthLabel, regNumLabel, regValLabel;
//sliders for positional data/delta
GSlider xSlider, ySlider, zSlider, wristAngleSlider, wristRotateSlider, gripperSlider, deltaSlider; 
//text labels for positional data/delta/extended
GLabel xLabel, yLabel, zLabel, wristAngleLabel, wristRotateLabel, gripperLabel, deltaLabel, extendedLabel,digitalsLabel,analogTextLabel, cameraLabel;
GLabel pauseLabel;
//checkboxes for digital output values
GCheckbox digitalCheckbox0, digitalCheckbox1, digitalCheckbox2, digitalCheckbox3, digitalCheckbox4, digitalCheckbox5, digitalCheckbox6, digitalCheckbox7, analogCheckbox, cameraCheckbox, setRegisterCheckbox, digitalOutputFileCheckbox; 
GCheckbox autoUpdateCheckbox;   //checkbox to enable auto-update mode
GButton updateButton;           //button to manually update
GImageButton waitingButton;    //waiting button, unused
GKnob baseKnob, shoulderKnob,elbowKnob ,wristAngleKnob ,wristRotateKnob; //knob to turn the base
GCustomSlider gripperLeftSlider, gripperRightSlider;

GSlider pauseSlider;//defunct

//error panel
GPanel errorPanel;//panel to warn users of errros
GButton errorOkButton;   //button to dismiss error panel
GButton errorLinkButton; //help link button
GLabel errorLabel;       //error text
GLabel statusLabel;

//settings panel
GPanel settingsPanel;//various settings for the program
GButton settingsDismissButton;//dismiss the settings panel
GCheckbox fileDebugCheckbox; //checkbox to enable debugging to file
GCheckbox debugFileCheckbox0; //???


GButton movePosesUp;//button 1 
GButton newPose; //button 2
GButton movePosesDown; //button3
GButton workspaceToPose; //button 2
GButton poseToWorkspace; //button3



GButton analog1; //button3
GButton playButton; //play sequence
GButton stopButton; //stop sequence

GButton savePosesButton, emergencyStopButton, loadPosesButton;


CopyOnWriteArrayList<GPanel> poses;


GLabel[] analogLabel = new GLabel[8];



// **********************Setup GUI functions

public void setupPanel_click(GPanel source, GEvent event) { 
  printlnDebug("setupPanel - GPanel event occured " + System.currentTimeMillis()%10000000, 1 );
} 
public void ioPanel_click(GPanel source, GEvent event) { 
  printlnDebug("ioPanel - GPanel event occured " + System.currentTimeMillis()%10000000, 1 );
} 
public void sequencePanel_click(GPanel source, GEvent event) { 
  printlnDebug("sequencePanel - GPanel event occured " + System.currentTimeMillis()%10000000, 1 );
} 

//Called when a new serial port is selected
public void serialList_click(GDropList source, GEvent event) 
{
  printlnDebug("serialList - GDropList event occured: item #" + serialList.getSelectedIndex() + " " + System.currentTimeMillis()%10000000, 1 );
  
  
  selectedSerialPort = serialList.getSelectedIndex()-1; //set the current selectSerialPort corresponding to the serial port selected in the menu. Subtract 1 to offset for the fact that the first item in the list is a placeholder text/title 'Select Serial Port'
  printlnDebug("Serial port at position " +selectedSerialPort+ " chosen");
} 

//called when the connect button is pressed
public void connectButton_click(GButton source, GEvent event) 
{
  printlnDebug("connectButton - GButton event occured " + System.currentTimeMillis()%10000000, 1);
 


  statusLabel.setText("Connecting...");
  connectFlag = 1;
} 

//disconnect from current serial port and set GUI element states appropriatley
public void disconnectButton_click(GButton source, GEvent event) 
{
  printlnDebug("disconnectButton - GButton event occured " + System.currentTimeMillis()%10000000, 1);
  
  
    statusLabel.setText("Disconnecting...");
  disconnectFlag = 1;
} 



//scan each serial port and querythe port for an active arm. Iterate through each port until a 
//port is found or the list is exhausted
public void autoConnectButton_click(GButton source, GEvent event) 
{
  printlnDebug("autoConnectButton - GButton event occured " + System.currentTimeMillis()%10000000, 1 );


    statusLabel.setText("Scanning...");
  autoConnectFlag = 1;
}



//**********************Mode GUI functions

public void modePanel_click(GPanel source, GEvent event) 
{ 
  printlnDebug("modePanel - GPanel event occured " + System.currentTimeMillis()%10000000 );
} 
public void wristPanel_click(GPanel source, GEvent event) 
{ 
  printlnDebug("wristPanel - GPanel event occured " + System.currentTimeMillis()%10000000 );
} 

//change mode data when button to straigten gripper angle is pressed
public void orientStraightButton_click(GButton source, GEvent event) 
{ 
  printlnDebug("armstraight - GCheckbox event occured " + System.currentTimeMillis()%10000000, 1 );
  
setOrientStraight();

} 
public void setOrientStraight()
{  
  if(currentOrientation != 1)
  {
   clearPoses();
  
  }
  if (currentMode == 0)
  {
  currentMode =1;
  cartesianModeButton.setLocalColorScheme(GCScheme.GOLD_SCHEME);
  }
  
  //DEPRECATED armStraightButton.setAlpha(255);
  //DEPRECATED arm90Button.setAlpha(128);
  
  
  orient90Button.setLocalColorScheme(GCScheme.BLUE_SCHEME);
  orientStraightButton.setLocalColorScheme(GCScheme.GOLD_SCHEME);
  
  currentOrientation = 1;
  setPositionParameters();
  changeArmMode();

}

//change mode data when button to move  gripper angle to 90 degrees is pressed
public void orient90Button_click(GButton source, GEvent event) 
{
  printlnDebug("arm90 - GCheckbox event occured " + System.currentTimeMillis()%10000000, 1 );
      setOrient90();

} 


public void setOrient90()
{
  
  //statusLabel.setText("Changing Mode...");
  
  if(currentOrientation != 2)
  {
   clearPoses();

  }
 
  //set default mode if none has been set
  if (currentMode == 0)
  {
    currentMode =1;
    cartesianModeButton.setLocalColorScheme(GCScheme.GOLD_SCHEME);

  }

  
  //DEPRECATED armStraightButton.setAlpha(128);
  //DEPRECATED arm90Button.setAlpha(255);
  
  
  orient90Button.setLocalColorScheme(GCScheme.GOLD_SCHEME);
  orientStraightButton.setLocalColorScheme(GCScheme.BLUE_SCHEME);
  
  currentOrientation = 2;
  setPositionParameters();
  changeArmMode();
  updateFlag = true;//set update flag to signal sending an update on the next cycle

}


//change ik mode to cartesian
public void setRegisterButton_click(GButton source, GEvent event) 
{ 
  printlnDebug("setRegisterButton_click - GButton event occured " + System.currentTimeMillis()%10000000, 1 );
   regIdCurrent = PApplet.parseInt(regId.getText());
 regNumCurrent = PApplet.parseInt(regNum.getText());
regLengthCurrent = PApplet.parseInt(regLength.getText());
regValCurrent = PApplet.parseInt(regVal.getText());

  sendCommanderPacket (regIdCurrent,regNumCurrent,regLengthCurrent, regValCurrent, 0, 0, 0, 0, 0x82);


} 

//change ik mode to cartesian
public void getRegisterButton_click(GButton source, GEvent event) 
{ 
  printlnDebug("getRegisterButton_click - GButton event occured " + System.currentTimeMillis()%10000000, 1 );
  
  
 regIdCurrent = PApplet.parseInt(regId.getText());
 regNumCurrent = PApplet.parseInt(regNum.getText());
regLengthCurrent = PApplet.parseInt(regLength.getText());
regValCurrent = PApplet.parseInt(regVal.getText());

  int tempReg = registerRead();

  regVal.setText(Integer.toString(tempReg));
  //sendCommanderPacket (regIdCurrent,regNumCurrent,regLengthCurrent, 0, 0, 0, 0, 0, 0x81);
  

} 




//change ik mode to cartesian
public void cartesianModeButton_click(GButton source, GEvent event) 
{ 
  printlnDebug("cartesianModeButton - GButton event occured " + System.currentTimeMillis()%10000000, 1 );
  

 regIdCurrent = PApplet.parseInt(regId.getText());
 regNumCurrent = PApplet.parseInt(regNum.getText());
regLengthCurrent = PApplet.parseInt(regLength.getText());
regValCurrent = PApplet.parseInt(regVal.getText());
  
  setCartesian();

} 

public void setCartesian()
{
  if(currentMode != 1)
  {
    clearPoses();
  }
  
    //set ik mode buttons to correct colors
  cartesianModeButton.setLocalColorScheme(GCScheme.GOLD_SCHEME);
  cylindricalModeButton.setLocalColorScheme(GCScheme.CYAN_SCHEME);
  backhoeModeButton.setLocalColorScheme(GCScheme.CYAN_SCHEME);
  
  
  //Show/enable straight/90 wrist mode
  wristPanel.setAlpha(255);
  wristPanel.setVisible(true);
  wristPanel.setEnabled(true);
  
  //set wrist angle orientation if not defined
  if (currentOrientation == 0)
  {
    currentOrientation =1;
  }
   
  
  //set wrist angle orientation if not defined
  if (currentOrientation == 0)
  {
    currentOrientation =1;
  }
  
  if (currentOrientation == 1)
  {
    orientStraightButton.setLocalColorScheme(GCScheme.GOLD_SCHEME);
    orient90Button.setLocalColorScheme(GCScheme.BLUE_SCHEME);
  }
  else if (currentOrientation == 2)
  {
    orientStraightButton.setLocalColorScheme(GCScheme.BLUE_SCHEME);
    orient90Button.setLocalColorScheme(GCScheme.GOLD_SCHEME);
  }

  currentMode = 1;//set mode data
  setPositionParameters();//set parameters in gui and working vars
  changeArmMode();//change arm mode
  updateFlag = true;//set update flag to signal sending an update on the next cycle
}



//change ik mode to cylindrical
public void cylindricalModeButton_click(GButton source, GEvent event) 
{ 
   printlnDebug("cylindricalModeButton - GButton event occured " + System.currentTimeMillis()%10000000, 1 );
  

  setCylindrical();

} 

public void setCylindrical()
{
  if(currentMode != 2)
  {
    clearPoses();
  }
  //set ik mode buttons to correct colors
  cylindricalModeButton.setLocalColorScheme(GCScheme.GOLD_SCHEME);
  cartesianModeButton.setLocalColorScheme(GCScheme.CYAN_SCHEME);
  backhoeModeButton.setLocalColorScheme(GCScheme.CYAN_SCHEME);
  
  
  
  //Enable straight/90 wrist panel
  wristPanel.setAlpha(255);
  wristPanel.setVisible(true);
  wristPanel.setEnabled(true);
  
  
  
  //set wrist angle orientation if not defined
  if (currentOrientation == 0)
  {
    currentOrientation =1;
  }
  
  if (currentOrientation == 1)
  {
    orientStraightButton.setLocalColorScheme(GCScheme.GOLD_SCHEME);
    orient90Button.setLocalColorScheme(GCScheme.BLUE_SCHEME);
  }
  else if (currentOrientation == 2)
  {
    orientStraightButton.setLocalColorScheme(GCScheme.BLUE_SCHEME);
    orient90Button.setLocalColorScheme(GCScheme.GOLD_SCHEME);
  }
  
  currentMode = 2;//set mode data
  setPositionParameters();//set parameters in gui and working vars
  changeArmMode();//change arm mode

}



public void clearPoses()
{
   for(int i = 0; i<poses.size(); i++)
  {
    poses.get(i).dispose();
    
  }
  poses.clear();
  poseData.clear();
  numPanels = 0;


  
}




//change ik mode to backhoe
public void backhoeModeButton_click(GButton source, GEvent event) 
{ 
  printlnDebug("backhoeModeButton - GButton event occured " + System.currentTimeMillis()%10000000, 1 );


  setBackhoe();


}

public void setBackhoe()
{
  if(currentMode != 3)
  {
    clearPoses();
  }
  //set ik mode buttons to correct colors
  backhoeModeButton.setLocalColorScheme(GCScheme.GOLD_SCHEME);
  cylindricalModeButton.setLocalColorScheme(GCScheme.CYAN_SCHEME);
  cartesianModeButton.setLocalColorScheme(GCScheme.CYAN_SCHEME);
  
  //DEPRICATED
  //set wrist angle alphas to 128(grey out)
  //armStraightButton.setAlpha(128);
  //arm90Button.setAlpha(128);
  
  
  //Backhoe mode does not need straight/90 wrist mode, so hide and disable the panel
  wristPanel.setAlpha(64);
//  wristPanel.setVisible(false);
  wristPanel.setEnabled(false);
  
  
  
  currentMode = 3;//set mode data
  
  setPositionParameters();//set parameters in gui and working vars
  changeArmMode();//change arm mode
  
  updateFlag = true;//set update flag to signal sending an update on the next cycle



}



//process when manual update button is pressed
public void updateButton_click(GButton source, GEvent event) 
{
  printlnDebug("updateButton - GButton event occured " + System.currentTimeMillis()%10000000, 1 );
  
  updateFlag = true;//set update flag to signal sending an update on the next cycle
  updateOffsetCoordinates();//update the coordinates to offset based on the current mode

  printlnDebug("X:"+xCurrentOffset+" Y:"+yCurrentOffset+" Z:"+zCurrentOffset+" Wrist Angle:"+wristAngleCurrentOffset+" Wrist Rotate:"+wristRotateCurrentOffset+" Gripper:"+gripperCurrentOffset+" Delta:"+deltaCurrentOffset);
}



//**********************Control GUI functions
public void controlPanel_click(GPanel source, GEvent event) { //_CODE_:controlPanel:613752:
  printlnDebug("controlPanel - GPanel event occured " + System.currentTimeMillis()%10000000, 1 );
} //_CODE_:controlPanel:613752:



//generic function that  will handle text field changes. This function will
//-check the text field for non numeric characters
//-check for values out of the current range
//-write the value to the slider upon an 'enter' or when the fiels loses focus
//-
public int armTextFieldChange(GTextField source, GEvent event, GValueControl targetSlider, int minVal, int maxVal, int currentVal )
{
   //swap min/max when needed
  if(minVal > maxVal)
  {
     int tempVal = minVal;
     minVal = maxVal;
     maxVal = tempVal; 
  }
  
  
  String textFieldString = source.getText();//string value from textField
  int textFieldValue;//converted integer from textField

  //parse through each character in the string to make sure that it is a digit
  for (int i = 0; i < textFieldString.length(); i++)
  {
    //get single character and check if it is not a digit
    //in non digit character is found, return text field to previous value
    //otherwise continue
    printDebug(textFieldString.charAt(i) + " ", 1 );
    if (!Character.isDigit(textFieldString.charAt(i)))
    {
      //'-' character is used for negative numbers, so check that the current character is not '-'
      //otherwise continue 
      if (textFieldString.charAt(i) != '-')
      {
        printlnDebug("Non Numeric Character in Textfield, reverting value", 1 );
        source.setText(Integer.toString(currentVal));//set string to global Current Value, last known good value  
        return(currentVal); 
        //TODO: alternativeley the program could remove the offending character and write the string back
      }
    }
  }
  printlnDebug("", 1 );

  //only write value to slider/global if the enter key is pressed or focus is lost on the text field
  if (event == GEvent.ENTERED || event == GEvent.LOST_FOCUS)
  {
    textFieldValue = PApplet.parseInt(textFieldString);//take String from text field and conver it to an int

    //check if the value is over the global max for this field - if so, set the textField value to the maximum value
    if (textFieldValue > maxVal)
    {
      source.setText(Integer.toString(maxVal));//append a "" for easy string conversion 
      textFieldValue = maxVal;
    }

    //check if the value is under the global min for this field - if so, set the textField value to the minimum value
    if (textFieldValue < minVal)
    {
      source.setText(Integer.toString(minVal));//append a "" for easy string conversion      
      textFieldValue = minVal;
    }

    targetSlider.setValue(textFieldValue);
    return(textFieldValue);
  }

  return(currentVal);
}


  public void regId_change(GTextField source, GEvent event) 
{
  printlnDebug("xTextField_change - GTextField event occured " + System.currentTimeMillis()%10000000, 1 );
  //regIdCurrent = int(source.getText());
}
public void regLength_change(GTextField source, GEvent event) 
{
  printlnDebug("regLength_change - GTextField event occured " + System.currentTimeMillis()%10000000, 1 );
  //regLengthCurrent = int(source.getText());
}
public void regNum_change(GTextField source, GEvent event) 
{
  printlnDebug("regNum_change - GTextField event occured " + System.currentTimeMillis()%10000000, 1 );
  //regNumCurrent = int(source.getText());
}
public void regVal_change(GTextField source, GEvent event) 
{
  printlnDebug("regVal_change - GTextField event occured " + System.currentTimeMillis()%10000000, 1 );
  //regValCurrent = int(source.getText());
}





public void xTextField_change(GTextField source, GEvent event) 
{
  printlnDebug("xTextField_change - GTextField event occured " + System.currentTimeMillis()%10000000, 1 );
  xCurrent = armTextFieldChange(source, event, xSlider, xParameters[1], xParameters[2], xCurrent);
  xCurrent = armTextFieldChange(source, event, baseKnob, xParameters[1], xParameters[2], xCurrent);
}

public void yTextField_change(GTextField source, GEvent event) 
{
  printlnDebug("yTextField_change - GTextField event occured " + System.currentTimeMillis()%10000000, 1 );
  yCurrent = armTextFieldChange(source, event, ySlider, yParameters[1], yParameters[2], yCurrent);
  yCurrent = armTextFieldChange(source, event, shoulderKnob, yParameters[1], yParameters[2], yCurrent);
}

public void zTextField_change(GTextField source, GEvent event) 
{
  printlnDebug("zTextField_change - GTextField event occured " + System.currentTimeMillis()%10000000, 1 );
  zCurrent = armTextFieldChange(source, event, zSlider, zParameters[1], zParameters[2], zCurrent);
  zCurrent = armTextFieldChange(source, event, elbowKnob, zParameters[1], zParameters[2], zCurrent);
}

public void wristAngleTextField_change(GTextField source, GEvent event) 
{
  printlnDebug("wristAngleTextField_change - GTextField event occured " + System.currentTimeMillis()%10000000, 1 );
  wristAngleCurrent = armTextFieldChange(source, event, wristAngleKnob, wristAngleParameters[1], wristAngleParameters[2], wristAngleCurrent);
}

public void wristRotateTextField_change(GTextField source, GEvent event) 
{
  printlnDebug("wristRotateTextField_change - GTextField event occured " + System.currentTimeMillis()%10000000, 1 );
  wristRotateCurrent = armTextFieldChange(source, event, wristRotateKnob, wristRotateParameters[1], wristRotateParameters[2], wristRotateCurrent);
}

public void gripperTextField_change(GTextField source, GEvent event) 
{
  printlnDebug("gripperTextField_change - GTextField event occured " + System.currentTimeMillis()%10000000, 1 );
  gripperCurrent = armTextFieldChange(source, event, gripperLeftSlider, gripperParameters[1], gripperParameters[2], gripperCurrent);
  gripperCurrent = armTextFieldChange(source, event, gripperRightSlider, gripperParameters[1], gripperParameters[2], gripperCurrent);
}


public void deltaTextField_change(GTextField source, GEvent event) 
{
  printlnDebug("deltaTextField_change - GTextField event occured " + System.currentTimeMillis()%10000000, 1 );
  deltaCurrent = armTextFieldChange(source, event, deltaSlider, deltaParameters[1], deltaParameters[2], deltaCurrent);
}



//special case for extended mode text field
public void extendedTextField_change(GTextField source, GEvent event) 
{
  printlnDebug("extendedTextField_change - GDropList event occured " + System.currentTimeMillis()%10000000, 1 );
  String textFieldString = source.getText();//string value from textField
  int textFieldValue = 0;//converted integer from textField

  //parse through each character in the string to make sure that it is a digit
  for (int i = 0; i < textFieldString.length(); i++)
  {
    //get single character and check if it is not a digit
    //in non digit character is found, return text field to previous value
    //otherwise continue
    //printlnDebug(textFieldString.charAt(i));
    if (!Character.isDigit(textFieldString.charAt(i)))
    {
      //'-' character is used for negative numbers, so check that the current character is not '-'
      //otherwise continue 
      if (textFieldString.charAt(i) != '-')
      {
        source.setText(Integer.toString(extendedByte));//set string to global xCurrent Value, last known good value  
        //TODO: alternativeley the program could remove the offending character and write the string back
      }
    }
  }

  //only write value to global if enter key is pressed or lose focus on fieles
  if (event == GEvent.ENTERED || event == GEvent.LOST_FOCUS)
  {
    printlnDebug("Change Extended Byte");
    extendedByte = PApplet.parseInt(textFieldString);//take String from text field and conver it to an int

    //check if the value is over the global max for this field - if so, set the textField value to the maximum value
    if (extendedByte > 255)
    {
      source.setText(Integer.toString(255));//append a "" for easy string conversion 
      extendedByte = 255;
    }

    //check if the value is under the global min for this field - if so, set the textField value to the minimum value
    if (extendedByte < 0)
    {
      source.setText(Integer.toString(0));//append a "" for easy string conversion      
      extendedByte = 0;
    }
  }
}



//text field to hold pause time between poses
public void pauseTextField_change(GTextField source, GEvent event) 
{
  printlnDebug("pauseTextField_change - GTextField event occured " + System.currentTimeMillis()%10000000, 1 );

    pauseCurrent = armTextFieldChange(source, event, pauseSlider, pauseParameters[1], pauseParameters[2], pauseCurrent);




}

public void baseKnob_change(GKnob source, GEvent event) 
{
  printlnDebug("baseKnob_change - GSlider event occured " + System.currentTimeMillis()%10000000, 1 );
  if (event == GEvent.VALUE_STEADY)
  {
    xTextField.setText(Integer.toString(source.getValueI()));//append a "" for easy string conversion
    xCurrent = source.getValueI();
  }
}

public void shoulderKnob_change(GKnob source, GEvent event) 
{
  printlnDebug("shoulderKnob_change - GSlider event occured " + System.currentTimeMillis()%10000000, 1 );
  if (event == GEvent.VALUE_STEADY)
  {
    yTextField.setText(Integer.toString(source.getValueI()));//append a "" for easy string conversion
    yCurrent = source.getValueI();
  }
}

public void elbowKnob_change(GKnob source, GEvent event) 
{
  printlnDebug("elbowKnob_change - GSlider event occured " + System.currentTimeMillis()%10000000, 1 );
  if (event == GEvent.VALUE_STEADY)
  {
    zTextField.setText(Integer.toString(source.getValueI()));//append a "" for easy string conversion
    zCurrent = source.getValueI();
  }
}

public void wristAngleKnob_change(GKnob source, GEvent event) 
{
  printlnDebug("xSlider_change - GSlider event occured " + System.currentTimeMillis()%10000000, 1 );
  if (event == GEvent.VALUE_STEADY)
  {
    wristAngleTextField.setText(Integer.toString(source.getValueI()));//append a "" for easy string conversion 
    wristAngleCurrent = source.getValueI();
  }
}

public void wristRotateKnob_change(GKnob source, GEvent event) 
{
  printlnDebug("xSlider_change - GSlider event occured " + System.currentTimeMillis()%10000000, 1 );
  if (event == GEvent.VALUE_STEADY)
  {
    wristRotateTextField.setText(Integer.toString(source.getValueI()));//append a "" for easy string conversion 
    wristRotateCurrent = source.getValueI();
  }
}







//update text field and working var based on slider change 
public void xSlider_change(GSlider source, GEvent event) 
{
  printlnDebug("xSlider_change - GSlider event occured " + System.currentTimeMillis()%10000000, 1 );
  if (event == GEvent.VALUE_STEADY)
  {
    xTextField.setText(Integer.toString(source.getValueI()));//append a "" for easy string conversion
    xCurrent = source.getValueI();
  }
}

//update text field and working var based on slider change 
public void ySlider_change(GSlider source, GEvent event) 
{
  printlnDebug("ySlider_change - GSlider event occured " + System.currentTimeMillis()%10000000, 1 );
  if (event == GEvent.VALUE_STEADY)
  {
    yTextField.setText(Integer.toString(source.getValueI()));//append a "" for easy string conversion 
    yCurrent = source.getValueI();
  }
}

//update text field and working var based on slider change 
public void zSlider_change(GSlider source, GEvent event) 
{
  printlnDebug("zSlider_change - GSlider event occured " + System.currentTimeMillis()%10000000, 1 );
  if (event == GEvent.VALUE_STEADY)
  {
    zTextField.setText(Integer.toString(source.getValueI()));//append a "" for easy string conversion 
    zCurrent = source.getValueI();
  }
}

//update text field and working var based on slider change 
public void wristAngleSlider_change(GSlider source, GEvent event) 
{
//  printlnDebug("wristAngleSlider_change - GSlider event occured " + System.currentTimeMillis()%10000000, 1 );
//  if (event == GEvent.VALUE_STEADY)
//  {
//    wristAngleTextField.setText(Integer.toString(source.getValueI()));//append a "" for easy string conversion 
//    wristAngleCurrent = source.getValueI();
//  }
}

//update text field and working var based on slider change 
public void wristRotateSlider_change(GSlider source, GEvent event) 
{
//  printlnDebug("wristRotateSlider_change - GSlider event occured " + System.currentTimeMillis()%10000000, 1 );
//  if (event == GEvent.VALUE_STEADY)
//  {
//    wristRotateTextField.setText(Integer.toString(source.getValueI()));//append a "" for easy string conversion 
//    wristRotateCurrent = source.getValueI();
//  }
}

//update text field and working var based on slider change 
public void gripperSlider_change(GSlider source, GEvent event) 
{
//  printlnDebug("gripperSlider_change - GSlider event occured " + System.currentTimeMillis()%10000000, 1 );
//  if (event == GEvent.VALUE_STEADY)
//  {
//    gripperTextField.setText(Integer.toString(source.getValueI()));//append a "" for easy string conversion 
//    gripperCurrent = source.getValueI();
//  }
}




//update text field and working var based on slider change 
public void gripperLeftSlider_change(GCustomSlider source, GEvent event) 
{
  printlnDebug("gripperSlider_change - GSlider event occured " + System.currentTimeMillis()%10000000, 1 );
  if (event == GEvent.VALUE_STEADY)
  {
    gripperTextField.setText(Integer.toString(source.getValueI()));//append a "" for easy string conversion 
    gripperCurrent = source.getValueI();
    gripperRightSlider.setValue(source.getValueI());
  }
}

//update text field and working var based on slider change 
public void gripperRightSlider_change(GCustomSlider source, GEvent event) 
{
  printlnDebug("gripperSlider_change - GSlider event occured " + System.currentTimeMillis()%10000000, 1 );
  if (event == GEvent.VALUE_STEADY)
  {
    gripperTextField.setText(Integer.toString(source.getValueI()));//append a "" for easy string conversion 
    gripperCurrent = source.getValueI();
    gripperLeftSlider.setValue(source.getValueI());
  }
}




//update text field and working var based on slider change 
//update text field and working var based on slider change 
public void deltaSlider_change(GSlider source, GEvent event) 
{
  printlnDebug("deltaSliderChange - GSlider event occured " + System.currentTimeMillis()%10000000, 1 );
  if (event == GEvent.VALUE_STEADY)
  {
    deltaTextField.setText(Integer.toString(source.getValueI())); 
    deltaCurrent = source.getValueI();
  }
}

//change to auto-update mode when box is checked
public void autoUpdateCheckbox_change(GCheckbox source, GEvent event)
{ 

  printlnDebug("autoUpdateCheckbox_change - GCheckbox event occured " + System.currentTimeMillis()%10000000, 1 );

  updateFlag = source.isSelected();//set the updateFlag to the current state of the autoUpdate checkbox
} 


//digitalOutputFileCheckbox
public void digitalOutputFileCheckbox_change(GCheckbox source, GEvent event) 
{
  printlnDebug("digitalOutputFileCheckbox - GCheckbox event occured " + System.currentTimeMillis()%10000000, 1 );

  //digitalButtons[0] = source.isSelected();//set the current array item for the corresponding digital output to the current state of the checkbox
} 


//digital output 0 checkbox
public void digitalCheckbox0_change(GCheckbox source, GEvent event) 
{
  printlnDebug("digitalCheckbox0 - GCheckbox event occured " + System.currentTimeMillis()%10000000, 1 );

  //digitalButtons[0] = source.isSelected();//set the current array item for the corresponding digital output to the current state of the checkbox
} 


public void digitalCheckbox1_change(GCheckbox source, GEvent event) 
{ 
  printlnDebug("digitalCheckbox1 - GCheckbox event occured " + System.currentTimeMillis()%10000000, 1 );
  digitalButtons[0] = source.isSelected();//set the current array item for the corresponding digital output to the current state of the checkbox
} 
public void digitalCheckbox2_change(GCheckbox source, GEvent event) 
{
  printlnDebug("digitalCheckbox2 - GCheckbox event occured " + System.currentTimeMillis()%10000000, 1 );
  digitalButtons[1] = source.isSelected();//set the current array item for the corresponding digital output to the current state of the checkbox
} 


public void digitalCheckbox3_change(GCheckbox source, GEvent event) 
{ 
  printlnDebug("digitalCheckbox3 - GCheckbox event occured " + System.currentTimeMillis()%10000000, 1 );
  digitalButtons[2] = source.isSelected();//set the current array item for the corresponding digital output to the current state of the checkbox
}


public void digitalCheckbox4_change(GCheckbox source, GEvent event) 
{ 
  printlnDebug("digitalCheckbox4 - GCheckbox event occured " + System.currentTimeMillis()%10000000, 1 );
  digitalButtons[3] = source.isSelected();//set the current array item for the corresponding digital output to the current state of the checkbox
} 


public void digitalCheckbox5_change(GCheckbox source, GEvent event) 
{ 
  printlnDebug("digitalCheckbox5 - GCheckbox event occured " + System.currentTimeMillis()%10000000, 1 );
  digitalButtons[4] = source.isSelected();//set the current array item for the corresponding digital output to the current state of the checkbox
} 


public void digitalCheckbox6_change(GCheckbox source, GEvent event) 
{
  printlnDebug("digitalCheckbox6 - GCheckbox event occured " + System.currentTimeMillis()%10000000, 1 );
  digitalButtons[5] = source.isSelected();//set the current array item for the corresponding digital output to the current state of the checkbox
} 


public void digitalCheckbox7_change(GCheckbox source, GEvent event) 
{
  printlnDebug("digitalCheckbox7 - GCheckbox event occured " + System.currentTimeMillis()%10000000, 1 );
  digitalButtons[6] = source.isSelected();//set the current array item for the corresponding digital output to the current state of the checkbox
} 

public void cameraCheckbox_change(GCheckbox source, GEvent event) 
{
  printlnDebug("cameraCheckbox_change - GCheckbox event occured " + System.currentTimeMillis()%10000000, 1 );
    
    if(source.isSelected())
    {
     cameraFlag = 1;
    }
    else
    {
      cameraFlag = 0;
      
    }
 // digitalButtons[6] = source.isSelected();//set the current array item for the corresponding digital output to the current state of the checkbox
} 

public void  setRegisterCheckbox_change(GCheckbox source, GEvent event) 
{
  printlnDebug("setRegisterCheckbox_change - GCheckbox event occured " + System.currentTimeMillis()%10000000, 1 );
    
      if(source.isSelected())
    {
      //displayError("Set register is intended for advanced use and has the capablity to change the baud/id of individual servos. Use Caution with this option.", "");
      genericMessageDialog("Set Register", "Set register is intended for advanced use and has the capablity to change<br /> the baud/id of individual servos. Use Caution with this option.", G4P.WARNING);

      
      setRegisterButton.setVisible(true);
    }
    else
    {
       setRegisterButton.setVisible(false);
      
    }


 // digitalButtons[6] = source.isSelected();//set the current array item for the corresponding digital output to the current state of the checkbox
} 





public void analogCheckbox_change(GCheckbox source, GEvent event) 
{
  printlnDebug("analogCheckbox - GCheckbox event occured " + System.currentTimeMillis()%10000000, 1 );
  
  if(source.isSelected())
  {
    enableAnalog = true;
    for(int j=0;j < 8; j++)
    {
      analogLabel[j].setVisible(true);
    }
  }
  else
  {
    enableAnalog = false;
    for(int j=0;j < 8; j++)
    {
      analogLabel[j].setVisible(false);
    }
  }
  
  

  
  
} 


//**********************Error GUI functions
public void errorPanel_Click(GPanel source, GEvent event) 
{ 
  printlnDebug("errorPanel_Click - GPanel event occured " + System.currentTimeMillis()%10000000, 1 );
} 



public void handlePanelEvents(GPanel source, GEvent event) 
{ 
    println("handlePanelEvents - GButton event occured " + System.currentTimeMillis()%10000000 );


  if(dragFlag == -1)
  {
     dragFlag = PApplet.parseInt(source.getText());
  } 
  
  
  if(source.isCollapsed() == false)
  {
    
    println(currentPose + " " +  poses.size() +" Select pose, collas" + System.currentTimeMillis()%10000000 );
    
    currentPose = PApplet.parseInt(source.getText());
    println(currentPose + " " +  poses.size() +" Select pose, collas" + System.currentTimeMillis()%10000000 );


    for(int i = 0; i < poses.size();i++)
    {
      if(i != currentPose)
      {
        
        poses.get(i).setCollapsed(true);
        poses.get(i).forceBufferUpdate();
        
      }
    }
    
  }
  
  
}

//move the list of poses up
public void movePosesUp_click(GButton source, GEvent event) 
{ 
  println("movePosesUp_click1 - GButton event occured " + System.currentTimeMillis()%10000000 );

  currentTopPanel++;
  println(currentTopPanel);
  for(int i = 0; i < poses.size();i++)
  {
   
    float px = poses.get(i).getX();
    float py= poses.get(i).getY()-panelYOffset;
    poses.get(i).moveTo(px,py);
 
    if(currentTopPanel > i)
    {
      poses.get(i).setVisible(false);
    }
     
    else if(i - currentTopPanel >= numberPanelsDisplay)
    {
      poses.get(i).setVisible(false);
    }
 
    else
    {
      poses.get(i).setVisible(true); 
    }
  } 
} //end movePosesUp_click

public void movePosesDown_click(GButton source, GEvent event) 
{
  println("button1 - GButton event occured " + System.currentTimeMillis()%10000000 );
  
  if(--currentTopPanel < 0)
  {
    currentTopPanel = 0;
    return;
  } 
  
  for(int i = 0; i < poses.size();i++)
  {
    float px = poses.get(i).getX();
    float py=panelYOffset +poses.get(i).getY();
   
    poses.get(i).moveTo(px,py);

    poses.get(i).moveTo(px,py);
    
    if(currentTopPanel > i)
    {
      poses.get(i).setVisible(false);
    }
   
    else if(i- currentTopPanel  >= numberPanelsDisplay)
    {
      poses.get(i).setVisible(false);
    }
     
    else
    {
       poses.get(i).setVisible(true); 
    }
   
   

   
   
 } 


} 

//save current workspace to the selected pose
public void workspaceToPose_click(GButton source, GEvent event) 
{
  workspaceToPoseInternal();
  
}
//load selected pose to workspace
public void poseToWorkspace_click(GButton source, GEvent event) 
{
//  poses.get(currentPose)[0];

poseToWorkspaceInternal(currentPose);
  
}


public void workspaceToPoseInternal()
{  
  updateButtonByte();
  print ("Digital Button " + digitalButtonByte); 
  int[] tempPose = {xCurrent, yCurrent, zCurrent,wristAngleCurrent,wristRotateCurrent,gripperCurrent,deltaCurrent,digitalButtonByte, pauseCurrent  };
  
  poseData.set(currentPose, tempPose);
  
  for(int i = 0; i < 9; i++)
  {
     print("-" + poseData.get(currentPose)[i]+"-");  
    
  }
  
  
}



public void poseToWorkspaceInternal(int pose)
{
  
  
int mask = 0;

xCurrent = poseData.get(pose)[0];//set the value that will be sent
xTextField.setText(Integer.toString(xCurrent));//set the text field
xSlider.setValue(xCurrent);//set gui elemeent to same value
baseKnob.setValue(xCurrent);


yCurrent = poseData.get(pose)[1];//set the value that will be sent
yTextField.setText(Integer.toString(yCurrent));//set the text field
ySlider.setValue(yCurrent);//set gui elemeent to same value
shoulderKnob.setValue(yCurrent);

zCurrent = poseData.get(pose)[2];//set the value that will be sent
zTextField.setText(Integer.toString(zCurrent));//set the text field
zSlider.setValue(zCurrent);//set gui elemeent to same value
elbowKnob.setValue(zCurrent);

wristAngleCurrent = poseData.get(pose)[3];//set the value that will be sent
wristAngleTextField.setText(Integer.toString(wristAngleCurrent));//set the text field
wristAngleKnob.setValue(wristAngleCurrent);//set gui elemeent to same value

wristRotateCurrent = poseData.get(pose)[4];//set the value that will be sent
wristRotateTextField.setText(Integer.toString(wristRotateCurrent));//set the text field
wristRotateKnob.setValue(wristRotateCurrent);//set gui elemeent to same value

gripperCurrent = poseData.get(pose)[5];//set the value that will be sent
gripperTextField.setText(Integer.toString(gripperCurrent));//set the text field
gripperSlider.setValue(gripperCurrent);//set gui elemeent to same value
gripperLeftSlider.setValue(gripperCurrent);//set gui elemeent to same value
gripperRightSlider.setValue(gripperCurrent);//set gui elemeent to same value




deltaCurrent = poseData.get(pose)[6];//set the value that will be sent
deltaTextField.setText(Integer.toString(deltaCurrent));//set the text field
deltaSlider.setValue(deltaCurrent);//set gui elemeent to same value

pauseCurrent = poseData.get(pose)[8];
pauseTextField.setText(Integer.toString(pauseCurrent));//set the text field




//extendedByte



int buttonByteFromPose = poseData.get(pose)[7];

 //I'm sure there's a better way to do this
  for (int i = 7; i>=0;i--)
  {
    //subtract 2^i from the button byte, if the value is non-negative, then that byte was active
    if(buttonByteFromPose - pow(2,i) >= 0 )
    {
      buttonByteFromPose = buttonByteFromPose - PApplet.parseInt(pow(2,i));
      switch(i)
      {
        case 0:
        digitalCheckbox1.setSelected(true);
        digitalButtons[0] = true;
        break;
        
        case 1:
        digitalCheckbox2.setSelected(true);
        digitalButtons[1] = true;
        break;
     
        case 2:
        digitalCheckbox3.setSelected(true);
        digitalButtons[2] = true;
        break;
        
        case 3:
        digitalCheckbox4.setSelected(true);
        digitalButtons[3] = true;
        break;
        
        case 4:
        digitalCheckbox5.setSelected(true);
        digitalButtons[4] = true;
        break;
        
        case 5:
        digitalCheckbox6.setSelected(true);
        digitalButtons[5] = true;
        break;
        
        case 6:
        digitalCheckbox7.setSelected(true);
        digitalButtons[6] = true;
        break;
        /*
        case 7:
        digitalCheckbox7.setSelected(true);
        digitalButtons[7] = true;
        break;
        */
      }
 
   }
   else
   {      
     switch(i)
      {
        case 0:
        digitalCheckbox1.setSelected(false);
        digitalButtons[0] = false;
        break;
        
        case 1:
        digitalCheckbox2.setSelected(false);
        digitalButtons[1] = false;
        break;
     
        case 2:
        digitalCheckbox3.setSelected(false);
        digitalButtons[2] = false;
        break;
        
        case 3:
        digitalCheckbox4.setSelected(false);
        digitalButtons[3] = false;
        break;
        
        case 4:
        digitalCheckbox5.setSelected(false);
        digitalButtons[4] = false;
        break;
        
        case 5:
        digitalCheckbox6.setSelected(false);
        digitalButtons[5] = false;
        break;
        
        case 6:
        digitalCheckbox7.setSelected(false);
        digitalButtons[6] = false;
        break;
        /*
        case 7:
        digitalCheckbox7.setSelected(false);
        digitalButtons[7] = false;
        break;
        */
      }
     
   }
 
 
 }
 
  updateButtonByte();


}












public void a1_click(GButton source, GEvent event) 
{
  println("a1click - GButton event occured " + System.currentTimeMillis()%10000000 );
    byte[] returnPacket = new byte[5];  //byte array to hold return packet, which is 5 bytes long
  int analog = 0;
   printlnDebug("sending request for anlaog 1"); 
        sendCommanderPacket(0, 200, 200, 0, 512, 256, 128, 0, 200);    //send a commander style packet - the first 8 bytes are inconsequntial, only the last byte matters. '112' is the extended byte that will request an ID packet
        returnPacket = readFromArmFast(5);//read raw data from arm, complete with wait time
        byte[] analogBytes = {returnPacket[3],returnPacket[2]};
        analog = bytesToInt(analogBytes);
        
        printlnDebug("Return Packet" + PApplet.parseInt(returnPacket[0]) + "-" +  PApplet.parseInt(returnPacket[1]) + "-"  + PApplet.parseInt(returnPacket[2]) + "-"  + PApplet.parseInt(returnPacket[3]) + "-"  + PApplet.parseInt(returnPacket[4]));
        printlnDebug("analog value: " + analog);
        //if(verifyPacket(returnPacket) == true)
       // {
      
          
          
        //}
        
        
        
}


public void playButton_click(GButton source, GEvent event) 
{
  
  
  lastPose = 0;//set lastPose to 0 - if nothing is selected, the playback will start with the 0th pose
  
  //scan the poses and set lastPose to the selected pose
  for (int i = 0; i < poses.size (); i++)
  {
    if(poses.get(i).isCollapsed() == false)
    {
       lastPose = i; 
    }
  }
    
      
  playSequence = true;
}

public void stopButton_click(GButton source, GEvent event) 
{
  playSequence = false;
}





public void emergencyStopButton_click(GButton source, GEvent event) 
{
  sendCommanderPacket(0, 0, 0, 0, 0, 0, 0, 0, 17);    //send a commander style packet - the first 8 bytes are inconsequntial, only the last byte matters. '17' is the extended byte that will stop the arm
  updateFlag = false;
  autoUpdateCheckbox.setSelected(false);
  emergencyStopMessageDialog();
  
}


public void savePosesButton_click(GButton source, GEvent event) 
{



    
    File testFile = new File(sketchPath(""));
    selectFolder("Select a folder to save armSequence.h in", "savePoseToFile", testFile);

}

public void loadPosesButton_click(GButton source, GEvent event) 
{

  selectInput("Select a file to process:", "readArmFile");

  //File testFile = new File(sketchPath(""));
  //readArmFile();
  //readArmFile();
}


public void savePoseToFile(File selection)
{
  
  
 if(selection == null)
  {
   
  } 
 
  else
  {
 
  
     pauseTime = PApplet.parseInt(pauseTextField.getText());



    
      String ikMode = "";
    if(currentMode == 1 && currentOrientation == 1)
    {
      ikMode = "IKM_IK3D_CARTESIAN";
    }
    
    else if(currentMode == 1 && currentOrientation == 2)
    {
      ikMode = "IKM_IK3D_CARTESIAN_90";
    }
    else if(currentMode == 2 && currentOrientation == 1)
    {
      ikMode = "IKM_CYLINDRICAL";
    }
    else if(currentMode == 2 && currentOrientation == 2)
    {
      ikMode = "IKM_CYLINDRICAL_90";
    }
    else if(currentMode == 3)
    {
      ikMode = "IKM_BACKHOE";
    }
  
  
    switch(currentMode)
    {
       case 1:        
         //x, wrist angle, and wrist rotate must be offset, all others are normal
         xCurrentOffset = xCurrent + 512;
         yCurrentOffset = yCurrent;
         zCurrentOffset = zCurrent;
         wristAngleCurrentOffset =  wristAngleCurrent + 90;
         wristRotateCurrentOffset = wristRotateCurrent + 512;
         gripperCurrentOffset = gripperCurrent;
         deltaCurrentOffset = deltaCurrent;
         break;
        
       case 2:
       
         //wrist angle, and wrist rotate must be offset, all others are normal
         xCurrentOffset = xCurrent;
         yCurrentOffset = yCurrent;
         zCurrentOffset = zCurrent;
         wristAngleCurrentOffset =  wristAngleCurrent + 90;
         wristRotateCurrentOffset = wristRotateCurrent + 512;
         gripperCurrentOffset = gripperCurrent;
         deltaCurrentOffset = deltaCurrent;
         break;
        
       case 3:
       
         //no offsets needed
         xCurrentOffset = xCurrent;
         yCurrentOffset = yCurrent;
         zCurrentOffset = zCurrent;
         wristAngleCurrentOffset =  wristAngleCurrent;
         wristRotateCurrentOffset = wristRotateCurrent;
         gripperCurrentOffset = gripperCurrent;
         deltaCurrentOffset = deltaCurrent;
        break; 
    }  
    
    
      int[] buttonPins = {1,2,3,4,5,6,7};

      if(currentArm == 5)
      {
        buttonPins[0] = 2;
        buttonPins[1] = 4;
        buttonPins[2] = 7;
        buttonPins[3] = 8;
        buttonPins[4] = 11;
        buttonPins[5] = 12;
        buttonPins[6] = 13;
      }
      else
      {
        buttonPins[0] = 1;
        buttonPins[1] = 2;
        buttonPins[2] = 3;
        buttonPins[3] = 4;
        buttonPins[4] = 5;
        buttonPins[5] = 6;
        buttonPins[6] = 7;
        
      }
      
            
  
  
  
    PrintWriter poseOutput; //create printWriter object so the program can write to a file
    
   // poseOutput = createWriter("poses.h"); //create a file in the same directory as the app - poses.h
    poseOutput = createWriter(selection.getAbsolutePath()+ "/armSequence.h");
    

    poseOutput.print("//Arm ");
    poseOutput.println(currentArm);

    poseOutput.print("//Sequence ");
    poseOutput.println(poses.size());

    poseOutput.print("//Mode ");
    poseOutput.println(currentMode);

    poseOutput.print("//Orientation ");
    poseOutput.println(currentOrientation);
    
    poseOutput.print("//DIO ");
    if(digitalOutputFileCheckbox.isSelected())
    {
    poseOutput.println(1);
    }
    else
    {
    poseOutput.println(0);
    }
    
    //set IK mode based on current IK mode
    poseOutput.println("#include \"Kinematics.h\"");
    poseOutput.println("#include \"GlobalArm.h\"");

    poseOutput.println("extern void IKSequencingControl(float X, float Y, float Z, float GA, float WR, int grip, int interpolate, int pause, int enable);");
    poseOutput.println("// We need to declare the data exchange");
    poseOutput.println("// variable to be volatile - the value is");
    poseOutput.println("// read from memory.");
    poseOutput.println("volatile int playState = 0; // 0 = stopped 1 = playing");
    poseOutput.println("");
    poseOutput.println("void playSequence()");
    poseOutput.println("{");
    
      if(digitalOutputFileCheckbox.isSelected() == true)
      {  
        for(int l =0; l<7;l++)
        {
          poseOutput.println("    pinMode(" + buttonPins[l] + ", OUTPUT);");
    
        }
      }
  
  
  
    poseOutput.println("  delay(500);");
    poseOutput.println("  Serial.println(\"Sequencing Mode Active.\"); ");
    poseOutput.println("  Serial.println(\"Press Pushbutton  to stop\");");
    poseOutput.println("  playState = 1;  //set playState to 1 as the sequence is now playing");
  
    //only add the g_bIKMode variable if the arm is NOT the snapper
  
  
        poseOutput.print("    g_bIKMode = ");
        poseOutput.print(ikMode);
        poseOutput.println(";");
  

    
    //poseOutput.println("    playState = 1;  //set playState to 1 as the sequence is now playing ");
     
    //print pose data from current sequence to the file   
    for(int i = 0; i < poseData.size();i++)
    {     
      
      
      poseOutput.println("    //###########################################################//");
      poseOutput.print("    // SEQUENCE ");
      poseOutput.print(i+1);
      poseOutput.println("");
      poseOutput.println("    //###########################################################// ");
      
      
      
            if(digitalOutputFileCheckbox.isSelected() == true)
            {      
              int butByteFromPose = poseData.get(i)[7];
                               
  
              poseOutput.println("    //DIO" + butByteFromPose);
                      
                for (int k = 6; k>=0;k--)
                {
                  //subtract 2^k from the button byte, if the value is non-negative, then that byte was active
                  if(butByteFromPose - pow(2,k) >= 0 )
                  {
                    butByteFromPose = butByteFromPose - PApplet.parseInt(pow(2,k));
                      poseOutput.println("    digitalWrite(" + buttonPins[k] + ", HIGH);");
                     
                     
               
                 }
                 else
                 {      
                      poseOutput.println("    digitalWrite(" + buttonPins[k] + ", LOW);");
                                       
                  
                   
                 }
               
               
               }
               
            }
            
            
            
      // 100, 150, 200, 0, 1500, 1000, 1000);
      poseOutput.print("    IKSequencingControl(");
      
      
      for(int j = 0; j < 6;j++)
      {
        
        int tempPoseData = poseData.get(i)[j]; //get single pose data out of pose
        
        
      
      
         poseOutput.print(tempPoseData); 
         poseOutput.print(" , ");
        
      }
      
       poseOutput.print(16 * poseData.get(i)[6]); //compute delta time in milliseconds
       poseOutput.print(" , ");
       //poseOutput.print("1000");//by defualt wait 1000ms between poses
       poseOutput.print(poseData.get(i)[8]);//by defualt wait 1000ms between poses
         
      
      poseOutput.println(", playState);");
   
      

  
      poseOutput.println("    //###########################################################// ");
      poseOutput.println("");
     
      
    }
    
  
    poseOutput.println(" delay(100);");
    poseOutput.println(" Serial.println(\"Pausing Sequencing Mode.\"); ");
    poseOutput.println(" delay(500);");
    poseOutput.println(" //uncomment this to  put the arm in sleep position after a sequence");
    poseOutput.println(" //PutArmToSleep();");
    poseOutput.println("}");

    
    
    poseOutput.flush();
    poseOutput.close();
    
    
  
  } 
  
  
}






public void newPose_click(GButton source, GEvent event) 
{
  println("button2 - GButton event occured " + System.currentTimeMillis()%10000000 );
  
        println("after " + System.currentTimeMillis()%10000000 );
        println(poses.size());

 // addNewPose();

     //to figure out the placement we need to get the 'y' coordinate of the latest pose panel. 
  //However if the first button is being created, the panel offset is all that is needed
  
  float newY;
  if(poses.size() == 0)
  {
    newY =  panelYOffset ;
    
    
  }
  
  else
  {
    newY = poses.get(poses.size()-1).getY() + panelYOffset ;
    
  }

  poseData.add(blankPose);
  
  poses.add(new GPanel(this, panelsX, newY, 50, 18, numPanels + ""));
  poses.get(numPanels).setCollapsible(true);
  poses.get(numPanels).setCollapsed(false);  //fixed??//there is an odd bug if this is set to 'setCollapsed(false)' where the first time you click on the panel, it jumps to the bottom. setting 'setCollapse(true) seems to  aleviate this.
  poses.get(numPanels).setLocalColorScheme(numPanels%8);
  
  sequencePanel.addControl(poses.get(numPanels));
  
  
  currentPose = numPanels;
       
       
  numPanels++;
  
  updateButtonByte();
   int[] tempPose = {xCurrent, yCurrent, zCurrent,wristAngleCurrent,wristRotateCurrent,gripperCurrent,deltaCurrent,digitalButtonByte, pauseCurrent  };
  
  poseData.set(poses.size()-1, tempPose);
  
  
  
  

     if(currentTopPanel > poses.size()-1)
   {
     poses.get(poses.size()-1).setVisible(false);
   }
   
   else if(currentTopPanel - poses.size() < -numberPanelsDisplay)
   {
     poses.get(poses.size()-1).setVisible(false);
   }
   
   
   else
   {
    
     poses.get(poses.size()-1).setVisible(true); 
   }
   
  for(int i = 0; i < poseData.size();i++)
  {
    for(int j = 0; j < (poseData.get(i).length);j++)
    {
       print(poseData.get(i)[j]); 
      
    }
    println("");
  }

       println("curr " + currentPose);
    for(int i = 0; i < poses.size();i++)
    {
      if(currentPose != i)
      {
       println("collapse" + i);
       poses.get(i).setCollapsed(true);
       poses.get(i).forceBufferUpdate();
      }
        
    }
    

  
  
} //_CODE_:button2:332945:

public void addNewPose( int[] filePose)
{



    //to figure out the placement we need to get the 'y' coordinate of the latest pose panel. 
    //However if the first button is being created, the panel offset is all that is needed
    
    float newY;
    if(poses.size() == 0)
    {
      newY =  panelYOffset ;
      
      
    }
    
    else
    {
      newY = poses.get(poses.size()-1).getY() + panelYOffset ;
      
    }
    int[] tempPose = {0,0,0,0,0,0,0,0,0};
    arrayCopy(filePose, tempPose);
  
    
   
   
    poseData.add(tempPose);
  
  
  
  
  
  
    poses.add(new GPanel(this, panelsX, newY, 50, 18, numPanels + ""));
  
            
    poses.get(numPanels).setCollapsible(true);
  
            
    poses.get(numPanels).setCollapsed(true);//there is an odd bug if this is set to 'setCollapsed(false)' where the first time you click on the panel, it jumps to the bottom. setting 'setCollapse(true) seems to  aleviate this.
  
  
    poses.get(numPanels).setLocalColorScheme(numPanels%8);
  
  
  
    sequencePanel.addControl(poses.get(numPanels));
  
  
          
          
    numPanels++;
    
   //int[] tempPose = {xCurrent, yCurrent, zCurrent,wristAngleCurrent,wristRotateCurrent,gripperCurrent,deltaCurrent,digitalButtonByte  };

  
  //poseData.set(poses.size()-1, tempPose);
  
  
  
  

     if(currentTopPanel > poses.size()-1)
   {
     poses.get(poses.size()-1).setVisible(false);
   }
   
   else if(currentTopPanel - poses.size() < -numberPanelsDisplay)
   {
     poses.get(poses.size()-1).setVisible(false);
   }
   
   
   else
   {
    
     poses.get(poses.size()-1).setVisible(true); 
   }
   
  // for(int i = 0; i < poseData.size();i++)
  // {
  //   for(int j = 0; j < (poseData.get(i).length);j++)
  //   {
  //      print(poseData.get(i)[j]); 
      
  //   }
  //   println("");
  // }





}






//display error and link of supplied strings
public void displayError(String message, String link)
{
  //show error panel
  errorPanel.setVisible(true); 
  errorPanel.setAlpha(255);

  //grey out other panels
  setupPanel.setAlpha(128);
  controlPanel.setAlpha(128);
  sequencePanel.setAlpha(128);
  modePanel.setAlpha(128);
  ioPanel.setAlpha(128);
  setupPanel.setEnabled(false);
  controlPanel.setEnabled(false);
  sequencePanel.setEnabled(false);
  modePanel.setEnabled(false);
  ioPanel.setEnabled(false);
  setupPanel.setFocus(false);


  //set text
  errorLabel.setText(message);

  //set help link and show it if the string is not empty, otherwise hide the button
  if (helpLink != "")
  {
    helpLink = link;
    errorLinkButton.setVisible(true);
    errorLinkButton.setEnabled(true);
  }
  else
  {
    errorLinkButton.setVisible(false);
    errorLinkButton.setEnabled(false);
  }
  //show error panel
  errorPanel.setVisible(true); 
  errorPanel.setAlpha(255);

}

//hide error panel and other panels to their normal state
public void hideError()
{
  //grey out other panels
  setupPanel.setAlpha(255);
  controlPanel.setAlpha(255);
  sequencePanel.setAlpha(255);
  modePanel.setAlpha(255);
  ioPanel.setAlpha(255);
  setupPanel.setEnabled(true);
  controlPanel.setEnabled(true);
  modePanel.setEnabled(true);
  sequencePanel.setEnabled(true);
  ioPanel.setEnabled(true);

  //show error panel
  errorPanel.setVisible(false);
}


//go to help link when button pressed
public void errorLinkButton_click(GButton source, GEvent event) { 

  printlnDebug("errorLinkButton_click - GButton event occured " + System.currentTimeMillis()%10000000, 1 );

  link(helpLink);
}

//dismiss error panel
public void errorOkButton_click(GButton source, GEvent event) { 

  printlnDebug("errorOkButton_click - GButton event occured " + System.currentTimeMillis()%10000000, 1);
  hideError();
}


//**********************Settings GUI functions

public void settingsPanel_Click(GPanel source, GEvent event) { 
  printlnDebug("settingsPanel_Click - GPanel event occured " + System.currentTimeMillis()%10000000, 1 );
} 

//dismiss settings panel
public void settingsDismissButton_click(GButton source, GEvent event) 
{
  printlnDebug("settingsDismissButton_click - GButton event occured " + System.currentTimeMillis()%10000000, 1);
  settingsPanel.setVisible(false);
}

//checkbox to enable debug-to-file
public void fileDebugCheckbox_change(GCheckbox source, GEvent event)
{ 

  printlnDebug(" - GCheckbox event occured " + System.currentTimeMillis()%10000000, 1 );

  debugFile = source.isSelected();//set the updateFlag to the current state of the autoUpdate checkbox
} 







//**********************Other GUI functions


//now used to enable settings panel
public void helpButton_click(GButton source, GEvent event) 
{ 
  settingsPanel.setVisible(true);
  //link("http://learn.trossenrobotics.com/");
}

// Create all the GUI controls. 
public void createGUI() {
  G4P.messagesEnabled(false);
  G4P.setGlobalColorScheme(8);
  G4P.setCursor(ARROW);


  if (frame != null)
    surface.setTitle("InterbotiX Arm Link " + programVersion);




  
//error
  errorPanel = new GPanel(this, 180, 49, 150, 150, "Error Panel");
  errorPanel.setText("Error Panel");
  errorPanel.setLocalColorScheme(GCScheme.RED_SCHEME);
  errorPanel.setOpaque(true);
  errorPanel.setVisible(false);
  errorPanel.addEventHandler(this, "errorPanel_Click");
  //errorPanel.setDraggable(false);
  errorPanel.setCollapsible(false);
  
  errorLinkButton = new GButton(this, 15, 120, 50, 20);
  errorLinkButton.setText("Link");
  errorLinkButton.addEventHandler(this, "errorLinkButton_click");
  errorLinkButton.setLocalColorScheme(GCScheme.RED_SCHEME);

  errorOkButton = new GButton(this, 80, 120, 50, 20);
  errorOkButton.setText("OK");
  errorOkButton.addEventHandler(this, "errorOkButton_click");
  errorOkButton.setLocalColorScheme(GCScheme.RED_SCHEME);

  errorLabel = new GLabel(this, 10, 25, 130, 80);
  errorLabel.setTextAlign(GAlign.LEFT, GAlign.MIDDLE);
  errorLabel.setText("Error: Error: Error: Error: Error: Error: Error: Error: Error: Error: Error: Error: Error: Error: Error: Error: ");
  errorLabel.setOpaque(false);
  errorLabel.setFont(new Font("Dialog", Font.PLAIN, 10)); 



  errorPanel.addControl(errorLinkButton);
  errorPanel.addControl(errorOkButton);
  errorPanel.addControl(errorLabel);




  statusLabel = new GLabel(this, 300, 25, 170, 25);
  statusLabel.setTextAlign(GAlign.LEFT, GAlign.MIDDLE);
 // statusLabel.setLocalColorScheme(GCScheme.GOLD_SCHEME);
  statusLabel.setText("Not Connected");
  statusLabel.setFont(new Font("Dialog", Font.PLAIN, 20)); 
//  statusLabel.setOpaque(false);







  helpButton = new GButton(this, 10, 775, 40, 20);
  helpButton.setText("More");
  helpButton.setLocalColorScheme(GCScheme.GOLD_SCHEME);
  helpButton.addEventHandler(this, "helpButton_click");




//Setup

  setupPanel = new GPanel(this, 5, 50, 465, 50, "Setup Panel");
  setupPanel.setText("Setup Panel");
  setupPanel.setLocalColorScheme(GCScheme.BLUE_SCHEME);
  setupPanel.setOpaque(true);
  setupPanel.addEventHandler(this, "setupPanel_click");
  //setupPanel.setDraggable(false);
  setupPanel.setCollapsible(false);
  
  
  


  serialList = new GDropList(this, 5, 24, 200, 200, 10);
  //serialList.setItems(loadStrings("list_700876"), 0);
  serialList.addEventHandler(this, "serialList_click");
  serialList.setFont(new Font("Dialog", Font.PLAIN, 9));  
  serialList.setLocalColorScheme(GCScheme.CYAN_SCHEME);

  connectButton = new GButton(this, 215, 24, 75, 20);
  connectButton.setText("Connect");
  connectButton.addEventHandler(this, "connectButton_click");

  disconnectButton = new GButton(this, 300, 24, 75, 20);
  disconnectButton.setText("Disconnect");
  disconnectButton.addEventHandler(this, "disconnectButton_click");

  disconnectButton.setEnabled(false);
  disconnectButton.setAlpha(128);

  autoConnectButton = new GButton(this, 385, 24, 75, 20);
  autoConnectButton.setText("Auto Search");
  autoConnectButton.addEventHandler(this, "autoConnectButton_click");
  autoConnectButton.setLocalColorScheme(GCScheme.PURPLE_SCHEME);


  setupPanel.addControl(serialList);
  setupPanel.addControl(connectButton);
  setupPanel.addControl(disconnectButton);
  setupPanel.addControl(autoConnectButton);


//mode
  modePanel = new GPanel(this, 5, 690, 240, 38, "Mode Panel");
  modePanel.setText("Mode Panel");
  modePanel.setLocalColorScheme(GCScheme.BLUE_SCHEME);
  modePanel.setOpaque(true);
  modePanel.addEventHandler(this, "modePanel_click");
  //modePanel.setDraggable(false);
  modePanel.setCollapsible(false);

  //modePanel.setVisible(false);
  //modePanel.setEnabled(false);

//wrist angle
  wristPanel = new GPanel(this, 310, 690, 160, 38, "Wrist Panel");
  wristPanel.setText("Wrist Panel");
  wristPanel.setLocalColorScheme(GCScheme.BLUE_SCHEME);
  wristPanel.setOpaque(true);
  wristPanel.addEventHandler(this, "wristPanel_click");
  //modePanel.setDraggable(false);
  wristPanel.setCollapsible(false);
  
  cartesianModeButton = new GButton(this, 0, 18, 80, 20);
  cartesianModeButton.setText("Cartesian");
  cartesianModeButton.addEventHandler(this, "cartesianModeButton_click");
  cartesianModeButton.setLocalColorScheme(GCScheme.CYAN_SCHEME);

  cylindricalModeButton = new GButton(this, 80, 18, 80, 20);
  cylindricalModeButton.setText("Cylindrical");
  cylindricalModeButton.addEventHandler(this, "cylindricalModeButton_click");
  cylindricalModeButton.setLocalColorScheme(GCScheme.CYAN_SCHEME);

  backhoeModeButton = new GButton(this, 160, 18, 80, 20);
  backhoeModeButton.setText("Backhoe");
  backhoeModeButton.addEventHandler(this, "backhoeModeButton_click");
  backhoeModeButton.setLocalColorScheme(GCScheme.CYAN_SCHEME);


  
  orientStraightButton = new GButton(this, 0, 18, 80, 20);
  orientStraightButton.setText("Straight");
  orientStraightButton.addEventHandler(this, "orientStraightButton_click");
  orientStraightButton.setLocalColorScheme(GCScheme.CYAN_SCHEME);
  
  orient90Button = new GButton(this, 80, 18, 80, 20);
  orient90Button.setText("90 Degrees");
  orient90Button.addEventHandler(this, "orient90Button_click");
  orient90Button.setLocalColorScheme(GCScheme.CYAN_SCHEME);

  
  

  //DEPRECATEDarmStraightButton = new GImageButton(this, 5, 40, 100, 65, new String[] { 
 //DEPRECATED   "armStraightm.png", "armStraightm.png", "armStraightm.png"
 //DEPRECATED } 
  //DEPRECATED);
  //DEPRECATEDarmStraightButton.addEventHandler(this, "armStraightButton_click");
 //DEPRECATED armStraightButton.setAlpha(128);

 //DEPRECATED arm90Button = new GImageButton(this, 130, 40, 100, 65, new String[] { 
  //DEPRECATED  "arm90m.png", "arm90m.png", "arm90m.png"
  //DEPRECATED} 
  //DEPRECATED);
  //DEPRECATEDarm90Button.addEventHandler(this, "arm90Button_click");
  //DEPRECATEDarm90Button.setAlpha(128);


  modePanel.addControl(cartesianModeButton);
  modePanel.addControl(cylindricalModeButton);
  modePanel.addControl(backhoeModeButton);
  
  modePanel.setVisible(false);
  modePanel.setEnabled(false);
  
  
  //modePanel.addControl(arm90Button);
  //modePanel.addControl(armStraightButton);

  wristPanel.addControl(orientStraightButton);
  wristPanel.addControl(orient90Button);
  wristPanel.setVisible(false);
  wristPanel.setEnabled(false);



//control
  controlPanel = new GPanel(this, 5, 105, 265, 475, "Control Panel");
  controlPanel.setText("Control Panel");
  controlPanel.setLocalColorScheme(GCScheme.BLUE_SCHEME);
  controlPanel.setOpaque(true);
  controlPanel.addEventHandler(this, "controlPanel_click");
  //controlPanel.setDraggable(false);
  controlPanel.setCollapsible(false);
  
  controlPanel.setVisible(false);
  controlPanel.setEnabled(false);
  
  
  
  sequencePanel = new GPanel(this, 310, 105, 160, 475, "Sequence Panel");
  sequencePanel.setText("Sequence Panel");
  sequencePanel.setLocalColorScheme(GCScheme.BLUE_SCHEME);
  sequencePanel.setOpaque(true);
  sequencePanel.addEventHandler(this, "sequencePanel_click");
  //controlPanel.setDraggable(false);
  sequencePanel.setCollapsible(false);
  
  sequencePanel.setVisible(false);
  sequencePanel.setEnabled(false);



  
  
  ioPanel = new GPanel(this, 5, 585, 465, 100, "I/O Panel");
  ioPanel.setText("I/O Panel");
  ioPanel.setLocalColorScheme(GCScheme.BLUE_SCHEME);
  ioPanel.setOpaque(true);
  ioPanel.addEventHandler(this, "ioPanel_click");
  //setupPanel.setDraggable(false);
  ioPanel.setCollapsible(false);
  ioPanel.setVisible(false);
  ioPanel.setEnabled(false);
  
  


  baseKnob = new GKnob(this, 100, 30, 50, 50, 1); 
  baseKnob.setTurnRange(120.0f, 60.0f); //set angle limits start/finish
  baseKnob.setLimits(512.0f, 1023.0f, 0.0f);//set value limits
  baseKnob.setShowArcOnly(true);   //show arc, hide par of circle you cannot interct with
  baseKnob.setStickToTicks(false);   //no need to stick to ticks
  baseKnob.setTurnMode(1281); //???
  baseKnob.addEventHandler(this, "baseKnob_change");//set event listener
  baseKnob.setLocalColorScheme(9);//set color scheme just for knobs, custom color in /data
  baseKnob.setVisible(false); //hide base knob by defualt

  shoulderKnob = new GKnob(this, 13, 130, 50, 50, 1); 
  shoulderKnob.setTurnRange(120.0f, 60.0f); //set angle limits start/finish
  shoulderKnob.setLimits(512.0f, 0, 1023.0f);//set value limits
  shoulderKnob.setShowArcOnly(true);   //show arc, hide par of circle you cannot interct with
  shoulderKnob.setStickToTicks(false);   //no need to stick to ticks
  shoulderKnob.setTurnMode(1281); //???
  shoulderKnob.addEventHandler(this, "shoulderKnob_change");//set event listener
  shoulderKnob.setLocalColorScheme(9);//set color scheme just for knobs, custom color in /data

  elbowKnob = new GKnob(this, 113, 130, 50, 50, 1); 
  elbowKnob.setTurnRange(120.0f, 60.0f); //set angle limits start/finish
  elbowKnob.setLimits(512.0f, 0, 1023.0f);//set value limits
  elbowKnob.setShowArcOnly(true);   //show arc, hide par of circle you cannot interct with
  elbowKnob.setStickToTicks(false);   //no need to stick to ticks
  elbowKnob.setTurnMode(1281); //???
  elbowKnob.addEventHandler(this, "elbowKnob_change");//set event listener
  elbowKnob.setLocalColorScheme(9);//set color scheme just for knobs, custom color in /data
  elbowKnob.setRotation(-PI*1/3,GControlMode.CENTER);
  
  
  wristAngleKnob = new GKnob(this, 5, 320, 50, 50, 1); 
  wristAngleKnob.setTurnRange(270.0f, 90.0f); //set angle limits start/finish
  wristAngleKnob.setLimits(512.0f, 0, 1023.0f);//set value limits
  wristAngleKnob.setShowArcOnly(true);   //show arc, hide par of circle you cannot interct with
  wristAngleKnob.setStickToTicks(false);   //no need to stick to ticks
  wristAngleKnob.setTurnMode(1281); //???
  wristAngleKnob.addEventHandler(this, "wristAngleKnob_change");//set event listener
  wristAngleKnob.setLocalColorScheme(9);//set color scheme just for knobs, custom color in /data
  //wristAngleKnob.setVisible(false);

  wristRotateKnob = new GKnob(this, 105, 320, 50, 50, 1); 
  wristRotateKnob.setTurnRange(120.0f, 60.0f); //set angle limits start/finish
  wristRotateKnob.setLimits(512.0f, 0, 1023.0f);//set value limits
  wristRotateKnob.setShowArcOnly(true);   //show arc, hide par of circle you cannot interct with
  wristRotateKnob.setStickToTicks(false);   //no need to stick to ticks
  wristRotateKnob.setTurnMode(1281); //???
  wristRotateKnob.addEventHandler(this, "wristRotateKnob_change");//set event listener
  wristRotateKnob.setLocalColorScheme(9);//set color scheme just for knobs, custom color in /data


  xTextField = new GTextField(this, 5, 40, 60, 20, G4P.SCROLLBARS_NONE);
  //xTextField.setText("0");
  xTextField.setText(Integer.toString(xParameters[0]));
  xTextField.setLocalColorScheme(GCScheme.BLUE_SCHEME);
  xTextField.setOpaque(true);
  xTextField.addEventHandler(this, "xTextField_change");
  
  xLabel = new GLabel(this, 5, 60, 60, 14);
  xLabel.setTextAlign(GAlign.LEFT, GAlign.MIDDLE);
  xLabel.setText("X Coord");
  xLabel.setOpaque(false);

  xSlider = new GSlider(this, 75, 30, 100, 40, 10.0f);
  xSlider.setShowLimits(true);
  xSlider.setLimits(0.0f, -200.0f, 200.0f);
  xSlider.setNbrTicks(50);
  xSlider.setEasing(0.0f);
  xSlider.setNumberFormat(G4P.INTEGER, 0);
  xSlider.setLocalColorScheme(GCScheme.BLUE_SCHEME);
  xSlider.setOpaque(false);
  xSlider.addEventHandler(this, "xSlider_change");
  xSlider.setShowValue(true);



  yTextField = new GTextField(this, 5, 90, 65, 20, G4P.SCROLLBARS_NONE);
  yTextField.setText(Integer.toString(yParameters[0]));
  yTextField.setLocalColorScheme(GCScheme.BLUE_SCHEME);
  yTextField.setOpaque(true);
  yTextField.addEventHandler(this, "yTextField_change");

  yLabel = new GLabel(this, 5, 110, 65, 14);
  yLabel.setTextAlign(GAlign.LEFT, GAlign.MIDDLE);
  yLabel.setText("Y Coord");
  yLabel.setOpaque(false);



  ySlider = new GSlider(this, -35, 165, 145, 65, 10.0f);
  ySlider.setShowLimits(true);
  ySlider.setLimits(200.0f, 50.0f, 240.0f);
  ySlider.setEasing(0.0f);
  ySlider.setNumberFormat(G4P.INTEGER, 0);
  ySlider.setLocalColorScheme(GCScheme.BLUE_SCHEME);
  ySlider.setOpaque(false);
  ySlider.addEventHandler(this, "ySlider_change");
  ySlider.setShowValue(true);
  ySlider.setTextOrientation(G4P.ORIENT_RIGHT);
  
  
  ySlider.setRotation(3.1415927f*1.5f, GControlMode.CENTER); 


  zTextField = new GTextField(this, 105, 90, 65, 20, G4P.SCROLLBARS_NONE);
  zTextField.setText(Integer.toString(zParameters[0]));
  zTextField.setLocalColorScheme(GCScheme.BLUE_SCHEME);
  zTextField.setOpaque(true);
  zTextField.addEventHandler(this, "zTextField_change");

  zLabel = new GLabel(this, 105, 110, 65, 14);
  zLabel.setTextAlign(GAlign.LEFT, GAlign.MIDDLE);
  zLabel.setText("Z Coord");
  zLabel.setOpaque(false);


  zSlider = new GSlider(this, 65, 165, 145, 65, 10.0f);
  zSlider.setShowLimits(true);
  zSlider.setLimits(200.0f, 20.0f, 250.0f);
  zSlider.setEasing(0.0f);
  zSlider.setNumberFormat(G4P.INTEGER, 0);
  zSlider.setLocalColorScheme(GCScheme.BLUE_SCHEME);
  zSlider.setOpaque(false);
  zSlider.addEventHandler(this, "zSlider_change"); 
  zSlider.setShowValue(true); 
  zSlider.setRotation(3.1415927f*1.5f, GControlMode.CENTER); 
  zSlider.setTextOrientation(G4P.ORIENT_RIGHT);
  



  wristAngleTextField = new GTextField(this, 5, 280, 60, 20, G4P.SCROLLBARS_NONE);
  wristAngleTextField.setText(Integer.toString(wristAngleParameters[0]));
  wristAngleTextField.setLocalColorScheme(GCScheme.BLUE_SCHEME);
  wristAngleTextField.setOpaque(true);
  wristAngleTextField.addEventHandler(this, "wristAngleTextField_change");

  wristAngleLabel = new GLabel(this, 5, 305, 70, 14);
  wristAngleLabel.setTextAlign(GAlign.LEFT, GAlign.MIDDLE);
  wristAngleLabel.setText("Wrist Angle");
  //wristAngleLabel.setLocalColorScheme(GCScheme.BLUE_SCHEME);
  wristAngleLabel.setOpaque(false);
  //wristAngleLabel.setFont(new Font("Dialog", Font.PLAIN, 10));

  wristAngleSlider = new GSlider(this, 75, 155, 145, 40, 10.0f);
  wristAngleSlider.setShowLimits(true);
  wristAngleSlider.setLimits(0.0f, -90.0f, 90.0f);
  wristAngleSlider.setEasing(0.0f);
  wristAngleSlider.setNumberFormat(G4P.INTEGER, 0);
  wristAngleSlider.setLocalColorScheme(GCScheme.BLUE_SCHEME);
  wristAngleSlider.setOpaque(false);
  wristAngleSlider.addEventHandler(this, "wristAngleSlider_change");
  wristAngleSlider.setShowValue(true);
  wristAngleSlider.setVisible(false);






  wristRotateTextField = new GTextField(this, 105, 280, 60, 20, G4P.SCROLLBARS_NONE);
  wristRotateTextField.setText(Integer.toString( wristRotateParameters[0]));
  wristRotateTextField.setLocalColorScheme(GCScheme.BLUE_SCHEME);
  wristRotateTextField.setOpaque(true);
  wristRotateTextField.addEventHandler(this, "wristRotateTextField_change");
  
  wristRotateLabel = new GLabel(this, 105, 305, 90, 14);
  wristRotateLabel.setTextAlign(GAlign.LEFT, GAlign.MIDDLE);
  wristRotateLabel.setText("Wrist Rotate");
  //wristRotateLabel.setLocalColorScheme(GCScheme.BLUE_SCHEME);
  wristRotateLabel.setOpaque(false);
  //wristRotateLabel.setFont(new Font("Dialog", Font.PLAIN, 10));

  wristRotateSlider = new GSlider(this, 75, 200, 145, 40, 10.0f);
  wristRotateSlider.setShowLimits(true);
  wristRotateSlider.setLimits(0.0f, -512.0f, 512.0f);
  wristRotateSlider.setEasing(0.0f);
  wristRotateSlider.setNumberFormat(G4P.INTEGER, 0);
  wristRotateSlider.setLocalColorScheme(GCScheme.BLUE_SCHEME);
  wristRotateSlider.setOpaque(false);
  wristRotateSlider.addEventHandler(this, "wristRotateSlider_change");
  wristRotateSlider.setShowValue(true);
  wristRotateSlider.setVisible(false);





  gripperTextField = new GTextField(this, 190, 90, 60, 20, G4P.SCROLLBARS_NONE);
  gripperTextField.setText(Integer.toString(gripperParameters[0]));
  gripperTextField.setLocalColorScheme(GCScheme.BLUE_SCHEME);
  gripperTextField.setOpaque(true);
  gripperTextField.addEventHandler(this, "gripperTextField_change");
  
  gripperLabel = new GLabel(this, 190, 110, 60, 14);
  gripperLabel.setTextAlign(GAlign.LEFT, GAlign.MIDDLE);
  gripperLabel.setText("Gripper");
  gripperLabel.setOpaque(false);
  

  gripperSlider = new GSlider(this, 75, 245, 145, 40, 10.0f);
  gripperSlider.setShowLimits(true);
  gripperSlider.setLimits(256.0f, 0.0f, 512.0f);
  gripperSlider.setEasing(0.0f);
  gripperSlider.setNumberFormat(G4P.INTEGER, 0);
  gripperSlider.setLocalColorScheme(GCScheme.BLUE_SCHEME);
  gripperSlider.setOpaque(false);
  gripperSlider.addEventHandler(this, "gripperSlider_change");
  gripperSlider.setShowValue(true);
  gripperSlider.setVisible(false);
  gripperSlider.setShowTicks(false);



  pauseSlider = new GSlider(this, 75, 245, 145, 40, 10.0f);

  pauseSlider.setVisible(false);



  deltaTextField = new GTextField(this, 5, 380, 60, 20, G4P.SCROLLBARS_NONE);
  deltaTextField.setText("125");
  deltaTextField.setLocalColorScheme(GCScheme.BLUE_SCHEME);
  deltaTextField.setOpaque(true);
  deltaTextField.addEventHandler(this, "deltaTextField_change");


  deltaSlider = new GSlider(this, 75, 370, 145, 40, 10.0f);
  deltaSlider.setShowValue(true);
  deltaSlider.setShowLimits(true);
  deltaSlider.setLimits(125.0f, 0.0f, 255.0f);
  deltaSlider.setEasing(0.0f);
  deltaSlider.setNumberFormat(G4P.INTEGER, 0);
  deltaSlider.setLocalColorScheme(GCScheme.BLUE_SCHEME);
  deltaSlider.setOpaque(false);
  deltaSlider.addEventHandler(this, "deltaSlider_change");

  deltaLabel = new GLabel(this, 5, 400, 60, 14);
  deltaLabel.setTextAlign(GAlign.LEFT, GAlign.MIDDLE);
  deltaLabel.setText("Delta");
  deltaLabel.setLocalColorScheme(GCScheme.BLUE_SCHEME);
  deltaLabel.setOpaque(false);

  extendedTextField = new GTextField(this, 190, 280, 60, 20, G4P.SCROLLBARS_NONE);
  extendedTextField.setText("0");
  extendedTextField.setLocalColorScheme(GCScheme.BLUE_SCHEME);
  extendedTextField.setOpaque(true);
  extendedTextField.addEventHandler(this, "extendedTextField_change");

  extendedLabel = new GLabel(this, 190, 300, 100, 14);
  extendedLabel.setTextAlign(GAlign.LEFT, GAlign.MIDDLE);
  extendedLabel.setText("Extended Byte");
  extendedLabel.setLocalColorScheme(GCScheme.BLUE_SCHEME);
  extendedLabel.setOpaque(false);
  extendedLabel.setFont(new Font("Dialog", Font.PLAIN, 10));





  digitalOutputFileCheckbox = new GCheckbox(this, 90, 360, 75, 40);
  digitalOutputFileCheckbox.setOpaque(false);
  digitalOutputFileCheckbox.addEventHandler(this, "digitalOutputFileCheckbox_change");
  digitalOutputFileCheckbox.setText("Save Digital Outputs");
  digitalOutputFileCheckbox.setVisible(true);
  digitalOutputFileCheckbox.setEnabled(true);


  digitalCheckbox0 = new GCheckbox(this, 5, 20, 28, 20);
  digitalCheckbox0.setOpaque(false);
  digitalCheckbox0.addEventHandler(this, "digitalCheckbox0_change");
  digitalCheckbox0.setText("0");
  digitalCheckbox0.setVisible(false);
  digitalCheckbox0.setEnabled(false);

  digitalCheckbox1 = new GCheckbox(this, 85, 18, 40, 20);
  digitalCheckbox1.setOpaque(false);
  digitalCheckbox1.addEventHandler(this, "digitalCheckbox1_change");
  digitalCheckbox1.setText("1");

  digitalCheckbox2 = new GCheckbox(this, 125, 18, 40, 20);
  digitalCheckbox2.setOpaque(false);
  digitalCheckbox2.addEventHandler(this, "digitalCheckbox2_change");
  digitalCheckbox2.setText("2");

  digitalCheckbox3 = new GCheckbox(this, 165, 18, 40, 20);
  digitalCheckbox3.setOpaque(false);
  digitalCheckbox3.addEventHandler(this, "digitalCheckbox3_change");
  digitalCheckbox3.setText("3");

  digitalCheckbox4 = new GCheckbox(this, 205, 18, 40, 20);
  digitalCheckbox4.setOpaque(false);
  digitalCheckbox4.addEventHandler(this, "digitalCheckbox4_change");
  digitalCheckbox4.setText("4");

  digitalCheckbox5 = new GCheckbox(this, 245, 18, 40, 20);
  digitalCheckbox5.setOpaque(false);
  digitalCheckbox5.addEventHandler(this, "digitalCheckbox5_change");
  digitalCheckbox5.setText("5");

  digitalCheckbox6 = new GCheckbox(this, 285, 18, 40, 20);
  digitalCheckbox6.setOpaque(false);
  digitalCheckbox6.addEventHandler(this, "digitalCheckbox6_change");
  digitalCheckbox6.setText("6");

  digitalCheckbox7 = new GCheckbox(this, 325, 18, 40, 20);
  digitalCheckbox7.setOpaque(false);
  digitalCheckbox7.addEventHandler(this, "digitalCheckbox7_change");
  digitalCheckbox7.setText("7");



  cameraCheckbox = new GCheckbox(this, 150, 58, 150, 20);
  cameraCheckbox.setOpaque(false);
  cameraCheckbox.addEventHandler(this, "cameraCheckbox_change");
  cameraCheckbox.setText("Activate Camera");
  
  
  cameraLabel = new GLabel(this, 5, 58, 100, 14);
  cameraLabel.setTextAlign(GAlign.LEFT, GAlign.MIDDLE);
  cameraLabel.setText("Experimental");
  cameraLabel.setLocalColorScheme(GCScheme.BLUE_SCHEME);
  cameraLabel.setOpaque(false);
  cameraLabel.setVisible(false);


  setRegisterCheckbox = new GCheckbox(this, 300, 58, 150, 20);
  setRegisterCheckbox.setOpaque(false);
  setRegisterCheckbox.addEventHandler(this, "setRegisterCheckbox_change");
  setRegisterCheckbox.setText("Enable Set Register");
  


  digitalsLabel = new GLabel(this, 5, 20, 100, 14);
  digitalsLabel.setTextAlign(GAlign.LEFT, GAlign.MIDDLE);
  digitalsLabel.setText("Digital Output");
  digitalsLabel.setLocalColorScheme(GCScheme.BLUE_SCHEME);
  digitalsLabel.setOpaque(false);




  analogCheckbox = new GCheckbox(this, 5, 58, 180, 20);
  analogCheckbox.setOpaque(false);
  analogCheckbox.addEventHandler(this, "analogCheckbox_change");
  analogCheckbox.setText("Enable Analog Input:", GAlign.LEFT, GAlign.MIDDLE);

  analogCheckbox.setVisible(true);
  analogCheckbox.setEnabled(true);
  
  
  
  analogTextLabel = new GLabel(this, 5, 38, 100, 14);
  analogTextLabel.setTextAlign(GAlign.LEFT, GAlign.MIDDLE);
  analogTextLabel.setText("Analog Input");
  analogTextLabel.setLocalColorScheme(GCScheme.BLUE_SCHEME);
  analogTextLabel.setOpaque(false);




  analogLabel[0] = new GLabel(this, 85, 38, 90, 14);
  analogLabel[0].setTextAlign(GAlign.LEFT, GAlign.MIDDLE);
  analogLabel[0].setText("0");
  analogLabel[0].setLocalColorScheme(GCScheme.BLUE_SCHEME);
  analogLabel[0].setOpaque(false);
  analogLabel[0].setVisible(false);

  analogLabel[1] = new GLabel(this, 130, 38, 60, 14);
  analogLabel[1].setTextAlign(GAlign.LEFT, GAlign.MIDDLE);
  analogLabel[1].setText("1");
  analogLabel[1].setLocalColorScheme(GCScheme.BLUE_SCHEME);
  analogLabel[1].setOpaque(false);
  analogLabel[1].setVisible(false);

  analogLabel[2] = new GLabel(this, 175, 38, 60, 14);
  analogLabel[2].setTextAlign(GAlign.LEFT, GAlign.MIDDLE);
  analogLabel[2].setText("2");
  analogLabel[2].setLocalColorScheme(GCScheme.BLUE_SCHEME);
  analogLabel[2].setOpaque(false);
  analogLabel[2].setVisible(false);

  analogLabel[3] = new GLabel(this, 215, 38, 60, 14);
  analogLabel[3].setTextAlign(GAlign.LEFT, GAlign.MIDDLE);
  analogLabel[3].setText("3");
  analogLabel[3].setLocalColorScheme(GCScheme.BLUE_SCHEME);
  analogLabel[3].setOpaque(false);
  analogLabel[3].setVisible(false);

  analogLabel[4] = new GLabel(this, 265, 38, 60, 14);
  analogLabel[4].setTextAlign(GAlign.LEFT, GAlign.MIDDLE);
  analogLabel[4].setText("4");
  analogLabel[4].setLocalColorScheme(GCScheme.BLUE_SCHEME);
  analogLabel[4].setOpaque(false);
  analogLabel[4].setVisible(false);

  analogLabel[5] = new GLabel(this, 310, 38, 60, 14);
  analogLabel[5].setTextAlign(GAlign.LEFT, GAlign.MIDDLE);
  analogLabel[5].setText("5");
  analogLabel[5].setLocalColorScheme(GCScheme.BLUE_SCHEME);
  analogLabel[5].setOpaque(false);
  analogLabel[5].setVisible(false);

  analogLabel[6] = new GLabel(this, 355, 38, 60, 14);
  analogLabel[6].setTextAlign(GAlign.LEFT, GAlign.MIDDLE);
  analogLabel[6].setText("6");
  analogLabel[6].setLocalColorScheme(GCScheme.BLUE_SCHEME);
  analogLabel[6].setOpaque(false);
  analogLabel[6].setVisible(false);

  analogLabel[7] = new GLabel(this, 395, 38, 60, 14);
  analogLabel[7].setTextAlign(GAlign.LEFT, GAlign.MIDDLE);
  analogLabel[7].setText("7");
  analogLabel[7].setLocalColorScheme(GCScheme.BLUE_SCHEME);
  analogLabel[7].setOpaque(false);
  analogLabel[7].setVisible(false);



  regId = new GTextField(this, 20, 75, 30, 20, G4P.SCROLLBARS_NONE);
  regId.setText("1");
  regId.setLocalColorScheme(GCScheme.BLUE_SCHEME);
  regId.setOpaque(true);
  regId.addEventHandler(this, "regId_change");
  
  regNum = new GTextField(this, 105, 75, 30, 20, G4P.SCROLLBARS_NONE);
  regNum.setText("0");
  regNum.setLocalColorScheme(GCScheme.BLUE_SCHEME);
  regNum.setOpaque(true);
  regNum.addEventHandler(this, "regNum_change");
  
  
  regLength = new GTextField(this, 185, 75, 30, 20, G4P.SCROLLBARS_NONE);
  regLength.setText("1");
  regLength.setLocalColorScheme(GCScheme.BLUE_SCHEME);
  regLength.setOpaque(true);
  regLength.addEventHandler(this, "regLength_change");
  
  regVal = new GTextField(this, 270, 75, 40, 20, G4P.SCROLLBARS_NONE);
  regVal.setText("0");
  regVal.setLocalColorScheme(GCScheme.BLUE_SCHEME);
  regVal.setOpaque(true);
  regVal.addEventHandler(this, "regVal_change");



  regIdLabel = new GLabel(this, 5, 78, 120, 14);
  regIdLabel.setTextAlign(GAlign.LEFT, GAlign.MIDDLE);
  regIdLabel.setText("ID");
  regIdLabel.setLocalColorScheme(GCScheme.BLUE_SCHEME);
  regIdLabel.setOpaque(false);

  regNumLabel = new GLabel(this, 65, 78, 120, 14);
  regNumLabel.setTextAlign(GAlign.LEFT, GAlign.MIDDLE);
  regNumLabel.setText("Reg #");
  regNumLabel.setLocalColorScheme(GCScheme.BLUE_SCHEME);
  regNumLabel.setOpaque(false);
  
  regLengthLabel = new GLabel(this, 145, 78, 120, 14);
  regLengthLabel.setTextAlign(GAlign.LEFT, GAlign.MIDDLE);
  regLengthLabel.setText("Length");
  regLengthLabel.setLocalColorScheme(GCScheme.BLUE_SCHEME);
  regLengthLabel.setOpaque(false);


  regValLabel = new GLabel(this, 215, 78, 120, 14);
  regValLabel.setTextAlign(GAlign.LEFT, GAlign.MIDDLE);
  regValLabel.setText("Value");
  regValLabel.setLocalColorScheme(GCScheme.BLUE_SCHEME);
  regValLabel.setOpaque(false);


  setRegisterButton = new GButton(this, 389, 75, 75, 20);
  setRegisterButton.setText("Set Register");
  setRegisterButton.addEventHandler(this, "setRegisterButton_click");
  setRegisterButton.setLocalColorScheme(GCScheme.CYAN_SCHEME);
  setRegisterButton.setVisible(false);

  getRegisterButton = new GButton(this, 310, 75, 75, 20);
  getRegisterButton.setText("Get Register");
  getRegisterButton.addEventHandler(this, "getRegisterButton_click");
  getRegisterButton.setLocalColorScheme(GCScheme.CYAN_SCHEME);

  


//400 470
  updateButton = new GButton(this, 5, 420, 100, 50);
  updateButton.setText("Update");
  updateButton.addEventHandler(this, "updateButton_click");
  updateButton.setLocalColorScheme(GCScheme.BLUE_SCHEME);
  updateButton.setFont(new Font("Dialog", Font.PLAIN, 20));  

 

  autoUpdateCheckbox = new GCheckbox(this, 105, 454, 100, 20);
  autoUpdateCheckbox.setOpaque(false);
  autoUpdateCheckbox.addEventHandler(this, "autoUpdateCheckbox_change");
  autoUpdateCheckbox.setText("Auto Update");
 
 
  
  //gripperLeftSlider = new GCustomSlider(this, 180, 100, 100, 200, "gripperL");
  gripperLeftSlider = new GCustomSlider(this, 180, 65, 75, 200, "gripperL");
  gripperLeftSlider.setLimits(256.0f, 512.0f, 0.0f);
  gripperLeftSlider.setShowDecor(false, true, false, false);
  //gripperLeftSlider.setShowLimits(true);
  gripperLeftSlider.setEasing(0.0f);
  gripperLeftSlider.setNumberFormat(G4P.INTEGER, 0);
  gripperLeftSlider.setLocalColorScheme(GCScheme.BLUE_SCHEME);
  gripperLeftSlider.setShowValue(false);
  gripperLeftSlider.addEventHandler(this, "gripperLeftSlider_change");
  gripperLeftSlider.setValue(256);
  gripperLeftSlider.setShowTicks(false);
  gripperLeftSlider.setRotation(PI/2,GControlMode.CENTER);
  //gripperLeftSlider.moveTo(180,100);
  
  
  //gripperRightSlider = new GCustomSlider(this, 258, 100, 100, 200, "gripperR");
  gripperRightSlider = new GCustomSlider(this, 180, 115, 75, 200, "gripperR");
  gripperRightSlider.setLimits(256.0f, 0.0f, 512.0f);
  gripperRightSlider.setShowDecor(false, true, false, false);
  gripperRightSlider.setShowDecor(false, true, false, false);
 // gripperRightSlider.setShowLimits(true);
  gripperRightSlider.setEasing(0.0f);
  gripperRightSlider.setNumberFormat(G4P.INTEGER, 0);
  gripperRightSlider.setLocalColorScheme(GCScheme.BLUE_SCHEME);
 // gripperRightSlider.setShowValue(true);
  gripperRightSlider.addEventHandler(this, "gripperRightSlider_change");
  gripperRightSlider.setValue(256);
  gripperRightSlider.setShowTicks(false);
  gripperRightSlider.setRotation(PI/2,GControlMode.CENTER);
  //gripperRightSlider.moveTo(258,100);
  

  poses = new CopyOnWriteArrayList<GPanel>();
  
  //poseData.add(blankPose);  
  poseData.add(defaultPose);
  
  //poses.add(new GPanel(this, panelsX, panelsYStart, 50, 18, "0"));
  //numPanels++;
  //poses.get(0).setCollapsible(true);
  //poses.get(0).setCollapsed(true);
  //poses.get(0).setLocalColorScheme(0);

  newPose = new GButton(this, 5, 25, 80, 30);
  newPose.setText("New Pose");
  newPose.addEventHandler(this, "newPose_click");
  
  
  playButton = new GButton(this, 5, 55, 80, 30);
  playButton.setText("Play Poses");
  playButton.addEventHandler(this, "playButton_click");
 

  stopButton = new GButton(this, 5, 85, 80, 30);
  stopButton.setText("Stop Poses");
  stopButton.addEventHandler(this, "stopButton_click");
  
  
  
 
  movePosesUp = new GButton(this, 5, 255, 80, 30);
  movePosesUp.setText("Scroll Up");
  movePosesUp.addEventHandler(this, "movePosesUp_click");
  
 
  movePosesDown = new GButton(this, 5, 280, 80, 30);
  movePosesDown.setText("Scroll Down");
  movePosesDown.addEventHandler(this, "movePosesDown_click");
  
  
  
  emergencyStopButton = new GButton(this, 5, 440, 150, 30);
  emergencyStopButton.setText("EMERGENCY STOP");
  emergencyStopButton.addEventHandler(this, "emergencyStopButton_click"); 
  emergencyStopButton.setLocalColorScheme(GCScheme.RED_SCHEME);
 
 
 
  workspaceToPose = new GButton(this, 5, 125, 80, 60);
  workspaceToPose.setText("Save Pose    -->");
  workspaceToPose.addEventHandler(this, "workspaceToPose_click");
  
  poseToWorkspace = new GButton(this, 5, 185, 80, 60);
  poseToWorkspace.setText(" Load Pose    <--");
  poseToWorkspace.addEventHandler(this, "poseToWorkspace_click");
  

  
  pauseTextField = new GTextField(this, 190, 320, 60, 20, G4P.SCROLLBARS_NONE);
  pauseTextField.setText("1000");
  pauseTextField.setLocalColorScheme(GCScheme.BLUE_SCHEME);
  pauseTextField.setOpaque(true);
  pauseTextField.addEventHandler(this, "pauseTextField_change");






  pauseLabel = new GLabel(this, 190, 340, 120, 14);
  pauseLabel.setTextAlign(GAlign.LEFT, GAlign.MIDDLE);
  pauseLabel.setText("Pause (Ms)");
  pauseLabel.setLocalColorScheme(GCScheme.BLUE_SCHEME);
  pauseLabel.setOpaque(false);


  savePosesButton = new GButton(this, 5, 365, 80, 30);
  savePosesButton.setText("Save to File");
  savePosesButton.addEventHandler(this, "savePosesButton_click"); 
 
  loadPosesButton = new GButton(this, 5, 400, 80, 30);
  loadPosesButton.setText("Load From File");
  loadPosesButton.addEventHandler(this, "loadPosesButton_click"); 
 
 
  
  
 


  sequencePanel.addControl(movePosesDown);
  sequencePanel.addControl(newPose);
  sequencePanel.addControl(movePosesUp);
  sequencePanel.addControl(poseToWorkspace);
  sequencePanel.addControl(workspaceToPose);
  //sequencePanel.addControl(analog1);
  sequencePanel.addControl(playButton);
  sequencePanel.addControl(stopButton);
  sequencePanel.addControl(digitalOutputFileCheckbox);
  
  
  
  
  //sequencePanel.addControl(poses.get(0));
  
  sequencePanel.addControl(savePosesButton);
  sequencePanel.addControl(loadPosesButton);
  sequencePanel.addControl(emergencyStopButton);
  
  // sequencePanel.addControl(pauseTextField);
  // sequencePanel.addControl(pauseLabel);
  
  controlPanel.addControl(pauseTextField);
  controlPanel.addControl(pauseLabel);
  

  
  
  
  controlPanel.addControl(xTextField);
  controlPanel.addControl(xSlider);
  controlPanel.addControl(yTextField);
  controlPanel.addControl(ySlider);
  controlPanel.addControl(yLabel);
  controlPanel.addControl(xLabel);
  controlPanel.addControl(zTextField);
  controlPanel.addControl(zSlider);
  controlPanel.addControl(zLabel);
  controlPanel.addControl(wristAngleTextField);
  controlPanel.addControl(wristAngleSlider);
  controlPanel.addControl(wristAngleLabel);
  controlPanel.addControl(wristRotateTextField);
  controlPanel.addControl(wristRotateSlider);
  controlPanel.addControl(wristRotateLabel);
  controlPanel.addControl(gripperTextField);
  controlPanel.addControl(gripperSlider);
  controlPanel.addControl(gripperLabel);
  controlPanel.addControl(deltaTextField);
  controlPanel.addControl(deltaSlider);
  controlPanel.addControl(deltaLabel);
  controlPanel.addControl(extendedTextField);
  controlPanel.addControl(extendedLabel);
  controlPanel.addControl(autoUpdateCheckbox);
  controlPanel.addControl(updateButton);
  controlPanel.addControl(baseKnob);
  controlPanel.addControl(shoulderKnob);
  controlPanel.addControl(elbowKnob);
  controlPanel.addControl(wristAngleKnob);
  controlPanel.addControl(wristRotateKnob);
  controlPanel.addControl(gripperLeftSlider);
  controlPanel.addControl(gripperRightSlider);
  //controlPanel.addControl(waitingButton);
  
  
  ioPanel.addControl(digitalsLabel);
  ioPanel.addControl(digitalCheckbox0);
  ioPanel.addControl(digitalCheckbox1);
  ioPanel.addControl(digitalCheckbox2);
  ioPanel.addControl(digitalCheckbox3);
  ioPanel.addControl(digitalCheckbox4);
  ioPanel.addControl(digitalCheckbox5);
  ioPanel.addControl(digitalCheckbox6);
  ioPanel.addControl(digitalCheckbox7);
  
  ioPanel.addControl(cameraCheckbox);
  ioPanel.addControl(cameraLabel);
  ioPanel.addControl(setRegisterCheckbox);
  
  
  ioPanel.addControl(analogTextLabel);
  ioPanel.addControl(analogCheckbox);
  ioPanel.addControl(analogLabel[0]);
  ioPanel.addControl(analogLabel[1]);
  ioPanel.addControl(analogLabel[2]);
  ioPanel.addControl(analogLabel[3]);
  ioPanel.addControl(analogLabel[4]);
  ioPanel.addControl(analogLabel[5]);
  ioPanel.addControl(analogLabel[6]);
  ioPanel.addControl(analogLabel[7]);
  
  
  ioPanel.addControl(regId);  
  ioPanel.addControl(regIdLabel);  
  ioPanel.addControl(regLength);  
  ioPanel.addControl(regLengthLabel);  
  ioPanel.addControl(regNum);  
  ioPanel.addControl(regNumLabel);  
  ioPanel.addControl(regVal);  
  ioPanel.addControl(regValLabel);
  ioPanel.addControl(setRegisterButton);  
  ioPanel.addControl(getRegisterButton);
  
  
    

  
  

  waitingButton = new GImageButton(this, 115, 408, 100, 30, new String[] { 
    "moving.jpg", "moving.jpg", "moving.jpg"
  } 
  );
  waitingButton.setAlpha(0);


//settings
  settingsPanel = new GPanel(this, 10, 280, 230, 230, "Settings Panel");
  settingsPanel.setText("Error Panel");
  settingsPanel.setLocalColorScheme(GCScheme.CYAN_SCHEME);
  settingsPanel.setOpaque(true);
  settingsPanel.setVisible(false);
  settingsPanel.addEventHandler(this, "settingsPanel_Click");
  //settingsPanel.setDraggable(false);
  settingsPanel.setCollapsible(false);


  fileDebugCheckbox = new GCheckbox(this, 5, 20, 200, 20);
  fileDebugCheckbox.setOpaque(false);
  fileDebugCheckbox.addEventHandler(this, "fileDebugCheckbox_change");
  fileDebugCheckbox.setText("Debug to File");

  settingsDismissButton = new GButton(this, 15, 120, 50, 20);
  settingsDismissButton.setText("Done");
  settingsDismissButton.addEventHandler(this, "settingsDismissButton_click");
  settingsDismissButton.setLocalColorScheme(GCScheme.RED_SCHEME);

  settingsPanel.addControl(settingsDismissButton);
  settingsPanel.addControl(fileDebugCheckbox);







  logoImg = loadImage("armLinkLogo.png");  // Load the image into the program  
  footerImg = loadImage("footer.png");  // Load the image into the program  




  //MOVE TO GUI
  
  
}



/******************************************************
 *  wristRotateTextFieldameters()
 *  This function will load the approriate position 
 *  defaults, minimums, and maximums into the GUI
 *  text fields and sliders, as well as the limit check
 *  logic. In addition, label elements will be changed
 *  to reflect the current controls. Finally, the current
 *  defaults will be written back to the internal variables
 *
 *
 *  Tasks to perform on end of program
 *  Arm
 *  1 - Pincher
 *  2 - Reactor
 *  3 - WidowX 
 *  4 - Snapper  
 *
 *  Mode
 *   1 - Cartesian
 *   2 - Cylindrical
 *   3 - backhoe
 *
 *  Wrist Orientaiton
 *    1- Straight
 *    2 - 90 degrees
 ******************************************************/ 

public void setPositionParameters()
{
  
  armParam0X = new int[][]{pincherNormalX,reactorNormalX,widowNormalX,widowNormalX, snapperNormalX};
  armParam0Y = new int[][]{pincherNormalY,reactorNormalY,widowNormalY,widowNormalY, snapperNormalY};
  armParam0Z = new int[][]{pincherNormalZ,reactorNormalZ,widowNormalZ,widowNormalZ, snapperNormalZ};
  armParam0WristAngle = new int[][]{pincherNormalWristAngle,reactorNormalWristAngle,widowNormalWristAngle, widowNormalWristAngle, snapperNormalWristAngle};
  
  armParam90X = new int[][]{pincher90X,reactor90X,widow90X, widow90X, snapper90X};
  armParam90Y = new int[][]{pincher90Y,reactor90Y,widow90Y,widow90Y, snapper90Y};
  armParam90Z = new int[][]{pincher90Z,reactor90Z,widow90Z,widow90Z, snapper90Z};
  armParam90WristAngle = new int[][]{pincher90WristAngle,reactor90WristAngle,widow90WristAngle,widow90WristAngle, snapper90WristAngle};
  
  armParamBase = new int[][]{pincherBase,reactorBase,widowBase,widowBase, snapperBase};
  armParamBHShoulder = new int[][]{pincherBHShoulder,reactorBHShoulder,widowBHShoulder,widowBHShoulder, snapperBHShoulder};
  armParamBHElbow = new int[][]{pincherBHElbow,reactorBHElbow,widowBHElbow,widowBHElbow, snapperBHElbow};
  armParamBHWristAngle = new int[][]{pincherBHWristAngle,reactorBHWristAngle,widowBHWristAngle,widowBHWristAngle, snapperBHWristAngle};
  armParamBHWristRot = new int[][]{pincherBHWristRot,reactorBHWristRot,widowBHWristRot, widowBHWristRot, snapperBHWristRot};
  
  armParam0WristRotate = new int[][]{pincherWristRotate,reactorWristRotate,widowWristRotate,widowWristRotate, snapperWristRotate};
  armParamGripper = new int[][]{pincherGripper,reactorGripper,widowGripper,widowGripper, snapperGripper};
  
  armParamWristAngle0Knob = new int[][]{pincherBHWristAngleNormalKnob,reactorWristAngleNormalKnob,widowBHWristAngleNormalKnob,widowBHWristAngleNormalKnob,snapperBHWristAngleNormalKnob};
  armParamWristAngle90Knob = new int[][]{pincherBHWristAngle90Knob,reactorWristAngle90Knob,widowBHWristAngle90Knob,widowBHWristAngle90Knob, snapperBHWristAngle90Knob};
  armParamWristAngleBHKnob = new int[][]{pincherWristAngleBHKnob,reactorWristAngleBHKnob,widowWristAngleBHKnob,widowWristAngleBHKnob, snapperWristAngleBHKnob};
  armParamWristRotKnob = new int[][]{pincherWristRotKnob,reactorWristRotKnob,widowWristRotKnob,widowWristRotKnob, snapperWristRotKnob};
  armParamBaseKnob = new int[][]{pincherBaseKnob,reactorBaseKnob,widowBaseKnob,widowBaseKnob, snapperBaseKnob};
  armParamShoulderKnob = new int[][]{pincherShoulderKnob,reactorShoulderKnob,widowShoulderKnob,widowShoulderKnob,snapperShoulderKnob};
  armParamElbowKnob = new int[][]{pincherElbowKnob,reactorElbowKnob,widowElbowKnob,widowElbowKnob,snapperElbowKnob}; 
  armParamElbowKnobRotation = new float[]{pincherElbowKnobRotation,reactorElbowKnobRotation,widowElbowKnobRotation,widowElbowKnobRotation,snapperElbowKnobRotation}; 
  
  



  //armParamDelta new int[][]{pincherElbowKnob,reactorElbowKnob,widowElbowKnob,widowElbowKnob,snapperElbowKnob}; 
             
            
  switch(currentMode)
  {
    //cartesian
    case 1:
    
      //hide/show appropriate GUI elements
      baseKnob.setVisible(false);
      shoulderKnob.setVisible(false);
      elbowKnob.setVisible(false);
      xSlider.setVisible(true);
      ySlider.setVisible(true);
      zSlider.setVisible(true);
      wristRotateKnob.setVisible(true);
      
      switch(currentOrientation)
      {
        //straight
        case 1:        
  
        xSlider.setLimits( armParam0X[currentArm-1][0], armParam0X[currentArm-1][1], armParam0X[currentArm-1][2]);    
        xTextField.setText(Integer.toString(armParam0X[currentArm-1][0]));
        xLabel.setText("X Coord");
        arrayCopy(armParam0X[currentArm-1], xParameters);
    
        ySlider.setLimits( armParam0Y[currentArm-1][0], armParam0Y[currentArm-1][1], armParam0Y[currentArm-1][2]) ; 
        yTextField.setText(Integer.toString(armParam0Y[currentArm-1][0]));
        yLabel.setText("Y Coord");
        arrayCopy(armParam0Y[currentArm-1], yParameters);
    
        zSlider.setLimits( armParam0Z[currentArm-1][0], armParam0Z[currentArm-1][1], armParam0Z[currentArm-1][2]) ;   
        zTextField.setText(Integer.toString(armParam0Z[currentArm-1][0]));
        zLabel.setText("Z Coord");
        arrayCopy(armParam0Z[currentArm-1], zParameters);
    
  
        wristAngleKnob.setTurnRange(armParamWristAngle0Knob[currentArm-1][0], armParamWristAngle0Knob[currentArm-1][1]); //set angle limits start/finish
        wristAngleKnob.setLimits(armParam0WristAngle[currentArm-1][0], armParam0WristAngle[currentArm-1][1], armParam0WristAngle[currentArm-1][2]);//set value limits
        wristAngleTextField.setText(Integer.toString(armParam0WristAngle[currentArm-1][0]));
        wristAngleLabel.setText("Wrist Angle");
        arrayCopy(armParam0WristAngle[currentArm-1], wristAngleParameters);
    
  
        wristRotateKnob.setTurnRange(armParamWristRotKnob[currentArm-1][0], armParamWristRotKnob[currentArm-1][1]); //set angle limits start/finish
        wristRotateKnob.setLimits(armParam0WristRotate[currentArm-1][0], armParam0WristRotate[currentArm-1][1], armParam0WristRotate[currentArm-1][2]);//set value limits
        wristRotateTextField.setText(Integer.toString(armParam0WristRotate[currentArm-1][0]));
        wristRotateLabel.setText("Wrist Rotate");
        arrayCopy(armParam0WristRotate[currentArm-1], wristRotateParameters);
  
    
        gripperSlider.setLimits( armParamGripper[currentArm-1][0], armParamGripper[currentArm-1][1], armParamGripper[currentArm-1][2]);    
        gripperTextField.setText(Integer.toString(armParamGripper[currentArm-1][0]));
        gripperLabel.setText("Gripper");
        arrayCopy(armParamGripper[currentArm-1], gripperParameters);
        
           gripperLeftSlider.setLimits( armParamGripper[currentArm-1][0], armParamGripper[currentArm-1][2], armParamGripper[currentArm-1][1]);    
        gripperRightSlider.setLimits( armParamGripper[currentArm-1][0], armParamGripper[currentArm-1][1], armParamGripper[currentArm-1][2]); 
        arrayCopy(armParamGripper[currentArm-1], gripperParameters);
        
        
        
        break;
  
        //90 degrees
        case 2:
  
  
   
  
        xSlider.setLimits( armParam90X[currentArm-1][0], armParam90X[currentArm-1][1], armParam90X[currentArm-1][2]);    
        xTextField.setText(Integer.toString(armParam90X[currentArm-1][0]));
        xLabel.setText("X Coord");
        arrayCopy(armParam90X[currentArm-1], xParameters);
    
        ySlider.setLimits( armParam90Y[currentArm-1][0], armParam90Y[currentArm-1][1], armParam90Y[currentArm-1][2]) ; 
        yTextField.setText(Integer.toString(armParam90Y[currentArm-1][0]));
        yLabel.setText("Y Coord");
        arrayCopy(armParam90Y[currentArm-1], yParameters);
    
        zSlider.setLimits( armParam90Z[currentArm-1][0], armParam90Z[currentArm-1][1], armParam90Z[currentArm-1][2]) ;   
        zTextField.setText(Integer.toString(armParam90Z[currentArm-1][0]));
        zLabel.setText("Z Coord");
        arrayCopy(armParam90Z[currentArm-1], zParameters);
    
        wristAngleKnob.setTurnRange(armParamWristAngle90Knob[currentArm-1][0], armParamWristAngle90Knob[currentArm-1][1]); //set angle limits start/finish
        wristAngleKnob.setLimits(armParam90WristAngle[currentArm-1][0], armParam90WristAngle[currentArm-1][1], armParam90WristAngle[currentArm-1][2]);//set value limits
        wristAngleTextField.setText(Integer.toString(armParam90WristAngle[currentArm-1][0]));
        wristAngleLabel.setText("Wrist Angle");
        arrayCopy(armParam90WristAngle[currentArm-1], wristAngleParameters);
   
  
        wristRotateKnob.setTurnRange(armParamWristRotKnob[currentArm-1][0], armParamWristRotKnob[currentArm-1][1]); //set angle limits start/finish
        wristRotateKnob.setLimits(armParam0WristRotate[currentArm-1][0], armParam0WristRotate[currentArm-1][1], armParam0WristRotate[currentArm-1][2]);//set value limits
        wristRotateTextField.setText(Integer.toString(armParam0WristRotate[currentArm-1][0]));
        wristRotateLabel.setText("Wrist Rotate");
        arrayCopy(armParam0WristRotate[currentArm-1], wristRotateParameters);
       
    
        gripperSlider.setLimits( armParamGripper[currentArm-1][0], armParamGripper[currentArm-1][1], armParamGripper[currentArm-1][2]);    
        gripperTextField.setText(Integer.toString(armParamGripper[currentArm-1][0]));
        gripperLabel.setText("Gripper");
        arrayCopy(armParamGripper[currentArm-1], gripperParameters);
       gripperLeftSlider.setLimits( armParamGripper[currentArm-1][0], armParamGripper[currentArm-1][2], armParamGripper[currentArm-1][1]);    
        gripperRightSlider.setLimits( armParamGripper[currentArm-1][0], armParamGripper[currentArm-1][1], armParamGripper[currentArm-1][2]); 
        arrayCopy(armParamGripper[currentArm-1], gripperParameters);
        
        break;
      }
    break;
    
      //cylindrical
    case 2:
  
      //hide/show appropriate GUI elements
      baseKnob.setVisible(true);
      shoulderKnob.setVisible(false);
      elbowKnob.setVisible(false);
      xSlider.setVisible(false);
      ySlider.setVisible(true);
      zSlider.setVisible(true);
      wristRotateKnob.setVisible(true);
      
      switch(currentOrientation)
      {
  
        //straight
        case 1: 
  
  
        wristAngleKnob.setTurnRange(armParamWristAngle0Knob[currentArm-1][0], armParamWristAngle0Knob[currentArm-1][1]); //set angle limits start/finish
        wristAngleKnob.setLimits(armParam0WristAngle[currentArm-1][0], armParam0WristAngle[currentArm-1][1], armParam0WristAngle[currentArm-1][2]);//set value limits
              
  
        baseKnob.setTurnRange(armParamBaseKnob[currentArm-1][0], armParamBaseKnob[currentArm-1][1]); //set angle limits start/finish
        baseKnob.setLimits(armParamBase[currentArm-1][0], armParamBase[currentArm-1][1], armParamBase[currentArm-1][2]);//set value limits
       
  
       // baseKnob.setRotation(HALF_PI);  
        //xSlider.setLimits( armParamBase[currentArm-1][0], armParamBase[currentArm-1][1], armParamBase[currentArm-1][2]);    
        xTextField.setText(Integer.toString(armParamBase[currentArm-1][0]));
        xLabel.setText("Base");
        arrayCopy(armParamBase[currentArm-1], xParameters);
    
    
    
      
        xSlider.setLimits( armParamBase[currentArm-1][0], armParamBase[currentArm-1][1], armParamBase[currentArm-1][2]);    
        
        ySlider.setLimits( armParam0Y[currentArm-1][0], armParam0Y[currentArm-1][1], armParam0Y[currentArm-1][2]) ; 
        yTextField.setText(Integer.toString(armParam0Y[currentArm-1][0]));
        yLabel.setText("Y Coord");
        arrayCopy(armParam0Y[currentArm-1], yParameters);
    
    
        zSlider.setLimits( armParam0Z[currentArm-1][0], armParam0Z[currentArm-1][1], armParam0Z[currentArm-1][2]) ;   
        zTextField.setText(Integer.toString(armParam0Z[currentArm-1][0]));
        zLabel.setText("Z Coord");
        arrayCopy(armParam0Z[currentArm-1], zParameters);
    
    
  
        wristAngleKnob.setTurnRange(armParamWristAngle0Knob[currentArm-1][0], armParamWristAngle0Knob[currentArm-1][1]); //set angle limits start/finish
        wristAngleKnob.setLimits(armParam0WristAngle[currentArm-1][0], armParam0WristAngle[currentArm-1][1], armParam0WristAngle[currentArm-1][2]);//set value limits
        wristAngleTextField.setText(Integer.toString(armParam0WristAngle[currentArm-1][0]));
        wristAngleLabel.setText("Wrist Angle");
        arrayCopy(armParam0WristAngle[currentArm-1], wristAngleParameters);
  
        wristRotateKnob.setTurnRange(armParamWristRotKnob[currentArm-1][0], armParamWristRotKnob[currentArm-1][1]); //set angle limits start/finish
        wristRotateKnob.setLimits(armParam0WristRotate[currentArm-1][0], armParam0WristRotate[currentArm-1][1], armParam0WristRotate[currentArm-1][2]);//set value limits
        wristRotateTextField.setText(Integer.toString(armParam0WristRotate[currentArm-1][0]));
        wristRotateLabel.setText("Wrist Rotate");
        arrayCopy(armParam0WristRotate[currentArm-1], wristRotateParameters);
  
    
        gripperSlider.setLimits( armParamGripper[currentArm-1][0], armParamGripper[currentArm-1][1], armParamGripper[currentArm-1][2]);    
        gripperTextField.setText(Integer.toString(armParamGripper[currentArm-1][0]));
        gripperLabel.setText("Gripper");
        arrayCopy(armParamGripper[currentArm-1], gripperParameters);
        gripperLeftSlider.setLimits( armParamGripper[currentArm-1][0], armParamGripper[currentArm-1][2], armParamGripper[currentArm-1][1]);    
        gripperRightSlider.setLimits( armParamGripper[currentArm-1][0], armParamGripper[currentArm-1][1], armParamGripper[currentArm-1][2]); 
        arrayCopy(armParamGripper[currentArm-1], gripperParameters);
        
        break;
    
        //90 degrees
        case 2:  
  
        wristAngleKnob.setTurnRange(armParamWristAngle90Knob[currentArm-1][0], armParamWristAngle90Knob[currentArm-1][1]); //set angle limits start/finish
        wristAngleKnob.setLimits(armParam90WristAngle[currentArm-1][0], armParam90WristAngle[currentArm-1][1], armParam90WristAngle[currentArm-1][2]);//set value limits
              
        xTextField.setText(Integer.toString(armParamBase[currentArm-1][0]));
        xLabel.setText("Base");
        arrayCopy(armParamBase[currentArm-1], xParameters);
    
        ySlider.setLimits( armParam90Y[currentArm-1][0], armParam90Y[currentArm-1][1], armParam90Y[currentArm-1][2]) ; 
        yTextField.setText(Integer.toString(armParam90Y
        [currentArm-1][0]));
        yLabel.setText("Y Coord");
        arrayCopy(armParam90Y[currentArm-1], yParameters);
    
    
        zSlider.setLimits( armParam90Z[currentArm-1][0], armParam90Z[currentArm-1][1], armParam90Z[currentArm-1][2]) ;   
        zTextField.setText(Integer.toString(armParam90Z[currentArm-1][0]));
        zLabel.setText("Z Coord");
        arrayCopy(armParam90Z[currentArm-1], zParameters);
    
    
    
    
      
        xSlider.setLimits( armParamBase[currentArm-1][0], armParamBase[currentArm-1][1], armParamBase[currentArm-1][2]);    
        xTextField.setText(Integer.toString(armParamBase[currentArm-1][0]));
        xLabel.setText("X Coord");
        arrayCopy(armParamBase[currentArm-1], xParameters);
    
    
        ySlider.setLimits( armParam90Y[currentArm-1][0], armParam90Y[currentArm-1][1], armParam90Y[currentArm-1][2]) ; 
        yTextField.setText(Integer.toString(armParam90Y[currentArm-1][0]));
        yLabel.setText("Y Coord");
        arrayCopy(armParam90Y[currentArm-1], yParameters);
    
        zSlider.setLimits( armParam90Z[currentArm-1][0], armParam90Z[currentArm-1][1], armParam90Z[currentArm-1][2]) ;   
        zTextField.setText(Integer.toString(armParam90Z[currentArm-1][0]));
        zLabel.setText("Z Coord");
        arrayCopy(armParam90Z[currentArm-1], zParameters);
    
        wristAngleKnob.setTurnRange(armParamWristAngle90Knob[currentArm-1][0], armParamWristAngle90Knob[currentArm-1][1]); //set angle limits start/finish
        wristAngleKnob.setLimits(armParam90WristAngle[currentArm-1][0], armParam90WristAngle[currentArm-1][1], armParam90WristAngle[currentArm-1][2]);//set value limits
        wristAngleTextField.setText(Integer.toString(armParam90WristAngle[currentArm-1][0]));
        wristAngleLabel.setText("Wrist Angle");
        arrayCopy(armParam90WristAngle[currentArm-1], wristAngleParameters);
   
  
        wristRotateKnob.setTurnRange(armParamWristRotKnob[currentArm-1][0], armParamWristRotKnob[currentArm-1][1]); //set angle limits start/finish
        wristRotateKnob.setLimits(armParam0WristRotate[currentArm-1][0], armParam0WristRotate[currentArm-1][1], armParam0WristRotate[currentArm-1][2]);//set value limits
        wristRotateTextField.setText(Integer.toString(armParam0WristRotate[currentArm-1][0]));
        wristRotateLabel.setText("Wrist Rotate");
        arrayCopy(armParam0WristRotate[currentArm-1], wristRotateParameters);
       
    
        gripperSlider.setLimits( armParamGripper[currentArm-1][0], armParamGripper[currentArm-1][1], armParamGripper[currentArm-1][2]);    
        gripperTextField.setText(Integer.toString(armParamGripper[currentArm-1][0]));
        gripperLabel.setText("Gripper");
        arrayCopy(armParamGripper[currentArm-1], gripperParameters);
       gripperLeftSlider.setLimits( armParamGripper[currentArm-1][0], armParamGripper[currentArm-1][2], armParamGripper[currentArm-1][1]);    
        gripperRightSlider.setLimits( armParamGripper[currentArm-1][0], armParamGripper[currentArm-1][1], armParamGripper[currentArm-1][2]); 
        arrayCopy(armParamGripper[currentArm-1], gripperParameters);
        
    
        break;
      }
    
      break;
      
      //backhoe
      case 3: 
            
        baseKnob.setVisible(true);
        shoulderKnob.setVisible(true);
        elbowKnob.setVisible(true);
        xSlider.setVisible(false);
        ySlider.setVisible(false);
        zSlider.setVisible(false);
        wristRotateKnob.setVisible(true);
    
  
  
  
        baseKnob.setTurnRange(armParamBaseKnob[currentArm-1][0], armParamBaseKnob[currentArm-1][1]); //set angle limits start/finish
        baseKnob.setLimits(armParamBase[currentArm-1][0], armParamBase[currentArm-1][1], armParamBase[currentArm-1][2]);//set value limits
        xTextField.setText(Integer.toString(armParamBase[currentArm-1][0]));
        xLabel.setText("Base");
        arrayCopy(armParamBase[currentArm-1], xParameters);
    
  
        shoulderKnob.setTurnRange(armParamShoulderKnob[currentArm-1][0], armParamShoulderKnob[currentArm-1][1]); //set angle limits start/finish
        shoulderKnob.setLimits(armParamBHShoulder[currentArm-1][0], armParamBHShoulder[currentArm-1][1], armParamBHShoulder[currentArm-1][2]);//set value limits
        yTextField.setText(Integer.toString(armParamBHShoulder[currentArm-1][0]));
        yLabel.setText("Shoulder");
        arrayCopy(armParamBHShoulder[currentArm-1], yParameters);
    
  
        elbowKnob.setTurnRange(armParamElbowKnob[currentArm-1][0], armParamElbowKnob[currentArm-1][1]); //set angle limits start/finish
        elbowKnob.setLimits(armParamBHElbow[currentArm-1][0], armParamBHElbow[currentArm-1][1], armParamBHElbow[currentArm-1][2]);//set value limits
        elbowKnob.setRotation(armParamElbowKnobRotation[currentArm-1],GControlMode.CENTER);

      
      
        zTextField.setText(Integer.toString(armParamBHElbow[currentArm-1][0]));
        zLabel.setText("Elbow");
        arrayCopy(armParamBHElbow[currentArm-1], zParameters);
    
        wristAngleSlider.setLimits(armParamBHWristAngle[currentArm-1][0], armParamBHWristAngle[currentArm-1][1], armParamBHWristAngle[currentArm-1][2]); 
        wristAngleTextField.setText(Integer.toString(armParamBHWristAngle[currentArm-1][0]));
        wristAngleLabel.setText("Wrist Angle");
        arrayCopy(armParamBHWristAngle[currentArm-1], wristAngleParameters);
    
        wristRotateTextField.setText(Integer.toString(armParamBHWristRot[currentArm-1][0]));
        wristRotateLabel.setText("Wrist Rotate");
        arrayCopy(armParamBHWristRot[currentArm-1], wristRotateParameters);
  
    
        gripperSlider.setLimits( armParamGripper[currentArm-1][0], armParamGripper[currentArm-1][1], armParamGripper[currentArm-1][2]);    
        gripperTextField.setText(Integer.toString(armParamGripper[currentArm-1][0]));
        gripperLabel.setText("Gripper");
        arrayCopy(armParamGripper[currentArm-1], gripperParameters);
    
  
  
        gripperLeftSlider.setLimits( armParamGripper[currentArm-1][0], armParamGripper[currentArm-1][2], armParamGripper[currentArm-1][1]);    
        gripperRightSlider.setLimits( armParamGripper[currentArm-1][0], armParamGripper[currentArm-1][1], armParamGripper[currentArm-1][2]); 
        arrayCopy(armParamGripper[currentArm-1], gripperParameters);
        
          
        wristAngleKnob.setTurnRange(reactorWristAngleBHKnob[0], reactorWristAngleBHKnob[1]); //set angle limits start/finish
        wristAngleKnob.setLimits(armParamBHWristAngle[currentArm-1][0], armParamBHWristAngle[currentArm-1][1], armParamBHWristAngle[currentArm-1][2]);//set value limits
        arrayCopy(armParamBHWristAngle[currentArm-1], wristAngleParameters);
        
                  
        wristRotateKnob.setTurnRange(reactorWristRotKnob[0], reactorWristRotKnob[1]); //set angle limits start/finish
        wristRotateKnob.setLimits(armParamBHWristRot[currentArm-1][0], armParamBHWristRot[currentArm-1][1], armParamBHWristRot[currentArm-1][2]);//set value limits
        
        
        wristRotateTextField.setText(Integer.toString(armParamBHWristRot[currentArm-1][0]));
        wristRotateLabel.setText("Wrist Rotate");
        arrayCopy(armParamBHWristRot[currentArm-1], wristRotateParameters);
        
        wristRotateSlider.setVisible(false);
        wristRotateTextField.setVisible(true);
        wristRotateLabel.setVisible(true);
        
        
        gripperSlider.setLimits( armParamGripper[currentArm-1][0], armParamGripper[currentArm-1][1], armParamGripper[currentArm-1][2]);    
        gripperTextField.setText(Integer.toString(armParamGripper[currentArm-1][0]));
        gripperLabel.setText("Gripper");
        arrayCopy(armParamGripper[currentArm-1], gripperParameters);
        
        
        
        
          xSlider.setLimits( armParamBase[currentArm-1][0], armParamBase[currentArm-1][1], armParamBase[currentArm-1][2]);    
          ySlider.setLimits( armParamBHShoulder[currentArm-1][0], armParamBHShoulder[currentArm-1][1], armParamBHShoulder[currentArm-1][2]);    
          zSlider.setLimits( armParamBHElbow[currentArm-1][0], armParamBHElbow[currentArm-1][1], armParamBHElbow[currentArm-1][2]);    

//        xSlider.setLimits( armParamBase[currentArm-1][0], armParamBase[currentArm-1][1], armParamBase[currentArm-1][2]);    
//        
//    
//        ySlider.setLimits( armParamShoulder[currentArm-1][0], armParamShoulderKnob[currentArm-1][1], armParamShoulderKnob[currentArm-1][2]) ; 
//        
//    
//        zSlider.setLimits( armParamElbowKnob[currentArm-1][0], armParamElbowKnob[currentArm-1][1], armParamElbowKnob[currentArm-1][2]) ;  
//       

    
    
    
    
        break;
      }
  
    
    
    
  //show or hide wrist rotate -if all the WR parameters are 0 then hide wrist angle 
  if(armParam0WristRotate[currentArm-1][0] == 0 && armParam0WristRotate[currentArm-1][1] == 0 && armParam0WristRotate[currentArm-1][2] == 0)
  {
    wristRotateTextField.setVisible(false);
    wristRotateKnob.setVisible(false);
    wristRotateLabel.setVisible(false);
  }
  else
  {
    
    wristRotateTextField.setVisible(true);
    wristRotateKnob.setVisible(true);
    wristRotateLabel.setVisible(true);
    
  }
  
  //reset deltas

  deltaTextField.setText("125");
  deltaTextField.setLocalColorScheme(GCScheme.BLUE_SCHEME);
  deltaTextField.setOpaque(true);
  deltaTextField.addEventHandler(this, "deltaTextField_change");



  deltaSlider.setShowValue(true);
  deltaSlider.setShowLimits(true);
  deltaSlider.setLimits(125.0f, 0.0f, 255.0f);
  deltaSlider.setEasing(0.0f);
  deltaSlider.setNumberFormat(G4P.INTEGER, 0);
  deltaSlider.setLocalColorScheme(GCScheme.BLUE_SCHEME);
  deltaSlider.setOpaque(false);
  deltaSlider.addEventHandler(this, "deltaSlider_change");



  deltaLabel.setTextAlign(GAlign.LEFT, GAlign.MIDDLE);
  deltaLabel.setText("Delta");
  deltaLabel.setLocalColorScheme(GCScheme.BLUE_SCHEME);
  deltaLabel.setOpaque(false);
  
            
    
    //hide and change for the snapper arm
  if(currentArm == 5)
  {
//    modePanel.setVisible(false);
    wristPanel.setVisible(false);
    emergencyStopButton.setVisible(true);

    getRegisterButton.setVisible(false);
    setRegisterButton.setVisible(false);
   regIdLabel.setVisible(false);
   regLengthLabel.setVisible(false);
   regNumLabel.setVisible(false);
   regValLabel.setVisible(false);
   regId.setVisible(false);
   regLength.setVisible(false);
   regNum.setVisible(false);
   regVal.setVisible(false);
   setRegisterCheckbox.setVisible(false);
   


  digitalCheckbox1.setText("2");
  digitalCheckbox2.setText("4");
  digitalCheckbox3.setText("7");
  digitalCheckbox4.setText("8");
  digitalCheckbox5.setText("11");
  digitalCheckbox6.setText("12");
  digitalCheckbox7.setText("13");
  
//  digitalCheckbox5.moveTo(154,35);
//  digitalCheckbox6.moveTo(192,35);
//  digitalCheckbox7.moveTo(230,35);
  }
  
  else
  {
    modePanel.setVisible(true);
    wristPanel.setVisible(true);
    emergencyStopButton.setVisible(true);

    getRegisterButton.setVisible(true);
    //setRegisterButton.setVisible(true);
   regIdLabel.setVisible(true);
   regLengthLabel.setVisible(true);
   regNumLabel.setVisible(true);
   regValLabel.setVisible(true);
   regId.setVisible(true);
   regLength.setVisible(true);
   regNum.setVisible(true);
   regVal.setVisible(true);
   setRegisterCheckbox.setVisible(true);
   
  

  digitalCheckbox1.setText("1");
  digitalCheckbox2.setText("2");
  digitalCheckbox3.setText("3");
  digitalCheckbox4.setText("4");
  digitalCheckbox5.setText("5");
  digitalCheckbox6.setText("6");
  digitalCheckbox7.setText("7");
  
//  digitalCheckbox5.moveTo(144,35);
//  digitalCheckbox6.moveTo(172,53);
//  digitalCheckbox7.moveTo(200,35);
//    
  }
  
  


  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
    //write defualt parameters back to internal values
    xCurrent = xParameters[0]; 
    yCurrent = yParameters[0]; 
    zCurrent = zParameters[0]; 
    wristAngleCurrent = wristAngleParameters[0];
    wristRotateCurrent = wristRotateParameters[0]; 
    gripperCurrent = gripperParameters[0]; 
    deltaCurrent = deltaParameters[0]; 
    extendedTextField.setText("0");
    updateOffsetCoordinates();

println(xCurrent);
println(xParameters[0]);
println(yCurrent);
println(zCurrent);
println(wristAngleCurrent);
println(wristRotateCurrent);
println(gripperCurrent);
}//end set postiion parameters




// G4P code for message dialogs
public void emergencyStopMessageDialog() {
  String message = "Your Arms's servos should now be disabled. <br />If your arm has been damaged or cannot be moved, unplug power immediately.<br /> Setting a new arm move, position, or disconnecting the program will re-active your arm is power is plugged in.";
  String title = "Emergency Stop";
  //snapper vs everything else
  if(currentArm == 5)
  {
      message= "Your Arms's servos should now be disabled. <br />If your arm has been damaged or cannot be moved, unplug power immediately.<br /> You will need to disconnect the program and reset your arm before continuing";
  
  }
  G4P.showMessage(this, message, title, G4P.WARNING);
  
    
}



public void setRegisterMessageDialog() 
{
  String message = "";
  String title = "Set Register";
  G4P.showMessage(this, message, title, G4P.WARNING);
}

public void genericMessageDialog(String title, String message, int mtype) {
  //// Determine message type
  //int mtype;
  //switch(md_mtype) {
  //default:
  //case 0: 
  //  mtype = G4P.PLAIN; 
  //  break;
  //case 1: 
  //  mtype = G4P.ERROR; 
  //  break;
  //case 2: 
  //  mtype = G4P.INFO; 
  //  break;
  //case 3: 
  //  mtype = G4P.WARNING; 
  //  break;
  //case 4: 
  //  mtype = G4P.QUERY; 
  //  break;
  //}
 // String message = "Your Arms's servos should now be disabled. If your arm has been damaged or cannot be moved, unplug power immediately. Setting a new arm move, position, or disconnecting the program will re-active your arm is power is plugged in.";
  //String title = "Emergency Stop";
  G4P.showMessage(this, message, title, mtype);
}
 /***********************************************************************************
 *  }--\     InterbotiX     /--{
 *      |    Arm Link      |
 *   __/                    \__
 *  |__|                    |__|
 *
 *  importParse.pde
 *  
 *  Test importing  functionality 
 *
********************************************************************************/

BufferedReader reader;
String line;
 

 
public void readArmFile(File selection)
{
  noLoop();
  String[] txtFile;

  if (selection == null) 
  {

    loop();
 //  displayError("No File Selected.","");
    genericMessageDialog("File Error", "No File Selected.", G4P.WARNING);


    return;
  }



  String filepath = selection.getAbsolutePath();
  println("User selected " + filepath);
  






        try 
      {
        txtFile = loadStrings(filepath);
      } 
      catch (Exception e) 
      {
        txtFile = null;
        loop();
        displayError("","");
        
         genericMessageDialog("File Error", "Problem With File.", G4P.WARNING);
    
        return;
      }


// load file here



 

  String armNumberString;
  int armNumberInt;
  String sequenceNumberString;
  int sequenceNumberInt;
  String armModeString;
  int armModeInt;
  String armOrientationString;
  int armOrientationInt;
  String armDIOString;
  int armDIOInt;
  String singleDIOString;
  int singleDIOInt = 0;
  
  String tempLine ;
  String[] splitPose = {"","","","","","","",""};
  int[] tempPoseData = {0, 0, 0, 0,0 ,0 ,0 ,0,0};
  int[] tempPoseData2 = {0, 0, 0, 0,0 ,0 ,0 ,0,0};
  int[] tempPoseData5=  {0, 0, 0, 0,0 ,0 ,0 ,0,0};




    if(txtFile[0].length() >= 6)
    {
      armNumberString = txtFile[0].substring(6, txtFile[0].length());
      armNumberInt = Integer.parseInt(armNumberString);

    }
    else  
    {

      loop();
      //displayError("Problem With File.","");
      
         genericMessageDialog("File Error", "Problem With File.", G4P.WARNING);
      return;
      
    }
    //println(armNumberInt);

    //check if this file works for the arm currently connected
    if(armNumberInt != currentArm)
    {
      loop();
      printlnDebug("Wrong Arm"); 
      //displayError("Incorrect File - File for the wrong Arm or the wrong type of file","");
      
         genericMessageDialog("File Error", "Incorrect File - File for the wrong Arm or the wrong type of file", G4P.WARNING);
         
      return;
    }


   clearPoses();


    sequenceNumberString = txtFile[1].substring(11, txtFile[1].length());
    sequenceNumberInt = Integer.parseInt(sequenceNumberString);



    armModeString = txtFile[2].substring(7, txtFile[2].length());
    armModeInt = Integer.parseInt(armModeString);
   
   

    if(armModeInt == 1)
    {

      setCartesian();
    }
    else if(armModeInt == 2)
    {
      setCylindrical();
    }
    else if(armModeInt == 3)
    {
      setBackhoe();
    }


   


    armOrientationString = txtFile[3].substring(14, txtFile[3].length());
    armOrientationInt = Integer.parseInt(armOrientationString);
    //println(armOrientationString);

    armDIOString = txtFile[4].substring(6, txtFile[4].length());
    armDIOInt = Integer.parseInt(armDIOString);
println(armDIOString);

    if (armOrientationInt == 1)
    {
      setOrientStraight();

    }
    else if(armOrientationInt == 2)
    {
      setOrient90();
    }


    //read the posotion data from the file.
    for(int j = 0; j < sequenceNumberInt ;j++)
    {

       tempLine = "";
      if(armDIOInt == 0)
      {
      tempLine = txtFile[23+(j*6)].replace("    IKSequencingControl(", "");
      }
      else if(armDIOInt == 1)
      {
      tempLine = txtFile[38+(j*14)].replace("    IKSequencingControl(", "");
      singleDIOString = txtFile[30+(j*14)].replace("    //DIO", "");
      
      
      
      
      
       println("DIOd" + singleDIOString);
        singleDIOInt = Integer.parseInt(singleDIOString);
    
      }
      
      tempLine = tempLine.replace(", playState);", "");


      splitPose = tempLine.split(",");
      
      


     for(int i = 0;i < splitPose.length;i++)
      {





        splitPose[i] = splitPose[i].replaceAll("\\s","") ;
        tempPoseData[i] = Integer.parseInt(splitPose[i]);
        println( tempPoseData[i]); 
      }


        println(" "); 
   switch(armModeInt)
    {
       case 1:        
         //x, wrist angle, and wrist rotate must be offset, all others are normal
         tempPoseData2[0] = tempPoseData[0] ;
         tempPoseData2[1] = tempPoseData[1];
         tempPoseData2[2] = tempPoseData[2];
         tempPoseData2[3] = tempPoseData[3];
         tempPoseData2[4] = tempPoseData[4];
         tempPoseData2[5] = tempPoseData[5];
         tempPoseData2[6] = tempPoseData[6]/16;
         tempPoseData2[7] = singleDIOInt;
         tempPoseData2[8] = tempPoseData[7];


        print("x" ); 
        println( tempPoseData2[0]); 
         break;
        
       case 2:
       
         //wrist angle, and wrist rotate must be offset, all others are normal


         tempPoseData2[0] = tempPoseData[0];




         tempPoseData2[1] = tempPoseData[1];
         tempPoseData2[2] = tempPoseData[2];
         tempPoseData2[3] = tempPoseData[3];
         tempPoseData2[4] = tempPoseData[4];
         tempPoseData2[5] = tempPoseData[5];
         tempPoseData2[6] = tempPoseData[6]/16;
         tempPoseData2[7] = singleDIOInt;   
         tempPoseData2[8] = tempPoseData[7];      



         break;
        
       case 3:
       
         //no offsets needed
         tempPoseData2[0] = tempPoseData[0];
         tempPoseData2[1] = tempPoseData[1];
         tempPoseData2[2] = tempPoseData[2];
         tempPoseData2[3] = tempPoseData[3];
         tempPoseData2[4] = tempPoseData[4];
         tempPoseData2[5] = tempPoseData[5];
         tempPoseData2[6] = tempPoseData[6]/16;
         tempPoseData2[7] = singleDIOInt;
         tempPoseData2[8] = tempPoseData[7];
        break; 
    }  
    
     //pauseTextField.setText(tempPoseData[7] + "");




          addNewPose(tempPoseData2);    
    
    

    
  
      printDebug("Added Pose ");
      printlnDebug(j + " ");



    }



  // for(int i = 0; i < poses.size(); i++)
  // {
  
  //   sequencePanel.addControl(poses.get(i));
  // }
  
   loop();
    return;

}
  public void settings() {  size(475, 733, JAVA2D); }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "ArmLink" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
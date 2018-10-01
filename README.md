# BLE Logger Formatter

Process logger files and convert analog read values to formatted values. Loggers are BluFeather PCB running Arduino code and built by GQC. Currently, program only calculates temperature values from analog read.

## Getting Started

Each board can have up to 6 sensors attached, up to:

* (2) 0 to 5v sensors
* (2) Thermistors
* (2) 4 to 20 mA sensors
* (1) Platinum RTD 

Data is recorded at 14 bits of accuracy. Sensor data is downloaded from Microsoft's IoT cloud (in separate application) and stored in CSV format. Sensor data stores raw analog read of pin.

Temperature lookup table `C.RVT` is provided by [Pace](https://www.pace-sci.com/), the thermistor manufacturer. The lookup table is Ohms -> degrees Celsius. Linear Interpolation is used to determine the temperature given Ohms.

### Prerequisites

* Gson 2.6.2
* Apache POI 4.0
* Apache Commons Compress 1.18

## Running

Program can be run in 3 different ways: calcVoltage, convertLogs, or compareTemps

* calcVoltage - use the lookup table and known values to back-calculate sensor voltage at read time
* convertLogs - convert analog read logs to formatted values given configuration file settings for loggers
* compareTemps - performs same conversion as convertLogs but also asks for known comparison temperature log files to match up with logger and calculate percent accuracy


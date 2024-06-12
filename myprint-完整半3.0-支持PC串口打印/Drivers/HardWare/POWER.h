#ifndef  __POWER_H
#define  __POWER_H
#include "stm32f1xx_hal.h"

#define AVERAGE_CNT 10               //平均数
uint16_t POWER_GET_ADC(void);
float POWER_GET_Value(void);
uint8_t POWER_GET_Capacity(void);
#endif

